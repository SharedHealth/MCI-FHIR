package org.sharedhealth.mci.web.repository;

import com.datastax.driver.mapping.Mapper;
import com.datastax.driver.mapping.MappingManager;
import org.junit.Before;
import org.junit.Test;
import org.sharedhealth.mci.web.BaseIntegrationTest;
import org.sharedhealth.mci.web.config.MCICassandraConfig;
import org.sharedhealth.mci.web.exception.PatientNotFoundException;
import org.sharedhealth.mci.web.model.Patient;
import org.sharedhealth.mci.web.util.DateUtil;

import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class PatientRepositoryIT extends BaseIntegrationTest {
    private MappingManager mappingManager;
    private PatientRepository patientRepository;
    private Mapper<Patient> patientMapper;

    private final String healthId = "HID";
    private final String givenName = "Bob the";
    private final String surName = "Builder";
    private final String gender = "M";
    private final Date dateOfBirth = DateUtil.parseDate("1995-07-01 00:00:00+0530");
    private final String countryCode = "050";
    private final String divisionId = "30";
    private final String districtId = "26";
    private final String upazilaId = "18";
    private final String cityId = "02";
    private final String urbanWardId = "01";
    private final String ruralWardId = "04";
    private final String addressLine = "Will Street";

    @Before
    public void setUp() throws Exception {
        mappingManager = MCICassandraConfig.getInstance().getMappingManager();
        patientRepository = new PatientRepository(mappingManager);
        patientMapper = mappingManager.mapper(Patient.class);
    }

    @Test
    public void shouldRetrievePatientByHealthID() throws Exception {
        Patient expectedPatient = createPatient();
        patientMapper.save(expectedPatient);

        Patient patient = patientRepository.findByHealthId(healthId);

        assertNotNull(patient);
        assertEquals(expectedPatient, patient);
    }

    @Test(expected = PatientNotFoundException.class)
    public void shouldThrowErrorWhenPatientNotFound() throws Exception {
        patientRepository.findByHealthId("HID1");
    }

    private Patient createPatient() {
        Patient expectedPatient = new Patient();
        expectedPatient.setHealthId(healthId);
        expectedPatient.setGivenName(givenName);
        expectedPatient.setSurName(surName);
        expectedPatient.setGender(gender);
        expectedPatient.setDateOfBirth(dateOfBirth);
        expectedPatient.setCountryCode(countryCode);
        expectedPatient.setDivisionId(divisionId);
        expectedPatient.setDistrictId(districtId);
        expectedPatient.setUpazilaId(upazilaId);
        expectedPatient.setCityCorporationId(cityId);
        expectedPatient.setUnionOrUrbanWardId(urbanWardId);
        expectedPatient.setRuralWardId(ruralWardId);
        expectedPatient.setAddressLine(addressLine);
        return expectedPatient;
    }
}