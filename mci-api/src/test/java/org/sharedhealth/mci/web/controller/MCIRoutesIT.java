package org.sharedhealth.mci.web.controller;

import com.datastax.driver.mapping.Mapper;
import com.datastax.driver.mapping.MappingManager;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.junit.*;
import org.sharedhealth.mci.web.BaseIntegrationTest;
import org.sharedhealth.mci.web.config.MCICassandraConfig;
import org.sharedhealth.mci.web.config.MCIProperties;
import org.sharedhealth.mci.web.launch.Application;
import org.sharedhealth.mci.web.model.Error;
import org.sharedhealth.mci.web.model.MCIResponse;
import org.sharedhealth.mci.web.model.MciHealthIdStore;
import org.sharedhealth.mci.web.model.Patient;
import org.sharedhealth.mci.web.util.DateUtil;
import org.sharedhealth.mci.web.util.TestUtil;
import spark.Spark;

import java.util.*;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.apache.http.HttpStatus.*;
import static org.junit.Assert.*;
import static org.sharedhealth.mci.web.launch.Application.getIdentityStore;
import static org.sharedhealth.mci.web.util.FhirContextHelper.parseResource;
import static org.sharedhealth.mci.web.util.FileUtil.asString;
import static org.sharedhealth.mci.web.util.HttpUtil.*;
import static org.sharedhealth.mci.web.util.MCIConstants.API_VERSION;
import static org.sharedhealth.mci.web.util.MCIConstants.PATIENT_URI_PATH;

public class MCIRoutesIT extends BaseIntegrationTest {
    private static final String GET = "GET";
    private static final String HOST_NAME = "http://localhost:9990";
    private static final String POST = "post";
    private static CloseableHttpClient httpClient;
    private Mapper<Patient> patientMapper;

