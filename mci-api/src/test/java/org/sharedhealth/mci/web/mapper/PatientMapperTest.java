package org.sharedhealth.mci.web.mapper;

import org.hl7.fhir.dstu3.model.*;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.sharedhealth.mci.web.config.MCIProperties;
import org.sharedhealth.mci.web.util.DateUtil;

import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.sharedhealth.mci.web.util.FHIRConstants.*;
import static org.sharedhealth.mci.web.util.MCIConstants.getMCIPatientURI;

public class PatientMapperTest {
    @Mock
    private MCIProperties mciProperties;
    private PatientMapper patientMapper;

    private final String healthId = "HID";
    private final String givenName = "Bob the";
    private final String surName = "Builder";
    private final String gender = "M";
    private final Date dateTimeOfBirth = DateUtil.parseDate("1995-07-01 14:20:00+0530");
    private final Date dateOfBirth = DateUtil.parseDate("1995-07-01");
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
        patientMapper = new PatientMapper(mciProperties);
    }

    @Test
    public void shouldMapMCIPatientToFHIRPatient() throws Exception {
        String mciBaseUrl = "https://mci-registry.com/";
        String patientLinkUri = "https://mci.com/api/v1/patients/";

        when(mciProperties.getMciBaseUrl()).thenReturn(mciBaseUrl);
        when(mciProperties.getPatientLinkUri()).thenReturn(patientLinkUri);

        org.sharedhealth.mci.web.model.Patient mciPatient = createMCIPatient();
        mciPatient.setHealthId(healthId);
        org.hl7.fhir.dstu3.model.Patient fhirPatient = patientMapper.mapToFHIRPatient(mciPatient);
        assertNotNull(fhirPatient);

        List<Identifier> identifiers = fhirPatient.getIdentifier();
        assertEquals(1, identifiers.size());
        Identifier hidIdentifier = identifiers.get(0);
        assertEquals(healthId, hidIdentifier.getValue());
        assertEquals(getMCIPatientURI(mciBaseUrl) + healthId, hidIdentifier.getSystem());

        CodeableConcept type = hidIdentifier.getType();
        Coding coding = type.getCodingFirstRep();
        assertEquals(getMCIValuesetURI(mciBaseUrl), coding.getSystem());
        assertEquals(MCI_IDENTIFIER_HID_CODE, coding.getCode());

        HumanName name = fhirPatient.getNameFirstRep();
        assertEquals(givenName, name.getGiven().get(0).getValue());
        assertEquals(surName, name.getFamily().get(0).getValue());

        assertEquals(Enumerations.AdministrativeGender.MALE, fhirPatient.getGender());
        assertEquals(dateTimeOfBirth, fhirPatient.getBirthDate());

        Address address = fhirPatient.getAddressFirstRep();
        List<Extension> extensions = address.getExtensionsByUrl(getFhirExtensionUrl(ADDRESS_CODE_EXTENSION_NAME));
        assertEquals(addressLine, address.getLine().get(0).getValue());
        assertEquals(1, extensions.size());
        StringType addressCode = (StringType) extensions.get(0).getValue();
        assertEquals("302618020104", addressCode.getValue());
        assertEquals(countryCode, address.getCountry());

        Patient.PatientLinkComponent link = fhirPatient.getLinkFirstRep();
        assertEquals(Patient.LinkType.SEEALSO, link.getType());
        assertEquals(patientLinkUri + healthId, link.getOther().getReference());
    }

    @Test
    public void shouldMapFHIRPatientToMCIPatient() throws Exception {
        org.sharedhealth.mci.web.model.Patient mciPatient = patientMapper.mapToMCIPatient(createFHIRPatient(true));
        assertEquals(createMCIPatient(), mciPatient);
    }

    @Test
    public void shouldMapFhirPatientNotHavingBirthTime() throws Exception {
        Patient fhirPatient = createFHIRPatient(false);
        org.sharedhealth.mci.web.model.Patient mciPatient = patientMapper.mapToMCIPatient(fhirPatient);
        org.sharedhealth.mci.web.model.Patient expectedMCIPatient = createMCIPatient();
        expectedMCIPatient.setDateOfBirth(dateOfBirth);
        assertEquals(expectedMCIPatient, mciPatient);
    }

    private org.hl7.fhir.dstu3.model.Patient createFHIRPatient(boolean timeOfBirthIncluded) {
        org.hl7.fhir.dstu3.model.Patient patient = new org.hl7.fhir.dstu3.model.Patient();
        patient.addName().addGiven(givenName).addFamily(surName);
        patient.setGender(Enumerations.AdministrativeGender.MALE);

        if (timeOfBirthIncluded) {
            DateType date = new DateType(dateTimeOfBirth);
            Extension extension = new Extension().setUrl(BIRTH_TIME_EXTENSION_URL).setValue(new DateTimeType(dateTimeOfBirth));
            date.addExtension(extension);
            patient.setBirthDate(date.getValue());
        } else {
            patient.setBirthDate(dateOfBirth);
        }

        Address address = new Address().addLine(addressLine);
        address.setCountry(countryCode);
        String addressCode = String.format("%s%s%s%s%s%s", divisionId, districtId, upazilaId, cityId, urbanWardId, ruralWardId);
        Extension addressCodeExtension = new Extension().
                setUrl(getFhirExtensionUrl(ADDRESS_CODE_EXTENSION_NAME)).setValue(new StringType(addressCode));
        address.addExtension(addressCodeExtension);
        patient.addAddress(address);
        return patient;
    }


    private org.sharedhealth.mci.web.model.Patient createMCIPatient() {
        org.sharedhealth.mci.web.model.Patient expectedPatient = new org.sharedhealth.mci.web.model.Patient();
        expectedPatient.setGivenName(givenName);
        expectedPatient.setSurName(surName);
        expectedPatient.setGender(gender);
        expectedPatient.setDateOfBirth(dateTimeOfBirth);
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