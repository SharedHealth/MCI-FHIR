package org.sharedhealth.mci.web.service;

import com.datastax.driver.mapping.Mapper;
import com.datastax.driver.mapping.MappingManager;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.gson.Gson;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sharedhealth.mci.web.BaseIntegrationTest;
import org.sharedhealth.mci.web.config.MCICassandraConfig;
import org.sharedhealth.mci.web.config.MCIProperties;
import org.sharedhealth.mci.web.model.IdentityStore;
import org.sharedhealth.mci.web.model.MciHealthId;
import org.sharedhealth.mci.web.model.MciHealthIdStore;
import org.sharedhealth.mci.web.model.OrgHealthId;
import org.sharedhealth.mci.web.util.TestUtil;

import java.io.File;
import java.io.FileInputStream;
import java.util.*;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;
import static org.sharedhealth.mci.web.util.HttpUtil.*;

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
        MCIProperties mciProperties = MCIProperties.getInstance();

        List<String> healthIdBlock = new ArrayList<>();
        healthIdBlock.add("healthId1");
        healthIdBlock.add("healthId2");
        healthIdBlock.add("healthId3");
        healthIdBlock.add("healthId4");
        mciHealthIdStore.addMciHealthIds(healthIdBlock);

        healthIdService.replenishIfNeeded();

        assertThat(mciHealthIdStore.noOfHIDsLeft(), is(4));

        verify(0, postRequestedFor(urlMatching("/signin"))
                        .withHeader(X_AUTH_TOKEN_KEY, equalTo(mciProperties.getIdpXAuthToken()))
                        .withHeader(CLIENT_ID_KEY, equalTo(mciProperties.getIdpClientId()))
                        .withRequestBody(containing("password=password&email=shrSysAdmin%40gmail.com"))
        );

        verify(0, getRequestedFor(urlPathMatching("/healthIds"))
                        .withHeader(CLIENT_ID_KEY, equalTo(mciProperties.getIdpClientId()))
                        .withHeader(FROM_KEY, equalTo(mciProperties.getIdpEmail()))
        );
    }

    @Test
    public void shouldAskHIDServiceForTheFirstEverStartup() throws Exception {
        MCIProperties mciProperties = MCIProperties.getInstance();
        assertThat(mciHealthIdStore.noOfHIDsLeft(), is(0));
        File hidLocalStorageFile = new File(mciProperties.getHidLocalStoragePath());
        assertFalse(hidLocalStorageFile.exists());

        setupStub(mciProperties);
        healthIdService.replenishIfNeeded();

        assertThat(mciHealthIdStore.noOfHIDsLeft(), is(mciProperties.getHealthIdReplenishBlockSize()));
        verify(1, postRequestedFor(urlMatching("/signin")));
        verify(1, getRequestedFor(urlPathMatching("/healthIds")));

        assertTrue(hidLocalStorageFile.exists());
        String content = IOUtils.toString(new FileInputStream(hidLocalStorageFile), "UTF-8");
        List<String> hids = Arrays.asList(new ObjectMapper().readValue(content, String[].class));

        List<String> expectedHIDs = getHIDs();
        assertEquals(hids.size(), expectedHIDs.size());
        assertTrue(hids.containsAll(expectedHIDs));
    }

    @Test
    public void shouldReplenishFromHIDServiceHIDCountReachesToThreshold() throws Exception {
        MCIProperties mciProperties = MCIProperties.getInstance();
        List<String> healthIdBlock = new ArrayList<>();
        healthIdBlock.add("healthId1");
        healthIdBlock.add("healthId2");
        mciHealthIdStore.addMciHealthIds(healthIdBlock);

        setupStub(mciProperties);

        healthIdService.replenishIfNeeded();
        assertThat(mciHealthIdStore.noOfHIDsLeft(), is(2 + mciProperties.getHealthIdReplenishBlockSize()));

        verify(1, postRequestedFor(urlMatching("/signin")));
        verify(1, getRequestedFor(urlPathMatching("/healthIds")));
    }

    private void setupStub(MCIProperties mciProperties) {
        String nextHIDBlockUrl = String.format("/healthIds/nextBlock/mci/%s?blockSize=%s",
                mciProperties.getIdpClientId(), mciProperties.getHealthIdReplenishBlockSize());

        UUID token = UUID.randomUUID();
        String idpResponse = "{\"access_token\" : \"" + token.toString() + "\"}";
        String hidResponse = getHidResponse();

        stubFor(post(urlMatching("/signin"))
                .withHeader(X_AUTH_TOKEN_KEY, equalTo(mciProperties.getIdpXAuthToken()))
                .withHeader(CLIENT_ID_KEY, equalTo(mciProperties.getIdpClientId()))
                .withRequestBody(containing("password=password&email=shrSysAdmin%40gmail.com"))
                .willReturn(aResponse()
                                .withStatus(HttpStatus.SC_OK)
                                .withBody(idpResponse)
                ));

        stubFor(get(urlPathEqualTo(nextHIDBlockUrl))
                .withHeader(X_AUTH_TOKEN_KEY, equalTo(token.toString()))
                .withHeader(CLIENT_ID_KEY, equalTo(mciProperties.getIdpClientId()))
                .withHeader(FROM_KEY, equalTo(mciProperties.getIdpEmail()))
                .willReturn(aResponse()
                                .withStatus(HttpStatus.SC_OK)
                                .withBody(hidResponse)
                ));
    }

    private String getHidResponse() {
        HashMap<String, Object> hidResponse = new HashMap<>();

        hidResponse.put("total", "10");
        hidResponse.put("hids", getHIDs());
        return new Gson().toJson(hidResponse);
    }

    private List<String> getHIDs() {
        return asList("98000430630",
                "98000429756",
                "98000430531",
                "98000430507",
                "98000430341",
                "98000430564",
                "98000429145",
                "98000430911",
                "98000429061",
                "98000430333");
    }
}