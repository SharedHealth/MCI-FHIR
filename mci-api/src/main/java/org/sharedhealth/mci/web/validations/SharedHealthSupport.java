package org.sharedhealth.mci.web.validations;

import ca.uhn.fhir.context.FhirContext;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.hapi.validation.FhirInstanceValidator;
import org.hl7.fhir.instance.hapi.validation.IValidationSupport;
import org.hl7.fhir.instance.model.ValueSet;
import org.hl7.fhir.instance.model.api.IBaseResource;

public class SharedHealthSupport implements IValidationSupport {
    @Override
    public ValueSet.ValueSetExpansionComponent expandValueSet(FhirContext theContext, ValueSet.ConceptSetComponent theInclude) {
        return null;
    }

    @Override
    public ValueSet fetchCodeSystem(FhirContext theContext, String theSystem) {
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends IBaseResource> T fetchResource(FhirContext theContext, Class<T> theClass, String theUri) {
        String fhirURIPrefix = "http://hl7.org/fhir/StructureDefinition/";
        String sharedHealthPrefix = "https://sharedhealth.atlassian.net/wiki/display/docs/fhir-extensions#";
        if (theUri.startsWith(fhirURIPrefix)) {
            String profileName = theUri.substring(fhirURIPrefix.length());
            return (T) FhirPatientValidator.loadProfileOrReturnNull(profileName);
        }
        if(theUri.startsWith(sharedHealthPrefix)){
            String profileName = theUri.substring(sharedHealthPrefix.length());
            return (T) FhirPatientValidator.loadProfileOrReturnNull(profileName);
        }
        return null;
    }

    @Override
    public boolean isCodeSystemSupported(FhirContext theContext, String theSystem) {
        String prefix = "https://sharedhealth.atlassian.net/wiki/display/docs/fhir-extensions";
        return StringUtils.startsWith(theSystem, prefix);
    }

    @Override
    public CodeValidationResult validateCode(FhirContext theContext, String theCodeSystem, String theCode, String theDisplay) {
        return null;
    }

}
