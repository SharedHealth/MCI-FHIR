package org.sharedhealth.mci.web.security;

import org.ehcache.Cache;
import org.sharedhealth.mci.web.config.MCIProperties;
import org.sharedhealth.mci.web.exception.IdentityUnauthorizedException;
import org.sharedhealth.mci.web.service.IdentityProviderService;
import spark.Filter;
import spark.Request;
import spark.Response;

public class TokenAuthenticationFilter implements Filter {
    private static final String FROM_KEY = "From";
    private static final String CLIENT_ID_KEY = "client_id";
    private static final String AUTH_TOKEN_KEY = "X-Auth-Token";

    private IdentityProviderService identityProviderService;
    private Cache<String, UserInfo> userInfoCache;
    private MCIProperties mciProperties;

    public TokenAuthenticationFilter(IdentityProviderService identityProviderService, Cache<String, UserInfo> userInfoCache) {
        this.identityProviderService = identityProviderService;
        this.userInfoCache = userInfoCache;
        this.mciProperties = MCIProperties.getInstance();
    }

    @Override
    public void handle(Request request, Response response) throws Exception {
        String from = request.headers(FROM_KEY);
        String authToken = request.headers(AUTH_TOKEN_KEY);
        String clientId = request.headers(CLIENT_ID_KEY);
        if (authToken == null || from == null || clientId == null) {
            throw new IdentityUnauthorizedException(String.format("Invalid user credentials. %s, %s, %s headers cannot be null.", FROM_KEY, CLIENT_ID_KEY, AUTH_TOKEN_KEY));
        }

        UserInfo userInfo;
        if (!userInfoCache.containsKey(authToken)) {
            userInfo = identityProviderService.getUserInfo(mciProperties, authToken);
            userInfoCache.put(authToken, userInfo);
        }
        userInfo = userInfoCache.get(authToken);

        if (!userInfo.getProperties().getEmail().equals(from)
                || !userInfo.getProperties().getId().equals(clientId)) {
            throw new IdentityUnauthorizedException("Invalid user credentials.");
        }
        request.attribute("userDetails", userInfo);
    }
}
