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

import static org.sharedhealth.mci.web.util.RepositoryConstants.EVENT_TYPE_CREATED;

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
        PatientUpdateLog patientUpdateLog = new PatientUpdateLog();
        patientUpdateLog.setHealthId(patient.getHealthId());
        patientUpdateLog.setEventId(patient.getCreatedAt());
        patientUpdateLog.setEventType(EVENT_TYPE_CREATED);
        return patientUpdateLog;
    }

    private PatientAuditLog getPatientAuditLog(Patient patient) {
        PatientAuditLog patientAuditLog = new PatientAuditLog();
        patientAuditLog.setHealthId(patient.getHealthId());
        patientAuditLog.setEventId(patient.getCreatedAt());
        return patientAuditLog;
    }
}
