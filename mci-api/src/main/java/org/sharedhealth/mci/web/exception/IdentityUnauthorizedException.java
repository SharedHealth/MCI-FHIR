package org.sharedhealth.mci.web.exception;

import java.io.IOException;

public class IdentityUnauthorizedException extends IOException {
    public IdentityUnauthorizedException(String message) {
        super(message);
    }
}
