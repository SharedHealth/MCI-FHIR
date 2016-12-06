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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static org.sharedhealth.mci.web.util.FHIRConstants.*;
import static org.sharedhealth.mci.web.util.MCIConstants.*;

public class FHIRBundleMapper {
    private final int ADDRESS_CODE_EACH_LEVEL_LENGTH = 2;
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

        fhirPatient.getIdentifier().forEach(mapIdentifiersToFields(mciPatient));

        String educationLevel = findCodeFromExtension(fhirPatient, EDUCATION_DETAILS_EXTENSION_NAME);
        if (StringUtils.isNoneEmpty(educationLevel)) {
            mciPatient.setEducationLevel(educationLevel);
        }
        String occupation = findCodeFromExtension(fhirPatient, OCCUPATION_EXTENSION_NAME);
        if (StringUtils.isNoneEmpty(occupation)) {
            mciPatient.setOccupation(occupation);
        }
        String dobType = findCodeFromExtension(fhirPatient, DOB_TYPE_EXTENSION_NAME);
        if (StringUtils.isNoneEmpty(dobType)) {
            mciPatient.setDobType(dobType);
        }

        List<ExtensionDt> confidentialityExtensions = fhirPatient.getUndeclaredExtensionsByUrl(getFhirExtensionUrl(CONFIDENTIALITY_EXTENSION_NAME));
        if (CollectionUtils.isNotEmpty(confidentialityExtensions)) {
            BooleanDt booleanDt = (BooleanDt) confidentialityExtensions.get(0).getValue();
            mciPatient.setConfidential(booleanDt.getValue());
        }

        Optional<ContactPointDt> phoneNumber = fhirPatient.getTelecom().stream().filter(
                contactPointDt -> ContactPointSystemEnum.PHONE.getCode().equals(contactPointDt.getSystem())
        ).findFirst();
        if (phoneNumber.isPresent() && StringUtils.isNotEmpty(phoneNumber.get().getValue())) {
            mciPatient.setPhoneNo(phoneNumber.get().getValue());
        }
        mciPatient.setActive(fhirPatient.getActive());

        IDatatype deceased = fhirPatient.getDeceased();
        mapStatusAndDateOfDeath(mciPatient, deceased);

        List<RelatedPerson> relatedPersonList = getAllRelatedPerson(fhirPatientBundle);
        for (RelatedPerson relatedPerson : relatedPersonList) {
            Relation relation = new Relation();
            String code = relatedPerson.getRelationship().getCodingFirstRep().getCode();
            relation.setType(code);
        }

        return mciPatient;
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

    private Consumer<IdentifierDt> mapIdentifiersToFields(org.sharedhealth.mci.web.model.Patient mciPatient) {
        return (IdentifierDt identifierDt) -> {
            CodingDt coding = identifierDt.getType().getCodingFirstRep();
            String mciValuesetURI = getMCIValuesetURI(mciProperties.getMciBaseUrl(), MCI_PATIENT_IDENTIFIERS_VALUESET);
            /*
                todo : once profiles are introduced, we should remove the check for system, as fhir itself should validate
            */
            if (!mciValuesetURI.equals(coding.getSystem())) return;
            if (MCI_IDENTIFIER_NID_CODE.equals(coding.getCode())) {
                mciPatient.setNationalId(identifierDt.getValue());
            }
            if (MCI_IDENTIFIER_BRN_CODE.equals(coding.getCode())) {
                mciPatient.setBirthRegistrationNumber(identifierDt.getValue());
            }
            if (MCI_IDENTIFIER_HOUSE_HOLD_NUMBER_CODE.equals(coding.getCode())) {
                mciPatient.setHouseholdCode(identifierDt.getValue());
            }
        };
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
