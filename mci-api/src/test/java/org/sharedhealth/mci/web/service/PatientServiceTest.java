package org.sharedhealth.mci.web.service;

import ca.uhn.fhir.model.api.ExtensionDt;
import ca.uhn.fhir.model.dstu2.composite.*;
import ca.uhn.fhir.model.dstu2.resource.Patient.Link;
import ca.uhn.fhir.model.dstu2.valueset.AdministrativeGenderEnum;
import ca.uhn.fhir.model.dstu2.valueset.IdentifierTypeCodesEnum;
import ca.uhn.fhir.model.dstu2.valueset.LinkTypeEnum;
import ca.uhn.fhir.model.primitive.DateDt;
import ca.uhn.fhir.model.primitive.DateTimeDt;
import ca.uhn.fhir.model.primitive.StringDt;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.sharedhealth.mci.web.config.MCIProperties;
import org.sharedhealth.mci.web.model.MCIResponse;
import org.sharedhealth.mci.web.model.MciHealthId;
import org.sharedhealth.mci.web.model.Patient;
import org.sharedhealth.mci.web.repository.PatientRepository;
import org.sharedhealth.mci.web.util.DateUtil;

import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.sharedhealth.mci.web.util.FHIRConstants.*;
import static org.sharedhealth.mci.web.util.MCIConstants.getMCIPatientURI;

public class PatientServiceTest {
    private PatientService patientService;
    @Mock
    private PatientRepository patientRepository;
    @Mock
    private MCIProperties mciProperties;
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
        patientService = new PatientService(healthIdService, patientRepository, mciProperties);
    }

    @Test
    public void shouldMapMCIPatientToFHIRPatient() throws Exception {
        String mciBaseUrl = "https://mci-registry.com/";
        String patientLinkUri = "https://mci.com/api/v1/patients/";

        when(patientRepository.findByHealthId(healthId)).thenReturn(createMCIPatient());
        when(mciProperties.getMciBaseUrl()).thenReturn(mciBaseUrl);
        when(mciProperties.getPatientLinkUri()).thenReturn(patientLinkUri);

        ca.uhn.fhir.model.dstu2.resource.Patient fhirPatient = patientService.findPatientByHealthId(healthId);
        assertNotNull(fhirPatient);

        verify(patientRepository).findByHealthId(healthId);

        List<IdentifierDt> identifiers = fhirPatient.getIdentifier();
        assertEquals(1, identifiers.size());
        IdentifierDt hidIdentifier = identifiers.get(0);
        assertEquals(healthId, hidIdentifier.getValue());
        assertEquals(getMCIPatientURI(mciBaseUrl) + healthId, hidIdentifier.getSystem());

        BoundCodeableConceptDt<IdentifierTypeCodesEnum> type = hidIdentifier.getType();
        CodingDt codingDt = type.getCodingFirstRep();
        assertEquals(getMCIValuesetURI(mciBaseUrl), codingDt.getSystem());
        assertEquals(MCI_IDENTIFIER_HID_CODE, codingDt.getCode());

        HumanNameDt name = fhirPatient.getNameFirstRep();
        assertEquals(givenName, name.getGivenFirstRep().getValue());
        assertEquals(surName, name.getFamilyFirstRep().getValue());

        assertEquals(AdministrativeGenderEnum.MALE.getCode(), fhirPatient.getGender());
        assertEquals(dateOfBirth, fhirPatient.getBirthDate());

        AddressDt address = fhirPatient.getAddressFirstRep();
        List<ExtensionDt> extensions = address.getUndeclaredExtensionsByUrl(getFhirExtensionUrl(ADDRESS_CODE_EXTENSION_NAME));
        assertEquals(addressLine, address.getLineFirstRep().getValue());
        assertEquals(1, extensions.size());
        StringDt addressCode = (StringDt) extensions.get(0).getValue();
        assertEquals("302618020104", addressCode.getValue());
        assertEquals(countryCode, address.getCountry());

        Link link = fhirPatient.getLinkFirstRep();
        assertEquals(LinkTypeEnum.SEE_ALSO.getCode(), link.getType());
        assertEquals(patientLinkUri + healthId, link.getOther().getReference().getValue());
    }

    @Test
    public void shouldCreateAPatient() throws Exception {
        MciHealthId mciHealthId = new MciHealthId(healthId);
        MCIResponse response = new MCIResponse(HttpStatus.SC_CREATED);
        response.setId(healthId);
        when(patientRepository.createPatient(any(Patient.class))).thenReturn(response);
        when(healthIdService.getNextHealthId()).thenReturn(mciHealthId);

        MCIResponse mciResponse = patientService.createPatient(createFHIRPatient());
        assertEquals(response, mciResponse);

        ArgumentCaptor<Patient> argumentCaptor = ArgumentCaptor.forClass(Patient.class);
        verify(patientRepository).createPatient(argumentCaptor.capture());
        Patient patientToBeCreated = argumentCaptor.getValue();
        assertEquals(createMCIPatient(), patientToBeCreated);

        verify(healthIdService).markUsed(mciHealthId);
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