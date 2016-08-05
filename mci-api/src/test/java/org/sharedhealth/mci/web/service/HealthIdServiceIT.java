package org.sharedhealth.mci.web.service;

import com.datastax.driver.mapping.Mapper;
import com.datastax.driver.mapping.MappingManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sharedhealth.mci.web.BaseIntegrationTest;
import org.sharedhealth.mci.web.config.MCICassandraConfig;
import org.sharedhealth.mci.web.model.MciHealthId;
import org.sharedhealth.mci.web.util.TestUtil;

import static org.junit.Assert.*;

public class HealthIdServiceIT extends BaseIntegrationTest{
    private Mapper<MciHealthId> healthIdMapper;
    private HealthIdService healthIdService;

    @Before
    public void setUp() throws Exception {
        MappingManager mappingManager = MCICassandraConfig.getInstance().getMappingManager();
        healthIdService = new HealthIdService(mappingManager);
        healthIdMapper = mappingManager.mapper(MciHealthId.class);
    }

    @After
    public void tearDown() throws Exception {
        TestUtil.truncateAllColumnFamilies();
    }

    @Test
    public void shouldGetHIDFromDatabase() throws Exception {
        MciHealthId hid = new MciHealthId("Hid");
        healthIdMapper.save(hid);

        MciHealthId nextHealthId = healthIdService.getNextHealthId();
        assertNotNull(nextHealthId);
    }

    @Test(expected = RuntimeException.class)
    public void shouldThrowAnExceptionWhenHIDNotFound() throws Exception {
        healthIdService.getNextHealthId();
    }
}