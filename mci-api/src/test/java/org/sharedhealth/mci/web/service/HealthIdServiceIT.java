package org.sharedhealth.mci.web.service;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.*;
import org.sharedhealth.mci.web.BaseIntegrationTest;
import org.sharedhealth.mci.web.config.MCIProperties;
import org.sharedhealth.mci.web.model.IdentityStore;
import org.sharedhealth.mci.web.model.MciHealthId;
import org.sharedhealth.mci.web.model.MciHealthIdStore;
import org.sharedhealth.mci.web.util.TestUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;
import static org.sharedhealth.mci.web.util.HttpUtil.*;

public class HealthIdServiceIT extends BaseIntegrationTest {
    private HealthIdService healthIdService;
    private IdentityProviderService identityProviderService;
    private MciHealthIdStore mciHealthIdStore;

    @Rule
    public WireMockRule idpService = new WireMockRule(9997);
    private MCIProperties mciProperties;

    @Before
    public void setUp() throws Exception {
        identityProviderService = new IdentityProviderService(new IdentityStore());
        mciHealthIdStore = new MciHealthIdStore();
        mciProperties = MCIProperties.getInstance();
        healthIdService = new HealthIdService(identityProviderService, mciHealthIdStore, mciProperties);
    }

    @After
    public void tearDown() throws Exception {
        TestUtil.truncateAllColumnFamilies();
    }

    @Test
    public void shouldGetNextHealthId() throws Exception {
        List<String> hidBlock = Lists.newArrayList("healthId1", "healthId2");
        mciHealthIdStore.addMciHealthIds(hidBlock);
        File hidLocalStorageFile = new File(mciProperties.getHidLocalStoragePath());
        IOUtils.write(new Gson().toJson(hidBlock), new FileOutputStream(hidLocalStorageFile));

        MciHealthId nextHealthId = healthIdService.getNextHealthId();

        assertNotNull(nextHealthId);
        assertEquals(1, mciHealthIdStore.noOfHIDsLeft());
    }

    @Test
    public void shouldAskHIDServiceToMarkAsUsedHID() throws Exception {
        MciHealthId hid = new MciHealthId("hid");
        UUID token = UUID.randomUUID();
        setUpIdentityStub(token);
        setupMarkUsedStub("/healthIds/markUsed/hid", "Accepted", token);

        healthIdService.markUsed(hid);

        verify(1, putRequestedFor(urlMatching("/healthIds/markUsed/hid"))
                        .withRequestBody(containing("\"used_at\":"))
        );
    }

    @Test
    public void shouldPutBackTheHIDToStore() throws Exception {
        List<String> hidBlock = Lists.newArrayList("healthId1");
        mciHealthIdStore.addMciHealthIds(hidBlock);
        File hidLocalStorageFile = new File(mciProperties.getHidLocalStoragePath());
        IOUtils.write(new Gson().toJson(hidBlock), new FileOutputStream(hidLocalStorageFile));

        healthIdService.putBack(new MciHealthId("healthId2"));

        assertEquals(2, mciHealthIdStore.noOfHIDsLeft());
    }

    @Test
    public void shouldAsHIDServiceForTheFirstEverStartup() throws Exception {
        assertThat(mciHealthIdStore.noOfHIDsLeft(), is(0));
        assertFalse(new File(mciProperties.getHidLocalStoragePath()).exists());

        String nextHIDBlockUrl = String.format("/healthIds/nextBlock/mci/%s?blockSize=%s",
                mciProperties.getIdpClientId(), mciProperties.getHealthIdReplenishBlockSize());
        String hidResponse = getHidResponse();
        UUID uuid = UUID.randomUUID();
        setUpIdentityStub(uuid);
        setupNextHidStub(nextHIDBlockUrl, hidResponse, uuid);

        healthIdService.replenishIfNeeded();

        assertThat(mciHealthIdStore.noOfHIDsLeft(), is(mciProperties.getHealthIdReplenishBlockSize()));
        verify(1, postRequestedFor(urlMatching("/signin")));
        verify(1, getRequestedFor(urlPathMatching("/healthIds")));

        List<String> hids = readHIDsFromFile();
        List<String> expectedHIDs = getHIDs();
        assertEquals(hids.size(), expectedHIDs.size());
        assertTrue(hids.containsAll(expectedHIDs));
    }


    @Test
    public void shouldReplenishFromHIDServiceHIDCountReachesToThreshold() throws Exception {
        List<String> healthIdBlock = Lists.newArrayList("healthId1", "healthId2");
        mciHealthIdStore.addMciHealthIds(healthIdBlock);
        IOUtils.write(new Gson().toJson(healthIdBlock), new FileOutputStream(new File(mciProperties.getHidLocalStoragePath())));

        String nextHIDBlockUrl = String.format("/healthIds/nextBlock/mci/%s?blockSize=%s",
                mciProperties.getIdpClientId(), mciProperties.getHealthIdReplenishBlockSize());
        String hidResponse = getHidResponse();
        UUID uuid = UUID.randomUUID();
        setUpIdentityStub(uuid);
        setupNextHidStub(nextHIDBlockUrl, hidResponse, uuid);

        healthIdService.replenishIfNeeded();

        assertThat(mciHealthIdStore.noOfHIDsLeft(), is(2 + mciProperties.getHealthIdReplenishBlockSize()));
        verify(1, postRequestedFor(urlMatching("/signin")));
        verify(1, getRequestedFor(urlPathMatching("/healthIds")));

        List<String> hids = readHIDsFromFile();
        List<String> expectedHIDs = getHIDs();
        expectedHIDs.addAll(healthIdBlock);
        assertEquals(hids.size(), expectedHIDs.size());
        assertTrue(hids.containsAll(expectedHIDs));
    }

