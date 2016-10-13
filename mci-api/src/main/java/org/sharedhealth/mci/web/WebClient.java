package org.sharedhealth.mci.web;

import com.google.gson.Gson;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sharedhealth.mci.web.exception.IdentityUnauthorizedException;
import org.sharedhealth.mci.web.exception.NotFoundException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.sharedhealth.mci.web.launch.Application.getIdentityStore;
import static org.sharedhealth.mci.web.util.MCIConstants.URL_SEPARATOR;
import static org.sharedhealth.mci.web.util.StringUtils.ensureSuffix;
import static org.sharedhealth.mci.web.util.StringUtils.removePrefix;
import static org.sharedhealth.mci.web.util.StringUtils.removeSuffix;

public class WebClient {
    private final static Logger logger = LogManager.getLogger(WebClient.class);

    public String get(String baseUrl, String path, Map<String, String> headers) throws IOException {
        String url = getUrl(baseUrl, path);
        logger.debug("HTTP GET request for {}", url);
        HttpGet request = new HttpGet(url);
        addHeaders(headers, request);
        return execute(request);
    }

    public String post(String baseUrl, String path, Map<String, String> headers, Map<String, String> formEntities) throws IOException {
        String url = getUrl(baseUrl, path);
        logger.debug("HTTP POST request for {}", url);
        HttpPost request = new HttpPost(url);
        addHeaders(headers, request);
        List<NameValuePair> valuePairs = new ArrayList<>();
        for (Map.Entry<String, String> entry : formEntities.entrySet()) {
            valuePairs.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
        }
        UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(valuePairs);
        request.setEntity(formEntity);
        return execute(request);
    }


    public String put(String baseUrl, String path, Map<String, String> headers, Map<String, String> data) throws IOException {
        String url = getUrl(baseUrl, path);
        logger.debug("HTTP put request for {}", url);
        HttpPut request = new HttpPut(url);
        addHeaders(headers, request);
        StringEntity stringEntity = new StringEntity(new Gson().toJson(data), ContentType.APPLICATION_JSON);
        request.setEntity(stringEntity);
        return execute(request);
    }

    private void addHeaders(Map<String, String> headers, HttpRequestBase request) {
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            request.addHeader(entry.getKey(), entry.getValue());
        }
    }

    private String getUrl(String baseUrl, String path) {
        return removeSuffix(ensureSuffix(baseUrl, URL_SEPARATOR) + removePrefix(path, URL_SEPARATOR), URL_SEPARATOR);
    }

    private String execute(HttpRequestBase request) throws IOException {
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            ResponseHandler<String> responseHandler = response -> {
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode >= 200 && statusCode < 300) {
                    HttpEntity entity = response.getEntity();
                    return entity != null ? parseContentInputString(entity) : null;
                } else if (statusCode == HttpStatus.SC_UNAUTHORIZED) {
                    getIdentityStore().clearIdentityToken();
                    throw new IdentityUnauthorizedException("Identity not authorized.");
                } else if (statusCode == HttpStatus.SC_NOT_FOUND) {
                    throw new NotFoundException("Invalid Url.");
                } else {
                    throw new ClientProtocolException("Unexpected Response status.");
                }
            };
            return client.execute(request, responseHandler);
        }
    }

    private String parseContentInputString(HttpEntity entity) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(entity.getContent()));
        String inputLine;
        StringBuilder responseString = new StringBuilder();
        while ((inputLine = bufferedReader.readLine()) != null) {
            responseString.append(inputLine);
        }
        bufferedReader.close();
        return responseString.toString().replace("\uFEFF", "");
    }
}