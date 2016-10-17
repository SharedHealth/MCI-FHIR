package org.sharedhealth.mci.web.service;

import ca.uhn.fhir.model.dstu2.resource.Patient;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sharedhealth.mci.web.exception.PatientNotFoundException;
import org.sharedhealth.mci.web.mapper.PatientMapper;
import org.sharedhealth.mci.web.model.Error;
import org.sharedhealth.mci.web.model.MCIResponse;
import org.sharedhealth.mci.web.model.MciHealthId;
import org.sharedhealth.mci.web.model.Requester;
import org.sharedhealth.mci.web.repository.PatientRepository;
import org.sharedhealth.mci.web.security.UserInfo;
import org.sharedhealth.mci.web.util.TimeUuidUtil;
import org.sharedhealth.mci.web.validations.FhirPatientValidator;
import org.sharedhealth.mci.web.validations.MCIValidationResult;


import java.nio.file.AccessDeniedException;
import java.util.Date;

import static org.sharedhealth.mci.web.util.JsonMapper.writeValueAsString;

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
        if (null == mciPatient) {
            throw new PatientNotFoundException("No patient found with health id: " + healthId);
        }
        return patientMapper.mapToFHIRPatient(mciPatient);
    }

    public MCIResponse createPatient(Patient fhirPatient, UserInfo userInfo) throws AccessDeniedException {
        MCIValidationResult validate = fhirPatientValidator.validate(fhirPatient);
        if (!validate.isSuccessful()) {
            return createMCIResponseForValidationFailure(validate);
        }
        org.sharedhealth.mci.web.model.Patient mciPatient = patientMapper.mapToMCIPatient(fhirPatient);
        MciHealthId healthId;
        healthId = healthIdService.getNextHealthId();
        mciPatient.setHealthId(healthId.getHid());
        mciPatient.setCreatedAt(TimeUuidUtil.uuidForDate(new Date()));
        UserInfo.UserInfoProperties userInfoProperties = userInfo.getProperties();
        mciPatient.setCreatedBy(writeValueAsString(new Requester(userInfoProperties.getFacilityId(), userInfoProperties.getProviderId(), userInfoProperties.getAdminId(), userInfoProperties.getName())));
        MCIResponse mciResponse = null;
        try {
            mciResponse = patientRepository.createPatient(mciPatient);
        } catch (Exception e) {
            logger.error("Error while creating patient: " + e.getMessage(), e);
            healthIdService.putBack(healthId);
            return getMciResponse("Error while creating patient: " + e.getMessage(),
                    HttpStatus.SC_INTERNAL_SERVER_ERROR);
        }
        return mciResponse;
    }

    private MCIResponse getMciResponse(String message, int status) {
        MCIResponse response = new MCIResponse(status);
        response.setMessage(message);
        return response;
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
