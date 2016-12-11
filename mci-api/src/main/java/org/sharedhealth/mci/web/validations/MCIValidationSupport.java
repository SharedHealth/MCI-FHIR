package org.sharedhealth.mci.web.validations;

import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.dstu3.hapi.validation.DefaultProfileValidationSupport;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.sharedhealth.mci.web.config.MCIProperties;

public class MCIValidationSupport extends DefaultProfileValidationSupport {
    private MCIProperties mciProperties;

    public MCIValidationSupport(MCIProperties mciProperties) {
        this.mciProperties = mciProperties;
    }

    @Override
    public <T extends IBaseResource> T fetchResource(FhirContext theContext, Class<T> theClass, String theUri) {
        String sharedHealthExtensionUriPrefix = "https://sharedhealth.atlassian.net/wiki/display/docs/fhir-extensions";
        String sharedHealthProfilesUriPrefix = "https://sharedhealth.atlassian.net/wiki/display/docs/fhir-profiles/";
        if (theUri.startsWith(sharedHealthExtensionUriPrefix)) {
            String[] parts = theUri.split("/");
            String extensionName = parts[parts.length - 1].toLowerCase();
            return (T) FhirPatientValidator.loadProfileOrReturnNull(mciProperties, extensionName + ".extension.xml");
        }
        if (theUri.startsWith(sharedHealthProfilesUriPrefix)) {
            String[] parts = theUri.split("/");
            String profileName = parts[parts.length - 1].toLowerCase();
            return (T) FhirPatientValidator.loadProfileOrReturnNull(mciProperties, profileName + ".profile.xml");
        }
        return super.fetchResource(theContext, theClass, theUri);
    }
}
