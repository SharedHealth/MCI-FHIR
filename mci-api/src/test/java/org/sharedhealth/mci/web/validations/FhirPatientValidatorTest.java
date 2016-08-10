package org.sharedhealth.mci.web.validations;

import ca.uhn.fhir.model.dstu2.resource.Patient;
import ca.uhn.fhir.validation.SingleValidationMessage;
import org.junit.Before;
import org.junit.Test;
import org.sharedhealth.mci.web.util.FileUtil;

import java.util.zip.DataFormatException;

import static junit.framework.Assert.*;
import static org.sharedhealth.mci.web.util.FhirContextHelper.parseResource;

public class FhirPatientValidatorTest {
    private FhirPatientValidator fhirPatientValidator;

    @Before
    public void setUp() throws Exception {
        fhirPatientValidator = new FhirPatientValidator();
    }

    @Test
    public void shouldValidateAPatientResource() throws Exception {
        Patient patient = createPatientFromFile("patients/valid_patient_with_mandatory_fields.xml");
        MCIValidationResult validationResult = fhirPatientValidator.validate(patient);
        assertTrue(validationResult.isSuccessful());
    }

    @Test
    public void shouldFailIfNotAValidGender() throws Exception {
        Patient patient = createPatientFromFile("patients/patient_with_invalid_gender.xml");
        MCIValidationResult validationResult = fhirPatientValidator.validate(patient);

        assertFalse(validationResult.isSuccessful());
        SingleValidationMessage message = validationResult.getMessages().get(0);
        assertEquals("/f:Patient/f:gender", message.getLocationString());
        assertEquals("The value provided is not in the value set http://hl7.org/fhir/ValueSet/administrative-gender (http://hl7.org/fhir/ValueSet/administrative-gender, and a code is required from this value set", message.getMessage());
    }

    private Patient createPatientFromFile(String filePath) throws DataFormatException {
        return (Patient) parseResource(FileUtil.asString(filePath));
    }
}