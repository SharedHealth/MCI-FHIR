package org.sharedhealth.mci.web.util;

import static org.sharedhealth.mci.web.util.StringUtils.ensureSuffix;
import static org.sharedhealth.mci.web.util.StringUtils.removePrefix;

public class MCIConstants {
    public static final String API_VERSION = "/api/v2";
    public static final String PATIENT_URI_PATH = "/patients/";
    public static final String URI_SEPARATOR = "/";

    public static final String MALE = "M";
    public static final String FEMALE = "F";
    public static final String OTHER = "O";

    public static final String HTTP_STATUS = "http_status";

    public static String getMCIPatientURI(String mciBaseUrl) {
        String baseUrl = ensureSuffix(mciBaseUrl, URI_SEPARATOR);
        String apiVersion = removePrefix(API_VERSION, URI_SEPARATOR);
        String patientUriPath = removePrefix(PATIENT_URI_PATH, URI_SEPARATOR);
        String mciPatientUri = String.format("%s%s%s", baseUrl, apiVersion, patientUriPath);
        return ensureSuffix(mciPatientUri, URI_SEPARATOR);
    }
}
