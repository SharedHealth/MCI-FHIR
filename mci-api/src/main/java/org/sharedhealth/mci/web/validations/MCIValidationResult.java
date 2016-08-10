package org.sharedhealth.mci.web.validations;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.validation.ResultSeverityEnum;
import ca.uhn.fhir.validation.SingleValidationMessage;
import ca.uhn.fhir.validation.ValidationResult;

import java.util.List;

public class MCIValidationResult extends ValidationResult {
    public MCIValidationResult(FhirContext theCtx, List<SingleValidationMessage> theMessages) {
        super(theCtx, theMessages);
    }

    @Override
    public boolean isSuccessful() {
        boolean successful = true;
        for (SingleValidationMessage next : this.getMessages()) {
            ResultSeverityEnum severity = next.getSeverity();
            if (severity == null || severity.ordinal() > ResultSeverityEnum.WARNING.ordinal()) {
                successful = false;
            }
        }
        return successful;
    }

}
