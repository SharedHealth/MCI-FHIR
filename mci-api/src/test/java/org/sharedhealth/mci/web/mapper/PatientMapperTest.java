package org.sharedhealth.mci.web.mapper;

import ca.uhn.fhir.model.api.ExtensionDt;
import ca.uhn.fhir.model.api.IDatatype;
import ca.uhn.fhir.model.dstu2.composite.*;
import ca.uhn.fhir.model.dstu2.resource.Patient;
import ca.uhn.fhir.model.dstu2.valueset.AdministrativeGenderEnum;
import ca.uhn.fhir.model.dstu2.valueset.ContactPointSystemEnum;
import ca.uhn.fhir.model.dstu2.valueset.IdentifierTypeCodesEnum;
import ca.uhn.fhir.model.dstu2.valueset.LinkTypeEnum;
import ca.uhn.fhir.model.primitive.BooleanDt;
import ca.uhn.fhir.model.primitive.DateDt;
import ca.uhn.fhir.model.primitive.DateTimeDt;
import ca.uhn.fhir.model.primitive.StringDt;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.sharedhealth.mci.web.config.MCIProperties;
import org.sharedhealth.mci.web.model.MasterData;
import org.sharedhealth.mci.web.repository.MasterDataRepository;
import org.sharedhealth.mci.web.util.FHIRConstants;
import org.sharedhealth.mci.web.util.PatientFactory;

import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.sharedhealth.mci.web.util.FHIRConstants.*;
import static org.sharedhealth.mci.web.util.MCIConstants.getMCIPatientURI;
import static org.sharedhealth.mci.web.util.PatientFactory.*;

public class PatientMapperTest {
    @Mock
    private MCIProperties mciProperties;
    @Mock
    private MasterDataRepository masterDataRepository;
    private PatientMapper patientMapper;

