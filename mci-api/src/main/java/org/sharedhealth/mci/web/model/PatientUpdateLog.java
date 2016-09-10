package org.sharedhealth.mci.web.model;


import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;
import org.sharedhealth.mci.web.util.DateUtil;

import java.util.UUID;

import static org.sharedhealth.mci.web.util.RepositoryConstants.*;

@Table(name = CF_PATIENT_UPDATE_LOG)
public class PatientUpdateLog {

    @PartitionKey()
    @Column(name = YEAR)
    private int year;

    @Column(name = EVENT_ID)
    private UUID eventId;

    @Column(name = HEALTH_ID)
    private String healthId;

    @Column(name = CHANGE_SET)
    private String changeSet;

    @Column(name = REQUESTED_BY)
    private String requestedBy;

    @Column(name = APPROVED_BY)
    private String approvedBy;

    @Column(name = EVENT_TYPE)
    private String eventType;

    public String getHealthId() {
        return healthId;
    }

    public void setHealthId(String healthId) {
        this.healthId = healthId;
    }

    public String getChangeSet() {
        return changeSet;
    }

    public void setChangeSet(String changeSet) {
        this.changeSet = changeSet;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public int getYear() {
        return year;
    }

//    @JsonProperty(CHANGE_SET)
//    public Map getChangeSetMap() {
//
//        if (this.changeSet == null) {
//            return null;
//        }
//
//        ObjectMapper mapper = new ObjectMapper();
//        try {
//            return mapper.readValue(this.changeSet, Map.class);
//        } catch (IOException e) {
//            return null;
//        }
//    }

//    @JsonProperty(UPDATED_AT)
//    public String getEventTimeAsString() {
//        if (this.eventId == null) return null;
//        return DateUtil.toIsoMillisFormat(eventId);
//    }

    public String getEventTime() {
        if (this.eventId == null) return null;
        return DateUtil.toIsoMillisFormat(eventId);
    }


    public UUID getEventId() {
        return eventId;
    }

    public void setEventId(UUID eventId) {
        this.eventId = eventId;
        this.year = DateUtil.getYearOf(eventId);
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

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }
}
