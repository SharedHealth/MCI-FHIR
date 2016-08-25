package org.sharedhealth.mci.web.config;

import org.sharedhealth.mci.web.util.StringUtils;

import java.util.Map;

public class MCIProperties {
    private static final MCIProperties mciProperties = new MCIProperties();
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

    public static MCIProperties getInstance() {
        return mciProperties;
    }
}
