package org.sharedhealth.mci.web.repository;

import com.datastax.driver.core.Session;
import com.datastax.driver.mapping.Mapper;
import com.datastax.driver.mapping.MappingManager;
import org.sharedhealth.mci.web.model.Patient;

public class PatientRepository {
    private final Mapper<Patient> patientMapper;

    public PatientRepository(Session session) {
        patientMapper = new MappingManager(session).mapper(Patient.class);
    }

    public Patient findByHealthId(String healthId) {
        Patient patient = patientMapper.get(healthId);
        if (null == patient) {
            //// TODO: 22/07/16 create new type of exception and throw
            throw new RuntimeException("No patient found with health id: " + healthId);
        }
        return patient;
    }
}
