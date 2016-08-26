package org.sharedhealth.mci.web.service;

import com.datastax.driver.mapping.Mapper;
import com.datastax.driver.mapping.MappingManager;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sharedhealth.mci.web.BaseIntegrationTest;
import org.sharedhealth.mci.web.config.MCICassandraConfig;
import org.sharedhealth.mci.web.model.IdentityStore;
import org.sharedhealth.mci.web.model.MciHealthId;
import org.sharedhealth.mci.web.model.MciHealthIdStore;
import org.sharedhealth.mci.web.model.OrgHealthId;
import org.sharedhealth.mci.web.util.TestUtil;

import java.util.ArrayList;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

public class HealthIdServiceIT extends BaseIntegrationTest {
    private Mapper<MciHealthId> mciHealthIdMapper;
    private Mapper<OrgHealthId> orgHealthIdMapper;
    private HealthIdService healthIdService;
    private IdentityProviderService identityProviderService;
    private MciHealthIdStore mciHealthIdStore;

    @Rule
    public WireMockRule idpService = new WireMockRule(9997);

    @Before
    public void setUp() throws Exception {
        identityProviderService = new IdentityProviderService(new IdentityStore());
        mciHealthIdStore = new MciHealthIdStore();
        MappingManager mappingManager = MCICassandraConfig.getInstance().getMappingManager();
        healthIdService = new HealthIdService(mappingManager, identityProviderService, mciHealthIdStore);
        mciHealthIdMapper = mappingManager.mapper(MciHealthId.class);
        orgHealthIdMapper = mappingManager.mapper(OrgHealthId.class);
    }

    @After
    public void tearDown() throws Exception {
        TestUtil.truncateAllColumnFamilies();
    }

    @Test
    public void shouldGetHIDFromDatabase() throws Exception {
        MciHealthId hid = new MciHealthId("Hid");
        mciHealthIdMapper.save(hid);

        MciHealthId nextHealthId = healthIdService.getNextHealthId();
        assertNotNull(nextHealthId);
    }

    @Test(expected = RuntimeException.class)
    public void shouldThrowAnExceptionWhenHIDNotFound() throws Exception {
        healthIdService.getNextHealthId();
    }

    @Test
    public void shouldDeleteFromMCIHealthIdAndPutInOrgHIDWhenAHIDMarkedAsUsed() throws Exception {
        String hid = "hid";
        MciHealthId mciHealthId = new MciHealthId(hid);
        mciHealthIdMapper.save(mciHealthId);
        assertNull(orgHealthIdMapper.get(hid));

        healthIdService.markUsed(mciHealthId);
        assertNull(mciHealthIdMapper.get(hid));
        assertNotNull(orgHealthIdMapper.get(hid));

    }

    @Test
    public void shouldNotReplenishIfTheThresholdIsNotReached() throws Exception {
        List<String> healthIdBlock = new ArrayList<>();
        healthIdBlock.add("healthId1");
        healthIdBlock.add("healthId2");
        healthIdBlock.add("healthId3");
        healthIdBlock.add("healthId4");
        mciHealthIdStore.addMciHealthIds(healthIdBlock);

        healthIdService.replenishIfNeeded();

        assertThat(mciHealthIdStore.noOfHidsLeft(), is(4));
    }

    @Test
    public void shouldReplenishIfTheThresholdIsReached() throws Exception {
        List<String> healthIdBlock = new ArrayList<>();
        healthIdBlock.add("healthId1");
        healthIdBlock.add("healthId2");
        mciHealthIdStore.addMciHealthIds(healthIdBlock);

        stubFor(get(urlMatching("/signin")))

        healthIdService.replenishIfNeeded();

        assertThat(mciHealthIdStore.noOfHidsLeft(), is(4));
    }
}