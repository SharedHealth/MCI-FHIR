package org.sharedhealth.mci.web.mapper;

import ca.uhn.fhir.model.api.ExtensionDt;
import ca.uhn.fhir.model.dstu2.composite.*;
import ca.uhn.fhir.model.dstu2.resource.Patient;
import ca.uhn.fhir.model.dstu2.valueset.AdministrativeGenderEnum;
import ca.uhn.fhir.model.dstu2.valueset.ContactPointSystemEnum;
import ca.uhn.fhir.model.dstu2.valueset.LinkTypeEnum;
import ca.uhn.fhir.model.primitive.BooleanDt;
import ca.uhn.fhir.model.primitive.DateDt;
import ca.uhn.fhir.model.primitive.DateTimeDt;
import ca.uhn.fhir.model.primitive.StringDt;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.apache.commons.lang3.StringUtils;
import org.sharedhealth.mci.web.config.MCIProperties;
import org.sharedhealth.mci.web.model.MCIIdentifierEnumBinder;
import org.sharedhealth.mci.web.repository.MasterDataRepository;
import org.sharedhealth.mci.web.util.MCIConstants;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.sharedhealth.mci.web.util.FHIRConstants.*;
import static org.sharedhealth.mci.web.util.MCIConstants.*;
import static org.sharedhealth.mci.web.util.StringUtils.ensureSuffix;

public class PatientMapper {
    private MCIProperties mciProperties;
    private MasterDataRepository masterDataRepository;

    private static final String RELATION_KEY_TYPE = "type";
    private static final String RELATION_KEY_SUR_NAME = "sur_name";
    private static final String RELATION_KEY_GIVEN_NAME = "given_name";
    private static final String MASTER_DATA_EDUCATION_LEVEL_TYPE = "education_level";
    private static final String MASTER_DATA_OCCUPATION_TYPE = "occupation";
    private static final String MASTER_DATA_RELATION_TYPE = "relations";

    private final int ADDRESS_CODE_EACH_LEVEL_LENGTH = 2;
    BidiMap<String, AdministrativeGenderEnum> mciToFhirGenderMap = new DualHashBidiMap<>();
    Map<String, String> mciDOBTypeMap = new HashMap<>();

    public PatientMapper(MCIProperties mciProperties, MasterDataRepository masterDataRepository) {
        this.mciProperties = mciProperties;
        this.masterDataRepository = masterDataRepository;

        mciToFhirGenderMap.put(MCIConstants.MALE, AdministrativeGenderEnum.MALE);
        mciToFhirGenderMap.put(MCIConstants.FEMALE, AdministrativeGenderEnum.FEMALE);
        mciToFhirGenderMap.put(MCIConstants.OTHER, AdministrativeGenderEnum.OTHER);

        mciDOBTypeMap.put("1", "Declared");
        mciDOBTypeMap.put("2", "Verified");
        mciDOBTypeMap.put("3", "Estimated");
    }

