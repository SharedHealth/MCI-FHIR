package org.sharedhealth.mci.web.mapper;

import org.sharedhealth.mci.web.model.Patient;
import org.sharedhealth.mci.web.model.PatientUpdateLog;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import static org.sharedhealth.mci.web.util.JsonMapper.writeValueAsString;
import static org.sharedhealth.mci.web.util.RepositoryConstants.*;

public class PatientUpdateLogMapper {

    public PatientUpdateLog map(Patient patient) {
        String healthId = patient.getHealthId();
        PatientUpdateLog patientUpdateLog = new PatientUpdateLog();
        patientUpdateLog.setHealthId(healthId);
        patientUpdateLog.setEventId(patient.getCreatedAt());
        patientUpdateLog.setEventType(EVENT_TYPE_CREATED);

        String changeSet = updateLogChangeSetForNewPatient(healthId);
        patientUpdateLog.setChangeSet(changeSet);

        return patientUpdateLog;
    }

    private String updateLogChangeSetForNewPatient(String healthId) {
        Map<String, Map<String, Object>> changeSet = new TreeMap<>();
        Map<String, Object> healthIdChangeSet = getInnerChangeSet(healthId);
        changeSet.put(HID, healthIdChangeSet);

        return writeValueAsString(changeSet);
    }

    private Map<String, Object> getInnerChangeSet(Object newValue) {
        Map<String, Object> innerChangeSet = new HashMap<>();
        innerChangeSet.put(OLD_VALUE, "");
        innerChangeSet.put(NEW_VALUE, newValue);
        return innerChangeSet;
    }
}
