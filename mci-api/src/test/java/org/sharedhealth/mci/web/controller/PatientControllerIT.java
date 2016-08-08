package org.sharedhealth.mci.web.controller;

import ca.uhn.fhir.model.api.ExtensionDt;
import ca.uhn.fhir.model.dstu2.composite.AddressDt;
import ca.uhn.fhir.model.dstu2.valueset.AdministrativeGenderEnum;
import ca.uhn.fhir.model.primitive.DateDt;
import ca.uhn.fhir.model.primitive.DateTimeDt;
import ca.uhn.fhir.model.primitive.StringDt;
import ca.uhn.fhir.parser.IParser;
import com.datastax.driver.mapping.Mapper;
import com.datastax.driver.mapping.MappingManager;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.junit.*;
import org.sharedhealth.mci.web.BaseIntegrationTest;
import org.sharedhealth.mci.web.config.MCICassandraConfig;
import org.sharedhealth.mci.web.launch.Application;
import org.sharedhealth.mci.web.model.MCIResponse;
import org.sharedhealth.mci.web.model.MciHealthId;
import org.sharedhealth.mci.web.model.OrgHealthId;
import org.sharedhealth.mci.web.model.Patient;
import org.sharedhealth.mci.web.util.DateUtil;
import org.sharedhealth.mci.web.util.FhirContextHelper;
import org.sharedhealth.mci.web.util.TestUtil;
import spark.Spark;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.apache.http.HttpStatus.*;
import static org.junit.Assert.*;
import static org.sharedhealth.mci.web.util.FHIRConstants.*;
import static org.sharedhealth.mci.web.util.MCIConstants.API_VERSION;
import static org.sharedhealth.mci.web.util.MCIConstants.PATIENT_URI_PATH;

public class PatientControllerIT extends BaseIntegrationTest {
    private static final String GET = "GET";
    private static final String HOST_NAME = "http://localhost:9997";
    private static final String POST = "post";
    private static CloseableHttpClient httpClient;
    private final IParser xmlParser = FhirContextHelper.getFhirContext().newXmlParser();
    private Mapper<MciHealthId> mciHealthIdMapper;
    private Mapper<OrgHealthId> orgHealthIdMapper;
    private Mapper<Patient> patientMapper;

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

    @BeforeClass
    public static void setupClass() throws Exception {
        Application.main(null);
        Spark.awaitInitialization();
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        Spark.stop();
    }

    @Before
    public void setUp() throws Exception {
        MappingManager mappingManager = MCICassandraConfig.getInstance().getMappingManager();
        patientMapper = mappingManager.mapper(Patient.class);
        mciHealthIdMapper = mappingManager.mapper(MciHealthId.class);
        orgHealthIdMapper = mappingManager.mapper(OrgHealthId.class);
        httpClient = HttpClientBuilder.create().build();
    }

    @After
    public void tearDown() throws Exception {
        TestUtil.truncateAllColumnFamilies();
    }

    @Test
    public void shouldGetThePatient() throws Exception {
        patientMapper.save(createMCIPatient());

        UrlResponse urlResponse = doMethod(GET, PATIENT_URI_PATH + healthId, null);

        assertNotNull(urlResponse);
        assertEquals(SC_OK, urlResponse.status);
        String body = urlResponse.body;
        assertNotNull(body);
        IBaseResource resource = xmlParser.parseResource(body);
        assertTrue(resource instanceof ca.uhn.fhir.model.dstu2.resource.Patient);
    }

    @Test
    public void shouldSendMessageIfPatientNotFound() throws Exception {
        MCIResponse mciResponse = new MCIResponse(404);
        mciResponse.setMessage("No patient found with health id: HID");

        UrlResponse urlResponse = doMethod(GET, PATIENT_URI_PATH + healthId, null);

        assertNotNull(urlResponse);
        assertEquals(SC_NOT_FOUND, urlResponse.status);
        assertEquals(mciResponse.toString(), urlResponse.body);
    }

    @Test
    public void shouldCreateAPatientForGivenData() throws Exception {
        mciHealthIdMapper.save(new MciHealthId(healthId));
        UrlResponse urlResponse = doMethod(POST, PATIENT_URI_PATH, xmlParser.encodeResourceToString(createFHIRPatient()));

        assertNotNull(urlResponse);
        assertEquals(SC_CREATED, urlResponse.status);
        String body = urlResponse.body;
        assertNotNull(body);

        MCIResponse mciResponse = new ObjectMapper().readValue(body, MCIResponse.class);
        assertEquals(healthId, mciResponse.getId());
        assertNull(mciResponse.getMessage());
    }

