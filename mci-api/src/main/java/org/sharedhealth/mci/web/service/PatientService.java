package org.sharedhealth.mci.web.service;

import ca.uhn.fhir.model.dstu2.resource.Bundle;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sharedhealth.mci.web.exception.PatientNotFoundException;
import org.sharedhealth.mci.web.mapper.FHIRBundleMapper;
import org.sharedhealth.mci.web.mapper.MCIPatientMapper;
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
import java.util.UUID;

import static org.sharedhealth.mci.web.util.JsonMapper.writeValueAsString;

public class PatientService {
    private static final Logger logger = LogManager.getLogger(PatientService.class);
    private MCIPatientMapper mciPatientMapper;
    private FHIRBundleMapper fhirBundleMapper;
    private PatientRepository patientRepository;
    private HealthIdService healthIdService;
    private FhirPatientValidator fhirPatientValidator;

    public PatientService(MCIPatientMapper mciPatientMapper, FHIRBundleMapper fhirBundleMapper, HealthIdService healthIdService,
                          PatientRepository patientRepository, FhirPatientValidator fhirPatientValidator) {
        this.mciPatientMapper = mciPatientMapper;
        this.fhirBundleMapper = fhirBundleMapper;
        this.healthIdService = healthIdService;
        this.patientRepository = patientRepository;
        this.fhirPatientValidator = fhirPatientValidator;
    }

    public Bundle findPatientByHealthId(String healthId) {
        org.sharedhealth.mci.web.model.Patient mciPatient = patientRepository.findByHealthId(healthId);
        if (null == mciPatient) {
            throw new PatientNotFoundException("No patient found with health id: " + healthId);
        }
        return mciPatientMapper.mapPatientToBundle(mciPatient);
    }

    public MCIResponse createPatient(Bundle bundle, UserInfo userInfo) throws AccessDeniedException {
        MCIValidationResult validate = fhirPatientValidator.validate(bundle);
        if (!validate.isSuccessful()) {
            return createMCIResponseForValidationFailure(validate);
        }
        org.sharedhealth.mci.web.model.Patient mciPatient = new org.sharedhealth.mci.web.model.Patient();
        mciPatient = fhirBundleMapper.mapToMCIPatient(bundle);
        MciHealthId healthId;
        healthId = healthIdService.getNextHealthId();
        mciPatient.setHealthId(healthId.getHid());
        UUID createdAt = TimeUuidUtil.uuidForDate(new Date());
        mciPatient.setCreatedAt(createdAt);
        mciPatient.setUpdatedAt(createdAt);
        UserInfo.UserInfoProperties userInfoProperties = userInfo.getProperties();
        String createdBy = writeValueAsString(new Requester(userInfoProperties.getFacilityId(),
                userInfoProperties.getProviderId(), userInfoProperties.getAdminId(),
                userInfoProperties.getName()));
        mciPatient.setCreatedBy(createdBy);
        mciPatient.setUpdatedBy(createdBy);
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
        validationResult.getMessages().forEach(message ->
                mciResponse.addError(new Error(message.getLocationString(), message.getSeverity().getCode(), message.getMessage()))
        );
        return mciResponse;
    }
}
