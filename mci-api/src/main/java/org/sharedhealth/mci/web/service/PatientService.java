package org.sharedhealth.mci.web.service;

import ca.uhn.fhir.model.dstu2.resource.Patient;
import org.apache.http.HttpStatus;
import org.sharedhealth.mci.web.mapper.PatientMapper;
import org.sharedhealth.mci.web.model.Error;
import org.sharedhealth.mci.web.model.MCIResponse;
import org.sharedhealth.mci.web.model.MciHealthId;
import org.sharedhealth.mci.web.repository.PatientRepository;
import org.sharedhealth.mci.web.validations.FhirPatientValidator;
import org.sharedhealth.mci.web.validations.MCIValidationResult;

import java.util.List;
import java.util.stream.Collectors;

public class PatientService {
    private PatientMapper patientMapper;
    private PatientRepository patientRepository;
    private HealthIdService healthIdService;
    private FhirPatientValidator fhirPatientValidator;

    public PatientService(PatientMapper patientMapper, HealthIdService healthIdService, PatientRepository patientRepository, FhirPatientValidator fhirPatientValidator) {
        this.patientMapper = patientMapper;
        this.healthIdService = healthIdService;
        this.patientRepository = patientRepository;
        this.fhirPatientValidator = fhirPatientValidator;
    }

    public Patient findPatientByHealthId(String healthId) {
        org.sharedhealth.mci.web.model.Patient mciPatient = patientRepository.findByHealthId(healthId);
        return patientMapper.mapToFHIRPatient(mciPatient);
    }

    public MCIResponse createPatient(Patient fhirPatient) {
        MCIValidationResult validate = fhirPatientValidator.validate(fhirPatient);
        if (!validate.isSuccessful()) {
            return createMCIResponseForValidationFailure(validate);
        }
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

    private MCIResponse createMCIResponseForValidationFailure(MCIValidationResult validationResult) {
        MCIResponse mciResponse = new MCIResponse(HttpStatus.SC_UNPROCESSABLE_ENTITY);

        List<Error> errors = validationResult.getMessages().stream().map(message ->
                        new Error(message.getLocationString(), message.getSeverity().getCode(), message.getMessage())
        ).collect(Collectors.<Error>toList());

        mciResponse.setMessage(errors.toString());
        return mciResponse;
    }
}
