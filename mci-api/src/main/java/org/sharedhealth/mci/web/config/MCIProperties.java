package org.sharedhealth.mci.web.config;

import java.util.Map;

public class MCIProperties {
    private static final MCIProperties mciProperties = new MCIProperties();

    private String cassandraKeySpace;
    private String cassandraHost;
    private int cassandraPort;
    private String cassandraUser;
    private String cassandraPassword;
    private int cassandraTimeout;
    private int cassandraVersion;

    private MCIProperties() {
        Map<String, String> env = System.getenv();
        this.cassandraKeySpace = env.get("CASSANDRA_KEYSPACE");
        this.cassandraHost = env.get("CASSANDRA_HOST");
        this.cassandraPort = Integer.parseInt(env.get("CASSANDRA_PORT"));
        this.cassandraPassword = env.get("CASSANDRA_PASSWORD");
        this.cassandraUser = env.get("CASSANDRA_USER");
        this.cassandraTimeout = Integer.parseInt(env.get("CASSANDRA_TIMEOUT"));
        this.cassandraVersion = Integer.parseInt(env.get("CASSANDRA_VERSION"));
    }

    public String getCassandraKeySpace() {
        return cassandraKeySpace;
    }

    public String getCassandraHost() {
        return cassandraHost;
    }

    public int getCassandraPort() {
        return cassandraPort;
    }

    public String getCassandraUser() {
        return cassandraUser;
    }

    public String getCassandraPassword() {
        return cassandraPassword;
    }

    public int getCassandraTimeout() {
        return cassandraTimeout;
    }

    public int getCassandraVersion() {
        return cassandraVersion;
    }

    public static MCIProperties getInstance() {
        return mciProperties;
    }
}
