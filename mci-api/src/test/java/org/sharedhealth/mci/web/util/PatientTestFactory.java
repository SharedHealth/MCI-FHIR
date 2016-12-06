package org.sharedhealth.mci.web.util;

import ca.uhn.fhir.model.api.ExtensionDt;
import ca.uhn.fhir.model.dstu2.composite.*;
import ca.uhn.fhir.model.dstu2.resource.Bundle;
import ca.uhn.fhir.model.dstu2.resource.RelatedPerson;
import ca.uhn.fhir.model.dstu2.valueset.AdministrativeGenderEnum;
import ca.uhn.fhir.model.dstu2.valueset.ContactPointSystemEnum;
import ca.uhn.fhir.model.primitive.BooleanDt;
import ca.uhn.fhir.model.primitive.DateDt;
import ca.uhn.fhir.model.primitive.DateTimeDt;
import ca.uhn.fhir.model.primitive.StringDt;
import org.hl7.fhir.instance.model.api.IBaseDatatype;
import org.hl7.fhir.instance.model.valuesets.V3RoleCode;
import org.sharedhealth.mci.web.model.MCIIdentifierEnumBinder;
import org.sharedhealth.mci.web.model.Patient;
import org.sharedhealth.mci.web.model.Requester;

import java.nio.file.AccessDeniedException;
import java.util.Date;
import java.util.UUID;

import static org.sharedhealth.mci.web.util.DateUtil.parseDate;
import static org.sharedhealth.mci.web.util.FHIRConstants.*;
import static org.sharedhealth.mci.web.util.MCIConstants.PATIENT_STATUS_ALIVE;

public class PatientTestFactory {
    public static final String mciBaseUrl = "https://mci-registry.com/";

    public static final String educationLevel = "01";
    public static final String occupation = "02";
    public static final String phoneNo = "12345678";
    public static final Date dateOfDeath = null;
    public static final String dobType = "1";
    public static final String healthId = "HID123";
    public static final String givenName = "Bob the";
    public static final String surName = "Builder";
    public static final String fatherName = "Father";
    public static final String motherName = "Mother";
    public static final String spouseName = "Spouse";
    public static final String gender = "M";
    public static final Date dateOfBirth = parseDate("1995-07-01 14:20:00+0530");
    public static final String countryCode = "050";
    public static final String divisionId = "30";
    public static final String districtId = "26";
    public static final String upazilaId = "18";
    public static final String cityId = "02";
    public static final String urbanWardId = "01";
    public static final String ruralWardId = "04";
    public static final String addressLine = "Will Street";
    public static final String brn = "BRN";
    public static final String nid = "NID";
    public static final String householdCode = "12345";
    private static final String MASTER_DATA_EDUCATION_LEVEL_TYPE = "education_level";

    public static Bundle createPatientBundleWithMandatoryFields(boolean timeOfBirthIncluded) {
        Bundle bundle = new Bundle();
        bundle.setId(UUID.randomUUID().toString());
        String patientEntryFullUrl = "urn:uuid:" + UUID.randomUUID();
        bundle.addEntry().setFullUrl(patientEntryFullUrl).setResource(createFhirPatientWithMandatoryFields(timeOfBirthIncluded));
        return bundle;
    }

    private static ca.uhn.fhir.model.dstu2.resource.Patient createFhirPatientWithMandatoryFields(boolean timeOfBirthIncluded) {
        ca.uhn.fhir.model.dstu2.resource.Patient patient = new ca.uhn.fhir.model.dstu2.resource.Patient();
        patient.addName().addGiven(givenName).addFamily(surName);
        patient.setGender(AdministrativeGenderEnum.MALE);

        if (timeOfBirthIncluded) {
            DateDt date = new DateDt(dateOfBirth);
            ExtensionDt extensionDt = new ExtensionDt().setUrl(BIRTH_TIME_EXTENSION_URL).setValue(new DateTimeDt(dateOfBirth));
            date.addUndeclaredExtension(extensionDt);
            patient.setBirthDate(date);
        } else {
            patient.setBirthDate(new DateDt(dateOfBirth));
        }

        AddressDt addressDt = new AddressDt().addLine(addressLine);
        addressDt.setCountry(countryCode);
        String addressCode = String.format("%s%s%s%s%s%s", divisionId, districtId, upazilaId, cityId, urbanWardId, ruralWardId);
        ExtensionDt addressCodeExtension = new ExtensionDt().
                setUrl(getFhirExtensionUrl(ADDRESS_CODE_EXTENSION_NAME)).setValue(new StringDt(addressCode));
        addressDt.addUndeclaredExtension(addressCodeExtension);
        patient.addAddress(addressDt);
        return patient;
    }

