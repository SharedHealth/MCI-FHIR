package org.sharedhealth.mci.web.service;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
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
        mciHealthIdStore = MciHealthIdStore.getInstance();
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

        healthIdService.putBack(new MciHealthId("healthId2"));

        assertEquals(2, mciHealthIdStore.noOfHIDsLeft());
    }

    @Test
    public void shouldAskHIDServiceForTheFirstEverStartup() throws Exception {
        assertThat(mciHealthIdStore.noOfHIDsLeft(), is(0));
        assertFalse(new File(mciProperties.getHidLocalStoragePath()).exists());

        String nextHIDBlockUrl = String.format("/healthIds/nextBlock/mci/%s?blockSize=%s",
                mciProperties.getIdpClientId(), mciProperties.getHealthIdReplenishBlockSize());
        List<String> expectedHIDs = getHIDs("9800043063", 10);
        String hidResponse = getHidResponse(expectedHIDs);
        UUID uuid = UUID.randomUUID();
        setUpIdentityStub(uuid);
        setupNextHidStub(nextHIDBlockUrl, hidResponse, uuid);

        healthIdService.replenishIfNeeded();

        assertThat(mciHealthIdStore.noOfHIDsLeft(), is(mciProperties.getHealthIdReplenishBlockSize()));
        verify(1, postRequestedFor(urlMatching("/signin")));
        verify(1, getRequestedFor(urlPathMatching("/healthIds")));

        List<String> hids = readHIDsFromFile();
        assertEquals(hids.size(), expectedHIDs.size());
        assertTrue(hids.containsAll(expectedHIDs));
    }

    @Test
    public void shouldReplenishFromHIDServiceHIDCountReachesToThreshold() throws Exception {
        List<String> initialHealthIdBlock = getHIDs("9800043044", 10);
        IOUtils.write(new Gson().toJson(initialHealthIdBlock), new FileOutputStream(new File(mciProperties.getHidLocalStoragePath())));

        List<String> healthIdBlock = initialHealthIdBlock.subList(0, 1);

        mciHealthIdStore.addMciHealthIds(healthIdBlock);

        String nextHIDBlockUrl = String.format("/healthIds/nextBlock/mci/%s?blockSize=%s",
                mciProperties.getIdpClientId(), mciProperties.getHealthIdReplenishBlockSize());
        List<String> newHealthIds = getHIDs("9800043063", 10);
        String hidResponse = getHidResponse(newHealthIds);
        UUID uuid = UUID.randomUUID();
        setUpIdentityStub(uuid);
        setupNextHidStub(nextHIDBlockUrl, hidResponse, uuid);

        healthIdService.replenishIfNeeded();

        verify(1, postRequestedFor(urlMatching("/signin")));
        verify(1, getRequestedFor(urlPathMatching("/healthIds")));

        assertThat(mciHealthIdStore.noOfHIDsLeft(), is(healthIdBlock.size() + mciProperties.getHealthIdReplenishBlockSize()));

        List<String> hidsInFile = readHIDsFromFile();
        List<String> expectedHIDs = ListUtils.union(newHealthIds, healthIdBlock);
        assertEquals(hidsInFile.size(), expectedHIDs.size());
        assertTrue(hidsInFile.containsAll(expectedHIDs));

        assertEquals(mciHealthIdStore.noOfHIDsLeft(), hidsInFile.size());
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

    private List<String> readHIDsFromFile() throws IOException {
        File hidLocalStorageFile = new File(mciProperties.getHidLocalStoragePath());
        assertTrue(hidLocalStorageFile.exists());
        return IOUtils.readLines(new FileInputStream(hidLocalStorageFile), "UTF-8");
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

    private String getHidResponse(List<String> hids) {
        HashMap<String, Object> hidResponse = new HashMap<>();

        hidResponse.put("total", "10");
        hidResponse.put("hids", hids);
        return new Gson().toJson(hidResponse);
    }

    private List<String> getHIDs(String prefix, int noOfHealthIds) {
        ArrayList<String> hids = new ArrayList<>();
        for (int i = 0; i < noOfHealthIds; i++) {
            hids.add(prefix + i);
        }
        return hids;
    }
}