    @Test
    public void shouldDeleteTheHealthIdAssignedToCreatedPatient() throws Exception {
        mciHealthIdMapper.save(new MciHealthId(healthId));
        assertNotNull(mciHealthIdMapper.get(healthId));
        assertNull(orgHealthIdMapper.get(healthId));
        UrlResponse urlResponse = doMethod(POST, PATIENT_URI_PATH, xmlParser.encodeResourceToString(createFHIRPatient()));

        assertNotNull(urlResponse);
        assertEquals(SC_CREATED, urlResponse.status);

        assertNull(mciHealthIdMapper.get(healthId));
        assertNotNull(orgHealthIdMapper.get(healthId));
    }

    @Test
    public void shouldThrowAnErrorWhenThereIsNoHIDLeft() throws Exception {
        ca.uhn.fhir.model.dstu2.resource.Patient fhirPatient = createFHIRPatient();

        UrlResponse urlResponse = doMethod(POST, PATIENT_URI_PATH, xmlParser.encodeResourceToString(fhirPatient));

        assertNotNull(urlResponse);
        assertEquals(SC_BAD_REQUEST, urlResponse.status);
        String body = urlResponse.body;
        assertNotNull(body);

        MCIResponse mciResponse = new ObjectMapper().readValue(body, MCIResponse.class);
        assertEquals("No HIDs available to assign", mciResponse.getMessage());
        assertNull(mciResponse.getId());
    }

    private static UrlResponse doMethod(String requestMethod, String path, String body) throws Exception {
        HttpResponse httpResponse = null;
        if (requestMethod.equals(GET)) {
            HttpGet httpGet = new HttpGet(HOST_NAME + API_VERSION + path);
            httpResponse = httpClient.execute(httpGet);
        } else if (requestMethod.equals(POST)) {
            HttpPost httpPost = new HttpPost(HOST_NAME + API_VERSION + path);
            httpPost.setEntity(new StringEntity(body));
            httpResponse = httpClient.execute(httpPost);
        }
        UrlResponse urlResponse = new UrlResponse();
        urlResponse.status = httpResponse.getStatusLine().getStatusCode();
        HttpEntity entity = httpResponse.getEntity();
        if (entity != null) {
            urlResponse.body = EntityUtils.toString(entity);
        } else {
            urlResponse.body = "";
        }
        Map<String, String> headers = new HashMap<>();
        Header[] allHeaders = httpResponse.getAllHeaders();
        for (Header header : allHeaders) {
            headers.put(header.getName(), header.getValue());
        }
        urlResponse.headers = headers;
        return urlResponse;
    }

    private static class UrlResponse {
        public Map<String, String> headers;
        private String body;
        private int status;
    }

    private Patient createMCIPatient() {
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

    private ca.uhn.fhir.model.dstu2.resource.Patient createFHIRPatient() {
        ca.uhn.fhir.model.dstu2.resource.Patient patient = new ca.uhn.fhir.model.dstu2.resource.Patient();
        patient.addName().addGiven(givenName).addFamily(surName);
        patient.setGender(AdministrativeGenderEnum.MALE);

        DateDt date = new DateDt(dateOfBirth);
        ExtensionDt extensionDt = new ExtensionDt().setUrl(BIRTH_TIME_EXTENSION_URL).setValue(new DateTimeDt(dateOfBirth));
        date.addUndeclaredExtension(extensionDt);
        patient.setBirthDate(date);

        AddressDt addressDt = new AddressDt().addLine(addressLine);
        addressDt.setCountry(countryCode);
        String addressCode = String.format("%s%s%s%s%s%s", divisionId, districtId, upazilaId, cityId, urbanWardId, ruralWardId);
        ExtensionDt addressCodeExtension = new ExtensionDt().
                setUrl(getFhirExtensionUrl(ADDRESS_CODE_EXTENSION_NAME)).setValue(new StringDt(addressCode));
        addressDt.addUndeclaredExtension(addressCodeExtension);
        patient.addAddress(addressDt);
        return patient;
    }

}