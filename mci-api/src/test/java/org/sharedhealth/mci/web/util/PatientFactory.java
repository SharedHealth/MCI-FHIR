package org.sharedhealth.mci.web.util;

import ca.uhn.fhir.model.api.ExtensionDt;
import ca.uhn.fhir.model.dstu2.composite.AddressDt;
import ca.uhn.fhir.model.dstu2.valueset.AdministrativeGenderEnum;
import ca.uhn.fhir.model.primitive.DateDt;
import ca.uhn.fhir.model.primitive.DateTimeDt;
import ca.uhn.fhir.model.primitive.StringDt;
import org.sharedhealth.mci.web.model.Patient;
import org.sharedhealth.mci.web.model.Requester;

import java.nio.file.AccessDeniedException;
import java.util.Date;

import static org.sharedhealth.mci.web.util.DateUtil.parseDate;
import static org.sharedhealth.mci.web.util.FHIRConstants.ADDRESS_CODE_EXTENSION_NAME;
import static org.sharedhealth.mci.web.util.FHIRConstants.BIRTH_TIME_EXTENSION_URL;
import static org.sharedhealth.mci.web.util.FHIRConstants.getFhirExtensionUrl;
import static org.sharedhealth.mci.web.util.JsonMapper.writeValueAsString;
import static org.sharedhealth.mci.web.util.MCIConstants.PATIENT_STATUS_ALIVE;

public class PatientFactory {
    public static final String educationLevel = "01";
    public static final String occupation = "02";
    public static final String phoneNo = "12345678";
    public static final Date dateOfDeath = null;
    public static final String dobType = "1";
    public static final String healthId = "HID123";
    public static final String givenName = "Bob the";
    public static final String surName = "Builder";
    public static final String fatherName = "Father";
    public static final String motherName = "Mother";
    public static final String spouseName = "Spouse";
    public static final String gender = "M";
    public static final Date dateOfBirth = parseDate("1995-07-01 14:20:00+0530");
    public static final String countryCode = "050";
    public static final String divisionId = "30";
    public static final String districtId = "26";
    public static final String upazilaId = "18";
    public static final String cityId = "02";
    public static final String urbanWardId = "01";
    public static final String ruralWardId = "04";
    public static final String addressLine = "Will Street";
    public static final String brn = "BRN";
    public static final String nid = "NID";
    public static final String householdCode = "12345";

    public static ca.uhn.fhir.model.dstu2.resource.Patient createFHIRPatientWithMandatoryFields(boolean timeOfBirthIncluded) {
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

    public static org.sharedhealth.mci.web.model.Patient createMCIPatientWithMandatoryFields() {
        org.sharedhealth.mci.web.model.Patient patient = new org.sharedhealth.mci.web.model.Patient();
        patient.setGivenName(givenName);
        patient.setSurName(surName);
        patient.setGender(gender);
        patient.setDateOfBirth(dateOfBirth);
        patient.setCountryCode(countryCode);
        patient.setDivisionId(divisionId);
        patient.setDistrictId(districtId);
        patient.setUpazilaId(upazilaId);
        patient.setCityCorporationId(cityId);
        patient.setUnionOrUrbanWardId(urbanWardId);
        patient.setRuralWardId(ruralWardId);
        patient.setAddressLine(addressLine);
        return patient;
    }


    public static Patient createMCIPatientWithAllFields() throws AccessDeniedException {
        org.sharedhealth.mci.web.model.Patient patient = createMCIPatientWithMandatoryFields();
        patient.setHealthId(healthId);
        patient.setCreatedAt(TimeUuidUtil.uuidForDate(new Date()));
        patient.setCreatedBy(writeValueAsString(getRequester()));
        patient.setUpdatedBy(writeValueAsString(getRequester()));
        patient.setActive(true);
        patient.setBirthRegistrationNumber(brn);
        patient.setNationalId(nid);
        patient.setFathersSurName(surName);
        patient.setFathersGivenName(fatherName);
        patient.setMothersSurName(surName);
        patient.setMothersGivenName(motherName);
        patient.setRelations("[{\"type\":\"FTH\",\"given_name\":\"Father\",\"sur_name\":\"Builder\"," +
                "\"id\":\"06941b0d-5f8a-487f-b1e0-f2777c2fbe44\"},{\"type\":\"MTH\",\"given_name\":\"Mother\",\"sur_name\":\"Builder\"," +
                "\"id\":\"06941b0d-5f8a-487f-b5e0-f2777c2fbe44\"},{\"type\":\"SPS\",\"given_name\":\"Spouse\"," +
                "\"sur_name\":\"Builder\",\"id\":\"6ab069a7-f3e9-4368-99c2-9896818e447f\"}]");
        patient.setEducationLevel(educationLevel);
        patient.setOccupation(occupation);
        patient.setPhoneNo(phoneNo);
        patient.setStatus(PATIENT_STATUS_ALIVE);
        patient.setDateOfDeath(dateOfDeath);
        patient.setDobType(dobType);
        patient.setHouseholdCode(householdCode);
        patient.setConfidential(false);
        return patient;
    }

    private static Requester getRequester() throws AccessDeniedException {
        return new Requester("100067", null, null, null);
    }

}
