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
import org.sharedhealth.mci.web.util.TestUtil;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.sharedhealth.mci.web.util.DateUtil.*;
import static org.sharedhealth.mci.web.util.JsonMapper.readValue;
import static org.sharedhealth.mci.web.util.PatientFactory.*;
import static org.sharedhealth.mci.web.util.RepositoryConstants.*;

public class PatientRepositoryIT extends BaseIntegrationTest {
    private static PatientRepository patientRepository;
    private static Mapper<Patient> patientDBMapper;
    private static Mapper<PatientUpdateLog> patientUpdateLogDBMapper;
    private static Mapper<PatientAuditLog> patientAuditLogDBMapper;

    private static final String healthId = "HID123";

    @Before
    public void setUp() throws Exception {
        MappingManager mappingManager = MCICassandraConfig.getInstance().getMappingManager();
        patientRepository = new PatientRepository(mappingManager);
        patientDBMapper = mappingManager.mapper(Patient.class);
        patientUpdateLogDBMapper = mappingManager.mapper(PatientUpdateLog.class);
        patientAuditLogDBMapper = mappingManager.mapper(PatientAuditLog.class);
    }

    @After
    public void tearDown() throws Exception {
        TestUtil.truncateAllColumnFamilies();
    }

    @Test
    public void shouldRetrievePatientByHealthID() throws Exception {
        Patient expectedPatient = createMCIPatientWithAllFields();
        patientDBMapper.save(expectedPatient);

        Patient patient = patientRepository.findByHealthId(healthId);

        assertNotNull(patient);
        assertEquals(expectedPatient, patient);
    }

    @Test
    public void shouldCreatePatientInDatabase() throws Exception {
        Patient patient = createMCIPatientWithAllFields();

        MCIResponse mciResponse = patientRepository.createPatient(patient);

        Patient byHealthId = patientDBMapper.get(patient.getHealthId());
        assertEquals(patient, byHealthId);
        assertEquals(patient.getHealthId(), mciResponse.getId());
        assertEquals(HttpStatus.SC_CREATED, mciResponse.getHttpStatus());
        assertPatientAuditLog(patient);
        assertPatientUpdateLog(patient);
    }

    private void assertPatientAuditLog(Patient patient) {
        PatientAuditLog patientAuditLog = patientAuditLogDBMapper.get(patient.getHealthId());
        assertNotNull(patientAuditLog);
        assertEquals(patient.getCreatedAt(), patientAuditLog.getEventId());
        assertNull(patientAuditLog.getApprovedBy());
        assertNull(patientAuditLog.getRequestedBy());
        String changeSet = patientAuditLog.getChangeSet();
        assertNotNull(changeSet);

        Map<String, Map<String, Object>> changeSetAsMap = readValue(changeSet, new TypeReference<Map<String, Map<String, Object>>>() {
        });
        assertChangeSet(changeSetAsMap, HID, healthId);
        assertChangeSet(changeSetAsMap, DATE_OF_BIRTH, toDateString(patient.getDateOfBirth(), ISO_8601_DATE_IN_MILLIS_FORMAT2));
        assertChangeSet(changeSetAsMap, GENDER, gender);
        assertChangeSet(changeSetAsMap, PRESENT_ADDRESS, getPresentAddress());
        assertChangeSet(changeSetAsMap, GIVEN_NAME, givenName);
        assertChangeSet(changeSetAsMap, SUR_NAME, surName);

    }

    private void assertPatientUpdateLog(Patient patient) {
        PatientUpdateLog patientUpdateLog = patientUpdateLogDBMapper.get(getYearOf(patient.getCreatedAt()));
        assertNotNull(patientUpdateLog);
        assertEquals(patient.getCreatedAt(), patientUpdateLog.getEventId());
        assertEquals(patient.getHealthId(), patientUpdateLog.getHealthId());
        assertEquals(EVENT_TYPE_CREATED, patientUpdateLog.getEventType());
        assertNull(patientUpdateLog.getApprovedBy());
        String changeSet = patientUpdateLog.getChangeSet();
        assertNotNull(changeSet);

        Map<String, Map<String, Object>> changeSetAsMap = readValue(changeSet, new TypeReference<Map<String, Map<String, Object>>>() {
        });

        assertChangeSet(changeSetAsMap, HID, healthId);
    }

    private void assertChangeSet(Map<String, Map<String, Object>> changeSetAsMap, String fieldName, Object fieldValue) {
        Map<String, Object> genderChangeSet = changeSetAsMap.get(fieldName);
        assertEquals(fieldValue, genderChangeSet.get(NEW_VALUE));
    }

    private Map<String, String> getPresentAddress() {
        Map<String, String> presentAddress = new HashMap<>();
        presentAddress.put(ADDRESS_LINE, addressLine);
        presentAddress.put(DIVISION_ID, divisionId);
        presentAddress.put(DISTRICT_ID, districtId);
        presentAddress.put(UPAZILA_ID, upazilaId);
        presentAddress.put(CITY_CORPORATION, cityId);
        presentAddress.put(UNION_OR_URBAN_WARD_ID, urbanWardId);
        presentAddress.put(RURAL_WARD_ID, ruralWardId);
        return presentAddress;
    }
}