package org.sharedhealth.mci.web.model;

import com.fasterxml.jackson.annotation.*;
import org.apache.commons.lang3.StringUtils;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;
import static org.sharedhealth.mci.web.util.RepositoryConstants.*;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE)
public class Relation {

    @JsonIgnore
    private final String RELATION_TYPE = "type";
    @JsonIgnore
    private final String RELATIONS_CODE_TYPE = "relations";

    @JsonProperty(RELATION_TYPE)
    @JsonInclude(NON_EMPTY)
    private String type;

    @JsonProperty(HID)
    @JsonInclude(NON_EMPTY)
    private String healthId;

    @JsonProperty(NID)
    @JsonInclude(NON_EMPTY)
    private String nationalId;

    @JsonProperty(UID)
    @JsonInclude(NON_EMPTY)
    private String uid;

    @JsonProperty(BIN_BRN)
    @JsonInclude(NON_EMPTY)
    private String birthRegistrationNumber;

    @JsonProperty(NAME_BANGLA)
    @JsonInclude(NON_EMPTY)
    private String nameBangla;

    @JsonProperty(GIVEN_NAME)
    @JsonInclude(NON_EMPTY)
    private String givenName;

    @JsonProperty(SUR_NAME)
    @JsonInclude(NON_EMPTY)
    private String surName;

    @JsonProperty(MARRIAGE_ID)
    @JsonInclude(NON_EMPTY)
    private String marriageId;

    @JsonProperty(RELATIONAL_STATUS)
    @JsonInclude(NON_EMPTY)
    private String relationalStatus;

    @JsonProperty("id")
    private String id;

    public String getNameBangla() {
        return nameBangla;
    }

    public void setNameBangla(String nameBangla) {
        this.nameBangla = nameBangla.trim();
    }

    public String getGivenName() {
        return givenName;
    }

    public void setGivenName(String givenName) {
        this.givenName = givenName.trim();
    }

    public String getSurName() {
        return surName;
    }

    public void setSurName(String surName) {
        this.surName = surName.trim();
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getNationalId() {
        return nationalId;
    }

    public void setNationalId(String nationalId) {
        this.nationalId = nationalId;
    }

    public String getBirthRegistrationNumber() {
        return birthRegistrationNumber;
    }

    public void setBirthRegistrationNumber(String birthRegistrationNumber) {
        this.birthRegistrationNumber = birthRegistrationNumber;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getMarriageId() {
        return marriageId;
    }

    public void setMarriageId(String marriageId) {
        this.marriageId = marriageId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRelationalStatus() {
        return relationalStatus;
    }

    public void setRelationalStatus(String relationalStatus) {
        this.relationalStatus = relationalStatus;
    }

    public String getHealthId() {
        return healthId;
    }

    public void setHealthId(String healthId) {
        this.healthId = healthId;
    }

    @Override
    public String toString() {
        return "Relation{" +
                "type='" + type + '\'' +
                ", healthId='" + healthId + '\'' +
                ", nationalId='" + nationalId + '\'' +
                ", uid='" + uid + '\'' +
                ", birthRegistrationNumber='" + birthRegistrationNumber + '\'' +
                ", nameBangla='" + nameBangla + '\'' +
                ", givenName='" + givenName + '\'' +
                ", surName='" + surName + '\'' +
                ", marriageId='" + marriageId + '\'' +
                ", relationalStatus='" + relationalStatus + '\'' +
                ", id='" + id + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Relation)) return false;

        Relation relation = (Relation) o;

        if (notEqualStrings(birthRegistrationNumber, relation.birthRegistrationNumber)) return false;
        if (notEqualStrings(givenName, relation.givenName)) return false;
        if (notEqualStrings(healthId, relation.healthId)) return false;
        if (notEqualStrings(marriageId, relation.marriageId)) return false;
        if (notEqualStrings(nameBangla, relation.nameBangla)) return false;
        if (notEqualStrings(nationalId, relation.nationalId)) return false;
        if (notEqualStrings(relationalStatus, relation.relationalStatus)) return false;
        if (notEqualStrings(surName, relation.surName)) return false;
        if (notEqualStrings(type, relation.type)) return false;
        if (notEqualStrings(uid, relation.uid)) return false;
        return true;
    }

    private boolean notEqualStrings(String currentFieldValue, String otherFieldValue) {
        return StringUtils.isNotEmpty(currentFieldValue) ? !currentFieldValue.equals(otherFieldValue) : StringUtils.isNotEmpty(otherFieldValue);
    }

    @JsonIgnore
    public boolean isEmpty() {

        if (StringUtils.isNotBlank(birthRegistrationNumber)) return false;
        if (StringUtils.isNotBlank(givenName)) return false;
        if (StringUtils.isNotBlank(healthId)) return false;
        if (StringUtils.isNotBlank(marriageId)) return false;
        if (StringUtils.isNotBlank(nameBangla)) return false;
        if (StringUtils.isNotBlank(nationalId)) return false;
        if (StringUtils.isNotBlank(relationalStatus)) return false;
        if (StringUtils.isNotBlank(surName)) return false;
        if (StringUtils.isNotBlank(uid)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (healthId != null ? healthId.hashCode() : 0);
        result = 31 * result + (nationalId != null ? nationalId.hashCode() : 0);
        result = 31 * result + (uid != null ? uid.hashCode() : 0);
        result = 31 * result + (birthRegistrationNumber != null ? birthRegistrationNumber.hashCode() : 0);
        result = 31 * result + (nameBangla != null ? nameBangla.hashCode() : 0);
        result = 31 * result + (givenName != null ? givenName.hashCode() : 0);
        result = 31 * result + (surName != null ? surName.hashCode() : 0);
        result = 31 * result + (marriageId != null ? marriageId.hashCode() : 0);
        result = 31 * result + (relationalStatus != null ? relationalStatus.hashCode() : 0);
        return result;
    }
}
