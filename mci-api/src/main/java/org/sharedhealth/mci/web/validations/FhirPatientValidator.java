package org.sharedhealth.mci.web.validations;

import ca.uhn.fhir.model.dstu2.resource.Patient;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.ResultSeverityEnum;
import ca.uhn.fhir.validation.ValidationResult;
import org.apache.commons.io.IOUtils;
import org.hl7.fhir.instance.hapi.validation.FhirInstanceValidator;
import org.hl7.fhir.instance.model.StructureDefinition;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.sharedhealth.mci.web.util.FhirContextHelper.fhirContext;
import static org.sharedhealth.mci.web.util.FhirContextHelper.fhirHL7Context;

public class FhirPatientValidator {
    private static final String PATH_TO_PATIENT_PROFILE = "/Users/anjalyj/IdeaProjects/SHR/MCI-Registry/fhirPatient.json";

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
                    fhirValidator.registerValidatorModule(createInstanceValidator());
                }
            }
        }
        return fhirValidator;
    }

    private FhirInstanceValidator createInstanceValidator() {
        FhirInstanceValidator validator = new FhirInstanceValidator();
        validator.setStructureDefintion(loadProfileOrReturnNull());
        return validator;
    }

    private StructureDefinition loadProfileOrReturnNull() {
        String profileText;
        try {
            profileText = IOUtils.toString(new FileInputStream(PATH_TO_PATIENT_PROFILE), "UTF-8");
        } catch (IOException e1) {
            throw new RuntimeException("No profile found for patient");
        }
        return fhirHL7Context.newJsonParser().parseResource(StructureDefinition.class,
                profileText);
    }

}
