package org.sharedhealth.mci.web.controller;


import ca.uhn.fhir.model.dstu2.resource.Patient;
import ca.uhn.fhir.parser.DataFormatException;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sharedhealth.mci.web.model.MCIResponse;
import org.sharedhealth.mci.web.security.UserInfo;
import org.sharedhealth.mci.web.service.PatientService;
import org.sharedhealth.mci.web.util.FhirContextHelper;
import spark.Request;
import spark.Response;

import java.io.IOException;
import java.nio.file.AccessDeniedException;

public class PatientController {
    private final static Logger logger = LogManager.getLogger(PatientController.class);
    private final PatientService patientService;

    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    public String createPatient(Request request, Response response) throws IOException {
        logger.debug("Create patient request");

        Patient patient;
        try {
            patient = (Patient) FhirContextHelper.parseResource(request.body());
        } catch (DataFormatException e) {
            logger.error("Can not parse", e);
            MCIResponse mciResponse = new MCIResponse(HttpStatus.SC_UNPROCESSABLE_ENTITY);
            mciResponse.setMessage(e.getMessage());
            response.status(mciResponse.getHttpStatus());
            return mciResponse.toString();
        }

        MCIResponse mciResponse = patientService.createPatient(patient);
        response.status(mciResponse.getHttpStatus());
        return mciResponse.toString();
    }

    public String getPatient(String hidParam, Request request, Response response) throws AccessDeniedException {
        String healthId = request.params(hidParam);
        UserInfo userDetails = request.attribute("userDetails");
        if (userDetails.getProperties().isPatientUserOnly()
                && !userDetails.getProperties().getPatientHid().equals(healthId)) {
            throw new AccessDeniedException(String.format("Access to user %s is denied", userDetails.getProperties().getEmail()));
        }

        logger.debug(String.format("find patient request by HID %s", healthId));
        Patient patient = patientService.findPatientByHealthId(healthId);
        response.status(200);
        return formatPatient(patient);
    }

    private String formatPatient(Patient patient) {
        return FhirContextHelper.encodeResource(patient);
    }
}
