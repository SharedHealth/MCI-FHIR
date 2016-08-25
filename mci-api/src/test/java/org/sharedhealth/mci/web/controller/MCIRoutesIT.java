package org.sharedhealth.mci.web.controller;

import com.datastax.driver.mapping.Mapper;
import com.datastax.driver.mapping.MappingManager;
import com.google.gson.Gson;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.junit.*;
import org.sharedhealth.mci.web.BaseIntegrationTest;
import org.sharedhealth.mci.web.config.MCICassandraConfig;
import org.sharedhealth.mci.web.launch.Application;
import org.sharedhealth.mci.web.model.Error;
import org.sharedhealth.mci.web.model.*;
import org.sharedhealth.mci.web.util.DateUtil;
import org.sharedhealth.mci.web.util.FileUtil;
import org.sharedhealth.mci.web.util.TestUtil;
import spark.Spark;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.http.HttpStatus.*;
import static org.junit.Assert.*;
import static org.sharedhealth.mci.web.util.FhirContextHelper.parseResource;
import static org.sharedhealth.mci.web.util.MCIConstants.API_VERSION;
import static org.sharedhealth.mci.web.util.MCIConstants.PATIENT_URI_PATH;

public class MCIRoutesIT extends BaseIntegrationTest {
    private static final String GET = "GET";
    private static final String HOST_NAME = "http://localhost:9997";
    private static final String POST = "post";
    private static CloseableHttpClient httpClient;
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

        UrlResponse urlResponse = doMethod(GET, PATIENT_URI_PATH + "/" + healthId, null);

        assertNotNull(urlResponse);
        assertEquals(SC_OK, urlResponse.status);
        String body = urlResponse.body;
        assertNotNull(body);
        IBaseResource resource = parseResource(body);
        assertTrue(resource instanceof ca.uhn.fhir.model.dstu2.resource.Patient);
    }

    @Test
    public void shouldSendMessageIfPatientNotFound() throws Exception {
        MCIResponse mciResponse = new MCIResponse(404);
        mciResponse.setMessage("No patient found with health id: HID");

        UrlResponse urlResponse = doMethod(GET, PATIENT_URI_PATH + "/" + healthId, null);

        assertNotNull(urlResponse);
        assertEquals(SC_NOT_FOUND, urlResponse.status);
        assertEquals(mciResponse.toString(), urlResponse.body);
    }

    @Test
    public void shouldCreateAPatientForGivenData() throws Exception {
        mciHealthIdMapper.save(new MciHealthId(healthId));
        String content = FileUtil.asString("patients/valid_patient_with_mandatory_fields.xml");

        UrlResponse urlResponse = doMethod(POST, PATIENT_URI_PATH, content);

        assertNotNull(urlResponse);
        assertEquals(SC_CREATED, urlResponse.status);
        String body = urlResponse.body;
        assertNotNull(body);

        MCIResponse mciResponse = new Gson().fromJson(body, MCIResponse.class);
        assertEquals(healthId, mciResponse.getId());
        assertNull(mciResponse.getMessage());
    }

    @Test
    public void shouldDeleteTheHealthIdAssignedToCreatedPatient() throws Exception {
        mciHealthIdMapper.save(new MciHealthId(healthId));
        assertNotNull(mciHealthIdMapper.get(healthId));
        assertNull(orgHealthIdMapper.get(healthId));

        String content = FileUtil.asString("patients/valid_patient_with_mandatory_fields.xml");
        UrlResponse urlResponse = doMethod(POST, PATIENT_URI_PATH, content);

        assertNotNull(urlResponse);
        assertEquals(SC_CREATED, urlResponse.status);

        assertNull(mciHealthIdMapper.get(healthId));
        assertNotNull(orgHealthIdMapper.get(healthId));
    }

    @Test
    public void shouldThrowAnErrorWhenThereIsNoHIDLeft() throws Exception {
        String content = FileUtil.asString("patients/valid_patient_with_mandatory_fields.xml");
        UrlResponse urlResponse = doMethod(POST, PATIENT_URI_PATH, content);

        assertNotNull(urlResponse);
        assertEquals(SC_BAD_REQUEST, urlResponse.status);
        String body = urlResponse.body;
        assertNotNull(body);

        MCIResponse mciResponse = new Gson().fromJson(body, MCIResponse.class);
        assertEquals("No HIDs available to assign", mciResponse.getMessage());
        assertNull(mciResponse.getId());
    }

    @Test
    public void shouldThrowErrorWhenCanNotParsePatientData() throws Exception {
        String content = FileUtil.asString("patients/patient_with_unknown_elements.xml");
        UrlResponse urlResponse = doMethod(POST, PATIENT_URI_PATH, content);

        assertNotNull(urlResponse);
        assertEquals(SC_UNPROCESSABLE_ENTITY, urlResponse.status);
        String body = urlResponse.body;
        assertNotNull(body);

        MCIResponse mciResponse = new Gson().fromJson(body, MCIResponse.class);
        assertTrue(mciResponse.getMessage().contains("Unknown element 'newElement' found during parse"));
    }

    @Test
    public void shouldThrowErrorWhenPatientDataHasInvalidValue() throws Exception {
        String content = FileUtil.asString("patients/patient_with_invalid_gender.xml");
        UrlResponse urlResponse = doMethod(POST, PATIENT_URI_PATH, content);

        assertNotNull(urlResponse);
        assertEquals(SC_UNPROCESSABLE_ENTITY, urlResponse.status);
        String body = urlResponse.body;
        assertNotNull(body);

        MCIResponse mciResponse = new Gson().fromJson(body, MCIResponse.class);

        String message = "The value provided is not in the value set http://hl7.org/fhir/ValueSet/administrative-gender (http://hl7.org/fhir/ValueSet/administrative-gender, and a code is required from this value set";
        List<Error> errors = mciResponse.getErrors();
        assertEquals(1, errors.size());
        assertTrue(errors.contains(new Error("/f:Patient/f:gender", "error", message)));
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
}