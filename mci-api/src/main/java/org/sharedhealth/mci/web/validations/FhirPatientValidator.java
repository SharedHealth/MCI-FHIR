package org.sharedhealth.mci.web.validations;

import ca.uhn.fhir.model.dstu2.resource.Patient;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.ResultSeverityEnum;
import ca.uhn.fhir.validation.ValidationResult;
import org.apache.commons.io.IOUtils;
import org.hl7.fhir.instance.hapi.validation.DefaultProfileValidationSupport;
import org.hl7.fhir.instance.hapi.validation.FhirInstanceValidator;
import org.hl7.fhir.instance.hapi.validation.ValidationSupportChain;
import org.hl7.fhir.instance.model.StructureDefinition;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.sharedhealth.mci.web.util.FhirContextHelper.fhirContext;
import static org.sharedhealth.mci.web.util.FhirContextHelper.fhirHL7Context;

public class FhirPatientValidator {
    public static final String PATH_TO_PROFILES_FOLDER = "/Users/mritunjd/Documents/projects/bdshr/MCI-Registry/profiles/";

    private List<Pattern> patientFieldErrors = new ArrayList<>();
    private volatile FhirValidator fhirValidator;

    public FhirPatientValidator() {
        this.patientFieldErrors.add(Pattern.compile("/f:Patient/f:gender"));
    }

    public MCIValidationResult validate(Patient patient) {
        FhirValidator fhirValidator = validatorInstance();
        ValidationResult validationResult = fhirValidator.validateWithResult(patient);
        MCIValidationResult mciValidationResult = new MCIValidationResult(fhirContext, validationResult.getMessages());
        changeWarningToErrorIfNeeded(mciValidationResult);
        return mciValidationResult;
    }

    private void changeWarningToErrorIfNeeded(MCIValidationResult validationResult) {
        validationResult.getMessages().forEach(validationMessage -> {
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
                    FhirInstanceValidator validator = new FhirInstanceValidator();

                    // loadProfileOrReturnNull reads from file mypatient.profile.xml and give StructureDefinition for that
                    StructureDefinition patient = loadProfileOrReturnNull("patient");
                    validator.setStructureDefintion(patient);

                    //SharedHealthSupport is IValidationSupport which gives definition of custom extensions
                    validator.setValidationSupport(new ValidationSupportChain(new DefaultProfileValidationSupport(), new SharedHealthSupport()));
                    fhirValidator.registerValidatorModule(validator);
                }
            }
        }
        return fhirValidator;
    }

    public static StructureDefinition loadProfileOrReturnNull(String profileName) {
        String profileText;
        try {
            String pathToProfile = PATH_TO_PROFILES_FOLDER + profileName.toLowerCase() + ".profile.xml";
            profileText = IOUtils.toString(new FileInputStream(pathToProfile), "UTF-8");
        } catch (IOException e1) {
            throw new RuntimeException("No profile found for patient");
        }
        return fhirHL7Context.newXmlParser().parseResource(StructureDefinition.class,
                profileText);
    }

}
