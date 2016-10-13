package org.sharedhealth.mci.web.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sharedhealth.mci.web.WebClient;
import org.sharedhealth.mci.web.config.MCIProperties;
import org.sharedhealth.mci.web.exception.IdentityUnauthorizedException;
import org.sharedhealth.mci.web.exception.NotFoundException;
import org.sharedhealth.mci.web.model.IdentityStore;
import org.sharedhealth.mci.web.security.UserInfo;

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
    private IdentityStore identityStore;

    public IdentityProviderService(IdentityStore identityStore) {
        this.identityStore = identityStore;
    }

    public String getOrCreateIdentityToken(MCIProperties mciProperties) throws IOException {
        if (!identityStore.hasIdentityToken()) {
            try {
                identityStore.setIdentityToken(getIdentityTokenFromIdp(mciProperties));
            } catch (IdentityUnauthorizedException e) {
                logger.info("Refreshing Identity Token.");
                identityStore.clearIdentityToken();
            }
        }
        return identityStore.getIdentityToken();
    }

    private String getIdentityTokenFromIdp(MCIProperties mciProperties) throws IOException {
        Map<String, String> headers = new HashMap<>();
        headers.put(X_AUTH_TOKEN_KEY, mciProperties.getIdpXAuthToken());
        headers.put(CLIENT_KEY, mciProperties.getIdpClientId());
        Map<String, String> formEntities = new HashMap<>();
        formEntities.put(EMAIL_KEY, mciProperties.getIdpEmail());
        formEntities.put(PASSWORD_KEY, mciProperties.getIdpPassword());
        String response = new WebClient().post(mciProperties.getIdpBaseUrl(), mciProperties.getIdpSignInUrl(), headers, formEntities);
        if (response != null) {
            Map map = new ObjectMapper().readValue(response, Map.class);
            return (String) map.get(ACCESS_TOKEN_KEY);
        }
        return null;
    }

    public UserInfo getUserInfo(MCIProperties mciProperties, String clientAuthToken) throws IOException {
        Map<String, String> headers = new HashMap<>();
        headers.put(X_AUTH_TOKEN_KEY, mciProperties.getIdpXAuthToken());
        headers.put(CLIENT_KEY, mciProperties.getIdpClientId());
        String userInfoUrl = String.format(mciProperties.getIdpUserInfoUrl(), clientAuthToken);
        String response;
        try {
            response = new WebClient().get(mciProperties.getIdpBaseUrl(), userInfoUrl, headers);
        } catch (NotFoundException e) {
            throw new IdentityUnauthorizedException("Identity not authorized.");
        }
        UserInfo userInfo = new ObjectMapper().readValue(response, UserInfo.class);
        return userInfo;
    }
}
