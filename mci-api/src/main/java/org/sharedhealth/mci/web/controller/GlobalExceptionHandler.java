package org.sharedhealth.mci.web.controller;

import org.apache.http.entity.ContentType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sharedhealth.mci.web.exception.HealthIdExhaustedException;
import org.sharedhealth.mci.web.exception.IdentityUnauthorizedException;
import org.sharedhealth.mci.web.exception.PatientNotFoundException;
import org.sharedhealth.mci.web.model.MCIResponse;

import java.nio.file.AccessDeniedException;

import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static spark.Spark.exception;

public class GlobalExceptionHandler {
    private static final Logger logger = LogManager.getLogger(GlobalExceptionHandler.class);

    public GlobalExceptionHandler() {
        handleException(PatientNotFoundException.class, SC_NOT_FOUND);
        handleException(HealthIdExhaustedException.class, SC_INTERNAL_SERVER_ERROR);
        handleException(IdentityUnauthorizedException.class, SC_UNAUTHORIZED);
        handleException(AccessDeniedException.class, SC_FORBIDDEN);
        handleException(Exception.class, SC_INTERNAL_SERVER_ERROR);
    }

    private <T extends Exception> void handleException(Class<T> exceptionClass, int status) {
        exception(exceptionClass, (exception, request, response) -> {
            response.status(status);
            MCIResponse mciResponse = new MCIResponse(status);
            mciResponse.setMessage(exception.getMessage());
            logger.error(exception.getMessage(), exception);
            response.type(ContentType.APPLICATION_JSON.getMimeType());
            response.body(mciResponse.toString());
        });
    }
}