    private final String healthId = "HID123";

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        patientMapper = new PatientMapper(mciProperties, masterDataRepository);
    }

    @Test
    public void shouldMapMCIPatientToFHIRPatientWithJustMandatoryFields() throws Exception {
        String mciBaseUrl = "https://mci-registry.com/";
        String patientLinkUri = "https://mci.com/api/v1/patients/";

        when(mciProperties.getMciBaseUrl()).thenReturn(mciBaseUrl);
        when(mciProperties.getPatientLinkUri()).thenReturn(patientLinkUri);

        org.sharedhealth.mci.web.model.Patient mciPatient = createMCIPatientWithMandatoryFields();
        mciPatient.setActive(true);
        mciPatient.setConfidential(false);
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
        assertEquals(getMCIValuesetURI(mciBaseUrl, MCI_PATIENT_IDENTIFIERS_VALUESET), codingDt.getSystem());
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

        Patient.Link link = fhirPatient.getLinkFirstRep();
        assertEquals(LinkTypeEnum.SEE_ALSO.getCode(), link.getType());
        assertEquals(patientLinkUri + healthId, link.getOther().getReference().getValue());
    }

    @Test
    public void shouldMapMCIPatientToFHIRPatientWithAllFields() throws Exception {
        String mciBaseUrl = "https://mci-registry.com/";
        String patientLinkUri = "https://mci.com/api/v1/patients/";

        String educationLevelKey = "education_level";
        String occupationKey = "occupation";
        String relationsKey = "relations";
        String educationDisplay = "Higher Secondary or Equivalent";
        String occupationDisplay = "Teacher";
        String spouseDisplay = "Spouse";
        String fatherDisplay = "Father";
        String motherDisplay = "Mother";

        MasterData educationMasterData = new MasterData(educationLevelKey, educationLevel, educationDisplay);
        MasterData occupationMasterData = new MasterData(occupationKey, occupation, occupationDisplay);

        when(mciProperties.getMciBaseUrl()).thenReturn(mciBaseUrl);
        when(mciProperties.getPatientLinkUri()).thenReturn(patientLinkUri);
        when(masterDataRepository.findByTypeAndKey(educationLevelKey, PatientFactory.educationLevel)).thenReturn(educationMasterData);
        when(masterDataRepository.findByTypeAndKey(occupationKey, PatientFactory.occupation)).thenReturn(occupationMasterData);
        when(masterDataRepository.findByTypeAndKey(relationsKey, "SPS")).thenReturn(new MasterData(relationsKey, "SPS", spouseDisplay));
        when(masterDataRepository.findByTypeAndKey(relationsKey, "MTH")).thenReturn(new MasterData(relationsKey, "MTH", motherDisplay));
        when(masterDataRepository.findByTypeAndKey(relationsKey, "FTH")).thenReturn(new MasterData(relationsKey, "FTH", fatherDisplay));

        org.sharedhealth.mci.web.model.Patient mciPatient = PatientFactory.createMCIPatient();
        mciPatient.setHealthId(healthId);
        ca.uhn.fhir.model.dstu2.resource.Patient fhirPatient = patientMapper.mapToFHIRPatient(mciPatient);
        assertNotNull(fhirPatient);

        List<IdentifierDt> identifiers = fhirPatient.getIdentifier();
        assertEquals(4, identifiers.size());
        containsIdentifier(identifiers, healthId, MCI_IDENTIFIER_HID_CODE);
        containsIdentifier(identifiers, nid, MCI_IDENTIFIER_NID_CODE);
        containsIdentifier(identifiers, brn, MCI_IDENTIFIER_BRN_CODE);
        containsIdentifier(identifiers, householdCode, MCI_IDENTIFIER_HOUSE_HOLD_NUMBER_CODE);

        IdentifierDt hidIdentifier = identifiers.get(0);
        assertEquals(healthId, hidIdentifier.getValue());
        assertEquals(getMCIPatientURI(mciBaseUrl) + healthId, hidIdentifier.getSystem());

        BoundCodeableConceptDt<IdentifierTypeCodesEnum> type = hidIdentifier.getType();
        CodingDt codingDt = type.getCodingFirstRep();
        assertEquals(getMCIValuesetURI(mciBaseUrl, MCI_PATIENT_IDENTIFIERS_VALUESET), codingDt.getSystem());
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

        Patient.Link link = fhirPatient.getLinkFirstRep();
        assertEquals(LinkTypeEnum.SEE_ALSO.getCode(), link.getType());
        assertEquals(patientLinkUri + healthId, link.getOther().getReference().getValue());

        List<Patient.Contact> contactPeople = fhirPatient.getContact();
        assertEquals(3, contactPeople.size());
        assertTrue(containsContact(contactPeople, "FTH", fatherDisplay, fatherName, surName));
        assertTrue(containsContact(contactPeople, "MTH", motherDisplay, motherName, surName));
        assertTrue(containsContact(contactPeople, "SPS", spouseDisplay, spouseName, surName));

        ContactPointDt telecom = fhirPatient.getTelecomFirstRep();
        assertEquals(ContactPointSystemEnum.PHONE.getCode(), telecom.getSystem());
        assertEquals(phoneNo, telecom.getValue());
        assertTrue(fhirPatient.getActive());
        IDatatype deceased = fhirPatient.getDeceased();
        assertTrue(deceased instanceof BooleanDt);
        assertFalse(((BooleanDt) deceased).getValue());

        BooleanDt confidentiality = (BooleanDt) fhirPatient.getUndeclaredExtensionsByUrl(getFhirExtensionUrl(CONFIDENTIALITY_EXTENSION_NAME)).get(0).getValue();
        assertFalse(confidentiality.getValue());

        CodingDt educationCoding = (CodingDt) fhirPatient.getUndeclaredExtensionsByUrl(getFhirExtensionUrl(EDUCATION_DETAILS_EXTENSION_NAME)).get(0).getValue();
        assertCoding(educationCoding, getMCIValuesetURI(mciBaseUrl, MCI_PATIENT_EDUCATION_DETAILS_VALUESET), educationLevel, educationDisplay);

        CodingDt occupationCoding = (CodingDt) fhirPatient.getUndeclaredExtensionsByUrl(getFhirExtensionUrl(OCCUPATION_EXTENSION_NAME)).get(0).getValue();
        assertCoding(occupationCoding, getMCIValuesetURI(mciBaseUrl, MCI_PATIENT_OCCUPATION_VALUESET), occupation, occupationDisplay);

        CodingDt dobTypeCoding = (CodingDt) fhirPatient.getUndeclaredExtensionsByUrl(getFhirExtensionUrl(DOB_TYPE_EXTENSION_NAME)).get(0).getValue();
        assertCoding(dobTypeCoding, getMCIValuesetURI(mciBaseUrl, MCI_PATIENT_DOB_TYPE_VALUESET), dobType, "Declared");
    }

    @Test
    public void shouldMapADeadPatient() throws Exception {
        String mciBaseUrl = "https://mci-registry.com/";
        String patientLinkUri = "https://mci.com/api/v1/patients/";

        when(mciProperties.getMciBaseUrl()).thenReturn(mciBaseUrl);
        when(mciProperties.getPatientLinkUri()).thenReturn(patientLinkUri);

        org.sharedhealth.mci.web.model.Patient mciPatient = createMCIPatientWithMandatoryFields();
        mciPatient.setActive(true);
        mciPatient.setConfidential(false);
        mciPatient.setHealthId(healthId);
        Date date = new Date();
        mciPatient.setDateOfDeath(date);
        ca.uhn.fhir.model.dstu2.resource.Patient fhirPatient = patientMapper.mapToFHIRPatient(mciPatient);
        assertNotNull(fhirPatient);
        IDatatype deceased = fhirPatient.getDeceased();
        assertTrue(deceased instanceof DateTimeDt);
        assertEquals(date, ((DateTimeDt) deceased).getValue());
    }

    @Test
    public void shouldMapFHIRPatientToMCIPatient() throws Exception {
        org.sharedhealth.mci.web.model.Patient mciPatient = patientMapper.mapToMCIPatient(createFHIRPatient(true));
        assertEquals(createMCIPatientWithMandatoryFields(), mciPatient);
    }

    @Test
    public void shouldMapFhirPatientNotHavingBirthTime() throws Exception {
        Patient fhirPatient = createFHIRPatient(false);
        org.sharedhealth.mci.web.model.Patient mciPatient = patientMapper.mapToMCIPatient(fhirPatient);
        org.sharedhealth.mci.web.model.Patient expectedMCIPatient = createMCIPatientWithMandatoryFields();
        expectedMCIPatient.setDateOfBirth(dateOfBirth);
        assertEquals(expectedMCIPatient, mciPatient);
    }

    private ca.uhn.fhir.model.dstu2.resource.Patient createFHIRPatient(boolean timeOfBirthIncluded) {
        ca.uhn.fhir.model.dstu2.resource.Patient patient = new ca.uhn.fhir.model.dstu2.resource.Patient();
        patient.addName().addGiven(givenName).addFamily(surName);
        patient.setGender(AdministrativeGenderEnum.MALE);

        if (timeOfBirthIncluded) {
            DateDt date = new DateDt(dateOfBirth);
            ExtensionDt extensionDt = new ExtensionDt().setUrl(BIRTH_TIME_EXTENSION_URL).setValue(new DateTimeDt(dateOfBirth));
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

    private void assertCoding(CodingDt education, String system, String code, String display) {
        assertEquals(system, education.getSystem());
        assertEquals(code, education.getCode());
        assertEquals(display, education.getDisplay());
    }

    private boolean containsIdentifier(List<IdentifierDt> identifiers, String value, String code) {
        String mciBaseUrl = mciProperties.getMciBaseUrl();
        String mciValuesetURI = FHIRConstants.getMCIValuesetURI(mciBaseUrl, MCI_PATIENT_IDENTIFIERS_VALUESET);
        return identifiers.stream().anyMatch(identifierDt -> {
            CodingDt coding = identifierDt.getType().getCodingFirstRep();
            String expectedPatientURL = getMCIPatientURI(mciBaseUrl) + healthId;
            return code.equals(coding.getCode()) && coding.getSystem().equals(mciValuesetURI)
                    && identifierDt.getValue().equals(value) && identifierDt.getSystem().equals(expectedPatientURL);

        });
    }

    private boolean containsContact(List<Patient.Contact> contactPeople, String code, String display, String givenName, String surName) {
        return contactPeople.stream().anyMatch(contact -> {
            CodingDt relationship = contact.getRelationshipFirstRep().getCodingFirstRep();
            HumanNameDt name = contact.getName();
            return code.equals(relationship.getCode()) && display.equals(relationship.getDisplay()) &&
                    givenName.equals(name.getGivenFirstRep().getValue()) &&
                    surName.equals(name.getFamilyFirstRep().getValue());
        });
    }


    private org.sharedhealth.mci.web.model.Patient createMCIPatientWithMandatoryFields() {
        org.sharedhealth.mci.web.model.Patient expectedPatient = new org.sharedhealth.mci.web.model.Patient();
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