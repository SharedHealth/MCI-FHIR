package org.sharedhealth.mci.web.validations;

import ca.uhn.fhir.model.dstu2.resource.Patient;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.ResultSeverityEnum;
import ca.uhn.fhir.validation.ValidationResult;
import org.hl7.fhir.instance.hapi.validation.FhirInstanceValidator;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.sharedhealth.mci.web.util.FhirContextHelper.fhirContext;

public class FhirPatientValidator {
    private List<Pattern> patientFieldErrors = new ArrayList<>();
    private volatile FhirValidator fhirValidator;

    public FhirPatientValidator() {
        this.patientFieldErrors.add(Pattern.compile("/f:Patient/f:gender"));
    }

    public MCIValidationResult validate(Patient patient) {
        ValidationResult validationResult = validatorInstance().validateWithResult(patient);
        MCIValidationResult mciValidationResult = new MCIValidationResult(fhirContext, validationResult.getMessages());
        changeWarningToErrorIfNeeded(mciValidationResult);
        return mciValidationResult;
    }

    private void changeWarningToErrorIfNeeded(MCIValidationResult validationResult) {
        validationResult.getMessages().stream().forEach(validationMessage -> {
            if (isPossiblePatientFieldError(validationMessage.getLocationString())) {
                if (validationMessage.getSeverity().ordinal() <= ResultSeverityEnum.WARNING.ordinal()) {
                    validationMessage.setSeverity(ResultSeverityEnum.ERROR);
                }
            }
        });
    }

    private boolean isPossiblePatientFieldError(String locationString) {
        return patientFieldErrors.stream().anyMatch(pattern -> {
            Matcher matcher = pattern.matcher(locationString);
            return matcher.matches();
        });
    }


    private FhirValidator validatorInstance() {
        if (fhirValidator == null) {
            synchronized (FhirValidator.class) {
                if (fhirValidator == null) {
                    fhirValidator = fhirContext.newValidator();
                    fhirValidator.registerValidatorModule(new FhirInstanceValidator());
                }
            }
        }
        return fhirValidator;
    }

}
