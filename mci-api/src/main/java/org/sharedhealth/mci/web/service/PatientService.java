package org.sharedhealth.mci.web.service;

import ca.uhn.fhir.model.dstu2.resource.Patient;
import org.apache.http.HttpStatus;
import org.sharedhealth.mci.web.mapper.PatientMapper;
import org.sharedhealth.mci.web.model.MCIResponse;
import org.sharedhealth.mci.web.model.MciHealthId;
import org.sharedhealth.mci.web.repository.PatientRepository;

public class PatientService {
    private PatientMapper patientMapper;
    private PatientRepository patientRepository;
    private HealthIdService healthIdService;

    public PatientService(PatientMapper patientMapper, HealthIdService healthIdService, PatientRepository patientRepository) {
        this.patientMapper = patientMapper;
        this.healthIdService = healthIdService;
        this.patientRepository = patientRepository;
    }

    public Patient findPatientByHealthId(String healthId) {
        org.sharedhealth.mci.web.model.Patient mciPatient = patientRepository.findByHealthId(healthId);
        return patientMapper.mapToFHIRPatient(mciPatient);
    }

    public MCIResponse createPatient(Patient fhirPatient) {
        org.sharedhealth.mci.web.model.Patient mciPatient = patientMapper.mapToMCIPatient(fhirPatient);
        MciHealthId healthId;
        try {
            healthId = healthIdService.getNextHealthId();
            mciPatient.setHealthId(healthId.getHid());
        } catch (Exception e) {
            MCIResponse mciResponse = new MCIResponse(HttpStatus.SC_BAD_REQUEST);
            mciResponse.setMessage(e.getMessage());
            return mciResponse;
        }
        MCIResponse mciResponse = patientRepository.createPatient(mciPatient);
        healthIdService.markUsed(healthId);
        return mciResponse;
    }
}
