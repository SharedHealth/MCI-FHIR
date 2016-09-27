package org.sharedhealth.mci.web.controller;

import org.sharedhealth.mci.web.exception.HealthIdExhaustedException;
import org.sharedhealth.mci.web.exception.IdentityUnauthorizedException;
import org.sharedhealth.mci.web.exception.PatientNotFoundException;
import org.sharedhealth.mci.web.model.MCIResponse;

import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static spark.Spark.exception;

public class GlobalExceptionHandler {
    public GlobalExceptionHandler() {
        handlePatientNotFoundException();
        handleHealthIdExhaustedException();
        handleIdentityUnauthorizedException();
        handleGenericException();
    }

    private void handlePatientNotFoundException() {
        exception(PatientNotFoundException.class, (exception, request, response) -> {
            response.status(SC_NOT_FOUND);
            MCIResponse mciResponse = new MCIResponse(SC_NOT_FOUND);
            mciResponse.setMessage(exception.getMessage());
            response.body(mciResponse.toString());
        });
    }

    private void handleHealthIdExhaustedException() {
        exception(HealthIdExhaustedException.class, (exception, request, response) -> {
            response.status(SC_INTERNAL_SERVER_ERROR);
            MCIResponse mciResponse = new MCIResponse(SC_INTERNAL_SERVER_ERROR);
            mciResponse.setMessage(exception.getMessage());
            response.body(mciResponse.toString());
        });
    }

    private void handleIdentityUnauthorizedException() {
        exception(IdentityUnauthorizedException.class, (exception, request, response) -> {
            response.status(SC_UNAUTHORIZED);
            MCIResponse mciResponse = new MCIResponse(SC_UNAUTHORIZED);
            mciResponse.setMessage(exception.getMessage());
            response.body(mciResponse.toString());
        });
    }

    private void handleGenericException() {
        exception(Exception.class, (exception, request, response) -> {
            response.status(SC_INTERNAL_SERVER_ERROR);
            MCIResponse mciResponse = new MCIResponse(SC_INTERNAL_SERVER_ERROR);
            mciResponse.setMessage(exception.getMessage());
            response.body(mciResponse.toString());
        });
    }
}