    public Patient mapToFHIRPatient(org.sharedhealth.mci.web.model.Patient mciPatient) {
        Patient fhirPatient = new Patient();
        fhirPatient.addName(mapName(mciPatient));
        fhirPatient.setGender(mciToFhirGenderMap.get(mciPatient.getGender()));
        fhirPatient.setBirthDate(mapDateOfBirth(mciPatient));
        fhirPatient.addAddress(mapAddress(mciPatient));
        fhirPatient.addLink(mapPatientReferenceLink(mciPatient.getHealthId()));

        mapAsIdIdentifier(fhirPatient, mciPatient.getHealthId(), MCI_IDENTIFIER_HID_CODE, mciPatient.getHealthId());
        mapAsIdIdentifier(fhirPatient, mciPatient.getNationalId(), MCI_IDENTIFIER_NID_CODE, mciPatient.getHealthId());
        mapAsIdIdentifier(fhirPatient, mciPatient.getBirthRegistrationNumber(), MCI_IDENTIFIER_BRN_CODE, mciPatient.getHealthId());
        mapAsIdIdentifier(fhirPatient, mciPatient.getHouseholdCode(), MCI_IDENTIFIER_HOUSE_HOLD_NUMBER_CODE, mciPatient.getHealthId());
        mapRelationsToContacts(fhirPatient, mciPatient.getRelations());

        if (StringUtils.isNotBlank(mciPatient.getPhoneNo())) {
            fhirPatient.addTelecom().setSystem(ContactPointSystemEnum.PHONE).setValue(mciPatient.getPhoneNo());
        }

        mapDeceased(fhirPatient, mciPatient);
        fhirPatient.setActive(mciPatient.getActive());

        ExtensionDt confidentiality = new ExtensionDt().setUrl(getFhirExtensionUrl(CONFIDENTIALITY_EXTENSION_NAME))
                .setValue(new BooleanDt(mciPatient.getConfidential()));
        fhirPatient.addUndeclaredExtension(confidentiality);
        mapCodingExtensionFor(MASTER_DATA_EDUCATION_LEVEL_TYPE, mciPatient.getEducationLevel(), MCI_PATIENT_EDUCATION_DETAILS_VALUESET, EDUCATION_DETAILS_EXTENSION_NAME, fhirPatient);
        mapCodingExtensionFor(MASTER_DATA_OCCUPATION_TYPE, mciPatient.getOccupation(), MCI_PATIENT_OCCUPATION_VALUESET, OCCUPATION_EXTENSION_NAME, fhirPatient);

        String dobType = mciPatient.getDobType();
        CodingDt dobTypeCoding = new CodingDt(getMCIValuesetURI(mciProperties.getMciBaseUrl(), MCI_PATIENT_DOB_TYPE_VALUESET), dobType);
        dobTypeCoding.setDisplay(mciDOBTypeMap.get(dobType));
        ExtensionDt dobTypeExtension = new ExtensionDt().setUrl(getFhirExtensionUrl(DOB_TYPE_EXTENSION_NAME)).setValue(dobTypeCoding);
        fhirPatient.addUndeclaredExtension(dobTypeExtension);
        return fhirPatient;
    }

    private void mapDeceased(Patient fhirPatient, org.sharedhealth.mci.web.model.Patient mciPatient) {
        if (PATIENT_STATUS_ALIVE.equals(mciPatient.getStatus())) {
            fhirPatient.setDeceased(new BooleanDt(false));
            return;
        }
        if (PATIENT_STATUS_DEAD.equals(mciPatient.getStatus())) {
            if (mciPatient.getDateOfDeath() != null) {
                fhirPatient.setDeceased(new DateTimeDt(mciPatient.getDateOfDeath()));
                return;
            }
            fhirPatient.setDeceased(new BooleanDt(true));
        }
    }

    private void mapCodingExtensionFor(String masterDataType, String masterDataKey, String valuesetName, String extensionName, Patient fhirPatient) {
        if (StringUtils.isBlank(masterDataKey)) return;
        ExtensionDt extension = new ExtensionDt();
        CodingDt coding = new CodingDt(getMCIValuesetURI(mciProperties.getMciBaseUrl(), valuesetName), masterDataKey);
        coding.setDisplay(masterDataRepository.findByTypeAndKey(masterDataType, masterDataKey).getValue());
        extension.setUrl(getFhirExtensionUrl(extensionName)).setValue(coding);
        fhirPatient.addUndeclaredExtension(extension);
    }

