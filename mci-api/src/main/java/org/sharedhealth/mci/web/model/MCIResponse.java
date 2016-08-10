package org.sharedhealth.mci.web.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.google.gson.Gson;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static org.sharedhealth.mci.web.util.MCIConstants.HTTP_STATUS;

@JsonIgnoreProperties(value = {"httpStatusObject"})
@JsonPropertyOrder({HTTP_STATUS, "id", "message"})
public class MCIResponse {
    private static final Logger logger = LogManager.getLogger(MCIResponse.class);

    private int httpStatus;
    private String id;
    private String message;

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

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }
}
