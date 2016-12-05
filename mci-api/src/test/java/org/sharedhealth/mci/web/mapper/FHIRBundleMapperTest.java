package org.sharedhealth.mci.web.mapper;

import ca.uhn.fhir.model.dstu2.resource.Patient;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.sharedhealth.mci.web.util.PatientFactory.*;

public class FHIRBundleMapperTest {
    private FHIRBundleMapper fhirBundleMapper;

    @Before
    public void setUp() throws Exception {
        fhirBundleMapper = new FHIRBundleMapper();
    }

    @Test
    public void shouldMapFHIRPatientToMCIPatientWithMandatoryFields() throws Exception {
        Patient fhirPatient = createFHIRPatientWithMandatoryFields(true);
        org.sharedhealth.mci.web.model.Patient mciPatient = fhirBundleMapper.mapToMCIPatient(fhirPatient);
        assertEquals(createMCIPatientWithMandatoryFields(), mciPatient);
    }

    @Test
    public void shouldMapFhirPatientNotHavingBirthTime() throws Exception {
        Patient fhirPatient = createFHIRPatientWithMandatoryFields(false);
        org.sharedhealth.mci.web.model.Patient mciPatient = fhirBundleMapper.mapToMCIPatient(fhirPatient);
        org.sharedhealth.mci.web.model.Patient expectedMCIPatient = createMCIPatientWithMandatoryFields();
        expectedMCIPatient.setDateOfBirth(dateOfBirth);
        assertEquals(expectedMCIPatient, mciPatient);
    }


}