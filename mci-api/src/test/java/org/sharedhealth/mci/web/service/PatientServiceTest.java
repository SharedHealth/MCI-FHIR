package org.sharedhealth.mci.web.service;

import ca.uhn.fhir.model.api.ExtensionDt;
import ca.uhn.fhir.model.dstu2.composite.AddressDt;
import ca.uhn.fhir.model.dstu2.composite.HumanNameDt;
import ca.uhn.fhir.model.dstu2.composite.IdentifierDt;
import ca.uhn.fhir.model.dstu2.resource.Patient.Link;
import ca.uhn.fhir.model.dstu2.valueset.AdministrativeGenderEnum;
import ca.uhn.fhir.model.dstu2.valueset.LinkTypeEnum;
import ca.uhn.fhir.model.primitive.StringDt;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.sharedhealth.mci.web.config.MCIProperties;
import org.sharedhealth.mci.web.model.Patient;
import org.sharedhealth.mci.web.repository.PatientRepository;
import org.sharedhealth.mci.web.util.DateUtil;

import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.sharedhealth.mci.web.util.FHIRConstants.ADDRESS_CODE_EXTENSION_NAME;
import static org.sharedhealth.mci.web.util.FHIRConstants.getFhirExtensionUrl;

public class PatientServiceTest {
    private PatientService patientService;
    @Mock
    private PatientRepository patientRepository;
    @Mock
    private MCIProperties mciProperties;

    private final String healthId = "HID";
    private final String givenName = "Bob the";
    private final String surName = "Builder";
    private final String gender = "M";
    private final Date dateOfBirth = DateUtil.parseDate("1995-07-01 00:00:00+0530");
    private final String countryCode = "050";
    private final String divisionId = "30";
    private final String districtId = "26";
    private final String upazilaId = "18";
    private final String cityId = "02";
    private final String urbanWardId = "01";
    private final String ruralWardId = "04";
    private final String addressLine = "Will Street";

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        patientService = new PatientService(patientRepository, mciProperties);
    }

    @Test
    public void shouldMapMCIPatientToFHIRPatient() throws Exception {
        String serverUri = "https://mci-registry.com/api/v2/patients/";
        String patientLinkUri = "https://mci.com/api/v1/patients/";
        when(patientRepository.findByHealthId(healthId)).thenReturn(createPatient());
        when(mciProperties.getServerUri()).thenReturn(serverUri);
        when(mciProperties.getPatientLinkUri()).thenReturn(patientLinkUri);

        ca.uhn.fhir.model.dstu2.resource.Patient fhirPatient = patientService.findPatientByHealthId(healthId);
        assertNotNull(fhirPatient);

        List<IdentifierDt> identifiers = fhirPatient.getIdentifier();
        assertEquals(1, identifiers.size());
        IdentifierDt hidIdentifier = identifiers.get(0);
        assertEquals(healthId, hidIdentifier.getValue());
        assertEquals(serverUri + healthId, hidIdentifier.getSystem());

        HumanNameDt name = fhirPatient.getNameFirstRep();
        assertEquals(givenName, name.getGivenFirstRep().getValue());
        assertEquals(surName, name.getFamilyFirstRep().getValue());

        assertEquals(AdministrativeGenderEnum.MALE.getCode(), fhirPatient.getGender());
        assertEquals(dateOfBirth, fhirPatient.getBirthDate());

        AddressDt address = fhirPatient.getAddressFirstRep();
        List<ExtensionDt> extensions = address.getUndeclaredExtensionsByUrl(getFhirExtensionUrl(ADDRESS_CODE_EXTENSION_NAME));
        assertEquals(addressLine, address.getLineFirstRep().getValue());
        assertEquals(1, extensions.size());
        StringDt addressCode = (StringDt) extensions.get(0).getValue();
        assertEquals("302618020104", addressCode.getValue());
        assertEquals(countryCode, address.getCountry());

        Link link = fhirPatient.getLinkFirstRep();
        assertEquals(LinkTypeEnum.SEE_ALSO.getCode(), link.getType());
        assertEquals(patientLinkUri+healthId, link.getOther().getReference().getValue());
    }

    private Patient createPatient() {
        Patient expectedPatient = new Patient();
        expectedPatient.setHealthId(healthId);
        expectedPatient.setGivenName(givenName);
        expectedPatient.setSurName(surName);
        expectedPatient.setGender(gender);
        expectedPatient.setDateOfBirth(dateOfBirth);
        expectedPatient.setCountryCode(countryCode);
        expectedPatient.setDivisionId(divisionId);
        expectedPatient.setDistrictId(districtId);
        expectedPatient.setUpazilaId(upazilaId);
        expectedPatient.setCityCorporationId(cityId);
        expectedPatient.setUnionOrUrbanWardId(urbanWardId);
        expectedPatient.setRuralWardId(ruralWardId);
        expectedPatient.setAddressLine(addressLine);
        return expectedPatient;
    }
}