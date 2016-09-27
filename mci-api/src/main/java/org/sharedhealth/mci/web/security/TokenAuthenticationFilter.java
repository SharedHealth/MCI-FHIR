package org.sharedhealth.mci.web.security;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sharedhealth.mci.web.config.MCIProperties;
import org.sharedhealth.mci.web.exception.IdentityUnauthorizedException;
import org.sharedhealth.mci.web.service.IdentityProviderService;
import spark.Filter;
import spark.Request;
import spark.Response;

public class TokenAuthenticationFilter implements Filter {
    private static final Logger logger = LogManager.getLogger(IdentityProviderService.class);
    private static final String FROM_KEY = "From";
    private static final String CLIENT_ID_KEY = "client_id";
    private static final String AUTH_TOKEN_KEY = "X-Auth-Token";

    private IdentityProviderService identityProviderService;
    private MCIProperties mciProperties;

    public TokenAuthenticationFilter(IdentityProviderService identityProviderService) {
        this.identityProviderService = identityProviderService;
        mciProperties = MCIProperties.getInstance();
    }

    @Override
    public void handle(Request request, Response response) throws Exception {
        String authToken = request.headers(AUTH_TOKEN_KEY);

        UserInfo userInfo = identityProviderService.getUserInfo(mciProperties, authToken);
        if (!request.headers(FROM_KEY).equals(userInfo.getProperties().getEmail())
                || !request.headers(CLIENT_ID_KEY).equals(userInfo.getProperties().getId())) {
            logger.error("Unable to authenticate user");
            throw new IdentityUnauthorizedException("Unable to authenticate user");
        }
    }
}
