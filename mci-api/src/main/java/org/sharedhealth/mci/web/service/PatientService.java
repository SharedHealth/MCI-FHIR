package org.sharedhealth.mci.web.service;

import org.sharedhealth.mci.web.repository.PatientRepository;

public class PatientService {

    private PatientRepository patientRepository;

    public PatientService(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }




}
