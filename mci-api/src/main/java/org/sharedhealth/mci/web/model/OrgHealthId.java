package org.sharedhealth.mci.web.model;


import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;
import org.sharedhealth.mci.web.util.TimeUuidUtil;

import java.util.Date;
import java.util.UUID;

import static org.sharedhealth.mci.web.util.RepositoryConstants.*;

@Table(name = CF_ORG_HEALTH_ID)
public class OrgHealthId {

    @PartitionKey
    @Column(name = HEALTH_ID)
    private String healthId;

    @Column(name = GENERATED_AT)
    private UUID generatedAt;

    @Column(name = ALLOCATED_FOR)
    private String allocatedFor;

    @Column(name = USED_AT)
    private UUID usedAt;

    @Column(name = IS_USED)
    private boolean isUsed;


    public OrgHealthId(String healthId, String allocatedFor, UUID generatedAt, UUID usedAt) {
        this.healthId = healthId;
        this.allocatedFor = allocatedFor;
        this.generatedAt = generatedAt;
        this.usedAt = usedAt;
        this.isUsed = false;
    }

    public String getHealthId() {
        return healthId;
    }

    public String getAllocatedFor() {
        return allocatedFor;
    }

    public UUID getUsedAt() {
        return usedAt;
    }

    public boolean isUsed() {
        return isUsed;
    }

    public void markUsed() {
        this.isUsed = Boolean.TRUE;
        this.usedAt = TimeUuidUtil.uuidForDate(new Date());
    }

    public void setUsedAt(UUID uuid) {
        this.usedAt = uuid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        OrgHealthId that = (OrgHealthId) o;

        if (isUsed != that.isUsed) return false;
        if (healthId != null ? !healthId.equals(that.healthId) : that.healthId != null) return false;
        if (generatedAt != null ? !generatedAt.equals(that.generatedAt) : that.generatedAt != null) return false;
        if (allocatedFor != null ? !allocatedFor.equals(that.allocatedFor) : that.allocatedFor != null) return false;
        return !(usedAt != null ? !usedAt.equals(that.usedAt) : that.usedAt != null);

    }

    @Override
    public int hashCode() {
        int result = healthId != null ? healthId.hashCode() : 0;
        result = 31 * result + (generatedAt != null ? generatedAt.hashCode() : 0);
        result = 31 * result + (allocatedFor != null ? allocatedFor.hashCode() : 0);
        result = 31 * result + (usedAt != null ? usedAt.hashCode() : 0);
        result = 31 * result + (isUsed ? 1 : 0);
        return result;
    }
}