    @Rule
    public WireMockRule idpService = new WireMockRule(9997);

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
        httpClient = HttpClientBuilder.create().build();
    }

    @After
    public void tearDown() throws Exception {
        MciHealthIdStore.getInstance().clear();
        getIdentityStore().clearIdentityToken();
        TestUtil.truncateAllColumnFamilies();
    }

    @Test
    public void shouldNotAllowGetRequestIfNotAuthenticated() throws Exception {
        MCIProperties mciProperties = MCIProperties.getInstance();
        String authToken = UUID.randomUUID().toString();

        patientMapper.save(createMCIPatient());
        stubFor(get(urlMatching("/token/" + authToken))
                .withHeader(X_AUTH_TOKEN_KEY, equalTo(mciProperties.getIdpXAuthToken()))
                .withHeader(CLIENT_ID_KEY, equalTo(mciProperties.getIdpClientId()))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.SC_UNAUTHORIZED)));
        Map<String, String> headers = new HashMap<>();
        headers.put("X-Auth-Token", authToken);
        headers.put("client_id", mciProperties.getIdpClientId());
        headers.put("From", mciProperties.getIdpEmail());

        UrlResponse urlResponse = doMethod(GET, PATIENT_URI_PATH + "/" + healthId, null, headers);

        assertNotNull(urlResponse);
        assertEquals(SC_UNAUTHORIZED, urlResponse.status);
    }

    @Test
    public void shouldGetThePatient() throws Exception {
        patientMapper.save(createMCIPatient());

        String authToken = "d324fe7a-156b-449c-93b2-1c9871ee306c";
        setUpValidClient(authToken);
        Map<String, String> headers = getHeader(authToken);

        UrlResponse urlResponse = doMethod(GET, PATIENT_URI_PATH + "/" + healthId, null, headers);

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

        String authToken = "d324fe7a-156b-449c-93b2-1c9871ee306c";
        setUpValidClient(authToken);
        Map<String, String> headers = getHeader(authToken);

        mciResponse.setMessage("No patient found with health id: HID");

        UrlResponse urlResponse = doMethod(GET, PATIENT_URI_PATH + "/" + healthId, null, headers);

        assertNotNull(urlResponse);
        assertEquals(SC_NOT_FOUND, urlResponse.status);
        assertEquals(mciResponse.toString(), urlResponse.body);
    }

    @Test
    public void shouldCreateAPatientForGivenData() throws Exception {
        setupStubForHealthIdService();
        String content = asString("patients/valid_patient_with_mandatory_fields.xml");

        String authToken = "d324fe7a-156b-449c-93b2-1c9871ee306c";
        setUpValidClient(authToken);
        Map<String, String> headers = getHeader(authToken);

        addHidsToMciHealthIdStore();
        UrlResponse urlResponse = doMethod(POST, PATIENT_URI_PATH, content, headers);

        assertNotNull(urlResponse);
        assertEquals(SC_CREATED, urlResponse.status);
        String body = urlResponse.body;
        assertNotNull(body);

        MCIResponse mciResponse = new Gson().fromJson(body, MCIResponse.class);
        assertNotNull(mciResponse.getId());
        assertTrue(getHIDs().contains(mciResponse.getId()));
        assertNull(mciResponse.getMessage());

        verify(1, putRequestedFor(urlMatching("/healthIds/markUsed/" + mciResponse.getId())));
    }

    @Test
    public void shouldThrowAnErrorWhenThereIsNoHIDLeft() throws Exception {
        String content = asString("patients/valid_patient_with_mandatory_fields.xml");

        String authToken = "d324fe7a-156b-449c-93b2-1c9871ee306c";
        setUpValidClient(authToken);
        Map<String, String> headers = getHeader(authToken);

        UrlResponse urlResponse = doMethod(POST, PATIENT_URI_PATH, content, headers);

        assertNotNull(urlResponse);
        assertEquals(SC_INTERNAL_SERVER_ERROR, urlResponse.status);
        String body = urlResponse.body;
        assertNotNull(body);

        MCIResponse mciResponse = new Gson().fromJson(body, MCIResponse.class);
        assertEquals("HealthIds are exhausted.", mciResponse.getMessage());
        assertNull(mciResponse.getId());
    }

    @Test
    public void shouldThrowErrorWhenCanNotParsePatientData() throws Exception {
        String content = asString("patients/patient_with_unknown_elements.xml");

        String authToken = "d324fe7a-156b-449c-93b2-1c9871ee306c";
        setUpValidClient(authToken);
        Map<String, String> headers = getHeader(authToken);

        UrlResponse urlResponse = doMethod(POST, PATIENT_URI_PATH, content, headers);

        assertNotNull(urlResponse);
        assertEquals(SC_UNPROCESSABLE_ENTITY, urlResponse.status);
        String body = urlResponse.body;
        assertNotNull(body);

        MCIResponse mciResponse = new Gson().fromJson(body, MCIResponse.class);
        assertTrue(mciResponse.getMessage().contains("Unknown element 'newElement' found during parse"));
    }

    @Test
    public void shouldThrowErrorWhenPatientDataHasInvalidValue() throws Exception {
        String content = asString("patients/patient_with_invalid_gender.xml");

        String authToken = "d324fe7a-156b-449c-93b2-1c9871ee306c";
        setUpValidClient(authToken);
        Map<String, String> headers = getHeader(authToken);

        UrlResponse urlResponse = doMethod(POST, PATIENT_URI_PATH, content, headers);

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

    private Map<String, String> getHeader(String authToken) {
        String clientId = "18548";
        String email = "facility@gmail.com";

        Map<String, String> headers = new HashMap<>();
        headers.put("X-Auth-Token", authToken);
        headers.put("client_id", clientId);
        headers.put("From", email);
        return headers;
    }

    private void addHidsToMciHealthIdStore() {
        MciHealthIdStore mciHealthIdStore = MciHealthIdStore.getInstance();
        mciHealthIdStore.addMciHealthIds(getHIDs());
    }

    private static UrlResponse doMethod(String requestMethod, String path, String body, Map<String, String> requestHeaders) throws Exception {
        HttpResponse httpResponse = null;
        if (requestMethod.equals(GET)) {
            HttpGet httpGet = new HttpGet(HOST_NAME + API_VERSION + path);
            addHeaders(requestHeaders, httpGet);
            httpResponse = httpClient.execute(httpGet);
        } else if (requestMethod.equals(POST)) {
            HttpPost httpPost = new HttpPost(HOST_NAME + API_VERSION + path);
            addHeaders(requestHeaders, httpPost);
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
        Map<String, String> responseHeaders = new HashMap<>();
        Header[] allHeaders = httpResponse.getAllHeaders();
        for (Header header : allHeaders) {
            responseHeaders.put(header.getName(), header.getValue());
        }
        urlResponse.headers = responseHeaders;
        return urlResponse;
    }

    private static void addHeaders(Map<String, String> requestHeaders, HttpRequestBase httpRequest) {
        for (Map.Entry<String, String> entry : requestHeaders.entrySet()) {
            httpRequest.addHeader(entry.getKey(), entry.getValue());
        }
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

    private void setUpValidClient(String authToken) {
        String idpResponse = asString("idpClients/facilityClient.json");
        MCIProperties mciProperties = MCIProperties.getInstance();

        stubFor(get(urlMatching("/token/" + authToken))
                .withHeader(X_AUTH_TOKEN_KEY, equalTo(mciProperties.getIdpXAuthToken()))
                .withHeader(CLIENT_ID_KEY, equalTo(mciProperties.getIdpClientId()))
                .willReturn(aResponse()
                                .withStatus(HttpStatus.SC_OK)
                                .withBody(idpResponse)
                ));
    }


    private void setupStubForHealthIdService() {
        MCIProperties mciProperties = MCIProperties.getInstance();
        UUID token = UUID.randomUUID();
        String hidResponse = getHidResponse();

        String idpResponse = "{\"access_token\" : \"" + token.toString() + "\"}";
        stubFor(post(urlMatching("/signin"))
                .withHeader(X_AUTH_TOKEN_KEY, equalTo(mciProperties.getIdpXAuthToken()))
                .withHeader(CLIENT_ID_KEY, equalTo(mciProperties.getIdpClientId()))
                .withRequestBody(containing("password=password&email=shrSysAdmin%40gmail.com"))
                .willReturn(aResponse()
                                .withStatus(HttpStatus.SC_OK)
                                .withBody(idpResponse)
                ));

        stubFor(get(urlPathEqualTo("/healthIds/nextBlock"))
                .withHeader(X_AUTH_TOKEN_KEY, equalTo(token.toString()))
                .withHeader(CLIENT_ID_KEY, equalTo(mciProperties.getIdpClientId()))
                .withHeader(FROM_KEY, equalTo(mciProperties.getIdpEmail()))
                .willReturn(aResponse()
                                .withStatus(HttpStatus.SC_OK)
                                .withBody(hidResponse)
                ));

        stubFor(put(urlPathEqualTo("/healthIds/markUsed"))
                .withHeader(X_AUTH_TOKEN_KEY, equalTo(token.toString()))
                .withHeader(CLIENT_ID_KEY, equalTo(mciProperties.getIdpClientId()))
                .withHeader(FROM_KEY, equalTo(mciProperties.getIdpEmail()))
                .willReturn(aResponse()
                                .withStatus(HttpStatus.SC_OK)
                                .withBody("Accepted")
                ));
    }

    private String getHidResponse() {
        HashMap<String, Object> hidResponse = new HashMap<>();

        hidResponse.put("total", "10");
        hidResponse.put("hids", getHIDs());
        return new Gson().toJson(hidResponse);
    }


    private List<String> getHIDs() {
        return Lists.newArrayList("98000430630",
                "98000429756",
                "98000430531",
                "98000430507",
                "98000430341",
                "98000430564",
                "98000429145",
                "98000430911",
                "98000429061",
                "98000430333");
    }

}