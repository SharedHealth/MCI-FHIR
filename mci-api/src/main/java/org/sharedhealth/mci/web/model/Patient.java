package org.sharedhealth.mci.web.model;

import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;

import java.util.Date;
import java.util.UUID;

import static org.sharedhealth.mci.web.util.RepositoryConstants.*;

@Table(name = CF_PATIENT)
public class Patient {
    @PartitionKey()
    @Column(name = HEALTH_ID)
    private String healthId;

    @Column(name = NATIONAL_ID)
    private String nationalId;
    @Column(name = BIN_BRN)
    private String birthRegistrationNumber;
    @Column(name = HOUSEHOLD_CODE)
    private String householdCode;

    @Column(name = GIVEN_NAME)
    private String givenName;
    @Column(name = SUR_NAME)
    private String surName;
    @Column(name = GENDER)
    private String gender;
    @Column(name = DATE_OF_BIRTH)
    private Date dateOfBirth;
    @Column(name = DOB_TYPE)
    private String dobType;

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

    @Column(name = FATHERS_GIVEN_NAME)
    private String fathersGivenName;
    @Column(name = FATHERS_SUR_NAME)
    private String fathersSurName;
    @Column(name = MOTHERS_GIVEN_NAME)
    private String mothersGivenName;
    @Column(name = MOTHERS_SUR_NAME)
    private String mothersSurName;
    @Column(name = RELATIONS)
    private String relations;

    @Column(name = PHONE_NO)
    private String phoneNo;
    @Column(name = PHONE_NUMBER_COUNTRY_CODE)
    private String phoneNumberCountryCode;
    @Column(name = PHONE_NUMBER_AREA_CODE)
    private String phoneNumberAreaCode;
    @Column(name = PHONE_NUMBER_EXTENSION)
    private String phoneNumberExtension;

    @Column(name = STATUS)
    private String status;
    @Column(name = DATE_OF_DEATH)
    private Date dateOfDeath;
    @Column(name = OCCUPATION)
    private String occupation;
    @Column(name = EDU_LEVEL)
    private String educationLevel;
    @Column(name = CONFIDENTIAL)
    private Boolean confidential;
    @Column(name = ACTIVE)
    private Boolean active;

    @Column(name = CREATED_AT)
    private UUID createdAt;
    @Column(name = UPDATED_AT)
    private UUID updatedAt;
    @Column(name = CREATED_BY)
    private String createdBy;
    @Column(name = UPDATED_BY)
    private String updatedBy;

    public String getHealthId() {
        return healthId;
    }

