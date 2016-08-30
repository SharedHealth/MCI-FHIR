package org.sharedhealth.mci.web.repository;

import com.datastax.driver.mapping.Mapper;
import com.datastax.driver.mapping.MappingManager;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sharedhealth.mci.web.exception.PatientNotFoundException;
import org.sharedhealth.mci.web.model.MCIResponse;
import org.sharedhealth.mci.web.model.Patient;

public class PatientRepository {
    private final Mapper<Patient> patientMapper;
    private static final Logger logger = LogManager.getLogger(PatientRepository.class);

    public PatientRepository(MappingManager mappingManager) {
        patientMapper = mappingManager.mapper(Patient.class);
    }

    public Patient findByHealthId(String healthId) {
        logger.info(String.format("Find patient by healthId: %s", healthId));
        Patient patient = patientMapper.get(healthId);
        if (null == patient) {
            throw new PatientNotFoundException("No patient found with health id: " + healthId);
        }
        return patient;
    }

    public MCIResponse createPatient(Patient patient) {
        String healthId = patient.getHealthId();
        logger.info(String.format("Creating patient with healthId: %s", healthId));
        patientMapper.save(patient);
        MCIResponse mciResponse = new MCIResponse(HttpStatus.SC_CREATED);
        mciResponse.setId(healthId);
        return mciResponse;
    }
}
