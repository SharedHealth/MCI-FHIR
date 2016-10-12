package org.sharedhealth.mci.web.model;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class IdentityStore {
    private final static Logger logger = LogManager.getLogger(IdentityStore.class);

    private String identityToken;

    public void setIdentityToken(String identityToken) {
        this.identityToken = identityToken;
    }

    public String getIdentityToken() {
        return identityToken;
    }

    public String clearIdentityToken() {
        logger.debug("Clearing Identity Token");
        return identityToken = null;
    }

    public boolean hasIdentityToken() {
        return identityToken != null;
    }
}
