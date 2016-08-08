package org.sharedhealth.mci.web.service;

import ca.uhn.fhir.model.api.ExtensionDt;
import ca.uhn.fhir.model.dstu2.composite.AddressDt;
import ca.uhn.fhir.model.dstu2.valueset.AdministrativeGenderEnum;
import ca.uhn.fhir.model.primitive.DateDt;
import ca.uhn.fhir.model.primitive.DateTimeDt;
import ca.uhn.fhir.model.primitive.StringDt;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.sharedhealth.mci.web.exception.PatientNotFoundException;
import org.sharedhealth.mci.web.mapper.PatientMapper;
import org.sharedhealth.mci.web.model.MCIResponse;
import org.sharedhealth.mci.web.model.MciHealthId;
import org.sharedhealth.mci.web.model.Patient;
import org.sharedhealth.mci.web.repository.PatientRepository;
import org.sharedhealth.mci.web.util.DateUtil;

import java.util.Date;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.sharedhealth.mci.web.util.FHIRConstants.*;

public class PatientServiceTest {
    private PatientService patientService;
    @Mock
    private PatientRepository patientRepository;
    @Mock
    private PatientMapper patientMapper;
    @Mock
    private HealthIdService healthIdService;

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
        patientService = new PatientService(patientMapper, healthIdService, patientRepository);
    }

    @Test
    public void shouldGiveFHIRPatientForGivenHID() throws Exception {
        Patient mciPatient = new Patient();
        ca.uhn.fhir.model.dstu2.resource.Patient expectedFHIRPatient = new ca.uhn.fhir.model.dstu2.resource.Patient();
        when(patientRepository.findByHealthId(healthId)).thenReturn(mciPatient);
        when(patientMapper.mapToFHIRPatient(mciPatient)).thenReturn(expectedFHIRPatient);

        ca.uhn.fhir.model.dstu2.resource.Patient fhirPatient = patientService.findPatientByHealthId(healthId);

        assertNotNull(fhirPatient);
        assertSame(expectedFHIRPatient, fhirPatient);
        InOrder inOrder = inOrder(patientRepository, patientMapper);
        inOrder.verify(patientRepository).findByHealthId(healthId);
        inOrder.verify(patientMapper).mapToFHIRPatient(mciPatient);
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

        when(healthIdService.getNextHealthId()).thenReturn(mciHealthId);
        when(patientMapper.mapToMCIPatient(fhirPatient)).thenReturn(mciPatient);
        when(patientRepository.createPatient(mciPatient)).thenReturn(response);

        MCIResponse mciResponse = patientService.createPatient(fhirPatient);
        assertEquals(response, mciResponse);

        ArgumentCaptor<Patient> argumentCaptor = ArgumentCaptor.forClass(Patient.class);
        verify(patientRepository).createPatient(argumentCaptor.capture());
        Patient patientToBeCreated = argumentCaptor.getValue();

        assertSame(mciPatient, patientToBeCreated);
        assertEquals(healthId, patientToBeCreated.getHealthId());

        InOrder inOrder = inOrder(patientMapper, healthIdService, patientRepository);
        inOrder.verify(patientMapper).mapToMCIPatient(fhirPatient);
        inOrder.verify(healthIdService).getNextHealthId();
        inOrder.verify(patientRepository).createPatient(mciPatient);
        inOrder.verify(healthIdService).markUsed(mciHealthId);
    }

    @Test
    public void shouldThrowAnErrorWhenNoHIDAvailable() throws Exception {
        String message = "No HIDs available to assign";
        when(healthIdService.getNextHealthId()).thenThrow(new RuntimeException(message));

        MCIResponse mciResponse = patientService.createPatient(new ca.uhn.fhir.model.dstu2.resource.Patient());

        assertEquals(message, mciResponse.getMessage());
        assertEquals(SC_BAD_REQUEST, mciResponse.getHttpStatus());
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

    private Patient createMCIPatient() {
        Patient expectedPatient = new Patient();
        expectedPatient.setHealthId(healthId);
        expectedPatient.setGivenName(givenName);
        expectedPatient.setSurName(surName);
        expectedPatient.setGender(gender);
        expectedPatient.setDateOfBirth(dateOfBirth);
        expectedPatient.setCountryCode(countryCode);
        expectedPatient.setDivisionId(divisionId);
        expectedPatient.setDistrictId(districtId);
        expectedPatient.setUpazilaId(upazilaId);
        expectedPatient.setCityCorporationId(cityId);
        expectedPatient.setUnionOrUrbanWardId(urbanWardId);
        expectedPatient.setRuralWardId(ruralWardId);
        expectedPatient.setAddressLine(addressLine);
        return expectedPatient;
    }
}