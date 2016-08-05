package org.sharedhealth.mci.web.controller;


import ca.uhn.fhir.model.dstu2.resource.Patient;
import ca.uhn.fhir.parser.IParser;
import org.sharedhealth.mci.web.model.MCIResponse;
import org.sharedhealth.mci.web.service.PatientService;
import org.sharedhealth.mci.web.util.FhirContextHelper;

import static org.sharedhealth.mci.web.util.MCIConstants.API_VERSION;
import static org.sharedhealth.mci.web.util.MCIConstants.PATIENT_URI_PATH;
import static spark.Spark.get;
import static spark.Spark.post;

public class PatientController {
    private final IParser xmlParser = FhirContextHelper.getFhirContext().newXmlParser();

    public PatientController(PatientService patientService) {

        String patientURIPath = String.format("%s%s", API_VERSION, PATIENT_URI_PATH);
        post(patientURIPath, (request, response) -> {
            String body = request.body();
            Patient patient = (Patient) xmlParser.parseResource(body);
            MCIResponse mciResponse = patientService.createPatient(patient);
            response.status(mciResponse.getHttpStatus());
            return mciResponse.toString();
        });


        String hidParam = ":hid";
        String patientByHIDURIPath = String.format("%s%s", patientURIPath, hidParam);
        get(patientByHIDURIPath, (request, response) -> {
            String healthId = request.params(hidParam);
            Patient patient = patientService.findPatientByHealthId(healthId);
            response.status(200);
            return formatPatient(patient);
        });
    }

    private String formatPatient(Patient patient) {
        return xmlParser.encodeResourceToString(patient);
    }
}
