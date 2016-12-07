package org.sharedhealth.mci.web.controller;

import ca.uhn.fhir.model.dstu2.resource.Bundle;
import com.google.gson.Gson;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.sharedhealth.mci.web.model.MCIResponse;
import org.sharedhealth.mci.web.security.UserInfo;
import org.sharedhealth.mci.web.security.UserProfile;
import org.sharedhealth.mci.web.service.PatientService;
import org.sharedhealth.mci.web.util.FileUtil;
import spark.Request;
import spark.Response;

import static java.util.Arrays.asList;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.http.HttpStatus.SC_UNPROCESSABLE_ENTITY;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.sharedhealth.mci.web.security.AuthorizationFilter.USER_DETAILS_KEY;
import static org.sharedhealth.mci.web.security.UserInfo.HRM_MCI_USER_GROUP;

public class PatientControllerTest {
    private PatientController patientController;
    @Mock
    private PatientService patientService;
    @Mock
    private Response response;
    @Mock
    private Request request;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        patientController = new PatientController(patientService);
    }

    @Test
    public void shouldCreatePatientAndGiveCreatedPatientHID() throws Exception {
        MCIResponse mciResponse = new MCIResponse(HttpStatus.SC_CREATED);
        mciResponse.setId("HID");
        String content = FileUtil.asString("patients/valid_patient_with_mandatory_fields.xml");

        when(request.body()).thenReturn(content);
        UserInfo userInfo = getUserInfo();
        when(patientService.createPatient(any(Bundle.class), eq(userInfo))).thenReturn(mciResponse);
        when(request.attribute(USER_DETAILS_KEY)).thenReturn(userInfo);

        String result = patientController.createPatient(request, response);

        assertNotNull(result);
        assertEquals(result, mciResponse.toString());

        verify(patientService).createPatient(any(Bundle.class), eq(userInfo));
        verify(response).status(HttpStatus.SC_CREATED);
    }

    @Test
    public void shouldFailToCreateWhenPatientDataContainsUnknownElement() throws Exception {
        String content = FileUtil.asString("patients/patient_with_unknown_elements.xml");
        UserInfo userInfo = getUserInfo();
        when(request.body()).thenReturn(content);
        when(request.attribute(USER_DETAILS_KEY)).thenReturn(userInfo);

        String result = patientController.createPatient(request, response);
        assertFalse(isBlank(result));

        MCIResponse mciResponse = new Gson().fromJson(result, MCIResponse.class);
        assertEquals(SC_UNPROCESSABLE_ENTITY, mciResponse.getHttpStatus());
        assertTrue(mciResponse.getMessage().contains("Unknown element 'newElement' found during parse"));

        verify(response).status(SC_UNPROCESSABLE_ENTITY);
        verify(patientService, never()).createPatient(any(Bundle.class), eq(userInfo));
    }

    @Test
    public void shouldFailToCreateWhenPatientDataContainsUnknownAttribute() throws Exception {
        String content = FileUtil.asString("patients/patient_with_unknown_attributes.xml");
        UserInfo userInfo = getUserInfo();
        when(request.body()).thenReturn(content);
        when(request.attribute(USER_DETAILS_KEY)).thenReturn(userInfo);

        String result = patientController.createPatient(request, response);
        assertFalse(isBlank(result));

        MCIResponse mciResponse = new Gson().fromJson(result, MCIResponse.class);
        assertEquals(SC_UNPROCESSABLE_ENTITY, mciResponse.getHttpStatus());
        assertNotNull(mciResponse.getMessage());
        assertTrue(mciResponse.getMessage().contains("Unknown attribute 'newAttribute' found during parse"));

        verify(response).status(SC_UNPROCESSABLE_ENTITY);
        verify(patientService, never()).createPatient(any(Bundle.class), eq(userInfo));
    }

    @Test
    public void shouldFailToCreateWhenPatientDataContainsUnexpectedRepeatingElement() throws Exception {
        String content = FileUtil.asString("patients/patient_with_multiple_dobs.xml");
        UserInfo userInfo = getUserInfo();
        when(request.body()).thenReturn(content);
        when(request.attribute(USER_DETAILS_KEY)).thenReturn(userInfo);

        String result = patientController.createPatient(request, response);
        assertFalse(isBlank(result));

        MCIResponse mciResponse = new Gson().fromJson(result, MCIResponse.class);
        assertEquals(SC_UNPROCESSABLE_ENTITY, mciResponse.getHttpStatus());
        assertNotNull(mciResponse.getMessage());
        assertTrue(mciResponse.getMessage().contains("Multiple repetitions of non-repeatable element 'birthDate' found during parse"));

        verify(response).status(SC_UNPROCESSABLE_ENTITY);
        verify(patientService, never()).createPatient(any(Bundle.class), eq(userInfo));
    }

    private UserInfo getUserInfo() {
        UserProfile userProfile = new UserProfile("facility", "100067", null);
        return new UserInfo("102", "ABC", "abc@mail", 1, true, "111100", asList(HRM_MCI_USER_GROUP), asList(userProfile));
    }
}