    @Test
    public void shouldNotReplenishIfTheThresholdIsNotReached() throws Exception {
        List<String> healthIdBlock = Lists.newArrayList("healthId1", "healthId2", "healthId3", "healthId4");
        mciHealthIdStore.addMciHealthIds(healthIdBlock);

        healthIdService.replenishIfNeeded();

        assertThat(mciHealthIdStore.noOfHIDsLeft(), is(4));
        verify(0, postRequestedFor(urlMatching("/signin")));
        verify(0, getRequestedFor(urlPathMatching("/healthIds")));
    }

    @Test
    public void shouldNotRequestHIDServiceWhenFileHasSufficientHIDsWhileInitialization() throws Exception {
        List<String> healthIdBlock = Lists.newArrayList("healthId1", "healthId2", "healthId3", "healthId4");
        IOUtils.write(new Gson().toJson(healthIdBlock), new FileOutputStream(new File(mciProperties.getHidLocalStoragePath())));

        assertThat(mciHealthIdStore.noOfHIDsLeft(), is(0));
        healthIdService.replenishIfNeeded();

        assertThat(mciHealthIdStore.noOfHIDsLeft(), is(4));
        verify(0, postRequestedFor(urlMatching("/signin")));
        verify(0, getRequestedFor(urlPathMatching("/healthIds")));
    }


    @Test
    public void shouldRequestHIDServiceWhenFileDoesNotHaveSufficientHIDsWhileInitialization() throws Exception {
        List<String> healthIdBlock = Lists.newArrayList("healthId1", "healthId2");
        IOUtils.write(new Gson().toJson(healthIdBlock), new FileOutputStream(new File(mciProperties.getHidLocalStoragePath())));
        assertThat(mciHealthIdStore.noOfHIDsLeft(), is(0));

        String nextHIDBlockUrl = String.format("/healthIds/nextBlock/mci/%s?blockSize=%s",
                mciProperties.getIdpClientId(), mciProperties.getHealthIdReplenishBlockSize());
        String hidResponse = getHidResponse();
        UUID uuid = UUID.randomUUID();
        setUpIdentityStub(uuid);
        setupNextHidStub(nextHIDBlockUrl, hidResponse, uuid);

        healthIdService.replenishIfNeeded();

        assertThat(mciHealthIdStore.noOfHIDsLeft(), is(2 + mciProperties.getHealthIdReplenishBlockSize()));
        verify(1, postRequestedFor(urlMatching("/signin")));
        verify(1, getRequestedFor(urlPathMatching("/healthIds")));

        List<String> hids = readHIDsFromFile();
        List<String> expectedHIDs = getHIDs();
        expectedHIDs.addAll(healthIdBlock);
        assertEquals(hids.size(), expectedHIDs.size());
        assertTrue(hids.containsAll(expectedHIDs));
    }

    private List<String> readHIDsFromFile() throws IOException {
        File hidLocalStorageFile = new File(mciProperties.getHidLocalStoragePath());
        assertTrue(hidLocalStorageFile.exists());
        String content = IOUtils.toString(new FileInputStream(hidLocalStorageFile), "UTF-8");
        return asList(new ObjectMapper().readValue(content, String[].class));
    }

    private void setupMarkUsedStub(String hidServiceUrl, String hidServiceResponse, UUID token) {
        stubFor(put(urlPathEqualTo(hidServiceUrl))
                .withHeader(X_AUTH_TOKEN_KEY, equalTo(token.toString()))
                .withHeader(CLIENT_ID_KEY, equalTo(mciProperties.getIdpClientId()))
                .withHeader(FROM_KEY, equalTo(mciProperties.getIdpEmail()))
                .willReturn(aResponse()
                                .withStatus(HttpStatus.SC_OK)
                                .withBody(hidServiceResponse)
                ));
    }

    private void setupNextHidStub(String hidServiceUrl, String hidServiceResponse, UUID token) {
        stubFor(get(urlPathEqualTo(hidServiceUrl))
                .withHeader(X_AUTH_TOKEN_KEY, equalTo(token.toString()))
                .withHeader(CLIENT_ID_KEY, equalTo(mciProperties.getIdpClientId()))
                .withHeader(FROM_KEY, equalTo(mciProperties.getIdpEmail()))
                .willReturn(aResponse()
                                .withStatus(HttpStatus.SC_OK)
                                .withBody(hidServiceResponse)
                ));
    }

    private void setUpIdentityStub(UUID uuid) {
        UUID token = uuid;
        String idpResponse = "{\"access_token\" : \"" + token.toString() + "\"}";

        stubFor(post(urlMatching("/signin"))
                .withHeader(X_AUTH_TOKEN_KEY, equalTo(mciProperties.getIdpXAuthToken()))
                .withHeader(CLIENT_ID_KEY, equalTo(mciProperties.getIdpClientId()))
                .withRequestBody(containing("password=password&email=shrSysAdmin%40gmail.com"))
                .willReturn(aResponse()
                                .withStatus(HttpStatus.SC_OK)
                                .withBody(idpResponse)
                ));
    }

    private String getHidResponse() {
        HashMap<String, Object> hidResponse = new HashMap<>();

        hidResponse.put("total", "10");
        hidResponse.put("hids", getHIDs());
        return new Gson().toJson(hidResponse);
    }

    private List<String> getHIDs() {
        return Lists.newArrayList("98000430630",
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