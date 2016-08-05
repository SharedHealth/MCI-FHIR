package org.sharedhealth.mci.web.service;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import com.datastax.driver.mapping.Mapper;
import com.datastax.driver.mapping.MappingManager;
import org.sharedhealth.mci.web.model.MciHealthId;

import static org.sharedhealth.mci.web.util.RepositoryConstants.CF_MCI_HEALTH_ID;
import static org.sharedhealth.mci.web.util.RepositoryConstants.HID;

public class HealthIdService {
    private Session session;
    private Mapper<MciHealthId> healthIdMapper;

    public HealthIdService(MappingManager mappingManager) {
        this.session = mappingManager.getSession();
        this.healthIdMapper = mappingManager.mapper(MciHealthId.class);
    }

    public MciHealthId getNextHealthId() {
        Select select = QueryBuilder.select().all().from(CF_MCI_HEALTH_ID).limit(1);
        ResultSet rows = session.execute(select);
        if (rows.isExhausted()) {
            throw new RuntimeException("No HIDs available to assign");
        }
        String healthId = rows.one().get(HID, String.class);
        return new MciHealthId(healthId);
    }

    public void markUsed(MciHealthId healthId) {
        healthIdMapper.delete(healthId);
    }
}
