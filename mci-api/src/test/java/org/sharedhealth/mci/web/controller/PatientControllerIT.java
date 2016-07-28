package org.sharedhealth.mci.web.controller;

import ca.uhn.fhir.parser.IParser;
import com.datastax.driver.mapping.Mapper;
import com.datastax.driver.mapping.MappingManager;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sharedhealth.mci.web.config.MCICassandraConfig;
import org.sharedhealth.mci.web.model.MCIResponse;
import org.sharedhealth.mci.web.model.Patient;
import org.sharedhealth.mci.web.util.DateUtil;
import org.sharedhealth.mci.web.util.FhirContextHelper;
import org.sharedhealth.mci.web.util.TestUtil;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static org.junit.Assert.*;

public class PatientControllerIT {
    private static final String GET = "GET";
    private static CloseableHttpClient httpClient;
    private final IParser xmlParser = FhirContextHelper.getFhirContext().newXmlParser();
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

    @Before
    public void setUp() throws Exception {
        MappingManager mappingManager = MCICassandraConfig.getInstance().getMappingManager();
        patientMapper = mappingManager.mapper(Patient.class);
        httpClient = HttpClientBuilder.create().build();
    }

    @After
    public void tearDown() throws Exception {
        TestUtil.truncateAllColumnFamilies();
    }

    @Test
    public void shouldGetThePatient() throws Exception {
        patientMapper.save(createPatient());

        UrlResponse urlResponse = doMethod(GET, "/patients/" + healthId);

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

        UrlResponse urlResponse = doMethod(GET, "/patients/" + healthId);

        assertNotNull(urlResponse);
        assertEquals(SC_NOT_FOUND, urlResponse.status);
        assertEquals(mciResponse.toString(), urlResponse.body);
    }

    private static UrlResponse doMethod(String requestMethod, String path) {
        try {
            return getResponse(requestMethod, path);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static UrlResponse getResponse(String requestMethod, String path)
            throws IOException {
        HttpGet httpGet = null;
        if (requestMethod.equals(GET)) {
            httpGet = new HttpGet("http://localhost:9997/api/v2" + path);
        }
        HttpResponse httpResponse = httpClient.execute(httpGet);
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