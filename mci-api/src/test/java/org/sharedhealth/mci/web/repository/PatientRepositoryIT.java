package org.sharedhealth.mci.web.repository;

import com.datastax.driver.mapping.Mapper;
import com.datastax.driver.mapping.MappingManager;
import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.http.HttpStatus;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sharedhealth.mci.web.BaseIntegrationTest;
import org.sharedhealth.mci.web.config.MCICassandraConfig;
import org.sharedhealth.mci.web.model.MCIResponse;
import org.sharedhealth.mci.web.model.Patient;
import org.sharedhealth.mci.web.model.PatientAuditLog;
import org.sharedhealth.mci.web.model.PatientUpdateLog;
import org.sharedhealth.mci.web.util.DateUtil;
import org.sharedhealth.mci.web.util.TestUtil;
import org.sharedhealth.mci.web.util.TimeUuidUtil;

import java.util.Date;
import java.util.Map;

import static org.junit.Assert.*;
import static org.sharedhealth.mci.web.util.JsonMapper.readValue;
import static org.sharedhealth.mci.web.util.RepositoryConstants.*;

public class PatientRepositoryIT extends BaseIntegrationTest {
    private PatientRepository patientRepository;
    private Mapper<Patient> patientMapper;
    private Mapper<PatientUpdateLog> patientUpdateLogMapper;
    private Mapper<PatientAuditLog> patientAuditLogMapper;

    private final String healthId = "HID123";
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
        MappingManager mappingManager = MCICassandraConfig.getInstance().getMappingManager();
        patientRepository = new PatientRepository(mappingManager);
        patientMapper = mappingManager.mapper(Patient.class);
        patientUpdateLogMapper = mappingManager.mapper(PatientUpdateLog.class);
        patientAuditLogMapper = mappingManager.mapper(PatientAuditLog.class);
    }

    @After
    public void tearDown() throws Exception {
        TestUtil.truncateAllColumnFamilies();
    }

    @Test
    public void shouldRetrievePatientByHealthID() throws Exception {
        Patient expectedPatient = preparePatientData();
        patientMapper.save(expectedPatient);

        Patient patient = patientRepository.findByHealthId(healthId);

        assertNotNull(patient);
        assertEquals(expectedPatient, patient);
    }

    @Test
    public void shouldCreatePatientInDatabase() throws Exception {
        Patient patient = preparePatientData();

        MCIResponse mciResponse = patientRepository.createPatient(patient);

        Patient byHealthId = patientMapper.get(patient.getHealthId());
        assertEquals(patient, byHealthId);
        assertEquals(patient.getHealthId(), mciResponse.getId());
        assertEquals(HttpStatus.SC_CREATED, mciResponse.getHttpStatus());

        assertPatientAuditLog(patient);

        assertPatientUpdateLog(patient);

    }

    private void assertPatientAuditLog(Patient patient) {
        PatientAuditLog patientAuditLog = patientAuditLogMapper.get(patient.getHealthId());
        assertNotNull(patientAuditLog);
        assertEquals(patient.getCreatedAt(), patientAuditLog.getEventId());
        assertNull(patientAuditLog.getApprovedBy());
        assertNull(patientAuditLog.getRequestedBy());
        String changeSet = patientAuditLog.getChangeSet();
        assertNotNull(changeSet);

        Map<String, Map<String, Object>> changeSetAsMap = readValue(changeSet, new TypeReference<Map<String, Map<String, Object>>>() {
        });
        assertHidChangeSet(changeSetAsMap);
        assertDOBChangeset(changeSetAsMap);
        assertGenderChangeSet(changeSetAsMap);
        assertGivenNameChangeSet(changeSetAsMap);
        assertSurNameChangeSet(changeSetAsMap);

    }

    private void assertPatientUpdateLog(Patient patient) {
        PatientUpdateLog patientUpdateLog = patientUpdateLogMapper.get(DateUtil.getYearOf(patient.getCreatedAt()));
        assertNotNull(patientUpdateLog);
        assertEquals(patient.getCreatedAt(), patientUpdateLog.getEventId());
        assertEquals(patient.getHealthId(), patientUpdateLog.getHealthId());
        assertEquals(EVENT_TYPE_CREATED, patientUpdateLog.getEventType());
        assertNull(patientUpdateLog.getApprovedBy());
        String changeSet = patientUpdateLog.getChangeSet();
        assertNotNull(changeSet);

        Map<String, Map<String, Object>> changeSetAsMap = readValue(changeSet, new TypeReference<Map<String, Map<String, Object>>>() {
        });

        assertHidChangeSet(changeSetAsMap);
    }

    private void assertHidChangeSet(Map<String, Map<String, Object>> changeSetAsMap) {
        Map<String, Object> hidChangeSetAsMap = changeSetAsMap.get(HID);
        assertEquals(healthId, hidChangeSetAsMap.get(NEW_VALUE).toString());
        assertEquals("", hidChangeSetAsMap.get(OLD_VALUE).toString());
    }

    private void assertDOBChangeset(Map<String, Map<String, Object>> changeSetAsMap) {
        Map<String, Object> dateOfBirthChangeSet = changeSetAsMap.get(DATE_OF_BIRTH);
        assertEquals(dateOfBirth.toString(), dateOfBirthChangeSet.get(NEW_VALUE));
    }

    private void assertGenderChangeSet(Map<String, Map<String, Object>> changeSetAsMap) {
        Map<String, Object> genderChangeSet = changeSetAsMap.get(GENDER);
        assertEquals(gender, genderChangeSet.get(NEW_VALUE));
    }

    private void assertGivenNameChangeSet(Map<String, Map<String, Object>> changeSetAsMap) {
        Map<String, Object> givenNameChangeSet = changeSetAsMap.get(GIVEN_NAME);
        assertEquals(givenName, givenNameChangeSet.get(NEW_VALUE));
    }
    private void assertSurNameChangeSet(Map<String, Map<String, Object>> changeSetAsMap) {
        Map<String, Object> surNameChangeSet = changeSetAsMap.get(SUR_NAME);
        assertEquals(surName, surNameChangeSet.get(NEW_VALUE));
    }

    private Patient preparePatientData() {
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
        expectedPatient.setCreatedAt(TimeUuidUtil.uuidForDate(new Date()));
        return expectedPatient;
    }
}