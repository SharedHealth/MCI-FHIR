package org.sharedhealth.mci.web.util;

public class FHIRConstants {
    public static final String BIRTH_TIME_EXTENSION_URL = "http://hl7.org/fhir/StructureDefinition/patient-birthTime";
    public static final String FHIR_EXTENSION_URL = "https://sharedhealth.atlassian.net/wiki/display/docs/fhir-extensions";
    public static final String ADDRESS_CODE_EXTENSION_NAME = "AddressCode";

    public static String getFhirExtensionUrl(String extensionName) {
        return FHIR_EXTENSION_URL + "#" + extensionName;
    }
}
