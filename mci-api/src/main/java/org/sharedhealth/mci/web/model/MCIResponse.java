package org.sharedhealth.mci.web.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import java.io.IOException;

import static org.sharedhealth.mci.web.util.MCIConstants.HTTP_STATUS;

@JsonIgnoreProperties(value = {"httpStatusObject"})
@JsonPropertyOrder({HTTP_STATUS, "id", "message"})
public class MCIResponse {
    private static final Logger logger = LogManager.getLogger(MCIResponse.class);

    private int httpStatus;
    private String id;
        private String message;

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
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.setSerializationInclusion(JsonSerialize.Inclusion.NON_NULL);
            return objectMapper.writeValueAsString(this);
        } catch (IOException e) {
            logger.error("Error while mapping MCI response to string ", e);
        }
        return null;
    }
}
