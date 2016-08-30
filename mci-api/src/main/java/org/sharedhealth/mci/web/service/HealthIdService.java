package org.sharedhealth.mci.web.service;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import com.datastax.driver.mapping.Mapper;
import com.datastax.driver.mapping.MappingManager;
import com.google.gson.Gson;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.sharedhealth.mci.web.WebClient;
import org.sharedhealth.mci.web.config.MCIProperties;
import org.sharedhealth.mci.web.model.MciHealthId;
import org.sharedhealth.mci.web.model.MciHealthIdStore;
import org.sharedhealth.mci.web.model.OrgHealthId;
import org.sharedhealth.mci.web.util.TimeUuidUtil;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

import static org.sharedhealth.mci.web.util.HttpUtil.*;
import static org.sharedhealth.mci.web.util.MCIConstants.URL_SEPARATOR;
import static org.sharedhealth.mci.web.util.RepositoryConstants.CF_MCI_HEALTH_ID;
import static org.sharedhealth.mci.web.util.RepositoryConstants.HID;
import static org.sharedhealth.mci.web.util.StringUtils.ensureSuffix;
import static org.sharedhealth.mci.web.util.StringUtils.removePrefix;

public class HealthIdService {
    private static final Logger logger = LogManager.getLogger(HealthIdService.class);
    private final String MCI_ORG_CODE = "MCI";

    private static final String HEALTH_ID_LIST_KEY = "hids";

    private IdentityProviderService identityProviderService;
    private MciHealthIdStore mciHealthIdStore;
    private Session session;
    private Mapper<MciHealthId> mciHealthIdMapper;
    private Mapper<OrgHealthId> orgHealthIdMapper;

    public HealthIdService(MappingManager mappingManager, IdentityProviderService identityProviderService,
                           MciHealthIdStore mciHealthIdStore) {
        this.identityProviderService = identityProviderService;
        this.mciHealthIdStore = mciHealthIdStore;
        this.session = mappingManager.getSession();
        this.mciHealthIdMapper = mappingManager.mapper(MciHealthId.class);
        this.orgHealthIdMapper = mappingManager.mapper(OrgHealthId.class);
    }

    public MciHealthId getNextHealthId() {
        Select select = QueryBuilder.select().all().from(CF_MCI_HEALTH_ID).limit(1);
        ResultSet rows = session.execute(select);
        if (rows.isExhausted()) {
            String hidExhaustedErrorMessage = "No HIDs available to assign";
            logger.error(hidExhaustedErrorMessage);
            throw new RuntimeException(hidExhaustedErrorMessage);
        }
        String healthId = rows.one().get(HID, String.class);
        return new MciHealthId(healthId);
    }

    public void markUsed(MciHealthId healthId) {
        OrgHealthId orgHealthId = new OrgHealthId(healthId.getHid(), MCI_ORG_CODE, null, TimeUuidUtil.uuidForDate(new Date()));
        orgHealthId.markUsed();
        orgHealthIdMapper.save(orgHealthId);
        mciHealthIdMapper.delete(healthId);
    }

    public void replenishIfNeeded() throws IOException {
        //todo: should inject mciProperties dependency in constructor
        MCIProperties mciProperties = MCIProperties.getInstance();
        if (mciHealthIdStore.noOfHIDsLeft() > mciProperties.getHealthIdReplenishThreshold()) return;
        List<String> existingHIDs = getExistingHIDs(mciProperties);
         /*
            * this is a case when in some situation it was not able to delete a HID from file after patient create
            * ideally we can overwrite the file with memory contents
            if (mciHealthIdStore.noOfHIDsLeft() > 0 && existingHIDs.size() > mciHealthIdStore.noOfHIDsLeft()) {
           }
           * */
        mciHealthIdStore.clear();
        mciHealthIdStore.addMciHealthIds(existingHIDs);
        if (mciHealthIdStore.noOfHIDsLeft() > mciProperties.getHealthIdReplenishThreshold()) return;

        List nextBlock = getNextBlockFromHidService(mciProperties);
        if (nextBlock != null) {
            mciHealthIdStore.addMciHealthIds(nextBlock);
            String hidsContent = new Gson().toJson(mciHealthIdStore.getAll());
            IOUtils.write(hidsContent, new FileOutputStream(mciProperties.getHidLocalStoragePath()));
        }
    }

    private List getNextBlockFromHidService(MCIProperties mciProperties) throws IOException {
        String idpToken = identityProviderService.getOrCreateIdentityToken(mciProperties);

        String hidServiceNextBlockURL = getHidServiceNextBlockURL(mciProperties);
        Map<String, String> healthIdServiceHeader = new HashMap<>();
        healthIdServiceHeader.put(X_AUTH_TOKEN_KEY, idpToken);
        healthIdServiceHeader.put(CLIENT_ID_KEY, mciProperties.getIdpClientId());
        healthIdServiceHeader.put(FROM_KEY, mciProperties.getIdpEmail());

        String response = new WebClient().get(hidServiceNextBlockURL, healthIdServiceHeader);
        if (response != null) {
            Map map = new ObjectMapper().readValue(response, Map.class);
            return (List) map.get(HEALTH_ID_LIST_KEY);
        }
        return null;
    }

    private List<String> getExistingHIDs(MCIProperties mciProperties) throws IOException {
        try {
            String content = IOUtils.toString(new FileInputStream(mciProperties.getHidLocalStoragePath()), "UTF-8");
            String[] hids = new ObjectMapper().readValue(content, String[].class);
            return Arrays.asList(hids);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    private String getHidServiceNextBlockURL(MCIProperties mciProperties) {
        String nextHIDBlockPath = String.format(mciProperties.getHidServiceNextBlockUrlPattern(),
                mciProperties.getIdpClientId(), mciProperties.getHealthIdReplenishBlockSize());
        return ensureSuffix(mciProperties.getHidServiceBaseUrl(), URL_SEPARATOR) +
                removePrefix(nextHIDBlockPath, URL_SEPARATOR);
    }
}
