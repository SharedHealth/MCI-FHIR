package org.sharedhealth.mci.web.validations;

import ca.uhn.fhir.context.FhirContext;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.hapi.validation.IValidationSupport;
import org.hl7.fhir.instance.model.StructureDefinition;
import org.hl7.fhir.instance.model.ValueSet;
import org.hl7.fhir.instance.model.api.IBaseResource;

import java.io.FileInputStream;
import java.io.IOException;

import static org.sharedhealth.mci.web.util.FhirContextHelper.fhirHL7Context;
import static org.sharedhealth.mci.web.validations.FhirPatientValidator.PATH_TO_PROFILES_FOLDER;
import static org.sharedhealth.mci.web.validations.FhirPatientValidator.loadProfileOrReturnNull;

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
        String profileName = theUri.substring("https://sharedhealth.atlassian.net/wiki/display/docs/fhir-extensions#".length());
        return (T) loadProfileOrReturnNull(profileName);
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
