package org.sharedhealth.mci.web.service;

import ca.uhn.fhir.model.api.ExtensionDt;
import ca.uhn.fhir.model.dstu2.composite.*;
import ca.uhn.fhir.model.dstu2.resource.Patient;
import ca.uhn.fhir.model.dstu2.resource.Patient.Link;
import ca.uhn.fhir.model.dstu2.valueset.AdministrativeGenderEnum;
import ca.uhn.fhir.model.dstu2.valueset.LinkTypeEnum;
import ca.uhn.fhir.model.primitive.DateDt;
import ca.uhn.fhir.model.primitive.DateTimeDt;
import ca.uhn.fhir.model.primitive.StringDt;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.apache.http.HttpStatus;
import org.sharedhealth.mci.web.config.MCIProperties;
import org.sharedhealth.mci.web.model.MCIIdentifierEnumBinder;
import org.sharedhealth.mci.web.model.MCIResponse;
import org.sharedhealth.mci.web.model.MciHealthId;
import org.sharedhealth.mci.web.repository.PatientRepository;
import org.sharedhealth.mci.web.util.MCIConstants;

import java.util.List;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.sharedhealth.mci.web.util.FHIRConstants.*;
import static org.sharedhealth.mci.web.util.MCIConstants.URL_SEPARATOR;
import static org.sharedhealth.mci.web.util.MCIConstants.getMCIPatientURI;
import static org.sharedhealth.mci.web.util.StringUtils.ensureSuffix;

public class PatientService {
    private final int ADDRESS_CODE_EACH_LEVEL_LENGTH = 2;
    private PatientRepository patientRepository;
    private MCIProperties mciProperties;
    private HealthIdService healthIdService;
    BidiMap<String, AdministrativeGenderEnum> mciToFhirGenderMap = new DualHashBidiMap<>();

    public PatientService(HealthIdService healthIdService, PatientRepository patientRepository, MCIProperties mciProperties) {
        this.healthIdService = healthIdService;
        this.patientRepository = patientRepository;
        this.mciProperties = mciProperties;
        mciToFhirGenderMap.put(MCIConstants.MALE, AdministrativeGenderEnum.MALE);
        mciToFhirGenderMap.put(MCIConstants.FEMALE, AdministrativeGenderEnum.FEMALE);
        mciToFhirGenderMap.put(MCIConstants.OTHER, AdministrativeGenderEnum.OTHER);
    }

    public Patient findPatientByHealthId(String healthId) {
        org.sharedhealth.mci.web.model.Patient mciPatient = patientRepository.findByHealthId(healthId);
        Patient fhirPatient = new Patient();
        fhirPatient.addName(mapName(mciPatient));
        fhirPatient.setGender(mciToFhirGenderMap.get(mciPatient.getGender()));
        fhirPatient.setBirthDate(mapDateOfBirth(mciPatient));
        fhirPatient.addAddress(mapAddress(mciPatient));
        fhirPatient.addIdentifier(mapHealthIdIdentifier(mciPatient));
        fhirPatient.addLink(mapPatientReferenceLink(healthId));
        return fhirPatient;
    }

    public MCIResponse createPatient(Patient fhirPatient) {
        org.sharedhealth.mci.web.model.Patient mciPatient = new org.sharedhealth.mci.web.model.Patient();
        MciHealthId healthId;
        try {
            healthId = healthIdService.getNextHealthId();
            mciPatient.setHealthId(healthId.getHid());
        } catch (Exception e) {
            MCIResponse mciResponse = new MCIResponse(HttpStatus.SC_BAD_REQUEST);
            mciResponse.setMessage(e.getMessage());
            return mciResponse;
        }

        HumanNameDt name = fhirPatient.getNameFirstRep();
        mciPatient.setGivenName(name.getGivenFirstRep().getValue());
        mciPatient.setSurName(name.getFamilyFirstRep().getValue());
        mciPatient.setGender(mciToFhirGenderMap.getKey(fhirPatient.getGenderElement().getValueAsEnum()));

        List<ExtensionDt> birthExtensions = fhirPatient.getBirthDateElement().getUndeclaredExtensionsByUrl(BIRTH_TIME_EXTENSION_URL);
        DateTimeDt birthTime = (DateTimeDt) birthExtensions.get(0).getValue();
        mciPatient.setDateOfBirth(birthTime.getValue());

        AddressDt address = fhirPatient.getAddressFirstRep();
        mciPatient.setAddressLine(address.getLineFirstRep().getValue());
        mciPatient.setCountryCode(address.getCountry());
        List<ExtensionDt> extensions = address.getUndeclaredExtensionsByUrl(getFhirExtensionUrl(ADDRESS_CODE_EXTENSION_NAME));
        StringDt addressCode = (StringDt) extensions.get(0).getValue();
        Iterable<String> codes = Splitter.fixedLength(ADDRESS_CODE_EACH_LEVEL_LENGTH).split(addressCode.getValue());
        List<String> addressLevels = Lists.newArrayList(codes);

        mciPatient.setDivisionId(addressLevels.get(0));
        mciPatient.setDistrictId(addressLevels.get(1));
        mciPatient.setUpazilaId(addressLevels.get(2));
        setCityCorporation(mciPatient, addressLevels);
        setUnionWard(mciPatient, addressLevels);
        setRuralWard(mciPatient, addressLevels);

        healthIdService.markUsed(healthId);
        return patientRepository.createPatient(mciPatient);
    }

    private DateDt mapDateOfBirth(org.sharedhealth.mci.web.model.Patient mciPatient) {
        DateDt dateOfBirth = new DateDt(mciPatient.getDateOfBirth());
        ExtensionDt extensionDt = new ExtensionDt().setUrl(BIRTH_TIME_EXTENSION_URL)
                .setValue(new DateTimeDt(mciPatient.getDateOfBirth()));
        dateOfBirth.addUndeclaredExtension(extensionDt);
        return dateOfBirth;
    }

    private Link mapPatientReferenceLink(String healthId) {
        Link link = new Link();
        link.setType(LinkTypeEnum.SEE_ALSO);
        String patientLinkUri = ensureSuffix(mciProperties.getPatientLinkUri(), URL_SEPARATOR);
        ResourceReferenceDt patientReference = new ResourceReferenceDt(String.format("%s%s", patientLinkUri, healthId));
        link.setOther(patientReference);
        return link;
    }

    private IdentifierDt mapHealthIdIdentifier(org.sharedhealth.mci.web.model.Patient mciPatient) {
        IdentifierDt healthIdIdentifierDt = new IdentifierDt();
        String healthId = mciPatient.getHealthId();
        healthIdIdentifierDt.setValue(healthId);
        String mciPatientURI = getMCIPatientURI(mciProperties.getMciBaseUrl());
        setIdentifierType(healthIdIdentifierDt, MCI_IDENTIFIER_HID_CODE);
        healthIdIdentifierDt.setSystem(String.format("%s%s", mciPatientURI, healthId));
        return healthIdIdentifierDt;
    }

    @SuppressWarnings("unchecked")
    private void setIdentifierType(IdentifierDt identifierDt, String hidCode) {
        BoundCodeableConceptDt identifierType = new BoundCodeableConceptDt<>(new MCIIdentifierEnumBinder());
        String system = getMCIValuesetURI(mciProperties.getMciBaseUrl());
        identifierType.addCoding(new CodingDt(system, hidCode));
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

}