    public static Bundle createFHIRPatientWithAllFields() throws Exception {
        ca.uhn.fhir.model.dstu2.resource.Patient fhirPatient = createFhirPatientWithMandatoryFields(true);
        Bundle bundle = new Bundle();
        bundle.setId(UUID.randomUUID().toString());
        String patientEntryFullUrl = "urn:uuid:" + UUID.randomUUID();
        bundle.addEntry().setFullUrl(patientEntryFullUrl).setResource(fhirPatient);


        setIdentifierType(fhirPatient.addIdentifier().setValue(nid), MCI_IDENTIFIER_NID_CODE);
        setIdentifierType(fhirPatient.addIdentifier().setValue(brn), MCI_IDENTIFIER_BRN_CODE);
        setIdentifierType(fhirPatient.addIdentifier().setValue(householdCode), MCI_IDENTIFIER_HOUSE_HOLD_NUMBER_CODE);

        fhirPatient.addUndeclaredExtension(createExtension(CONFIDENTIALITY_EXTENSION_NAME, new BooleanDt(false)));
        fhirPatient.addUndeclaredExtension(createExtension(EDUCATION_DETAILS_EXTENSION_NAME,
                createCodeableConcept(MCI_PATIENT_EDUCATION_DETAILS_VALUESET, educationLevel, "Higher Secondary")));
        fhirPatient.addUndeclaredExtension(createExtension(OCCUPATION_EXTENSION_NAME,
                createCodeableConcept(MCI_PATIENT_OCCUPATION_VALUESET, occupation, "Student")));
        fhirPatient.addUndeclaredExtension(createExtension(DOB_TYPE_EXTENSION_NAME,
                createCodeableConcept(MCI_PATIENT_DOB_TYPE_VALUESET, dobType, "Declared")));

        fhirPatient.addTelecom().setSystem(ContactPointSystemEnum.EMAIL).setValue("mail@gmail.com");
        fhirPatient.addTelecom().setSystem(ContactPointSystemEnum.PHONE).setValue(phoneNo);
        fhirPatient.setActive(true);
        fhirPatient.setDeceased(new BooleanDt(false));

        bundle.addEntry().setFullUrl("urn:uuid" + UUID.randomUUID().toString()).setResource(
                createRelationPerson(patientEntryFullUrl, fatherName, "FTH")
        );
        bundle.addEntry().setFullUrl("urn:uuid" + UUID.randomUUID().toString()).setResource(
                createRelationPerson(patientEntryFullUrl, motherName, "MTH")
        );
        bundle.addEntry().setFullUrl("urn:uuid" + UUID.randomUUID().toString()).setResource(
                createRelationPerson(patientEntryFullUrl, spouseName, "SPS")
        );

        return bundle;
    }

    private static RelatedPerson createRelationPerson(String patientEntryFullUrl, String givenName, String relationCode) throws Exception {
        RelatedPerson relatedPerson = new RelatedPerson();
        relatedPerson.setName(new HumanNameDt().addFamily(surName).addGiven(givenName)).setPatient(new ResourceReferenceDt(patientEntryFullUrl));
        V3RoleCode relation = V3RoleCode.fromCode(relationCode);
        relatedPerson.setRelationship(new CodeableConceptDt().addCoding(createCodingDt(relation.getSystem(), relation.toCode(), relation.getDisplay())));
        return relatedPerson;
    }

    private static ExtensionDt createExtension(String extensionName, IBaseDatatype value) {
        ExtensionDt extension = new ExtensionDt();
        extension.setUrl(getFhirExtensionUrl(extensionName));
        extension.setValue(value);
        return extension;
    }

    private static CodeableConceptDt createCodeableConcept(String valueSetName, String code, String display) {
        return new CodeableConceptDt().addCoding(createCodingDt(getMCIValuesetURI(mciBaseUrl, valueSetName), code, display));
    }

    private static CodingDt createCodingDt(String system, String code, String display) {
        return new CodingDt()
                .setSystem(system)
                .setCode(code)
                .setDisplay(display);
    }

    @SuppressWarnings("unchecked")
    private static void setIdentifierType(IdentifierDt identifierDt, String identifierCode) {
        BoundCodeableConceptDt identifierType = new BoundCodeableConceptDt<>(new MCIIdentifierEnumBinder());
        String system = getMCIValuesetURI(mciBaseUrl, MCI_PATIENT_IDENTIFIERS_VALUESET);
        identifierType.addCoding(new CodingDt(system, identifierCode));
        identifierDt.setType(identifierType);
    }

    public static Patient createMCIPatientWithMandatoryFields() {
        Patient patient = new Patient();
        patient.setGivenName(givenName);
        patient.setSurName(surName);
        patient.setGender(gender);
        patient.setDateOfBirth(dateOfBirth);
        patient.setCountryCode(countryCode);
        patient.setDivisionId(divisionId);
        patient.setDistrictId(districtId);
        patient.setUpazilaId(upazilaId);
        patient.setCityCorporationId(cityId);
        patient.setUnionOrUrbanWardId(urbanWardId);
        patient.setRuralWardId(ruralWardId);
        patient.setAddressLine(addressLine);
        return patient;
    }


    public static Patient createMCIPatientWithAllFields() throws AccessDeniedException {
        Patient patient = createMCIPatientWithMandatoryFields();
        patient.setActive(true);
        patient.setBirthRegistrationNumber(brn);
        patient.setNationalId(nid);
        patient.setRelations("[{\"type\":\"FTH\",\"given_name\":\"Father\",\"sur_name\":\"Builder\"," +
                "\"id\":\"06941b0d-5f8a-487f-b1e0-f2777c2fbe44\"},{\"type\":\"MTH\",\"given_name\":\"Mother\",\"sur_name\":\"Builder\"," +
                "\"id\":\"06941b0d-5f8a-487f-b5e0-f2777c2fbe44\"},{\"type\":\"SPS\",\"given_name\":\"Spouse\"," +
                "\"sur_name\":\"Builder\",\"id\":\"6ab069a7-f3e9-4368-99c2-9896818e447f\"}]");
        patient.setEducationLevel(educationLevel);
        patient.setOccupation(occupation);
        patient.setPhoneNo(phoneNo);
        patient.setStatus(PATIENT_STATUS_ALIVE);
        patient.setDateOfDeath(dateOfDeath);
        patient.setDobType(dobType);
        patient.setHouseholdCode(householdCode);
        patient.setConfidential(false);
        return patient;
    }

    private static Requester getRequester() throws AccessDeniedException {
        return new Requester("100067", null, null, null);
    }

}
