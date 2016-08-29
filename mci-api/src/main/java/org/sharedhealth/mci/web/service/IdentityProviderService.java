package org.sharedhealth.mci.web.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.sharedhealth.mci.web.WebClient;
import org.sharedhealth.mci.web.config.MCIProperties;
import org.sharedhealth.mci.web.exception.IdentityUnauthorizedException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class IdentityProviderService {
    private final static Logger logger = LogManager.getLogger(IdentityProviderService.class);

    private static final String ACCESS_TOKEN_KEY = "access_token";
    private static final String PASSWORD_KEY = "password";
    private static final String EMAIL_KEY = "email";
    private static final String CLIENT_KEY = "client_id";
    private static final String X_AUTH_TOKEN_KEY = "X-Auth-Token";

    private String identityToken;

    public String getOrCreateIdentityToken(MCIProperties mciProperties) throws IOException {
        if (identityToken == null) {
            try {
                identityToken = getIdentityTokenFromIdp(mciProperties);
            } catch (IdentityUnauthorizedException e) {
                logger.info("Refreshing Identity Token.");
                identityToken = null;
            }
        }
        return identityToken;
    }

    private String getIdentityTokenFromIdp(MCIProperties mciProperties) throws IOException {
        String idpUrl = mciProperties.getIdpUrl();
        Map<String, String> headers = new HashMap<>();
        headers.put(X_AUTH_TOKEN_KEY, mciProperties.getIdpXAuthToken());
        headers.put(CLIENT_KEY, mciProperties.getIdpClientId());
        Map<String, String> formEntities = new HashMap<>();
        formEntities.put(EMAIL_KEY, mciProperties.getIdpEmail());
        formEntities.put(PASSWORD_KEY, mciProperties.getIdpPassword());
        String response = new WebClient().post(idpUrl, headers, formEntities);
        if (response != null) {
            Map map = new ObjectMapper().readValue(response, Map.class);
            return (String) map.get(ACCESS_TOKEN_KEY);
        }
        return null;
    }
}
