package org.sharedhealth.mci.web.mapper;

import ca.uhn.fhir.model.primitive.StringDt;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.hl7.fhir.dstu3.model.*;
import org.sharedhealth.mci.web.config.MCIProperties;
import org.sharedhealth.mci.web.util.MCIConstants;

import java.util.Date;
import java.util.List;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.sharedhealth.mci.web.util.FHIRConstants.*;
import static org.sharedhealth.mci.web.util.MCIConstants.URL_SEPARATOR;
import static org.sharedhealth.mci.web.util.MCIConstants.getMCIPatientURI;
import static org.sharedhealth.mci.web.util.StringUtils.ensureSuffix;

public class PatientMapper {
    private MCIProperties mciProperties;

    private final int ADDRESS_CODE_EACH_LEVEL_LENGTH = 2;
    BidiMap<String, Enumerations.AdministrativeGender> mciToFhirGenderMap = new DualHashBidiMap<>();

    public PatientMapper(MCIProperties mciProperties) {
        this.mciProperties = mciProperties;
        mciToFhirGenderMap.put(MCIConstants.MALE, Enumerations.AdministrativeGender.MALE);
        mciToFhirGenderMap.put(MCIConstants.FEMALE, Enumerations.AdministrativeGender.FEMALE);
        mciToFhirGenderMap.put(MCIConstants.OTHER, Enumerations.AdministrativeGender.OTHER);
    }

    public Patient mapToFHIRPatient(org.sharedhealth.mci.web.model.Patient mciPatient) {
        Patient fhirPatient = new Patient();
        fhirPatient.addName(mapName(mciPatient));
        fhirPatient.setGender(mciToFhirGenderMap.get(mciPatient.getGender()));
        fhirPatient.setBirthDate(mapDateOfBirth(mciPatient).getValue());
        fhirPatient.addAddress(mapAddress(mciPatient));
        fhirPatient.addIdentifier(mapHealthIdIdentifier(mciPatient));
        fhirPatient.addLink(mapPatientReferenceLink(mciPatient.getHealthId()));
        return fhirPatient;
    }

    public org.sharedhealth.mci.web.model.Patient mapToMCIPatient(Patient fhirPatient) {
        org.sharedhealth.mci.web.model.Patient mciPatient = new org.sharedhealth.mci.web.model.Patient();

        HumanName name = fhirPatient.getNameFirstRep();
        mciPatient.setGivenName(name.getGiven().get(0).getValueNotNull());
        mciPatient.setSurName(name.getFamily().get(0).getValueNotNull());
        mciPatient.setGender(mciToFhirGenderMap.getKey(fhirPatient.getGenderElement().getValueAsString()));

        List<Extension> birthExtensions = fhirPatient.getBirthDateElement().getExtensionsByUrl(BIRTH_TIME_EXTENSION_URL);
        Date birthTime;
        if (CollectionUtils.isEmpty(birthExtensions)) {
            birthTime = fhirPatient.getBirthDate();
        } else {
            birthTime = ((DateTimeType) birthExtensions.get(0).getValue()).getValue();
        }
        mciPatient.setDateOfBirth(birthTime);

        Address address = fhirPatient.getAddressFirstRep();
        mciPatient.setAddressLine(address.getLine().get(0).asStringValue());
        mciPatient.setCountryCode(address.getCountry());
        List<Extension> extensions = address.getExtensionsByUrl(getFhirExtensionUrl(ADDRESS_CODE_EXTENSION_NAME));
        String addressCode = ((StringType) extensions.get(0).getValue()).getValue();
        Iterable<String> codes = Splitter.fixedLength(ADDRESS_CODE_EACH_LEVEL_LENGTH).split(addressCode);
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


    private DateType mapDateOfBirth(org.sharedhealth.mci.web.model.Patient mciPatient) {
        DateType dateOfBirth = new DateType(mciPatient.getDateOfBirth());
        Extension extension = new Extension().setUrl(BIRTH_TIME_EXTENSION_URL)
                .setValue(new DateTimeType(mciPatient.getDateOfBirth()));
        dateOfBirth.addExtension(extension);
        return dateOfBirth;
    }

    private Patient.PatientLinkComponent mapPatientReferenceLink(String healthId) {
        Patient.PatientLinkComponent link = new Patient.PatientLinkComponent();
        link.setType(Patient.LinkType.SEEALSO);
        String patientLinkUri = ensureSuffix(mciProperties.getPatientLinkUri(), URL_SEPARATOR);
        Reference patientReference = new Reference(String.format("%s%s", patientLinkUri, healthId));
        link.setOther(patientReference);
        return link;
    }

    private Identifier mapHealthIdIdentifier(org.sharedhealth.mci.web.model.Patient mciPatient) {
        Identifier healthIdIdentifier = new Identifier();
        String healthId = mciPatient.getHealthId();
        healthIdIdentifier.setValue(healthId);
        String mciPatientURI = getMCIPatientURI(mciProperties.getMciBaseUrl());
        setIdentifierType(healthIdIdentifier, MCI_IDENTIFIER_HID_CODE);
        healthIdIdentifier.setSystem(String.format("%s%s", mciPatientURI, healthId));
        return healthIdIdentifier;
    }

    @SuppressWarnings("unchecked")
    private void setIdentifierType(Identifier identifier, String hidCode) {
        CodeableConcept identifierType = new CodeableConcept();
        String system = getMCIValuesetURI(mciProperties.getMciBaseUrl());
        identifierType.addCoding(new Coding().setCode(hidCode).setSystem(system));
        identifier.setType(identifierType);
    }

    private HumanName mapName(org.sharedhealth.mci.web.model.Patient mciPatient) {
        HumanName name = new HumanName();
        name.addGiven(mciPatient.getGivenName());
        name.addFamily(mciPatient.getSurName());
        return name;
    }

    private Address mapAddress(org.sharedhealth.mci.web.model.Patient mciPatient) {
        Address address = new Address().addLine(mciPatient.getAddressLine());
        address.setCountry(mciPatient.getCountryCode());
        Extension addressCodeExtension = new Extension().
                setUrl(getFhirExtensionUrl(ADDRESS_CODE_EXTENSION_NAME)).setValue(buildAddressCode(mciPatient));
        address.addExtension(addressCodeExtension);
        return address;
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
