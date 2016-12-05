package org.sharedhealth.mci.web.mapper;

import ca.uhn.fhir.model.api.ExtensionDt;
import ca.uhn.fhir.model.api.IDatatype;
import ca.uhn.fhir.model.api.IResource;
import ca.uhn.fhir.model.dstu2.composite.*;
import ca.uhn.fhir.model.dstu2.resource.Bundle;
import ca.uhn.fhir.model.dstu2.resource.Patient;
import ca.uhn.fhir.model.dstu2.resource.RelatedPerson;
import ca.uhn.fhir.model.dstu2.valueset.AdministrativeGenderEnum;
import ca.uhn.fhir.model.dstu2.valueset.ContactPointSystemEnum;
import ca.uhn.fhir.model.dstu2.valueset.IdentifierTypeCodesEnum;
import ca.uhn.fhir.model.dstu2.valueset.LinkTypeEnum;
import ca.uhn.fhir.model.primitive.BooleanDt;
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
import org.sharedhealth.mci.web.util.TimeUuidUtil;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.sharedhealth.mci.web.util.FHIRConstants.*;
import static org.sharedhealth.mci.web.util.MCIConstants.*;
import static org.sharedhealth.mci.web.util.PatientFactory.*;

public class MCIPatientMapperTest {
    @Mock
    private MCIProperties mciProperties;
    @Mock
    private MasterDataRepository masterDataRepository;
    private MCIPatientMapper mciPatientMapper;

