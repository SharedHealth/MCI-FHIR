package org.sharedhealth.mci.web.service;

import ca.uhn.fhir.model.api.ExtensionDt;
import ca.uhn.fhir.model.dstu2.composite.AddressDt;
import ca.uhn.fhir.model.dstu2.resource.Bundle;
import ca.uhn.fhir.model.dstu2.valueset.AdministrativeGenderEnum;
import ca.uhn.fhir.model.primitive.DateDt;
import ca.uhn.fhir.model.primitive.DateTimeDt;
import ca.uhn.fhir.model.primitive.StringDt;
import ca.uhn.fhir.validation.ResultSeverityEnum;
import ca.uhn.fhir.validation.SingleValidationMessage;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.sharedhealth.mci.web.exception.PatientNotFoundException;
import org.sharedhealth.mci.web.mapper.FHIRBundleMapper;
import org.sharedhealth.mci.web.mapper.MCIPatientMapper;
import org.sharedhealth.mci.web.model.Error;
import org.sharedhealth.mci.web.model.MCIResponse;
import org.sharedhealth.mci.web.model.MciHealthId;
import org.sharedhealth.mci.web.model.Patient;
import org.sharedhealth.mci.web.repository.PatientRepository;
import org.sharedhealth.mci.web.security.UserInfo;
import org.sharedhealth.mci.web.security.UserProfile;
import org.sharedhealth.mci.web.util.DateUtil;
import org.sharedhealth.mci.web.validations.FhirPatientValidator;
import org.sharedhealth.mci.web.validations.MCIValidationResult;

import java.util.Date;
import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.sharedhealth.mci.web.security.UserInfo.HRM_MCI_USER_GROUP;
import static org.sharedhealth.mci.web.util.FHIRConstants.*;

public class PatientServiceTest {
    private PatientService patientService;
    @Mock
    private PatientRepository patientRepository;
    @Mock
    private MCIPatientMapper mciPatientMapper;
    @Mock
    private FHIRBundleMapper fhirBundleMapper;
    @Mock
    private HealthIdService healthIdService;
    @Mock
    private FhirPatientValidator fhirPatientValidator;

