package org.sharedhealth.mci.web.mapper;

import ca.uhn.fhir.model.api.ExtensionDt;
import ca.uhn.fhir.model.dstu2.composite.*;
import ca.uhn.fhir.model.dstu2.resource.Patient;
import ca.uhn.fhir.model.dstu2.valueset.AdministrativeGenderEnum;
import ca.uhn.fhir.model.dstu2.valueset.IdentifierTypeCodesEnum;
import ca.uhn.fhir.model.dstu2.valueset.LinkTypeEnum;
import ca.uhn.fhir.model.primitive.DateDt;
import ca.uhn.fhir.model.primitive.DateTimeDt;
import ca.uhn.fhir.model.primitive.StringDt;
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
        ca.uhn.fhir.model.dstu2.resource.Patient fhirPatient = patientMapper.mapToFHIRPatient(mciPatient);
        assertNotNull(fhirPatient);

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
        assertEquals(dateTimeOfBirth, fhirPatient.getBirthDate());

        AddressDt address = fhirPatient.getAddressFirstRep();
        List<ExtensionDt> extensions = address.getUndeclaredExtensionsByUrl(getFhirExtensionUrl(ADDRESS_CODE_EXTENSION_NAME));
        assertEquals(addressLine, address.getLineFirstRep().getValue());
        assertEquals(1, extensions.size());
        StringDt addressCode = (StringDt) extensions.get(0).getValue();
        assertEquals("302618020104", addressCode.getValue());
        assertEquals(countryCode, address.getCountry());

        Patient.Link link = fhirPatient.getLinkFirstRep();
        assertEquals(LinkTypeEnum.SEE_ALSO.getCode(), link.getType());
        assertEquals(patientLinkUri + healthId, link.getOther().getReference().getValue());
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

    private ca.uhn.fhir.model.dstu2.resource.Patient createFHIRPatient(boolean timeOfBirthIncluded) {
        ca.uhn.fhir.model.dstu2.resource.Patient patient = new ca.uhn.fhir.model.dstu2.resource.Patient();
        patient.addName().addGiven(givenName).addFamily(surName);
        patient.setGender(AdministrativeGenderEnum.MALE);

        if (timeOfBirthIncluded) {
            DateDt date = new DateDt(dateTimeOfBirth);
            ExtensionDt extensionDt = new ExtensionDt().setUrl(BIRTH_TIME_EXTENSION_URL).setValue(new DateTimeDt(dateTimeOfBirth));
            date.addUndeclaredExtension(extensionDt);
            patient.setBirthDate(date);
        } else {
            patient.setBirthDate(new DateDt(dateOfBirth));
        }

        AddressDt addressDt = new AddressDt().addLine(addressLine);
        addressDt.setCountry(countryCode);
        String addressCode = String.format("%s%s%s%s%s%s", divisionId, districtId, upazilaId, cityId, urbanWardId, ruralWardId);
        ExtensionDt addressCodeExtension = new ExtensionDt().
                setUrl(getFhirExtensionUrl(ADDRESS_CODE_EXTENSION_NAME)).setValue(new StringDt(addressCode));
        addressDt.addUndeclaredExtension(addressCodeExtension);
        patient.addAddress(addressDt);
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