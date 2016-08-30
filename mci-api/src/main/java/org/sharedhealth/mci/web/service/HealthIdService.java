package org.sharedhealth.mci.web.service;

import com.google.gson.Gson;
import org.apache.commons.io.IOUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.sharedhealth.mci.web.WebClient;
import org.sharedhealth.mci.web.config.MCIProperties;
import org.sharedhealth.mci.web.model.MciHealthId;
import org.sharedhealth.mci.web.model.MciHealthIdStore;
import org.sharedhealth.mci.web.util.TimeUuidUtil;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

import static org.sharedhealth.mci.web.util.HttpUtil.*;
import static org.sharedhealth.mci.web.util.MCIConstants.URL_SEPARATOR;
import static org.sharedhealth.mci.web.util.StringUtils.ensureSuffix;
import static org.sharedhealth.mci.web.util.StringUtils.removePrefix;

public class HealthIdService {

    private final String USED_AT_KEY = "used_at";
    private static final String HEALTH_ID_LIST_KEY = "hids";

    private IdentityProviderService identityProviderService;
    private MciHealthIdStore mciHealthIdStore;
    private MCIProperties mciProperties;

    public HealthIdService(IdentityProviderService identityProviderService,
                           MciHealthIdStore mciHealthIdStore, MCIProperties mciProperties) {
        this.identityProviderService = identityProviderService;
        this.mciHealthIdStore = mciHealthIdStore;
        this.mciProperties = mciProperties;
    }

    public MciHealthId getNextHealthId() throws IOException {
        System.out.println("next");
        String nextHealthId = mciHealthIdStore.getNextHealthId();
        writeHIDsToFile();
        return new MciHealthId(nextHealthId);
    }

    public void markUsed(MciHealthId healthId) {
        UUID usedAt = TimeUuidUtil.uuidForDate(new Date());
        String markUsedPath = String.format(mciProperties.getHidServiceMarkUsedUrlPattern(), healthId.getHid());
        String markUsedUrl = ensureSuffix(mciProperties.getHidServiceBaseUrl(), URL_SEPARATOR)
                + removePrefix(markUsedPath, URL_SEPARATOR);
        try {
            Map<String, String> hidServiceHeaders = getHIDServiceHeaders();
            Map<String, String> data = new HashMap<>();
            data.put(USED_AT_KEY, usedAt.toString());
            new WebClient().put(markUsedUrl, hidServiceHeaders, data);
            //need to send to failed events if failure
        } catch (IOException e) {
            e.printStackTrace();
            //need to send to failed events if failure
        }
    }

    public void putBack(MciHealthId healthId) {
        mciHealthIdStore.addMciHealthIds(Arrays.asList(healthId.getHid()));
        try {
            writeHIDsToFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void replenishIfNeeded() throws IOException {
        System.out.println("hid");
        //todo: should inject mciProperties dependency in constructor
        if (mciHealthIdStore.noOfHIDsLeft() > mciProperties.getHealthIdReplenishThreshold()) return;
        List<String> existingHIDs = getExistingHIDs();
         /*
            * this is a case when in some situation it was not able to delete a HID from file after patient create
            * ideally we can overwrite the file with memory contents
            if (mciHealthIdStore.noOfHIDsLeft() > 0 && existingHIDs.size() > mciHealthIdStore.noOfHIDsLeft()) {
           }
           * */
        mciHealthIdStore.clear();
        mciHealthIdStore.addMciHealthIds(existingHIDs);
        if (mciHealthIdStore.noOfHIDsLeft() > mciProperties.getHealthIdReplenishThreshold()) return;
        System.out.println("existing");
        List nextBlock = getNextBlockFromHidService();
        System.out.println("from request");
        if (nextBlock != null) {
            mciHealthIdStore.addMciHealthIds(nextBlock);
            writeHIDsToFile();
        }
    }

    private void writeHIDsToFile() throws IOException {
        String hidsContent = new Gson().toJson(mciHealthIdStore.getAll());
        IOUtils.write(hidsContent, new FileOutputStream(mciProperties.getHidLocalStoragePath()));
    }

    private List getNextBlockFromHidService() throws IOException {
        String hidServiceNextBlockURL = getHidServiceNextBlockURL();
        String response = new WebClient().get(hidServiceNextBlockURL, getHIDServiceHeaders());
        if (response != null) {
            Map map = new ObjectMapper().readValue(response, Map.class);
            return (List) map.get(HEALTH_ID_LIST_KEY);
        }
        return null;
    }

    private Map<String, String> getHIDServiceHeaders() throws IOException {
        Map<String, String> healthIdServiceHeader = new HashMap<>();
        String idpToken = identityProviderService.getOrCreateIdentityToken(mciProperties);
        healthIdServiceHeader.put(X_AUTH_TOKEN_KEY, idpToken);
        healthIdServiceHeader.put(CLIENT_ID_KEY, mciProperties.getIdpClientId());
        healthIdServiceHeader.put(FROM_KEY, mciProperties.getIdpEmail());
        return healthIdServiceHeader;
    }

    private List<String> getExistingHIDs() throws IOException {
        try {
            String content = IOUtils.toString(new FileInputStream(mciProperties.getHidLocalStoragePath()), "UTF-8");
            String[] hids = new ObjectMapper().readValue(content, String[].class);
            return Arrays.asList(hids);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    private String getHidServiceNextBlockURL() {
        String nextHIDBlockPath = String.format(mciProperties.getHidServiceNextBlockUrlPattern(),
                mciProperties.getIdpClientId(), mciProperties.getHealthIdReplenishBlockSize());
        return ensureSuffix(mciProperties.getHidServiceBaseUrl(), URL_SEPARATOR) +
                removePrefix(nextHIDBlockPath, URL_SEPARATOR);
    }
}