    private void mapRelationsToContacts(Patient fhirPatient, String relationsString) {
        if (StringUtils.isBlank(relationsString)) return;
        ObjectMapper objectMapper = new ObjectMapper();
        List<Map> relations;
        try {
            relations = objectMapper.readValue(relationsString, objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));
        } catch (IOException e) {
            throw new RuntimeException("Can not parse patient relations", e);
        }
        if (CollectionUtils.isEmpty(relations)) return;
        String system = getMCIValuesetURI(mciProperties.getMciBaseUrl(), MCI_PATIENT_CONTACT_RELATIONSHIP_VALUESET);
        for (Map relation : relations) {
            String relationType = (String) relation.get(RELATION_KEY_TYPE);
            CodingDt relationCoding = new CodingDt(system, relationType);
            relationCoding.setDisplay(masterDataRepository.findByTypeAndKey(MASTER_DATA_RELATION_TYPE, relationType).getValue());
            fhirPatient.addContact().setName(getName(relation)).addRelationship().addCoding(relationCoding);
        }
    }

    private HumanNameDt getName(Map relation) {
        String givenName = (String) relation.get(RELATION_KEY_GIVEN_NAME);
        Object surName = relation.get(RELATION_KEY_SUR_NAME);
        HumanNameDt humanName = new HumanNameDt();
        humanName.addGiven(givenName);
        if (surName == null) return humanName;
        String familyName = (String) surName;
        if (StringUtils.isBlank(familyName)) return humanName;
        return humanName.addFamily(familyName);
    }

    public org.sharedhealth.mci.web.model.Patient mapToMCIPatient(Patient fhirPatient) {
        org.sharedhealth.mci.web.model.Patient mciPatient = new org.sharedhealth.mci.web.model.Patient();

        HumanNameDt name = fhirPatient.getNameFirstRep();
        mciPatient.setGivenName(name.getGivenFirstRep().getValue());
        mciPatient.setSurName(name.getFamilyFirstRep().getValue());
        mciPatient.setGender(mciToFhirGenderMap.getKey(fhirPatient.getGenderElement().getValueAsEnum()));

        List<ExtensionDt> birthExtensions = fhirPatient.getBirthDateElement().getUndeclaredExtensionsByUrl(BIRTH_TIME_EXTENSION_URL);
        DateTimeDt birthTime;
        if (CollectionUtils.isEmpty(birthExtensions)) {
            birthTime = new DateTimeDt(fhirPatient.getBirthDate());
        } else {
            birthTime = (DateTimeDt) birthExtensions.get(0).getValue();
        }
        mciPatient.setDateOfBirth(birthTime.getValue());

        AddressDt address = fhirPatient.getAddressFirstRep();
        mciPatient.setAddressLine(address.getLineFirstRep().getValue());
        mciPatient.setCountryCode(address.getCountry());
        List<ExtensionDt> extensions = address.getUndeclaredExtensionsByUrl(getFhirExtensionUrl(ADDRESS_CODE_EXTENSION_NAME));
        StringDt addressCode = (StringDt) extensions.get(0).getValue();
        Iterable<String> codes = Splitter.fixedLength(ADDRESS_CODE_EACH_LEVEL_LENGTH).split(addressCode.getValue());
        List<String> addressLevels = Lists.newArrayList(codes);

        setDivision(mciPatient, addressLevels);
        setDistrict(mciPatient, addressLevels);
        setUpazila(mciPatient, addressLevels);
        setCityCorporation(mciPatient, addressLevels);
        setUnionWard(mciPatient, addressLevels);
        setRuralWard(mciPatient, addressLevels);
        return mciPatient;
    }

    private void setRuralWard(org.sharedhealth.mci.web.model.Patient mciPatient, List<String> addressLevels) {
        if (addressLevels.size() > 5) {
            mciPatient.setRuralWardId(addressLevels.get(5));
        }
    }

    private void setUnionWard(org.sharedhealth.mci.web.model.Patient mciPatient, List<String> addressLevels) {
        if (addressLevels.size() > 4) {
            mciPatient.setUnionOrUrbanWardId(addressLevels.get(4));
        }
    }

    private void setCityCorporation(org.sharedhealth.mci.web.model.Patient mciPatient, List<String> addressLevels) {
        if (addressLevels.size() > 3) {
            mciPatient.setCityCorporationId(addressLevels.get(3));
        }
    }

    private void setUpazila(org.sharedhealth.mci.web.model.Patient mciPatient, List<String> addressLevels) {
        if (addressLevels.size() > 2) {
            mciPatient.setUpazilaId(addressLevels.get(2));
        }
    }

    private void setDistrict(org.sharedhealth.mci.web.model.Patient mciPatient, List<String> addressLevels) {
        if (addressLevels.size() > 1) {
            mciPatient.setDistrictId(addressLevels.get(1));
        }
    }

    private void setDivision(org.sharedhealth.mci.web.model.Patient mciPatient, List<String> addressLevels) {
        if (addressLevels.size() > 0) {
            mciPatient.setDivisionId(addressLevels.get(0));
        }
    }

    private DateDt mapDateOfBirth(org.sharedhealth.mci.web.model.Patient mciPatient) {
        DateDt dateOfBirth = new DateDt(mciPatient.getDateOfBirth());
        ExtensionDt extensionDt = new ExtensionDt().setUrl(BIRTH_TIME_EXTENSION_URL)
                .setValue(new DateTimeDt(mciPatient.getDateOfBirth()));
        dateOfBirth.addUndeclaredExtension(extensionDt);
        return dateOfBirth;
    }

    private Patient.Link mapPatientReferenceLink(String healthId) {
        Patient.Link link = new Patient.Link();
        link.setType(LinkTypeEnum.SEE_ALSO);
        String patientLinkUri = ensureSuffix(mciProperties.getPatientLinkUri(), URL_SEPARATOR);
        ResourceReferenceDt patientReference = new ResourceReferenceDt(String.format("%s%s", patientLinkUri, healthId));
        link.setOther(patientReference);
        return link;
    }

    private void mapAsIdIdentifier(Patient fhirPatient, String value, String identifierTypeCode, String healthId) {
        if (StringUtils.isBlank(value)) return;
        String mciPatientURI = getMCIPatientURI(mciProperties.getMciBaseUrl());
        String system = String.format("%s%s", mciPatientURI, healthId);
        IdentifierDt identifierDt = fhirPatient.addIdentifier().setValue(value).setSystem(system);
        setIdentifierType(identifierDt, identifierTypeCode);
    }

    @SuppressWarnings("unchecked")
    private void setIdentifierType(IdentifierDt identifierDt, String identifierCode) {
        BoundCodeableConceptDt identifierType = new BoundCodeableConceptDt<>(new MCIIdentifierEnumBinder());
        String system = getMCIValuesetURI(mciProperties.getMciBaseUrl(), MCI_PATIENT_IDENTIFIERS_VALUESET);
        identifierType.addCoding(new CodingDt(system, identifierCode));
        identifierDt.setType(identifierType);
    }

    private HumanNameDt mapName(org.sharedhealth.mci.web.model.Patient mciPatient) {
        HumanNameDt name = new HumanNameDt();
        name.addGiven(mciPatient.getGivenName());
        name.addFamily(mciPatient.getSurName());
        return name;
    }

    private AddressDt mapAddress(org.sharedhealth.mci.web.model.Patient mciPatient) {
        AddressDt addressDt = new AddressDt().addLine(mciPatient.getAddressLine());
        addressDt.setCountry(mciPatient.getCountryCode());
        ExtensionDt addressCodeExtension = new ExtensionDt().
                setUrl(getFhirExtensionUrl(ADDRESS_CODE_EXTENSION_NAME)).setValue(buildAddressCode(mciPatient));
        addressDt.addUndeclaredExtension(addressCodeExtension);
        return addressDt;
    }

    private StringDt buildAddressCode(org.sharedhealth.mci.web.model.Patient mciPatient) {
        StringBuilder addressCode = new StringBuilder();
        appendAddressPart(addressCode, mciPatient.getDivisionId());
        appendAddressPart(addressCode, mciPatient.getDistrictId());
        appendAddressPart(addressCode, mciPatient.getUpazilaId());
        appendAddressPart(addressCode, mciPatient.getCityCorporationId());
        appendAddressPart(addressCode, mciPatient.getUnionOrUrbanWardId());
        appendAddressPart(addressCode, mciPatient.getRuralWardId());
        return new StringDt(addressCode.toString());
    }

    private void appendAddressPart(StringBuilder addressCode, String addressPart) {
        if (isNotBlank(addressPart)) {
            addressCode.append(format("%s", addressPart));
        }
    }

}
