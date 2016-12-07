package org.sharedhealth.mci.web.controller;


import ca.uhn.fhir.model.dstu2.resource.Bundle;
import ca.uhn.fhir.parser.DataFormatException;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
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

import static org.sharedhealth.mci.web.security.AuthorizationFilter.USER_DETAILS_KEY;

public class PatientController {
    private final static Logger logger = LogManager.getLogger(PatientController.class);
    private final PatientService patientService;

    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    public String createPatient(Request request, Response response) throws IOException {
        logAccessDetails("Creating a new patient", request.attribute(USER_DETAILS_KEY));
        logger.debug("Create patient request");

        Bundle bundle;
        try {
            bundle = (Bundle) FhirContextHelper.parseResource(request.body());
        } catch (DataFormatException e) {
            logger.error("Can not parse", e);
            MCIResponse mciResponse = new MCIResponse(HttpStatus.SC_UNPROCESSABLE_ENTITY);
            mciResponse.setMessage(e.getMessage());
            response.status(mciResponse.getHttpStatus());
            return mciResponse.toString();
        }
        UserInfo userInfo = request.attribute(USER_DETAILS_KEY);
        MCIResponse mciResponse = patientService.createPatient(bundle, userInfo);
        response.status(mciResponse.getHttpStatus());
        response.type(ContentType.APPLICATION_JSON.getMimeType());
        return mciResponse.toString();
    }

    private void logAccessDetails(String action, UserInfo userDetails) {
        logger.info(String.format("ACCESS: EMAIL=%s ACTION=%s", userDetails.getProperties().getEmail(), action));
    }

    public String getPatient(String hidParam, Request request, Response response) throws AccessDeniedException {
        String healthId = request.params(hidParam);
        UserInfo userDetails = request.attribute(USER_DETAILS_KEY);
        if (userDetails.getProperties().isPatientUserOnly()
                && !userDetails.getProperties().getPatientHid().equals(healthId)) {
            throw new AccessDeniedException(String.format("Access to user %s is denied", userDetails.getProperties().getEmail()));
        }
        logAccessDetails(String.format("Find patient given (healthId) : %s", healthId), request.attribute(USER_DETAILS_KEY));
        logger.debug(String.format("find patient request by HID %s", healthId));
        Bundle patientBundle = patientService.findPatientByHealthId(healthId);
        response.status(200);
        response.type(ContentType.APPLICATION_XML.getMimeType());
        return formatPatient(patientBundle);
    }

    private String formatPatient(Bundle patientBundle) {
        return FhirContextHelper.encodeResource(patientBundle);
    }
}
