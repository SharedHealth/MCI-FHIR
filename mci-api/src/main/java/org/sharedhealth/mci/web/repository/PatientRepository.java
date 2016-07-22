package org.sharedhealth.mci.web.repository;

import com.datastax.driver.core.Session;
import com.datastax.driver.mapping.Mapper;
import com.datastax.driver.mapping.MappingManager;
import org.sharedhealth.mci.web.exception.PatientNotFoundException;
import org.sharedhealth.mci.web.model.Patient;

public class PatientRepository {
    private final Mapper<Patient> patientMapper;

    public PatientRepository(Session session) {
        patientMapper = new MappingManager(session).mapper(Patient.class);
    }

    public Patient findByHealthId(String healthId) {
        Patient patient = patientMapper.get(healthId);
        if (null == patient) {
            throw new PatientNotFoundException("No patient found with health id: " + healthId);
        }
        return patient;
    }
}
