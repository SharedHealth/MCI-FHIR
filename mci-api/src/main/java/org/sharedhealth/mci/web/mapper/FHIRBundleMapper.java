package org.sharedhealth.mci.web.mapper;


import ca.uhn.fhir.model.api.ExtensionDt;
import ca.uhn.fhir.model.dstu2.composite.AddressDt;
import ca.uhn.fhir.model.dstu2.composite.HumanNameDt;
import ca.uhn.fhir.model.dstu2.resource.Patient;
import ca.uhn.fhir.model.dstu2.valueset.AdministrativeGenderEnum;
import ca.uhn.fhir.model.primitive.DateTimeDt;
import ca.uhn.fhir.model.primitive.StringDt;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.sharedhealth.mci.web.util.MCIConstants;

import java.util.List;

import static org.sharedhealth.mci.web.util.FHIRConstants.*;

public class FHIRBundleMapper {
    private BidiMap<String, AdministrativeGenderEnum> mciToFhirGenderMap = new DualHashBidiMap<>();
    private final int ADDRESS_CODE_EACH_LEVEL_LENGTH = 2;

    public FHIRBundleMapper() {
        mciToFhirGenderMap.put(MCIConstants.MALE, AdministrativeGenderEnum.MALE);
        mciToFhirGenderMap.put(MCIConstants.FEMALE, AdministrativeGenderEnum.FEMALE);
        mciToFhirGenderMap.put(MCIConstants.OTHER, AdministrativeGenderEnum.OTHER);
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

}
