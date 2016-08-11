package org.sharedhealth.mci.web.model;


import com.google.gson.Gson;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

@XmlRootElement
public class Error implements Serializable {

    @XmlElement
    private String field;
    @XmlElement
    private String type;
    @XmlElement
    private String reason;

    public Error() {
    }

    public Error(String field, String type, String reason) {
        this.field = field;
        this.type = type;
        this.reason = reason;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String toString() {
        return new Gson().toJson(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Error error = (Error) o;

        if (field != null ? !field.equals(error.field) : error.field != null) return false;
        if (reason != null ? !reason.equals(error.reason) : error.reason != null) return false;
        if (type != null ? !type.equals(error.type) : error.type != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = field != null ? field.hashCode() : 0;
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (reason != null ? reason.hashCode() : 0);
        return result;
    }
}