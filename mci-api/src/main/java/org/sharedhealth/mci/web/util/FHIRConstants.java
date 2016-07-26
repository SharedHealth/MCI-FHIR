package org.sharedhealth.mci.web.util;

import static org.sharedhealth.mci.web.util.MCIConstants.URI_SEPARATOR;

public class FHIRConstants {
    public static final String MCI_IDENTIFIER_VALUESET_URI_PATH = "/api/v2/vs/patient-identifiers";
    public static final String MCI_IDENTIFIER_HID_CODE = "HID";

    public static final String BIRTH_TIME_EXTENSION_URL = "http://hl7.org/fhir/StructureDefinition/patient-birthTime";
    public static final String FHIR_EXTENSION_URL = "https://sharedhealth.atlassian.net/wiki/display/docs/fhir-extensions";
    public static final String ADDRESS_CODE_EXTENSION_NAME = "AddressCode";

    public static String getFhirExtensionUrl(String extensionName) {
        return FHIR_EXTENSION_URL + "#" + extensionName;
    }

    public static String getMCIValuesetURI(String mciBaseUrl){
        String baseUrl = StringUtils.ensureSuffix(mciBaseUrl, URI_SEPARATOR);
        String identifierURI = StringUtils.removePrefix(MCI_IDENTIFIER_VALUESET_URI_PATH, URI_SEPARATOR);
        return String.format("%s%s", baseUrl, identifierURI);
    }

}
