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
        String sharedHealthUriPrefix = "https://sharedhealth.atlassian.net/wiki/display/docs/fhir-";
        if (theUri.startsWith(sharedHealthUriPrefix)) {
            String[] parts = theUri.split("/");
            String profileName = parts[parts.length - 1].toLowerCase();
            return (T) FhirPatientValidator.loadProfileOrReturnNull(mciProperties, profileName + ".profile.xml");
        }
        return super.fetchResource(theContext, theClass, theUri);
    }
}
