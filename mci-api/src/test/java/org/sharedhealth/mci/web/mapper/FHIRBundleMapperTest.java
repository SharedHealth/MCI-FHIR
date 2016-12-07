package org.sharedhealth.mci.web.mapper;

import ca.uhn.fhir.model.dstu2.resource.Bundle;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.sharedhealth.mci.web.config.MCIProperties;
import org.sharedhealth.mci.web.model.Patient;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.sharedhealth.mci.web.util.MCIConstants.PATIENT_STATUS_UNKNOWN;
import static org.sharedhealth.mci.web.util.PatientTestFactory.*;

public class FHIRBundleMapperTest {
    private FHIRBundleMapper fhirBundleMapper;
    @Mock
    private MCIProperties mciProperties;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        fhirBundleMapper = new FHIRBundleMapper(mciProperties);
        when(mciProperties.getMciBaseUrl()).thenReturn(mciBaseUrl);
    }

    @Test
    public void shouldMapFHIRPatientToMCIPatientWithMandatoryFields() throws Exception {
        Bundle fhirPatientBundle = createPatientBundleWithMandatoryFields(true);
        org.sharedhealth.mci.web.model.Patient mciPatient = fhirBundleMapper.mapToMCIPatient(fhirPatientBundle);
        Patient expectedPatient = createMCIPatientWithMandatoryFields();
        expectedPatient.setStatus(PATIENT_STATUS_UNKNOWN);
        assertEquals(expectedPatient, mciPatient);
    }

    @Test
    public void shouldMapFhirPatientNotHavingBirthTime() throws Exception {
        Bundle fhirPatientBundle = createPatientBundleWithMandatoryFields(false);
        org.sharedhealth.mci.web.model.Patient mciPatient = fhirBundleMapper.mapToMCIPatient(fhirPatientBundle);
        org.sharedhealth.mci.web.model.Patient expectedMCIPatient = createMCIPatientWithMandatoryFields();
        expectedMCIPatient.setDateOfBirth(dateOfBirth);
        expectedMCIPatient.setStatus(PATIENT_STATUS_UNKNOWN);
        assertEquals(expectedMCIPatient, mciPatient);
    }

    @Test
    public void shouldMapFHIRPatientToMCIPatientWithAllFields() throws Exception {
        Bundle fhirPatientBundle = createPatientBundleWithAllFields();
        org.sharedhealth.mci.web.model.Patient mciPatient = fhirBundleMapper.mapToMCIPatient(fhirPatientBundle);
        assertEquals(createMCIPatientWithAllFields(), mciPatient);
    }
}