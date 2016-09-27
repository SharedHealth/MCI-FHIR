package org.sharedhealth.mci.web.config;

import org.sharedhealth.mci.web.util.StringUtils;

import java.util.Map;

public class MCIProperties {
    private static MCIProperties mciProperties;
    private String cassandraKeySpace;
    private String cassandraHost;
    private String cassandraPort;

    private String cassandraUser;
    private String cassandraPassword;
    private String cassandraTimeout;
    private String cassandraVersion;
    private String mciBaseUrl;
    private String patientLinkUri;
    private String profilesFolderPath;
    private String idpBaseUrl;
    private String idpSignInUrl;
    private String idpUserInfoUrl;
    private String idpClientId;
    private String idpXAuthToken;
    private String idpEmail;
    private String idpPassword;
    private String hidServiceBaseUrl;
    private String hidServiceNextBlockUrlPattern;
    private String hidServiceMarkUsedUrlPattern;
    private String healthIdReplenishInitialDelay;
    private String healthIdReplenishDelay;
    private String healthIdReplenishBlockSize;
    private String healthIdReplenishThreshold;
    private String hidLocalStoragePath;

    private MCIProperties() {
        Map<String, String> env = System.getenv();
        this.cassandraKeySpace = env.get("CASSANDRA_KEYSPACE");
        this.cassandraHost = env.get("CASSANDRA_HOST");
        this.cassandraPort = env.get("CASSANDRA_PORT");
        this.cassandraPassword = env.get("CASSANDRA_PASSWORD");
        this.cassandraUser = env.get("CASSANDRA_USER");
        this.cassandraTimeout = env.get("CASSANDRA_TIMEOUT");
        this.cassandraVersion = env.get("CASSANDRA_VERSION");
        this.mciBaseUrl = env.get("MCI_BASE_URL");
        this.patientLinkUri = env.get("PATIENT_LINK_URI");
        this.profilesFolderPath = env.get("PROFILES_FOLDER_PATH");
        this.idpBaseUrl = env.get("IDP_BASE_URL");
        this.idpSignInUrl = env.get("IDP_SIGNIN_URL");
        this.idpUserInfoUrl = env.get("IDP_USERINFO_URL");
        this.idpClientId = env.get("IDP_CLIENT_ID");
        this.idpXAuthToken = env.get("IDP_X_AUTH_TOKEN");
        this.idpEmail = env.get("IDP_EMAIL");
        this.idpPassword = env.get("IDP_PASSWORD");
        this.hidServiceBaseUrl = env.get("HID_SERVICE_BASE_URL");
        this.hidServiceNextBlockUrlPattern = env.get("HID_SERVICE_NEXT_BLOCK_URL");
        this.hidServiceMarkUsedUrlPattern = env.get("HID_SERVICE_MARK_USED_URL");
        this.healthIdReplenishInitialDelay = env.get("HID_REPLENISH_INITIAL_DELAY");
        this.healthIdReplenishDelay = env.get("HID_REPLENISH_DELAY");
        this.healthIdReplenishBlockSize = env.get("HID_REPLENISH_BLOCK_SIZE");
        this.healthIdReplenishThreshold = env.get("HID_REPLENISH_THRESHOLD");
        this.hidLocalStoragePath = env.get("HID_LOCAL_STORAGE_PATH");
    }

    public static MCIProperties getInstance() {
        if (mciProperties != null) return mciProperties;
        mciProperties = new MCIProperties();
        return mciProperties;
    }

    public String getCassandraKeySpace() {
        return cassandraKeySpace;
    }

    public String getCassandraHost() {
        return cassandraHost;
    }

    public int getCassandraPort() {
        return Integer.parseInt(cassandraPort);
    }

    public String getCassandraUser() {
        return cassandraUser;
    }

    public String getCassandraPassword() {
        return cassandraPassword;
    }

    public int getCassandraTimeout() {
        return Integer.parseInt(cassandraTimeout);
    }

    public int getCassandraVersion() {
        return Integer.parseInt(cassandraVersion);
    }

    public String getMciBaseUrl() {
        return mciBaseUrl;
    }

    public String getPatientLinkUri() {
        return patientLinkUri;
    }

    public String getProfilesFolderPath() {
        return StringUtils.ensureSuffix(profilesFolderPath, "/");
    }

    public Integer getHealthIdReplenishBlockSize() {
        return Integer.parseInt(healthIdReplenishBlockSize);
    }

    public String getHidServiceBaseUrl() {
        return hidServiceBaseUrl;
    }

    public String getHidServiceNextBlockUrlPattern() {
        return hidServiceNextBlockUrlPattern;
    }

    public String getHidServiceMarkUsedUrlPattern() {
        return hidServiceMarkUsedUrlPattern;
    }

    public String getIdpClientId() {
        return idpClientId;
    }

    public Integer getHealthIdReplenishDelay() {
        return Integer.parseInt(healthIdReplenishDelay);
    }

    public Integer getHealthIdReplenishInitialDelay() {
        return Integer.parseInt(healthIdReplenishInitialDelay);
    }

    public Integer getHealthIdReplenishThreshold() {
        return Integer.parseInt(healthIdReplenishThreshold);
    }

    public String getIdpXAuthToken() {
        return idpXAuthToken;
    }

    public String getIdpEmail() {
        return idpEmail;
    }

    public String getIdpPassword() {
        return idpPassword;
    }

    public String getIdpBaseUrl() {
        return idpBaseUrl;
    }

    public String getIdpSignInUrl() {
        return idpSignInUrl;
    }

    public String getHidLocalStoragePath() {
        return hidLocalStoragePath;
    }

    public String getIdpUserInfoUrl() {
        return idpUserInfoUrl;
    }

    public void setIdpUserInfoUrl(String idpUserInfoUrl) {
        this.idpUserInfoUrl = idpUserInfoUrl;
    }
}
