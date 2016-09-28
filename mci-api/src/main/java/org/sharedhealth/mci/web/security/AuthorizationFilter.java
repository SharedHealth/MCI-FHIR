package org.sharedhealth.mci.web.security;

import org.apache.commons.collections4.CollectionUtils;
import spark.Filter;
import spark.Request;
import spark.Response;

import java.nio.file.AccessDeniedException;
import java.util.List;

public class AuthorizationFilter implements Filter {
    private List<String> allowedUserList;

    public AuthorizationFilter(List<String> allowedUserList) {
        this.allowedUserList = allowedUserList;
    }

    @Override
    public void handle(Request request, Response response) throws Exception {
        authorizeRequests(request.attribute("userDetails"));
    }

    private void authorizeRequests(UserInfo userDetails) throws AccessDeniedException {
        if (!CollectionUtils.containsAny(userDetails.getProperties().getUserGroups(), allowedUserList)) {
            throw new AccessDeniedException(String.format("Access to user %s is denied", userDetails.getProperties().getEmail()));
        }
    }
}