    private final String healthId = "HID";
    private final String givenName = "Bob the";
    private final String surName = "Builder";
    private final String gender = "M";
    private final Date dateOfBirth = DateUtil.parseDate("1995-07-01 14:20:00+0530");
    private final String countryCode = "050";
    private final String divisionId = "30";
    private final String districtId = "26";
    private final String upazilaId = "18";
    private final String cityId = "02";
    private final String urbanWardId = "01";
    private final String ruralWardId = "04";
    private final String addressLine = "Will Street";

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        patientService = new PatientService(mciPatientMapper, fhirBundleMapper, healthIdService, patientRepository, fhirPatientValidator);
    }

    @Test
    public void shouldGiveFHIRPatientForGivenHID() throws Exception {
        Patient mciPatient = new Patient();
        Bundle expectedFHIRBundle = new Bundle();
        when(patientRepository.findByHealthId(healthId)).thenReturn(mciPatient);
        when(mciPatientMapper.mapPatientToBundle(mciPatient)).thenReturn(expectedFHIRBundle);

        Bundle bundle = patientService.findPatientByHealthId(healthId);

        assertNotNull(bundle);
        assertSame(expectedFHIRBundle, bundle);
        InOrder inOrder = inOrder(patientRepository, mciPatientMapper);
        inOrder.verify(patientRepository).findByHealthId(healthId);
        inOrder.verify(mciPatientMapper).mapPatientToBundle(mciPatient);
    }

    @Test(expected = PatientNotFoundException.class)
    public void shouldThrowErrorWhenPatientNotFound() throws Exception {
        String message = "patient does not exist";
        PatientNotFoundException exception = new PatientNotFoundException(message);
        when(patientRepository.findByHealthId(healthId)).thenThrow(exception);

        try {
            patientService.findPatientByHealthId(healthId);
        } catch (Exception e) {
            assertEquals(message, e.getMessage());
            throw e;
        }
    }

    @Test
    public void shouldCreateAnMCIPatient() throws Exception {
        MCIResponse response = new MCIResponse(HttpStatus.SC_CREATED);
        response.setId(healthId);

        MciHealthId mciHealthId = new MciHealthId(healthId);
        ca.uhn.fhir.model.dstu2.resource.Patient fhirPatient = createFHIRPatient();
        Patient mciPatient = new Patient();

        MCIValidationResult mockValidationResult = mock(MCIValidationResult.class);
        when(fhirPatientValidator.validate(fhirPatient)).thenReturn(mockValidationResult);
        when(mockValidationResult.isSuccessful()).thenReturn(true);

        when(healthIdService.getNextHealthId()).thenReturn(mciHealthId);
        when(fhirBundleMapper.mapToMCIPatient(new Bundle())).thenReturn(mciPatient);
        when(patientRepository.createPatient(mciPatient)).thenReturn(response);
        UserInfo userInfo = getUserInfo();
        MCIResponse mciResponse = patientService.createPatient(fhirPatient, userInfo);
        assertEquals(response, mciResponse);

        ArgumentCaptor<Patient> argumentCaptor = ArgumentCaptor.forClass(Patient.class);
        verify(patientRepository).createPatient(argumentCaptor.capture());
        Patient patientToBeCreated = argumentCaptor.getValue();

        assertSame(mciPatient, patientToBeCreated);
        assertNotNull(patientToBeCreated.getCreatedAt());

        String expectedCreatedBy = "{\"facility\":{\"id\":\"100067\",\"name\":null},\"provider\":null,\"admin\":null}";
        assertEquals(expectedCreatedBy, patientToBeCreated.getCreatedBy());
        assertEquals(expectedCreatedBy, patientToBeCreated.getUpdatedBy());
        assertEquals(healthId, patientToBeCreated.getHealthId());

        InOrder inOrder = inOrder(fhirBundleMapper, healthIdService, patientRepository);
        inOrder.verify(fhirBundleMapper).mapToMCIPatient(new Bundle());
        inOrder.verify(healthIdService).getNextHealthId();
        inOrder.verify(patientRepository).createPatient(patientToBeCreated);
    }

    @Test
    public void shouldThrowAnErrorIfPatientDataIsNotValid() throws Exception {
        ca.uhn.fhir.model.dstu2.resource.Patient fhirPatient = new ca.uhn.fhir.model.dstu2.resource.Patient();
        String invalidGender = "Invalid gender";
        String genderLocation = "/f:Patient/f:gender";
        String invalidDOB = "Invalid DOB";
        String dobLocation = "/f:Patient/f:DOB";
        List<SingleValidationMessage> validationMessages = asList(createMessage(invalidGender, genderLocation), createMessage(invalidDOB, dobLocation));
        MCIValidationResult mockValidationResult = mock(MCIValidationResult.class);

        when(fhirPatientValidator.validate(fhirPatient)).thenReturn(mockValidationResult);
        when(mockValidationResult.isSuccessful()).thenReturn(false);
        when(mockValidationResult.getMessages()).thenReturn(validationMessages);

        MCIResponse mciResponse = patientService.createPatient(fhirPatient, getUserInfo());

        assertNotNull(mciResponse);
        assertEquals(HttpStatus.SC_UNPROCESSABLE_ENTITY, mciResponse.getHttpStatus());
        Error genderError = new Error(genderLocation, "error", invalidGender);
        Error dobError = new Error(dobLocation, "error", invalidDOB);
        assertTrue(mciResponse.getErrors().contains(genderError));
        assertTrue(mciResponse.getErrors().contains(dobError));
    }

    private SingleValidationMessage createMessage(String theMessage, String locationString) {
        SingleValidationMessage singleValidationMessage = new SingleValidationMessage();
        singleValidationMessage.setMessage(theMessage);
        singleValidationMessage.setLocationString(locationString);
        singleValidationMessage.setSeverity(ResultSeverityEnum.ERROR);
        return singleValidationMessage;
    }

    private ca.uhn.fhir.model.dstu2.resource.Patient createFHIRPatient() {
        ca.uhn.fhir.model.dstu2.resource.Patient patient = new ca.uhn.fhir.model.dstu2.resource.Patient();
        patient.addName().addGiven(givenName).addFamily(surName);
        patient.setGender(AdministrativeGenderEnum.MALE);

        DateDt date = new DateDt(dateOfBirth);
        ExtensionDt extensionDt = new ExtensionDt().setUrl(BIRTH_TIME_EXTENSION_URL).setValue(new DateTimeDt(dateOfBirth));
        date.addUndeclaredExtension(extensionDt);
        patient.setBirthDate(date);

        AddressDt addressDt = new AddressDt().addLine(addressLine);
        addressDt.setCountry(countryCode);
        String addressCode = String.format("%s%s%s%s%s%s", divisionId, districtId, upazilaId, cityId, urbanWardId, ruralWardId);
        ExtensionDt addressCodeExtension = new ExtensionDt().
                setUrl(getFhirExtensionUrl(ADDRESS_CODE_EXTENSION_NAME)).setValue(new StringDt(addressCode));
        addressDt.addUndeclaredExtension(addressCodeExtension);
        patient.addAddress(addressDt);
        return patient;
    }

    private UserInfo getUserInfo() {
        UserProfile userProfile = new UserProfile("facility", "100067", null);
        return new UserInfo("102", "ABC", "abc@mail", 1, true, "111100", asList(HRM_MCI_USER_GROUP), asList(userProfile));
    }
}