package org.sharedhealth.mci.web.mapper;


import ca.uhn.fhir.model.api.ExtensionDt;
import ca.uhn.fhir.model.api.IDatatype;
import ca.uhn.fhir.model.dstu2.composite.*;
import ca.uhn.fhir.model.dstu2.resource.Bundle;
import ca.uhn.fhir.model.dstu2.resource.Patient;
import ca.uhn.fhir.model.dstu2.resource.RelatedPerson;
import ca.uhn.fhir.model.dstu2.valueset.AdministrativeGenderEnum;
import ca.uhn.fhir.model.dstu2.valueset.ContactPointSystemEnum;
import ca.uhn.fhir.model.primitive.BooleanDt;
import ca.uhn.fhir.model.primitive.DateTimeDt;
import ca.uhn.fhir.model.primitive.StringDt;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.apache.commons.lang3.StringUtils;
import org.sharedhealth.mci.web.config.MCIProperties;
import org.sharedhealth.mci.web.model.Relation;
import org.sharedhealth.mci.web.util.MCIConstants;

import java.util.*;

import static org.sharedhealth.mci.web.util.FHIRConstants.*;
import static org.sharedhealth.mci.web.util.JsonMapper.writeValueAsString;
import static org.sharedhealth.mci.web.util.MCIConstants.*;

public class FHIRBundleMapper {
    private final int ADDRESS_CODE_EACH_LEVEL_LENGTH = 2;
    private final String DEFAULT_DOB_TYPE = "1";

    private MCIProperties mciProperties;
    private BidiMap<String, AdministrativeGenderEnum> mciToFhirGenderMap = new DualHashBidiMap<>();

    public FHIRBundleMapper(MCIProperties mciProperties) {
        this.mciProperties = mciProperties;

        mciToFhirGenderMap.put(MCIConstants.MALE, AdministrativeGenderEnum.MALE);
        mciToFhirGenderMap.put(MCIConstants.FEMALE, AdministrativeGenderEnum.FEMALE);
        mciToFhirGenderMap.put(MCIConstants.OTHER, AdministrativeGenderEnum.OTHER);
    }

    public org.sharedhealth.mci.web.model.Patient mapToMCIPatient(Bundle fhirPatientBundle) {
        org.sharedhealth.mci.web.model.Patient mciPatient = new org.sharedhealth.mci.web.model.Patient();
        Patient fhirPatient = getPatientResource(fhirPatientBundle);

        HumanNameDt name = fhirPatient.getNameFirstRep();
        mciPatient.setGivenName(name.getGivenFirstRep().getValue());
        mciPatient.setSurName(name.getFamilyFirstRep().getValue());
        mciPatient.setGender(mciToFhirGenderMap.getKey(fhirPatient.getGenderElement().getValueAsEnum()));
        setDateOfBirth(mciPatient, fhirPatient);

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

        Map<String, String> patientIdentifiersMap = getMapForIdentifiers(fhirPatient.getIdentifier());
        mciPatient.setNationalId(patientIdentifiersMap.get(MCI_IDENTIFIER_NID_CODE));
        mciPatient.setBirthRegistrationNumber(patientIdentifiersMap.get(MCI_IDENTIFIER_BRN_CODE));

        String educationLevel = findCodeFromExtension(fhirPatient, EDUCATION_DETAILS_EXTENSION_NAME);
        if (StringUtils.isNotEmpty(educationLevel)) {
            mciPatient.setEducationLevel(educationLevel);
        }
        String occupation = findCodeFromExtension(fhirPatient, OCCUPATION_EXTENSION_NAME);
        if (StringUtils.isNotEmpty(occupation)) {
            mciPatient.setOccupation(occupation);
        }
        String dobType = findCodeFromExtension(fhirPatient, DOB_TYPE_EXTENSION_NAME);
        if (StringUtils.isNotEmpty(dobType)) {
            mciPatient.setDobType(dobType);
        } else {
            mciPatient.setDobType(DEFAULT_DOB_TYPE);
        }

        List<ExtensionDt> confidentialityExtensions = fhirPatient.getUndeclaredExtensionsByUrl(getFhirExtensionUrl(CONFIDENTIALITY_EXTENSION_NAME));
        if (CollectionUtils.isNotEmpty(confidentialityExtensions)) {
            BooleanDt booleanDt = (BooleanDt) confidentialityExtensions.get(0).getValue();
            mciPatient.setConfidential(booleanDt.getValue());
        } else {
            mciPatient.setConfidential(false);
        }

        List<ExtensionDt> houseHoldCodeExtn = fhirPatient.getUndeclaredExtensionsByUrl(getFhirExtensionUrl(HOUSE_HOLD_CODE_EXTENSION_NAME));
        if (CollectionUtils.isNotEmpty(houseHoldCodeExtn)) {
            StringDt houseHoldCode = (StringDt) houseHoldCodeExtn.get(0).getValue();
            mciPatient.setHouseholdCode(houseHoldCode.getValue());
        }

        Optional<ContactPointDt> phoneNumber = fhirPatient.getTelecom().stream().filter(
                contactPointDt -> ContactPointSystemEnum.PHONE.getCode().equals(contactPointDt.getSystem())
        ).findFirst();
        if (phoneNumber.isPresent() && StringUtils.isNotEmpty(phoneNumber.get().getValue())) {
            mciPatient.setPhoneNo(phoneNumber.get().getValue());
        }

        boolean active = fhirPatient.getActive() != null ? fhirPatient.getActive() : true;
        mciPatient.setActive(active);
        IDatatype deceased = fhirPatient.getDeceased();
        mapStatusAndDateOfDeath(mciPatient, deceased);
        mapRelatedPeopleAsRelations(fhirPatientBundle, mciPatient);
        return mciPatient;
    }

