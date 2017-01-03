package org.sharedhealth.mci.web.controller;

import org.sharedhealth.mci.web.config.MCIProperties;
import org.sharedhealth.mci.web.security.AuthorizationFilter;
import org.sharedhealth.mci.web.security.TokenAuthenticationFilter;
import org.sharedhealth.mci.web.util.MCIConstants;

import static java.util.Arrays.asList;
import static org.sharedhealth.mci.web.security.UserInfo.*;
import static org.sharedhealth.mci.web.util.MCIConstants.API_VERSION;
import static org.sharedhealth.mci.web.util.MCIConstants.PATIENT_URI_PATH;
import static spark.Spark.*;

public class MCIRoutes {
    public MCIRoutes(PatientController patientController, TokenAuthenticationFilter authenticationFilter) {
        before(authenticationFilter);

        String patientURIPath = String.format("%s%s", API_VERSION, PATIENT_URI_PATH);

        if(!MCIProperties.getInstance().getDisablePost()){
            before(patientURIPath, new AuthorizationFilter(asList(PROVIDER_GROUP, FACILITY_GROUP)));
            post(patientURIPath, patientController::createPatient);
        }

        String hidParam = ":hid";
        String patientByHIDURIPath = String.format("%s%s%s", patientURIPath, MCIConstants.URL_SEPARATOR, hidParam);
        before(patientByHIDURIPath, new AuthorizationFilter(asList(PROVIDER_GROUP, FACILITY_GROUP, PATIENT_GROUP, MCI_ADMIN, MCI_APPROVER, SHR_SYSTEM_ADMIN_GROUP)));
        get(patientByHIDURIPath, (request, response) -> patientController.getPatient(hidParam, request, response));
    }
}
