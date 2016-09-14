package org.sharedhealth.mci.web.mapper;

import org.sharedhealth.mci.web.model.Patient;
import org.sharedhealth.mci.web.model.PatientAuditLog;
import org.sharedhealth.mci.web.util.DateUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import static org.sharedhealth.mci.web.util.DateUtil.toDateString;
import static org.sharedhealth.mci.web.util.JsonMapper.writeValueAsString;
import static org.sharedhealth.mci.web.util.RepositoryConstants.*;

public class PatientAuditLogMapper {

    public PatientAuditLog map(Patient patient) {
        PatientAuditLog patientAuditLog = new PatientAuditLog();
        patientAuditLog.setHealthId(patient.getHealthId());
        patientAuditLog.setEventId(patient.getCreatedAt());

        String changeSet = auditLogChangeSetForNewPatient(patient);
        patientAuditLog.setChangeSet(changeSet);
        return patientAuditLog;
    }

    private String auditLogChangeSetForNewPatient(Patient patient) {
        Map<String, Map<String, Object>> changeSet = new TreeMap<>();

        String dateOfBirth = toDateString(patient.getDateOfBirth(), DateUtil.ISO_8601_DATE_IN_MILLIS_FORMAT2);
        Map<String, Object> dobChangeSet = getInnerChangeSet(dateOfBirth);
        changeSet.put(DATE_OF_BIRTH, dobChangeSet);

        changeSet.put(GENDER, getInnerChangeSet(patient.getGender()));
        changeSet.put(GIVEN_NAME, getInnerChangeSet(patient.getGivenName()));
        changeSet.put(SUR_NAME, getInnerChangeSet(patient.getSurName()));
        Map<String, String> newAddressMap = getPresentAddress(patient);

        changeSet.put(PRESENT_ADDRESS, getInnerChangeSet(newAddressMap));
        Map<String, Object> healthIdChangeSet = getInnerChangeSet(patient.getHealthId());
        changeSet.put(HID, healthIdChangeSet);

        return writeValueAsString(changeSet);

    }

    private Map<String, String> getPresentAddress(Patient patient) {
        Map<String, String> presentAddress = new HashMap<>();
        presentAddress.put(ADDRESS_LINE, patient.getAddressLine());
        presentAddress.put(DIVISION_ID, patient.getDivisionId());
        presentAddress.put(DISTRICT_ID, patient.getDistrictId());
        presentAddress.put(UPAZILA_ID, patient.getUpazilaId());

        String cityCorporationId = patient.getCityCorporationId();
        if (null != cityCorporationId) presentAddress.put(CITY_CORPORATION, cityCorporationId);

        String unionOrUrbanWardId = patient.getUnionOrUrbanWardId();
        if (null != unionOrUrbanWardId) presentAddress.put(UNION_OR_URBAN_WARD_ID, unionOrUrbanWardId);

        String ruralWardId = patient.getRuralWardId();
        if (null != ruralWardId) presentAddress.put(RURAL_WARD_ID, ruralWardId);

        return presentAddress;
    }

    private Map<String, Object> getInnerChangeSet(Object newValue) {
        Map<String, Object> innerChangeSet = new HashMap<>();
        innerChangeSet.put(OLD_VALUE, "");
        innerChangeSet.put(NEW_VALUE, newValue);
        return innerChangeSet;
    }

}
