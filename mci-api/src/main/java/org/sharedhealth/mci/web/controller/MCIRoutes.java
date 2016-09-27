package org.sharedhealth.mci.web.controller;

import org.sharedhealth.mci.web.security.TokenAuthenticationFilter;
import org.sharedhealth.mci.web.util.MCIConstants;

import static org.sharedhealth.mci.web.util.MCIConstants.API_VERSION;
import static org.sharedhealth.mci.web.util.MCIConstants.PATIENT_URI_PATH;
import static spark.Spark.before;
import static spark.Spark.get;
import static spark.Spark.post;

public class MCIRoutes {
    public MCIRoutes(PatientController patientController, TokenAuthenticationFilter authenticationFilter) {
        String patientURIPath = String.format("%s%s", API_VERSION, PATIENT_URI_PATH);

        before(authenticationFilter);
        post(patientURIPath, patientController::createPatient);

        String hidParam = ":hid";
        String patientByHIDURIPath = String.format("%s%s%s", patientURIPath, MCIConstants.URL_SEPARATOR, hidParam);
        get(patientByHIDURIPath, (request, response) -> patientController.getPatient(hidParam, request, response));
    }
}
