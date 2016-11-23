package org.sharedhealth.mci.web.util;

import org.sharedhealth.mci.web.model.Patient;
import org.sharedhealth.mci.web.model.Requester;

import java.nio.file.AccessDeniedException;
import java.util.Date;

import static org.sharedhealth.mci.web.util.DateUtil.parseDate;
import static org.sharedhealth.mci.web.util.JsonMapper.writeValueAsString;

public class PatientFactory {
    public static final String educationLevel = "01";
    public static final String occupation = "02";
    public static final String phoneNo = "12345678";
    public static final String status = "1";
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

    public static Patient createMCIPatient() throws AccessDeniedException {
        Patient expectedPatient = new Patient();
        expectedPatient.setHealthId(healthId);
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
        expectedPatient.setCreatedAt(TimeUuidUtil.uuidForDate(new Date()));
        expectedPatient.setCreatedBy(writeValueAsString(getRequester()));
        expectedPatient.setUpdatedBy(writeValueAsString(getRequester()));
        expectedPatient.setActive(true);
        expectedPatient.setBirthRegistrationNumber(brn);
        expectedPatient.setNationalId(nid);
        expectedPatient.setFathersSurName(surName);
        expectedPatient.setFathersGivenName(fatherName);
        expectedPatient.setMothersSurName(surName);
        expectedPatient.setMothersGivenName(motherName);
        expectedPatient.setRelations("[{\"type\":\"FTH\",\"given_name\":\"Father\",\"sur_name\":\"Builder\"," +
                "\"id\":\"06941b0d-5f8a-487f-b1e0-f2777c2fbe44\"},{\"type\":\"MTH\",\"given_name\":\"Mother\",\"sur_name\":\"Builder\"," +
                "\"id\":\"06941b0d-5f8a-487f-b5e0-f2777c2fbe44\"},{\"type\":\"SPS\",\"given_name\":\"Spouse\"," +
                "\"sur_name\":\"Builder\",\"id\":\"6ab069a7-f3e9-4368-99c2-9896818e447f\"}]");
        expectedPatient.setEducationLevel(educationLevel);
        expectedPatient.setOccupation(occupation);
        expectedPatient.setPhoneNo(phoneNo);
        expectedPatient.setStatus(status);
        expectedPatient.setDateOfDeath(dateOfDeath);
        expectedPatient.setDobType(dobType);
        expectedPatient.setHouseholdCode(householdCode);
        expectedPatient.setConfidential(false);
        return expectedPatient;
    }

    private static Requester getRequester() throws AccessDeniedException {
        return new Requester("100067", null, null, null);
    }

}