    private final String healthId = "HID123";

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        mciPatientMapper = new MCIPatientMapper(mciProperties, masterDataRepository);
    }

    @Test
    public void shouldMapMCIPatientWithJustMandatoryFieldsToFHIRBundleWithPatient() throws Exception {
        String mciBaseUrl = "https://mci-registry.com/";
        String patientLinkUri = "https://mci.com/api/v1/patients/";

        when(mciProperties.getMciBaseUrl()).thenReturn(mciBaseUrl);
        when(mciProperties.getPatientLinkUri()).thenReturn(patientLinkUri);

        org.sharedhealth.mci.web.model.Patient mciPatient = createMCIPatientWithMandatoryFields();
        mciPatient.setActive(true);
        mciPatient.setConfidential(false);
        mciPatient.setHealthId(healthId);
        UUID updatedAt = TimeUuidUtil.uuidForDate(new Date());
        mciPatient.setUpdatedAt(updatedAt);
        mciPatient.setCreatedAt(updatedAt);
        Bundle patientBundle = mciPatientMapper.mapPatientToBundle(mciPatient);
        assertNotNull(patientBundle);
        Patient patient = (Patient) patientBundle.getEntryFirstRep().getResource();

        List<IdentifierDt> identifiers = patient.getIdentifier();
        assertEquals(1, identifiers.size());
        IdentifierDt hidIdentifier = identifiers.get(0);
        assertEquals(healthId, hidIdentifier.getValue());
        assertEquals(getMCIPatientURI(mciBaseUrl) + healthId, hidIdentifier.getSystem());

        BoundCodeableConceptDt<IdentifierTypeCodesEnum> type = hidIdentifier.getType();
        CodingDt codingDt = type.getCodingFirstRep();
        assertEquals(getMCIValuesetURI(mciBaseUrl, MCI_PATIENT_IDENTIFIERS_VALUESET), codingDt.getSystem());
        assertEquals(MCI_IDENTIFIER_HID_CODE, codingDt.getCode());

        HumanNameDt name = patient.getNameFirstRep();
        assertEquals(givenName, name.getGivenFirstRep().getValue());
        assertEquals(surName, name.getFamilyFirstRep().getValue());

        assertEquals(AdministrativeGenderEnum.MALE.getCode(), patient.getGender());
        assertEquals(dateOfBirth, patient.getBirthDate());

        AddressDt address = patient.getAddressFirstRep();
        List<ExtensionDt> extensions = address.getUndeclaredExtensionsByUrl(getFhirExtensionUrl(ADDRESS_CODE_EXTENSION_NAME));
        assertEquals(addressLine, address.getLineFirstRep().getValue());
        assertEquals(1, extensions.size());
        StringDt addressCode = (StringDt) extensions.get(0).getValue();
        assertEquals("302618020104", addressCode.getValue());
        assertEquals(countryCode, address.getCountry());

        Patient.Link link = patient.getLinkFirstRep();
        assertEquals(LinkTypeEnum.SEE_ALSO.getCode(), link.getType());
        assertEquals(patientLinkUri + healthId, link.getOther().getReference().getValue());
    }

    @Test
    public void shouldMapMCIPatientToFHIRBundleWithAllFields() throws Exception {
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

        org.sharedhealth.mci.web.model.Patient mciPatient = PatientFactory.createMCIPatientWithAllFields();
        mciPatient.setHealthId(healthId);
        UUID updatedAt = TimeUuidUtil.uuidForDate(new Date());
        mciPatient.setUpdatedAt(updatedAt);
        mciPatient.setCreatedAt(updatedAt);
        Bundle patientBundle = mciPatientMapper.mapPatientToBundle(mciPatient);
        assertNotNull(patientBundle);
        Patient fhirPatient = (Patient) getResourceByType(new Patient().getResourceName(), patientBundle).get(0);

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

        ContactPointDt telecom = fhirPatient.getTelecomFirstRep();
        assertEquals(ContactPointSystemEnum.PHONE.getCode(), telecom.getSystem());
        assertEquals(phoneNo, telecom.getValue());
        assertTrue(fhirPatient.getActive());
        IDatatype deceased = fhirPatient.getDeceased();
        assertTrue(deceased instanceof BooleanDt);
        assertFalse(((BooleanDt) deceased).getValue());

        BooleanDt confidentiality = (BooleanDt) fhirPatient.getUndeclaredExtensionsByUrl(getFhirExtensionUrl(CONFIDENTIALITY_EXTENSION_NAME)).get(0).getValue();
        assertFalse(confidentiality.getValue());

        CodingDt educationCoding = ((CodeableConceptDt) fhirPatient.getUndeclaredExtensionsByUrl(
                getFhirExtensionUrl(EDUCATION_DETAILS_EXTENSION_NAME)).get(0).getValue()).getCodingFirstRep();
        assertCoding(educationCoding, getMCIValuesetURI(mciBaseUrl, MCI_PATIENT_EDUCATION_DETAILS_VALUESET), educationLevel, educationDisplay);

        CodingDt occupationCoding = ((CodeableConceptDt) fhirPatient.getUndeclaredExtensionsByUrl(
                getFhirExtensionUrl(OCCUPATION_EXTENSION_NAME)).get(0).getValue()).getCodingFirstRep();
        assertCoding(occupationCoding, getMCIValuesetURI(mciBaseUrl, MCI_PATIENT_OCCUPATION_VALUESET), occupation, occupationDisplay);

        CodingDt dobTypeCoding = ((CodeableConceptDt) fhirPatient.getUndeclaredExtensionsByUrl(
                getFhirExtensionUrl(DOB_TYPE_EXTENSION_NAME)).get(0).getValue()).getCodingFirstRep();
        assertCoding(dobTypeCoding, getMCIValuesetURI(mciBaseUrl, MCI_PATIENT_DOB_TYPE_VALUESET), dobType, "Declared");

        ArrayList<IResource> relations = getResourceByType(new RelatedPerson().getResourceName(), patientBundle);
        containsRelation(relations, "FTH", fatherDisplay, fatherName, surName);
        containsRelation(relations, "MTH", motherDisplay, motherName, surName);
        containsRelation(relations, "SPS", spouseDisplay, spouseName, surName);
    }

    @Test
    public void shouldMapADeadMCIPatient() throws Exception {
        String mciBaseUrl = "https://mci-registry.com/";
        String patientLinkUri = "https://mci.com/api/v1/patients/";

        when(mciProperties.getMciBaseUrl()).thenReturn(mciBaseUrl);
        when(mciProperties.getPatientLinkUri()).thenReturn(patientLinkUri);

        org.sharedhealth.mci.web.model.Patient mciPatient = createMCIPatientWithMandatoryFields();
        mciPatient.setActive(true);
        mciPatient.setConfidential(false);
        mciPatient.setHealthId(healthId);
        mciPatient.setStatus(PATIENT_STATUS_DEAD);
        UUID updatedAt = TimeUuidUtil.uuidForDate(new Date());
        mciPatient.setUpdatedAt(updatedAt);
        mciPatient.setCreatedAt(updatedAt);
        Date date = new Date();
        mciPatient.setDateOfDeath(date);
        Bundle patientBundle = mciPatientMapper.mapPatientToBundle(mciPatient);
        assertNotNull(patientBundle);
        Patient fhirPatient = (Patient) patientBundle.getEntryFirstRep().getResource();
        assertNotNull(fhirPatient);
        IDatatype deceased = fhirPatient.getDeceased();
        assertTrue(deceased instanceof DateTimeDt);
        assertEquals(date, ((DateTimeDt) deceased).getValue());
    }

    @Test
    public void shouldMapADeadPatientWhenDateOfDeathIsUnknown() throws Exception {
        String mciBaseUrl = "https://mci-registry.com/";
        String patientLinkUri = "https://mci.com/api/v1/patients/";

        when(mciProperties.getMciBaseUrl()).thenReturn(mciBaseUrl);
        when(mciProperties.getPatientLinkUri()).thenReturn(patientLinkUri);

        org.sharedhealth.mci.web.model.Patient mciPatient = createMCIPatientWithMandatoryFields();
        mciPatient.setActive(true);
        mciPatient.setConfidential(false);
        mciPatient.setHealthId(healthId);
        mciPatient.setStatus(PATIENT_STATUS_DEAD);
        UUID updatedAt = TimeUuidUtil.uuidForDate(new Date());
        mciPatient.setUpdatedAt(updatedAt);
        mciPatient.setCreatedAt(updatedAt);
        Bundle patientBundle = mciPatientMapper.mapPatientToBundle(mciPatient);
        assertNotNull(patientBundle);
        Patient fhirPatient = (Patient) patientBundle.getEntryFirstRep().getResource();
        IDatatype deceased = fhirPatient.getDeceased();
        assertTrue(deceased instanceof BooleanDt);
        assertTrue(((BooleanDt) deceased).getValue());
    }

    @Test
    public void shouldMapAPatientWithUnknownStatus() throws Exception {
        String mciBaseUrl = "https://mci-registry.com/";
        String patientLinkUri = "https://mci.com/api/v1/patients/";

        when(mciProperties.getMciBaseUrl()).thenReturn(mciBaseUrl);
        when(mciProperties.getPatientLinkUri()).thenReturn(patientLinkUri);

        org.sharedhealth.mci.web.model.Patient mciPatient = createMCIPatientWithMandatoryFields();
        mciPatient.setActive(true);
        mciPatient.setConfidential(false);
        mciPatient.setHealthId(healthId);
        mciPatient.setStatus(PATIENT_STATUS_UNKNOWN);
        UUID updatedAt = TimeUuidUtil.uuidForDate(new Date());
        mciPatient.setUpdatedAt(updatedAt);
        mciPatient.setCreatedAt(updatedAt);
        Bundle patientBundle = mciPatientMapper.mapPatientToBundle(mciPatient);
        assertNotNull(patientBundle);
        Patient fhirPatient = (Patient) patientBundle.getEntryFirstRep().getResource();
        assertNotNull(fhirPatient);
        IDatatype deceased = fhirPatient.getDeceased();
        assertNull(deceased);
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

    private boolean containsRelation(ArrayList<IResource> relations, String code, String display, String givenName, String surName) {
        return relations.stream().anyMatch(relation -> {
            RelatedPerson relatedPerson = (RelatedPerson) relation;
            CodingDt relationship = relatedPerson.getRelationship().getCodingFirstRep();
            HumanNameDt name = relatedPerson.getName();
            return "http://hl7.org/fhir/v3/RoleCode".equals(relationship.getSystem())
                    && code.equals(relationship.getCode()) && display.equals(relationship.getDisplay()) &&
                    givenName.equals(name.getGivenFirstRep().getValue()) &&
                    surName.equals(name.getFamilyFirstRep().getValue());
        });
    }

    private static ArrayList<IResource> getResourceByType(String resourceName, Bundle bundle) {
        return bundle.getEntry().stream().filter(entry -> resourceName.equals(entry.getResource().getResourceName())).map(Bundle.Entry::getResource).collect(Collectors.toCollection(ArrayList::new));
    }

}