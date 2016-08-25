package org.sharedhealth.mci.web.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.google.gson.Gson;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

import static org.sharedhealth.mci.web.util.MCIConstants.HTTP_STATUS;

@JsonIgnoreProperties(value = {"httpStatusObject"})
@JsonPropertyOrder({HTTP_STATUS, "id", "message"})
public class MCIResponse {

    /*
        MCI response will be passed as response to HTTP calls in MCI

        httpStatus the status of HTTP response
        id will be used to return HID after create/update request
        message is an error message whenever there is a generic failure while processing request
        errors should be used while FHIR validation failure
    * */

    private int httpStatus;
    private String id;
    private String message;
    private List<Error> errors;

    public MCIResponse() {
    }

    public MCIResponse(int httpStatus) {
        this.httpStatus = httpStatus;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public String getId() {
        return id;
    }

    public String getMessage() {
        return message;
    }

    public void addError(Error error) {
        if (this.errors == null) {
            this.errors = new ArrayList<>();
        }
        this.errors.add(error);
    }

    public List<Error> getErrors() {
        return new ArrayList<>(errors);
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }
}
