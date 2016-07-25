package org.sharedhealth.mci.web.controller;

import org.junit.BeforeClass;
import org.junit.Test;
import spark.Spark;
import spark.utils.IOUtils;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class PatientControllerTest {

    private static final String GET = "GET";

    @BeforeClass
    public static void setUp() throws Exception {
        Spark.init();
        new PatientController(patientService);
        Spark.awaitInitialization();
    }

    @Test
    public void shouldGetThePatient() throws Exception {
        UrlResponse urlResponse = doMethod(GET, "/patients/HID");
        assertEquals(200, urlResponse.status);
        assertEquals("HID", urlResponse.body);

    }

    private static UrlResponse doMethod(String requestMethod, String path) {
        UrlResponse response = new UrlResponse();

        try {
            getResponse(requestMethod, path, response);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return response;
    }

    private static void getResponse(String requestMethod, String path, UrlResponse response)
            throws IOException {
        URL url = new URL("http://localhost:4567/api/v2" + path);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod(requestMethod);
        connection.connect();
        response.body = IOUtils.toString(connection.getInputStream());
        response.status = connection.getResponseCode();
        response.headers = connection.getHeaderFields();
    }

    private static class UrlResponse {
        public Map<String, List<String>> headers;
        private String body;
        private int status;
    }


}