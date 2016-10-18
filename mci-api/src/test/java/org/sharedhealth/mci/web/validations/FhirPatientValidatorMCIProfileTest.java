package org.sharedhealth.mci.web.validations;

import ca.uhn.fhir.parser.DataFormatException;
import ca.uhn.fhir.validation.SingleValidationMessage;
import org.hl7.fhir.dstu3.model.Patient;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.sharedhealth.mci.web.config.MCIProperties;
import org.sharedhealth.mci.web.util.FileUtil;

import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.sharedhealth.mci.web.util.FhirContextHelper.parseResource;

public class FhirPatientValidatorMCIProfileTest {
    @Mock
    private MCIProperties mciProperties;

    private FhirPatientValidator fhirPatientValidator;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        fhirPatientValidator = new FhirPatientValidator(mciProperties);
        String path = this.getClass().getResource("/profiles/").getPath();
        when(mciProperties.getProfilesFolderPath()).thenReturn(path);
    }

    @Test
    public void shouldValidateAPatientResource() throws Exception {
        Patient patient = createPatientFromFile("patients/valid_patient_with_mandatory_fields_for_profile.xml");
        MCIValidationResult validationResult = fhirPatientValidator.validate(patient);
        assertTrue(validationResult.isSuccessful());
    }

    @Test
    public void shouldFailWhenMandatoryFieldsAndExtensionsAreMissing() throws Exception {
        Patient patient = createPatientFromFile("patients/invalid_patient_missing_mandatory_fields_and_extensions.xml");
        MCIValidationResult validationResult = fhirPatientValidator.validate(patient);
        assertFalse(validationResult.isSuccessful());
        List<SingleValidationMessage> validationMessages = validationResult.getMessages();
        assertEquals(6, validationMessages.size());
        assertTrue(containsError(validationMessages, "Patient", "Element 'Patient.extension[bloodGroup]': minimum required = 1, but only found 0"));
        assertTrue(containsError(validationMessages, "Patient", "Element 'Patient.extension[confidentiality]': minimum required = 1, but only found 0"));
        assertTrue(containsError(validationMessages, "Patient", "Element 'Patient.name': minimum required = 1, but only found 0"));
        assertTrue(containsError(validationMessages, "Patient", "Element 'Patient.gender': minimum required = 1, but only found 0"));
        assertTrue(containsError(validationMessages, "Patient", "Element 'Patient.birthDate': minimum required = 1, but only found 0"));
        assertTrue(containsError(validationMessages, "Patient", "Element 'Patient.address': minimum required = 1, but only found 0"));
    }

    @Test
    public void shouldFailWhenMandatorySubFieldsAndSubExtensionsAreMissing() throws Exception {
        Patient patient = createPatientFromFile("patients/invalid_patient_missing_mandatory_sub_fields_and_sub_extensions.xml");
        MCIValidationResult validationResult = fhirPatientValidator.validate(patient);
        assertFalse(validationResult.isSuccessful());
        List<SingleValidationMessage> validationMessages = validationResult.getMessages();
        assertEquals(3, validationMessages.size());
        assertTrue(containsError(validationMessages, "Patient.name", "Element 'Patient.name.family': minimum required = 1, but only found 0"));
        assertTrue(containsError(validationMessages, "Patient.name", "Element 'Patient.name.given': minimum required = 1, but only found 0"));
        assertTrue(containsError(validationMessages, "Patient.address", "Element 'Patient.address.extension[addressCode]': minimum required = 1, but only found 0"));
    }

    @Test
    public void shouldFailWhenExtensionsOrFieldsHaveWrongDatatype() throws Exception {
        Patient patient = createPatientFromFile("patients/invalid_patient_extensions_with_wrong_datatype.xml");
        MCIValidationResult validationResult = fhirPatientValidator.validate(patient);
        assertFalse(validationResult.isSuccessful());
        List<SingleValidationMessage> validationMessages = validationResult.getMessages();
        assertEquals(8, validationMessages.size());
        for (SingleValidationMessage validationMessage : validationMessages) {
            System.out.println(validationMessage.getLocationString());
            System.out.println(validationMessage.getMessage());
        }
        assertTrue(containsError(validationMessages, "Patient.extension[1].valueBoolean", "Could not verify slice for profile https://sharedhealth.atlassian.net/wiki/display/docs/fhir-extensions/BloodGroup"));
        assertTrue(containsError(validationMessages, "Patient.name", "Element 'Patient.name.given': minimum required = 1, but only found 0"));
        assertTrue(containsError(validationMessages, "Patient.address", "Element 'Patient.address.extension[addressCode]': minimum required = 1, but only found 0"));
    }

    private boolean containsError(List<SingleValidationMessage> messages, String location, String message) {
        return messages.stream().anyMatch(validationMessage -> validationMessage.getLocationString().equals(location)
                && validationMessage.getMessage().endsWith(message));
    }


    private Patient createPatientFromFile(String filePath) throws DataFormatException {
        return (Patient) parseResource(FileUtil.asString(filePath));
    }


}