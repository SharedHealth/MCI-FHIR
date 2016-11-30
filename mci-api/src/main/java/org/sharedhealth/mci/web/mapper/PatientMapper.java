package org.sharedhealth.mci.web.mapper;

import ca.uhn.fhir.model.api.ExtensionDt;
import ca.uhn.fhir.model.api.ResourceMetadataKeyEnum;
import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import ca.uhn.fhir.model.base.resource.ResourceMetadataMap;
import ca.uhn.fhir.model.dstu2.composite.*;
import ca.uhn.fhir.model.dstu2.resource.Bundle;
import ca.uhn.fhir.model.dstu2.resource.Patient;
import ca.uhn.fhir.model.dstu2.resource.RelatedPerson;
import ca.uhn.fhir.model.dstu2.valueset.AdministrativeGenderEnum;
import ca.uhn.fhir.model.dstu2.valueset.BundleTypeEnum;
import ca.uhn.fhir.model.dstu2.valueset.ContactPointSystemEnum;
import ca.uhn.fhir.model.dstu2.valueset.LinkTypeEnum;
import ca.uhn.fhir.model.primitive.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.apache.commons.lang3.StringUtils;
import org.sharedhealth.mci.web.config.MCIProperties;
import org.sharedhealth.mci.web.model.MCIIdentifierEnumBinder;
import org.sharedhealth.mci.web.model.Relation;
import org.sharedhealth.mci.web.repository.MasterDataRepository;
import org.sharedhealth.mci.web.util.FHIRConstants;
import org.sharedhealth.mci.web.util.MCIConstants;
import org.sharedhealth.mci.web.util.TimeUuidUtil;

import java.io.IOException;
import java.util.*;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.sharedhealth.mci.web.util.FHIRConstants.*;
import static org.sharedhealth.mci.web.util.MCIConstants.*;
import static org.sharedhealth.mci.web.util.StringUtils.ensureSuffix;

public class PatientMapper {
    private MCIProperties mciProperties;
    private MasterDataRepository masterDataRepository;

    private static final String MASTER_DATA_EDUCATION_LEVEL_TYPE = "education_level";
    private static final String MASTER_DATA_OCCUPATION_TYPE = "occupation";
    private static final String MASTER_DATA_RELATION_TYPE = "relations";

    private final int ADDRESS_CODE_EACH_LEVEL_LENGTH = 2;
    private BidiMap<String, AdministrativeGenderEnum> mciToFhirGenderMap = new DualHashBidiMap<>();
    private Map<String, String> mciDOBTypeMap = new HashMap<>();

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

    public Bundle mapPatientToBundle(org.sharedhealth.mci.web.model.Patient mciPatient) {
        Bundle bundle = new Bundle();
        bundle.setType(BundleTypeEnum.COLLECTION);
        bundle.setId(UUID.randomUUID().toString());
        ResourceMetadataMap metadataMap = new ResourceMetadataMap();
        metadataMap.put(ResourceMetadataKeyEnum.UPDATED, new InstantDt(TimeUuidUtil.getDateFromUUID(mciPatient.getUpdatedAt()), TemporalPrecisionEnum.MILLI));
        bundle.setResourceMetadata(metadataMap);
        mapPatientAndAddToBundle(mciPatient, bundle);
        return bundle;
    }

