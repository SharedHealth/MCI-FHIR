package org.sharedhealth.mci.web.model;

import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;

import java.util.Date;

import static org.sharedhealth.mci.web.util.RepositoryConstants.*;

@Table(name = CF_PATIENT)
public class Patient {
    @PartitionKey()
    @Column(name = HEALTH_ID)
    private String healthId;

    @Column(name = GIVEN_NAME)
    private String givenName;

    @Column(name = SUR_NAME)
    private String surName;

    @Column(name = GENDER)
    private String gender;

    @Column(name = DATE_OF_BIRTH)
    private Date dateOfBirth;

    @Column(name = ADDRESS_LINE)
    private String addressLine;

    @Column(name = DIVISION_ID)
    private String divisionId;

    @Column(name = DISTRICT_ID)
    private String districtId;

    @Column(name = UPAZILA_ID)
    private String upazilaId;

    @Column(name = UNION_OR_URBAN_WARD_ID)
    private String unionOrUrbanWardId;

    @Column(name = RURAL_WARD_ID)
    private String ruralWardId;

    @Column(name = CITY_CORPORATION)
    private String cityCorporationId;

    @Column(name = COUNTRY_CODE)
    private String countryCode;


    public String getHealthId() {
        return healthId;
    }

    public void setHealthId(String healthId) {
        this.healthId = healthId;
    }

    public String getGivenName() {
        return givenName;
    }

    public void setGivenName(String givenName) {
        this.givenName = givenName;
    }

    public String getSurName() {
        return surName;
    }

    public void setSurName(String surName) {
        this.surName = surName;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public Date getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(Date dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getAddressLine() {
        return addressLine;
    }

    public void setAddressLine(String addressLine) {
        this.addressLine = addressLine;
    }

    public String getDivisionId() {
        return divisionId;
    }

    public void setDivisionId(String divisionId) {
        this.divisionId = divisionId;
    }

    public String getDistrictId() {
        return districtId;
    }

    public void setDistrictId(String districtId) {
        this.districtId = districtId;
    }

    public String getUpazilaId() {
        return upazilaId;
    }

    public void setUpazilaId(String upazilaId) {
        this.upazilaId = upazilaId;
    }

    public String getUnionOrUrbanWardId() {
        return unionOrUrbanWardId;
    }

    public void setUnionOrUrbanWardId(String unionOrUrbanWardId) {
        this.unionOrUrbanWardId = unionOrUrbanWardId;
    }

    public String getRuralWardId() {
        return ruralWardId;
    }

    public void setRuralWardId(String ruralWardId) {
        this.ruralWardId = ruralWardId;
    }

    public String getCityCorporationId() {
        return cityCorporationId;
    }

    public void setCityCorporationId(String cityCorporationId) {
        this.cityCorporationId = cityCorporationId;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Patient patient = (Patient) o;

        if (healthId != null ? !healthId.equals(patient.healthId) : patient.healthId != null) return false;
        if (givenName != null ? !givenName.equals(patient.givenName) : patient.givenName != null) return false;
        if (surName != null ? !surName.equals(patient.surName) : patient.surName != null) return false;
        if (gender != null ? !gender.equals(patient.gender) : patient.gender != null) return false;
        if (dateOfBirth != null ? !dateOfBirth.equals(patient.dateOfBirth) : patient.dateOfBirth != null) return false;
        if (addressLine != null ? !addressLine.equals(patient.addressLine) : patient.addressLine != null) return false;
        if (divisionId != null ? !divisionId.equals(patient.divisionId) : patient.divisionId != null) return false;
        if (districtId != null ? !districtId.equals(patient.districtId) : patient.districtId != null) return false;
        if (upazilaId != null ? !upazilaId.equals(patient.upazilaId) : patient.upazilaId != null) return false;
        if (unionOrUrbanWardId != null ? !unionOrUrbanWardId.equals(patient.unionOrUrbanWardId) : patient.unionOrUrbanWardId != null)
            return false;
        if (ruralWardId != null ? !ruralWardId.equals(patient.ruralWardId) : patient.ruralWardId != null) return false;
        if (cityCorporationId != null ? !cityCorporationId.equals(patient.cityCorporationId) : patient.cityCorporationId != null)
            return false;
        return countryCode != null ? countryCode.equals(patient.countryCode) : patient.countryCode == null;

    }

    @Override
    public int hashCode() {
        int result = healthId != null ? healthId.hashCode() : 0;
        result = 31 * result + (givenName != null ? givenName.hashCode() : 0);
        result = 31 * result + (surName != null ? surName.hashCode() : 0);
        result = 31 * result + (gender != null ? gender.hashCode() : 0);
        result = 31 * result + (dateOfBirth != null ? dateOfBirth.hashCode() : 0);
        result = 31 * result + (addressLine != null ? addressLine.hashCode() : 0);
        result = 31 * result + (divisionId != null ? divisionId.hashCode() : 0);
        result = 31 * result + (districtId != null ? districtId.hashCode() : 0);
        result = 31 * result + (upazilaId != null ? upazilaId.hashCode() : 0);
        result = 31 * result + (unionOrUrbanWardId != null ? unionOrUrbanWardId.hashCode() : 0);
        result = 31 * result + (ruralWardId != null ? ruralWardId.hashCode() : 0);
        result = 31 * result + (cityCorporationId != null ? cityCorporationId.hashCode() : 0);
        result = 31 * result + (countryCode != null ? countryCode.hashCode() : 0);
        return result;
    }
}