    public void setHealthId(String healthId) {
        this.healthId = healthId;
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

    public String getHouseholdCode() {
        return householdCode;
    }

    public void setHouseholdCode(String householdCode) {
        this.householdCode = householdCode;
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

    public String getDobType() {
        return dobType;
    }

    public void setDobType(String dobType) {
        this.dobType = dobType;
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

    public String getFathersGivenName() {
        return fathersGivenName;
    }

    public void setFathersGivenName(String fathersGivenName) {
        this.fathersGivenName = fathersGivenName;
    }

    public String getFathersSurName() {
        return fathersSurName;
    }

    public void setFathersSurName(String fathersSurName) {
        this.fathersSurName = fathersSurName;
    }

    public String getMothersGivenName() {
        return mothersGivenName;
    }

    public void setMothersGivenName(String mothersGivenName) {
        this.mothersGivenName = mothersGivenName;
    }

    public String getMothersSurName() {
        return mothersSurName;
    }

    public void setMothersSurName(String mothersSurName) {
        this.mothersSurName = mothersSurName;
    }

    public String getRelations() {
        return relations;
    }

    public void setRelations(String relations) {
        this.relations = relations;
    }

    public String getPhoneNo() {
        return phoneNo;
    }

    public void setPhoneNo(String phoneNo) {
        this.phoneNo = phoneNo;
    }

    public String getPhoneNumberCountryCode() {
        return phoneNumberCountryCode;
    }

    public void setPhoneNumberCountryCode(String phoneNumberCountryCode) {
        this.phoneNumberCountryCode = phoneNumberCountryCode;
    }

    public String getPhoneNumberAreaCode() {
        return phoneNumberAreaCode;
    }

    public void setPhoneNumberAreaCode(String phoneNumberAreaCode) {
        this.phoneNumberAreaCode = phoneNumberAreaCode;
    }

    public String getPhoneNumberExtension() {
        return phoneNumberExtension;
    }

    public void setPhoneNumberExtension(String phoneNumberExtension) {
        this.phoneNumberExtension = phoneNumberExtension;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getDateOfDeath() {
        return dateOfDeath;
    }

    public void setDateOfDeath(Date dateOfDeath) {
        this.dateOfDeath = dateOfDeath;
    }

    public String getOccupation() {
        return occupation;
    }

    public void setOccupation(String occupation) {
        this.occupation = occupation;
    }

    public String getEducationLevel() {
        return educationLevel;
    }

    public void setEducationLevel(String educationLevel) {
        this.educationLevel = educationLevel;
    }

    public Boolean getConfidential() {
        return confidential;
    }

    public void setConfidential(Boolean confidential) {
        this.confidential = confidential;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public UUID getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(UUID createdAt) {
        this.createdAt = createdAt;
    }

    public UUID getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(UUID updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Patient patient = (Patient) o;

        if (healthId != null ? !healthId.equals(patient.healthId) : patient.healthId != null) return false;
        if (nationalId != null ? !nationalId.equals(patient.nationalId) : patient.nationalId != null) return false;
        if (birthRegistrationNumber != null ? !birthRegistrationNumber.equals(patient.birthRegistrationNumber) : patient.birthRegistrationNumber != null)
            return false;
        if (householdCode != null ? !householdCode.equals(patient.householdCode) : patient.householdCode != null)
            return false;
        if (givenName != null ? !givenName.equals(patient.givenName) : patient.givenName != null) return false;
        if (surName != null ? !surName.equals(patient.surName) : patient.surName != null) return false;
        if (gender != null ? !gender.equals(patient.gender) : patient.gender != null) return false;
        if (dateOfBirth != null ? !dateOfBirth.equals(patient.dateOfBirth) : patient.dateOfBirth != null) return false;
        if (dobType != null ? !dobType.equals(patient.dobType) : patient.dobType != null) return false;
        if (addressLine != null ? !addressLine.equals(patient.addressLine) : patient.addressLine != null) return false;
        if (divisionId != null ? !divisionId.equals(patient.divisionId) : patient.divisionId != null) return false;
        if (districtId != null ? !districtId.equals(patient.districtId) : patient.districtId != null) return false;
        if (upazilaId != null ? !upazilaId.equals(patient.upazilaId) : patient.upazilaId != null) return false;
        if (unionOrUrbanWardId != null ? !unionOrUrbanWardId.equals(patient.unionOrUrbanWardId) : patient.unionOrUrbanWardId != null)
            return false;
        if (ruralWardId != null ? !ruralWardId.equals(patient.ruralWardId) : patient.ruralWardId != null) return false;
        if (cityCorporationId != null ? !cityCorporationId.equals(patient.cityCorporationId) : patient.cityCorporationId != null)
            return false;
        if (countryCode != null ? !countryCode.equals(patient.countryCode) : patient.countryCode != null) return false;
        if (fathersGivenName != null ? !fathersGivenName.equals(patient.fathersGivenName) : patient.fathersGivenName != null)
            return false;
        if (fathersSurName != null ? !fathersSurName.equals(patient.fathersSurName) : patient.fathersSurName != null)
            return false;
        if (mothersGivenName != null ? !mothersGivenName.equals(patient.mothersGivenName) : patient.mothersGivenName != null)
            return false;
        if (mothersSurName != null ? !mothersSurName.equals(patient.mothersSurName) : patient.mothersSurName != null)
            return false;
        if (relations != null ? !relations.equals(patient.relations) : patient.relations != null) return false;
        if (phoneNo != null ? !phoneNo.equals(patient.phoneNo) : patient.phoneNo != null) return false;
        if (phoneNumberCountryCode != null ? !phoneNumberCountryCode.equals(patient.phoneNumberCountryCode) : patient.phoneNumberCountryCode != null)
            return false;
        if (phoneNumberAreaCode != null ? !phoneNumberAreaCode.equals(patient.phoneNumberAreaCode) : patient.phoneNumberAreaCode != null)
            return false;
        if (phoneNumberExtension != null ? !phoneNumberExtension.equals(patient.phoneNumberExtension) : patient.phoneNumberExtension != null)
            return false;
        if (status != null ? !status.equals(patient.status) : patient.status != null) return false;
        if (dateOfDeath != null ? !dateOfDeath.equals(patient.dateOfDeath) : patient.dateOfDeath != null) return false;
        if (occupation != null ? !occupation.equals(patient.occupation) : patient.occupation != null) return false;
        if (educationLevel != null ? !educationLevel.equals(patient.educationLevel) : patient.educationLevel != null)
            return false;
        if (confidential != null ? !confidential.equals(patient.confidential) : patient.confidential != null)
            return false;
        if (active != null ? !active.equals(patient.active) : patient.active != null) return false;
        if (createdAt != null ? !createdAt.equals(patient.createdAt) : patient.createdAt != null) return false;
        if (updatedAt != null ? !updatedAt.equals(patient.updatedAt) : patient.updatedAt != null) return false;
        if (createdBy != null ? !createdBy.equals(patient.createdBy) : patient.createdBy != null) return false;
        return updatedBy != null ? updatedBy.equals(patient.updatedBy) : patient.updatedBy == null;

    }

    @Override
    public int hashCode() {
        int result = healthId != null ? healthId.hashCode() : 0;
        result = 31 * result + (nationalId != null ? nationalId.hashCode() : 0);
        result = 31 * result + (birthRegistrationNumber != null ? birthRegistrationNumber.hashCode() : 0);
        result = 31 * result + (householdCode != null ? householdCode.hashCode() : 0);
        result = 31 * result + (givenName != null ? givenName.hashCode() : 0);
        result = 31 * result + (surName != null ? surName.hashCode() : 0);
        result = 31 * result + (gender != null ? gender.hashCode() : 0);
        result = 31 * result + (dateOfBirth != null ? dateOfBirth.hashCode() : 0);
        result = 31 * result + (dobType != null ? dobType.hashCode() : 0);
        result = 31 * result + (addressLine != null ? addressLine.hashCode() : 0);
        result = 31 * result + (divisionId != null ? divisionId.hashCode() : 0);
        result = 31 * result + (districtId != null ? districtId.hashCode() : 0);
        result = 31 * result + (upazilaId != null ? upazilaId.hashCode() : 0);
        result = 31 * result + (unionOrUrbanWardId != null ? unionOrUrbanWardId.hashCode() : 0);
        result = 31 * result + (ruralWardId != null ? ruralWardId.hashCode() : 0);
        result = 31 * result + (cityCorporationId != null ? cityCorporationId.hashCode() : 0);
        result = 31 * result + (countryCode != null ? countryCode.hashCode() : 0);
        result = 31 * result + (fathersGivenName != null ? fathersGivenName.hashCode() : 0);
        result = 31 * result + (fathersSurName != null ? fathersSurName.hashCode() : 0);
        result = 31 * result + (mothersGivenName != null ? mothersGivenName.hashCode() : 0);
        result = 31 * result + (mothersSurName != null ? mothersSurName.hashCode() : 0);
        result = 31 * result + (relations != null ? relations.hashCode() : 0);
        result = 31 * result + (phoneNo != null ? phoneNo.hashCode() : 0);
        result = 31 * result + (phoneNumberCountryCode != null ? phoneNumberCountryCode.hashCode() : 0);
        result = 31 * result + (phoneNumberAreaCode != null ? phoneNumberAreaCode.hashCode() : 0);
        result = 31 * result + (phoneNumberExtension != null ? phoneNumberExtension.hashCode() : 0);
        result = 31 * result + (status != null ? status.hashCode() : 0);
        result = 31 * result + (dateOfDeath != null ? dateOfDeath.hashCode() : 0);
        result = 31 * result + (occupation != null ? occupation.hashCode() : 0);
        result = 31 * result + (educationLevel != null ? educationLevel.hashCode() : 0);
        result = 31 * result + (confidential != null ? confidential.hashCode() : 0);
        result = 31 * result + (active != null ? active.hashCode() : 0);
        result = 31 * result + (createdAt != null ? createdAt.hashCode() : 0);
        result = 31 * result + (updatedAt != null ? updatedAt.hashCode() : 0);
        result = 31 * result + (createdBy != null ? createdBy.hashCode() : 0);
        result = 31 * result + (updatedBy != null ? updatedBy.hashCode() : 0);
        return result;
    }
}
