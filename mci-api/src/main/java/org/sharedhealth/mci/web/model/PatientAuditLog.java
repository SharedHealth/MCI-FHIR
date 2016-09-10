package org.sharedhealth.mci.web.model;


import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;

import java.util.UUID;

import static org.sharedhealth.mci.web.util.RepositoryConstants.*;

@Table(name = CF_PATIENT_AUDIT_LOG)
public class PatientAuditLog {

    @PartitionKey()
    @Column(name = HEALTH_ID)
    private String healthId;

    @Column(name = EVENT_ID)
    private UUID eventId;

    @Column(name = CHANGE_SET)
    private String changeSet;

    @Column(name = REQUESTED_BY)
    private String requestedBy;

    @Column(name = APPROVED_BY)
    private String approvedBy;

//    public static PatientAuditLog toPatientAuditLog(PatientUpdateLog feed) {
//        PatientAuditLog log = new PatientAuditLog();
//        log.setHealthId(feed.getHealthId());
//        log.setEventId(feed.getEventId());
//        log.setChangeSet(feed.getChangeSet());
//        log.setRequestedBy(feed.getRequestedBy());
//        log.setApprovedBy(feed.getApprovedBy());
//        return log;
//    }

    public String getHealthId() {
        return healthId;
    }

    public void setHealthId(String healthId) {
        this.healthId = healthId;
    }

    public UUID getEventId() {
        return eventId;
    }

    public void setEventId(UUID eventId) {
        this.eventId = eventId;
    }

    public String getChangeSet() {
        return changeSet;
    }

    public void setChangeSet(String changeSet) {
        this.changeSet = changeSet;
    }

    public String getRequestedBy() {
        return requestedBy;
    }

    public void setRequestedBy(String requestedBy) {
        this.requestedBy = requestedBy;
    }

    public String getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(String approvedBy) {
        this.approvedBy = approvedBy;
    }
}