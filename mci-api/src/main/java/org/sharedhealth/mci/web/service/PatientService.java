package org.sharedhealth.mci.web.service;

import ca.uhn.fhir.model.dstu2.resource.Patient;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sharedhealth.mci.web.mapper.PatientMapper;
import org.sharedhealth.mci.web.model.Error;
import org.sharedhealth.mci.web.model.MCIResponse;
import org.sharedhealth.mci.web.model.MciHealthId;
import org.sharedhealth.mci.web.repository.PatientRepository;
import org.sharedhealth.mci.web.validations.FhirPatientValidator;
import org.sharedhealth.mci.web.validations.MCIValidationResult;

public class PatientService {
    private PatientMapper patientMapper;
    private PatientRepository patientRepository;
    private HealthIdService healthIdService;
    private FhirPatientValidator fhirPatientValidator;

    private static final Logger logger = LogManager.getLogger(PatientService.class);

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
        MCIResponse mciResponse = null;
        try {
            mciResponse = patientRepository.createPatient(mciPatient);
        } catch (Exception e) {
            String message = "Error while creating patient: ";
            logger.error(message, e);
            healthIdService.putBack(healthId);
            MCIResponse response = new MCIResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            response.setMessage(message + e.getMessage());
            return response;
        }
        healthIdService.markUsed(healthId);
        return mciResponse;
    }

    private MCIResponse createMCIResponseForValidationFailure(MCIValidationResult validationResult) {
        MCIResponse mciResponse = new MCIResponse(HttpStatus.SC_UNPROCESSABLE_ENTITY);
        mciResponse.setMessage("Validation Failed");
        validationResult.getMessages().stream().forEach(message ->
                        mciResponse.addError(new Error(message.getLocationString(), message.getSeverity().getCode(), message.getMessage()))
        );
        return mciResponse;
    }
}
