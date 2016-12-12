package org.sharedhealth.mci.web.service;

import com.google.common.base.Charsets;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.sharedhealth.mci.web.WebClient;
import org.sharedhealth.mci.web.config.MCIProperties;
import org.sharedhealth.mci.web.launch.Application;
import org.sharedhealth.mci.web.model.MciHealthId;
import org.sharedhealth.mci.web.model.MciHealthIdStore;
import org.sharedhealth.mci.web.repository.PatientRepository;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.sharedhealth.mci.web.util.HttpUtil.*;

public class HealthIdService {
    private static final Logger logger = LogManager.getLogger(HealthIdService.class);

    private static final String HEALTH_ID_LIST_KEY = "hids";

    private IdentityProviderService identityProviderService;
    private MciHealthIdStore mciHealthIdStore;
    private MCIProperties mciProperties;
    private PatientRepository patientRepository;

    public HealthIdService(IdentityProviderService identityProviderService,
                           MciHealthIdStore mciHealthIdStore, MCIProperties mciProperties) {
        this.identityProviderService = identityProviderService;
        this.mciHealthIdStore = mciHealthIdStore;
        this.mciProperties = mciProperties;
        this.patientRepository = Application.getPatientRepository();
    }

    public MciHealthId getNextHealthId() {
        String nextHealthId = mciHealthIdStore.getNextHealthId();
        return new MciHealthId(nextHealthId);
    }


    public void putBack(MciHealthId healthId) {
        mciHealthIdStore.addMciHealthIds(Arrays.asList(healthId.getHid()));
    }

    public void loadFromFile() throws IOException {
        logger.info("Loading HealthIds from file.");
        List<String> existingHIDs = getExistingHIDs();
        ConcurrentLinkedQueue<String> hidBlock = new ConcurrentLinkedQueue<>();
        for (String existingHID : existingHIDs) {
            if (null == patientRepository.findByHealthId(existingHID)) {
                hidBlock.add(existingHID);
            }
        }
        addMciHealthIds(hidBlock);
    }

    public void replenishIfNeeded() throws IOException {
        if (mciHealthIdStore.noOfHIDsLeft() > mciProperties.getHealthIdReplenishThreshold()) return;
        List nextBlock = getNextBlockFromHidService();
        if (nextBlock != null) {
            addMciHealthIds(nextBlock);
        }
    }

    private void addMciHealthIds(Collection nextBlock) throws IOException {
        Collection hids = CollectionUtils.union(mciHealthIdStore.getAll(), nextBlock);
        writeHIDsToFile(hids);
        mciHealthIdStore.addMciHealthIds(nextBlock);
        logger.info("Replenished {} healthIds from HID service", nextBlock.size());
    }

    private void writeHIDsToFile(Collection hids) throws IOException {
        IOUtils.writeLines(hids, IOUtils.LINE_SEPARATOR_UNIX,
                new FileOutputStream(mciProperties.getHidLocalStoragePath()), Charsets.UTF_8);
    }

    private List getNextBlockFromHidService() throws IOException {
        String hidServiceNextBlockPath = getHidServiceNextBlockPath();
        String response = new WebClient().get(mciProperties.getHidServiceBaseUrl(), hidServiceNextBlockPath, getHIDServiceHeaders());
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
        File file = getHidFile();
        return IOUtils.readLines(new FileInputStream(file), Charsets.UTF_8);
    }

    private File getHidFile() throws IOException {
        String filePath = mciProperties.getHidLocalStoragePath();
        File file = new File(filePath);
        if (!file.isFile() && !file.createNewFile()) {
            throw new IOException("Error creating new file: " + file.getAbsolutePath());
        }
        return file;
    }

    private String getHidServiceNextBlockPath() {
        return String.format(mciProperties.getHidServiceNextBlockUrlPattern(),
                mciProperties.getMciOrgCode(), mciProperties.getHealthIdReplenishBlockSize());
    }
}
