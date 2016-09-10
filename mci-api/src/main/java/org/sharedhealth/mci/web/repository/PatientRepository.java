package org.sharedhealth.mci.web.repository;

import com.datastax.driver.core.BatchStatement;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.datastax.driver.mapping.Mapper;
import com.datastax.driver.mapping.MappingManager;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sharedhealth.mci.web.model.MCIResponse;
import org.sharedhealth.mci.web.model.Patient;
import org.sharedhealth.mci.web.model.PatientAuditLog;
import org.sharedhealth.mci.web.model.PatientUpdateLog;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import static org.sharedhealth.mci.web.util.JsonMapper.writeValueAsString;
import static org.sharedhealth.mci.web.util.RepositoryConstants.*;

public class PatientRepository {
    private Session session;
    private final Mapper<Patient> patientMapper;
    private final Mapper<PatientUpdateLog> patientUpdateLogMapper;
    private final Mapper<PatientAuditLog> patientAuditLogMapper;
    private static final Logger logger = LogManager.getLogger(PatientRepository.class);

    public PatientRepository(MappingManager mappingManager) {
        session = mappingManager.getSession();
        patientMapper = mappingManager.mapper(Patient.class);
        patientUpdateLogMapper = mappingManager.mapper(PatientUpdateLog.class);
        patientAuditLogMapper = mappingManager.mapper(PatientAuditLog.class);
    }

    public Patient findByHealthId(String healthId) {
        logger.info(String.format("Find patient by healthId: %s", healthId));
        Patient patient = patientMapper.get(healthId);
        return patient;
    }

    public MCIResponse createPatient(Patient patient) {
        String healthId = patient.getHealthId();
        logger.info(String.format("Creating patient with healthId: %s", healthId));
        Statement patientCreateStatement = patientMapper.saveQuery(patient);

        PatientAuditLog patientAuditLog = getPatientAuditLog(patient);
        Statement patientAuditLogStatement = patientAuditLogMapper.saveQuery(patientAuditLog);

        PatientUpdateLog patientUpdateLog = getPatientUpdateLog(patient);
        Statement patientUpdateLogStatement = patientUpdateLogMapper.saveQuery(patientUpdateLog);


        BatchStatement batch = new BatchStatement();
        batch.add(patientCreateStatement);
        batch.add(patientAuditLogStatement);
        batch.add(patientUpdateLogStatement);
        session.execute(batch);
        MCIResponse mciResponse = new MCIResponse(HttpStatus.SC_CREATED);
        mciResponse.setId(healthId);
        return mciResponse;
    }

    private PatientUpdateLog getPatientUpdateLog(Patient patient) {
        String healthId = patient.getHealthId();
        PatientUpdateLog patientUpdateLog = new PatientUpdateLog();
        patientUpdateLog.setHealthId(healthId);
        patientUpdateLog.setEventId(patient.getCreatedAt());
        patientUpdateLog.setEventType(EVENT_TYPE_CREATED);

        String changeSet = updateLogChangeSetForNewPatient(healthId);
        patientUpdateLog.setChangeSet(changeSet);

        return patientUpdateLog;
    }

    private PatientAuditLog getPatientAuditLog(Patient patient) {
        PatientAuditLog patientAuditLog = new PatientAuditLog();
        patientAuditLog.setHealthId(patient.getHealthId());
        patientAuditLog.setEventId(patient.getCreatedAt());

        String changeSet = auditLogChangeSetForNewPatient(patient);
        patientAuditLog.setChangeSet(changeSet);
        return patientAuditLog;
    }

    private String auditLogChangeSetForNewPatient(Patient patient) {
        Map<String, Map<String, Object>> changeSet = new TreeMap<>();

        Map<String, Object> dobChangeSet = getInnerChangeSet(patient.getDateOfBirth().toString());
        changeSet.put(DATE_OF_BIRTH, dobChangeSet);
        changeSet.put(GENDER, getInnerChangeSet(patient.getGender()));
        changeSet.put(GIVEN_NAME, getInnerChangeSet(patient.getGivenName()));
        changeSet.put(SUR_NAME, getInnerChangeSet(patient.getSurName()));
        Map<String, String> stringStringMap = getPresentAddress(patient);

        changeSet.put(PRESENT_ADDRESS, getInnerChangeSet(stringStringMap));
        Map<String, Object> healthIdChangeSet = getInnerChangeSet(patient.getHealthId());
        changeSet.put(HID, healthIdChangeSet);

        return writeValueAsString(changeSet);

    }

    private Map<String, String> getPresentAddress(Patient patient) {
        Map<String, String> presentAddress = new HashMap<>();
        presentAddress.put(ADDRESS_LINE, patient.getAddressLine());
        presentAddress.put(DIVISION_ID, patient.getDivisionId());
        presentAddress.put(DISTRICT_ID, patient.getDistrictId());
        presentAddress.put(UPAZILA_ID, patient.getUpazilaId());

        String cityCorporationId = patient.getCityCorporationId();
        if (null != cityCorporationId) presentAddress.put(CITY_CORPORATION, cityCorporationId);

        String unionOrUrbanWardId = patient.getUnionOrUrbanWardId();
        if (null != unionOrUrbanWardId) presentAddress.put(UNION_OR_URBAN_WARD_ID, unionOrUrbanWardId);

        String ruralWardId = patient.getRuralWardId();
        if (null != ruralWardId) presentAddress.put(RURAL_WARD_ID, ruralWardId);

        return presentAddress;
    }

    private String updateLogChangeSetForNewPatient(String healthId) {
        Map<String, Map<String, Object>> changeSet = new TreeMap<>();
        Map<String, Object> healthIdChangeSet = getInnerChangeSet(healthId);
        changeSet.put(HID, healthIdChangeSet);

        return writeValueAsString(changeSet);
    }


    private Map<String, Object> getInnerChangeSet(Object newValue) {
        Map<String, Object> innerChangeSet = new HashMap<>();
        innerChangeSet.put(OLD_VALUE, "");
        innerChangeSet.put(NEW_VALUE, newValue);
        return innerChangeSet;
    }

}
