package org.sharedhealth.mci.web.controller;


import ca.uhn.fhir.model.dstu2.resource.Patient;
import org.sharedhealth.mci.web.service.PatientService;
import org.sharedhealth.mci.web.util.FhirContextHelper;

import static org.sharedhealth.mci.web.util.MCIConstants.API_PREFIX;
import static org.sharedhealth.mci.web.util.MCIConstants.PATIENT_URI_PATH;
import static spark.Spark.get;

public class PatientController {

    public PatientController(PatientService patientService) {
        String hidParam = ":hid";
        String api_prefix = String.format("%s%s%s", API_PREFIX, PATIENT_URI_PATH, hidParam);
        get(api_prefix, (request, response) -> {
            String healthId = request.params(hidParam);
            Patient patient = patientService.findPatientByHealthId(healthId);
            return FhirContextHelper.getFhirContext().newXmlParser().encodeResourceToString(patient);
        });
    }
}
