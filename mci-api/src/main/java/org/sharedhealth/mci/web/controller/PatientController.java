package org.sharedhealth.mci.web.controller;


import static spark.Spark.get;

public class PatientController {

    private final String API_PREFIX = "/api/v2";

    public PatientController() {
        String uriPath = "/patients/";
        String hidParam = ":hid";
        String api_prefix = String.format("%s%s%s", API_PREFIX, uriPath, hidParam);
        get(api_prefix, (request, response) -> {
            System.out.println("I am here ");
            return request.params(hidParam);
        });
    }
}
