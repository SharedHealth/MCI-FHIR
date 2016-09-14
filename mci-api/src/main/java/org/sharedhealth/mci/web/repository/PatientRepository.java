package org.sharedhealth.mci.web.repository;

import com.datastax.driver.core.BatchStatement;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.datastax.driver.mapping.Mapper;
import com.datastax.driver.mapping.MappingManager;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sharedhealth.mci.web.mapper.PatientAuditLogMapper;
import org.sharedhealth.mci.web.mapper.PatientUpdateLogMapper;
import org.sharedhealth.mci.web.model.MCIResponse;
import org.sharedhealth.mci.web.model.Patient;
import org.sharedhealth.mci.web.model.PatientAuditLog;
import org.sharedhealth.mci.web.model.PatientUpdateLog;

public class PatientRepository {
    private Session session;
    private final Mapper<Patient> patientDBMapper;
    private PatientUpdateLogMapper patientUpdateLogMapper;
    private final Mapper<PatientUpdateLog> patientUpdateLogDBMapper;
    private PatientAuditLogMapper patientAuditLogMapper;
    private final Mapper<PatientAuditLog> patientAuditLogDBMapper;
    private static final Logger logger = LogManager.getLogger(PatientRepository.class);

    public PatientRepository(MappingManager mappingManager) {
        session = mappingManager.getSession();
        patientDBMapper = mappingManager.mapper(Patient.class);
        patientUpdateLogDBMapper = mappingManager.mapper(PatientUpdateLog.class);
        patientAuditLogDBMapper = mappingManager.mapper(PatientAuditLog.class);
        patientUpdateLogMapper = new PatientUpdateLogMapper();
        patientAuditLogMapper = new PatientAuditLogMapper();
    }

    public Patient findByHealthId(String healthId) {
        logger.info(String.format("Find patient by healthId: %s", healthId));
        Patient patient = patientDBMapper.get(healthId);
        return patient;
    }

    public MCIResponse createPatient(Patient patient) {
        String healthId = patient.getHealthId();
        logger.info(String.format("Creating patient with healthId: %s", healthId));

        Statement patientCreateStatement = patientDBMapper.saveQuery(patient);

        PatientAuditLog patientAuditLog = patientAuditLogMapper.map(patient);
        Statement patientAuditLogStatement = patientAuditLogDBMapper.saveQuery(patientAuditLog);

        PatientUpdateLog patientUpdateLog = patientUpdateLogMapper.map(patient);
        Statement patientUpdateLogStatement = patientUpdateLogDBMapper.saveQuery(patientUpdateLog);

        BatchStatement batch = new BatchStatement();
        batch.add(patientCreateStatement);
        batch.add(patientAuditLogStatement);
        batch.add(patientUpdateLogStatement);
        session.execute(batch);
        MCIResponse mciResponse = new MCIResponse(HttpStatus.SC_CREATED);
        mciResponse.setId(healthId);
        return mciResponse;
    }

}