    private void mapPatientAndAddToBundle(org.sharedhealth.mci.web.model.Patient mciPatient, Bundle bundle) {
        Patient fhirPatient = new Patient();
        String patientEntryUri = TimeUuidUtil.uuidForDate(new Date()).toString();
        bundle.addEntry().setResource(fhirPatient).setFullUrl(createFullUrlFromUUID(patientEntryUri));

        fhirPatient.addName(new HumanNameDt().addGiven(mciPatient.getGivenName()).addFamily(mciPatient.getSurName()));
        fhirPatient.setGender(mciToFhirGenderMap.get(mciPatient.getGender()));
        fhirPatient.setBirthDate(mapDateOfBirth(mciPatient));
        fhirPatient.addAddress(mapAddress(mciPatient));
        fhirPatient.addLink(mapPatientReferenceLink(mciPatient.getHealthId()));

        mapAsIdentifier(fhirPatient, mciPatient.getHealthId(), MCI_IDENTIFIER_HID_CODE, mciPatient.getHealthId());
        mapAsIdentifier(fhirPatient, mciPatient.getNationalId(), MCI_IDENTIFIER_NID_CODE, mciPatient.getHealthId());
        mapAsIdentifier(fhirPatient, mciPatient.getBirthRegistrationNumber(), MCI_IDENTIFIER_BRN_CODE, mciPatient.getHealthId());
        mapAsIdentifier(fhirPatient, mciPatient.getHouseholdCode(), MCI_IDENTIFIER_HOUSE_HOLD_NUMBER_CODE, mciPatient.getHealthId());

        if (StringUtils.isNotBlank(mciPatient.getPhoneNo())) {
            fhirPatient.addTelecom().setSystem(ContactPointSystemEnum.PHONE).setValue(mciPatient.getPhoneNo());
        }

        mapDeceased(fhirPatient, mciPatient);
        fhirPatient.setActive(mciPatient.getActive());

        ExtensionDt confidentiality = new ExtensionDt().setUrl(getFhirExtensionUrl(CONFIDENTIALITY_EXTENSION_NAME))
                .setValue(new BooleanDt(mciPatient.getConfidential()));
        fhirPatient.addUndeclaredExtension(confidentiality);
        mapCodeableConceptExtensionFor(MASTER_DATA_EDUCATION_LEVEL_TYPE, mciPatient.getEducationLevel(), MCI_PATIENT_EDUCATION_DETAILS_VALUESET, EDUCATION_DETAILS_EXTENSION_NAME, fhirPatient);
        mapCodeableConceptExtensionFor(MASTER_DATA_OCCUPATION_TYPE, mciPatient.getOccupation(), MCI_PATIENT_OCCUPATION_VALUESET, OCCUPATION_EXTENSION_NAME, fhirPatient);

        String dobType = mciPatient.getDobType();
        CodingDt dobTypeCoding = new CodingDt(getMCIValuesetURI(mciProperties.getMciBaseUrl(), MCI_PATIENT_DOB_TYPE_VALUESET), dobType);
        dobTypeCoding.setDisplay(mciDOBTypeMap.get(dobType));
        ExtensionDt dobTypeExtension = new ExtensionDt()
                .setUrl(getFhirExtensionUrl(DOB_TYPE_EXTENSION_NAME))
                .setValue(new CodeableConceptDt().addCoding(dobTypeCoding));
        fhirPatient.addUndeclaredExtension(dobTypeExtension);

        mapRelationsAndAddToBundle(mciPatient.getRelations(), bundle, patientEntryUri);
    }

    private String createFullUrlFromUUID(String patientEntryUri) {
        return "urn:uuid:" + patientEntryUri;
    }

