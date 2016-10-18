package org.sharedhealth.mci.web.validations;

import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.ResultSeverityEnum;
import ca.uhn.fhir.validation.ValidationResult;
import org.apache.commons.io.IOUtils;
import org.hl7.fhir.dstu3.hapi.validation.FhirInstanceValidator;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.StructureDefinition;
import org.sharedhealth.mci.web.config.MCIProperties;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.sharedhealth.mci.web.util.FhirContextHelper.fhirContext;

public class FhirPatientValidator {
    private final String PATIENT_PROFILE_FILE_PREFIX = "mcipatient.profile.xml";
    private volatile FhirValidator fhirValidator;
    private MCIProperties mciProperties;

    public FhirPatientValidator(MCIProperties mciProperties) {
        this.mciProperties = mciProperties;
    }

    public MCIValidationResult validate(Patient patient) {
        FhirValidator fhirValidator = validatorInstance();
        ValidationResult validationResult = fhirValidator.validateWithResult(patient);
        return new MCIValidationResult(fhirContext, validationResult.getMessages());
    }

    private FhirValidator validatorInstance() {
        if (fhirValidator == null) {
            synchronized (FhirValidator.class) {
                if (fhirValidator == null) {
                    fhirValidator = fhirContext.newValidator();
                    FhirInstanceValidator validator = new FhirInstanceValidator(new MCIValidationSupport(mciProperties));
                    validator.setStructureDefintion(loadProfileOrReturnNull(mciProperties, PATIENT_PROFILE_FILE_PREFIX));
                    fhirValidator.registerValidatorModule(validator);
                }
            }
        }
        return fhirValidator;
    }

    public static StructureDefinition loadProfileOrReturnNull(MCIProperties mciProperties, String profileName) {
        String profileText;
        try {
            String pathToProfile = mciProperties.getProfilesFolderPath() + profileName.toLowerCase();
            profileText = IOUtils.toString(new FileInputStream(pathToProfile), "UTF-8");
        } catch (IOException e) {
            throw new RuntimeException(String.format("No profile found for %s", profileName));
        }
        IParser parser;
        if (profileName.endsWith(".xml")) {
            parser = fhirContext.newXmlParser();
        } else {
            parser = fhirContext.newJsonParser();
        }
        return parser.parseResource(StructureDefinition.class,
                profileText);
    }
}