    private void setDateOfBirth(org.sharedhealth.mci.web.model.Patient mciPatient, Patient fhirPatient) {
        List<ExtensionDt> birthExtensions = fhirPatient.getBirthDateElement().getUndeclaredExtensionsByUrl(BIRTH_TIME_EXTENSION_URL);
        DateTimeDt birthTime;
        if (CollectionUtils.isEmpty(birthExtensions)) {
            birthTime = new DateTimeDt(fhirPatient.getBirthDate());
        } else {
            birthTime = (DateTimeDt) birthExtensions.get(0).getValue();
        }
        mciPatient.setDateOfBirth(birthTime.getValue());
    }

    private void mapRelatedPeopleAsRelations(Bundle fhirPatientBundle, org.sharedhealth.mci.web.model.Patient mciPatient) {
        List<RelatedPerson> relatedPersonList = getAllRelatedPerson(fhirPatientBundle);
        if (CollectionUtils.isEmpty(relatedPersonList)) return;
        List<Relation> patientRelations = new ArrayList<>();
        for (RelatedPerson relatedPerson : relatedPersonList) {
            Relation relation = new Relation();
            relation.setType(relatedPerson.getRelationship().getCodingFirstRep().getCode());
            HumanNameDt relatedPersonName = relatedPerson.getName();
            relation.setGivenName(relatedPersonName.getGivenFirstRep().getValue());
            relation.setSurName(relatedPersonName.getFamilyFirstRep().getValue());
            List<ExtensionDt> relationId = relatedPerson.getUndeclaredExtensionsByUrl(getFhirExtensionUrl(RELATION_ID_EXTENSION_NAME));
            if (CollectionUtils.isNotEmpty(relationId)) {
                relation.setId(((StringDt) relationId.get(0).getValue()).getValue());
            }
            Map<String, String> relationIdentifiersMap = getMapForIdentifiers(relatedPerson.getIdentifier());
            relation.setBirthRegistrationNumber(relationIdentifiersMap.get(MCI_IDENTIFIER_BRN_CODE));
            relation.setNationalId(relationIdentifiersMap.get(MCI_IDENTIFIER_NID_CODE));
            relation.setUid(relationIdentifiersMap.get(MCI_IDENTIFIER_UID_CODE));
            relation.setHealthId(relationIdentifiersMap.get(MCI_IDENTIFIER_HID_CODE));
            patientRelations.add(relation);
        }
        mciPatient.setRelations(writeValueAsString(patientRelations));
    }

    private Map<String, String> getMapForIdentifiers(List<IdentifierDt> identifiers) {
        Map<String, String> identifiersMap = new HashMap<>();
        identifiers.forEach((IdentifierDt identifierDt) -> {
            CodingDt coding = identifierDt.getType().getCodingFirstRep();
            String mciValuesetURI = getMCIValuesetURI(mciProperties.getMciBaseUrl(), MCI_PATIENT_IDENTIFIERS_VALUESET);
            /*
                todo : once profiles are introduced, we should remove the check for system, as fhir itself should validate
            */
            if (!mciValuesetURI.equals(coding.getSystem())) return;
            identifiersMap.put(coding.getCode(), identifierDt.getValue());
        });
        return identifiersMap;
    }

    private Patient getPatientResource(Bundle fhirPatientBundle) {
        Optional<Bundle.Entry> entryOptional = fhirPatientBundle.getEntry().stream().filter(entry -> new Patient().getResourceName().equals(entry.getResource().getResourceName())).findFirst();
        return entryOptional.map(entry -> (Patient) entry.getResource()).orElse(null);
    }

    private List<RelatedPerson> getAllRelatedPerson(Bundle fhirBundle) {
        List<RelatedPerson> relatedPersonList = new ArrayList<>();
        for (Bundle.Entry entry : fhirBundle.getEntry()) {
            if (new RelatedPerson().getResourceName().equals(entry.getResource().getResourceName())) {
                relatedPersonList.add((RelatedPerson) entry.getResource());
            }
        }
        return relatedPersonList;
    }

    private void mapStatusAndDateOfDeath(org.sharedhealth.mci.web.model.Patient mciPatient, IDatatype deceased) {
        if (null == deceased) {
            mciPatient.setStatus(PATIENT_STATUS_UNKNOWN);
            return;
        }
        if (deceased instanceof BooleanDt) {
            String patientStatus = ((BooleanDt) deceased).getValue() ? PATIENT_STATUS_DEAD : PATIENT_STATUS_ALIVE;
            mciPatient.setStatus(patientStatus);
            return;
        }
        if (deceased instanceof DateTimeDt) {
            mciPatient.setStatus(PATIENT_STATUS_DEAD);
            mciPatient.setDateOfDeath(((DateTimeDt) deceased).getValue());
        }
    }

    private String findCodeFromExtension(Patient fhirPatient, String extensionName) {
        List<ExtensionDt> educationExtensions = fhirPatient.getUndeclaredExtensionsByUrl(getFhirExtensionUrl(extensionName));
        if (CollectionUtils.isNotEmpty(educationExtensions)) {
            return ((CodeableConceptDt) educationExtensions.get(0).getValue()).getCodingFirstRep().getCode();
        }
        return null;
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

}
