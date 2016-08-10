package org.sharedhealth.mci.web.controller;

import static org.sharedhealth.mci.web.util.MCIConstants.API_VERSION;
import static org.sharedhealth.mci.web.util.MCIConstants.PATIENT_URI_PATH;
import static spark.Spark.get;
import static spark.Spark.post;

public class MCIRoutes {
    public MCIRoutes(PatientController patientController) {

        String patientURIPath = String.format("%s%s", API_VERSION, PATIENT_URI_PATH);
        post(patientURIPath, patientController::createPatient);

        String hidParam = ":hid";
        String patientByHIDURIPath = String.format("%s%s", patientURIPath, hidParam);
        get(patientByHIDURIPath, (request, response) -> patientController.getPatient(hidParam, request, response));
    }
}