    private void mapDeceased(Patient fhirPatient, org.sharedhealth.mci.web.model.Patient mciPatient) {
        if (PATIENT_STATUS_UNKNOWN.equals(mciPatient.getStatus())) return;
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

    private void mapCodeableConceptExtensionFor(String masterDataType, String masterDataKey, String valuesetName, String extensionName, Patient fhirPatient) {
        if (StringUtils.isBlank(masterDataKey)) return;
        ExtensionDt extension = new ExtensionDt();
        CodingDt coding = new CodingDt(getMCIValuesetURI(mciProperties.getMciBaseUrl(), valuesetName), masterDataKey);
        coding.setDisplay(masterDataRepository.findByTypeAndKey(masterDataType, masterDataKey).getValue());
        extension.setUrl(getFhirExtensionUrl(extensionName)).setValue(new CodeableConceptDt().addCoding(coding));
        fhirPatient.addUndeclaredExtension(extension);
    }

    private void mapRelationsAndAddToBundle(String relationsString, Bundle bundle, String patientEntryUri) {
        if (StringUtils.isBlank(relationsString)) return;
        ObjectMapper objectMapper = new ObjectMapper();
        List<Relation> relations;
        try {
            relations = objectMapper.readValue(relationsString,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, Relation.class));
        } catch (IOException e) {
            throw new RuntimeException("Can not parse patient relations", e);
        }
        if (CollectionUtils.isEmpty(relations)) return;
        for (Relation relation : relations) {
            RelatedPerson relatedPerson = new RelatedPerson().setPatient(new ResourceReferenceDt(createFullUrlFromUUID(patientEntryUri)));

            mapAsIdentifier(relatedPerson, relation.getNationalId(), MCI_IDENTIFIER_NID_CODE, relation.getHealthId());
            mapAsIdentifier(relatedPerson, relation.getBirthRegistrationNumber(), MCI_IDENTIFIER_BRN_CODE, relation.getHealthId());
            mapAsIdentifier(relatedPerson, relation.getUid(), MCI_IDENTIFIER_UID_CODE, relation.getHealthId());
            mapAsIdentifier(relatedPerson, relation.getHealthId(), MCI_IDENTIFIER_HID_CODE, relation.getHealthId());

            String type = relation.getType();
            CodeableConceptDt relationship = new CodeableConceptDt();
            relationship.addCoding()
                    .setSystem(FHIR_RELATED_PERSON_RELATIONSHIP_VALUESET_URL)
                    .setCode(type)
                    .setDisplay(masterDataRepository.findByTypeAndKey(MASTER_DATA_RELATION_TYPE, type).getValue());

            relatedPerson.setRelationship(relationship);

            relatedPerson.setName(new HumanNameDt().addFamily(relation.getSurName()).addGiven(relation.getGivenName()));
            ExtensionDt extensionDt = new ExtensionDt()
                    .setUrl(FHIRConstants.getFhirExtensionUrl(RELATED_PERSON_ID_EXTENSION_NAME))
                    .setValue(new StringDt(relation.getId()));
            relatedPerson.addUndeclaredExtension(extensionDt);
            bundle.addEntry().setFullUrl(TimeUuidUtil.uuidForDate(new Date()).toString()).setResource(relatedPerson);
        }
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
        String patientLinkUri = ensureSuffix(mciProperties.getPatientLinkUri(), URL_SEPARATOR);
        ResourceReferenceDt patientReference = new ResourceReferenceDt(String.format("%s%s", patientLinkUri, healthId));
        return new Patient.Link().setType(LinkTypeEnum.SEE_ALSO).setOther(patientReference);
    }

    private void mapAsIdentifier(Patient fhirPatient, String value, String identifierTypeCode, String healthId) {
        if (StringUtils.isBlank(value)) return;
        fhirPatient.addIdentifier(createIdentifier(value, identifierTypeCode, healthId));
    }

    private void mapAsIdentifier(RelatedPerson relation, String value, String identifierTypeCode, String healthId) {
        if (StringUtils.isBlank(value)) return;
        relation.addIdentifier(createIdentifier(value, identifierTypeCode, healthId));
    }

    private IdentifierDt createIdentifier(String value, String identifierTypeCode, String healthId) {
        IdentifierDt identifierDt = new IdentifierDt().setValue(value);
        if (StringUtils.isNotBlank(healthId)) {
            String mciPatientURI = getMCIPatientURI(mciProperties.getMciBaseUrl());
            identifierDt.setSystem(String.format("%s%s", mciPatientURI, healthId));
        }
        setIdentifierType(identifierDt, identifierTypeCode);
        return identifierDt;
    }

    @SuppressWarnings("unchecked")
    private void setIdentifierType(IdentifierDt identifierDt, String identifierCode) {
        BoundCodeableConceptDt identifierType = new BoundCodeableConceptDt<>(new MCIIdentifierEnumBinder());
        String system = getMCIValuesetURI(mciProperties.getMciBaseUrl(), MCI_PATIENT_IDENTIFIERS_VALUESET);
        identifierType.addCoding(new CodingDt(system, identifierCode));
        identifierDt.setType(identifierType);
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
