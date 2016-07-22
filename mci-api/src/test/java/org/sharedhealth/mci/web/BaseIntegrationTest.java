package org.sharedhealth.mci.web;

import org.cassandraunit.utils.EmbeddedCassandraServerHelper;
import org.junit.Before;
import org.junit.Rule;
import org.junit.contrib.java.lang.system.EnvironmentVariables;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class BaseIntegrationTest {
    private static boolean isOneTimeSetupDone = false;

    @Rule
    public final EnvironmentVariables environmentVariables
            = new EnvironmentVariables();

    @Before
    public void finishOneTimeSetup() throws Exception {
        if (isOneTimeSetupDone) return;
        EmbeddedCassandraServerHelper.startEmbeddedCassandra("cassandra-template.yaml");
        new TestMigrations(mockPropertySources()).migrate();
        isOneTimeSetupDone = true;
    }

    private Map<String, String> mockPropertySources() {
        Map<String, String> env = new HashMap<>();

        try {
            InputStream inputStream = this.getClass().getResourceAsStream("/test.properties");
            Properties properties = new Properties();
            properties.load(inputStream);

            for (Object key : properties.keySet()) {
                environmentVariables.set(key.toString(), properties.getProperty(key.toString()));
                env.put(key.toString(), properties.getProperty(key.toString()));
            }
        } catch (Exception ignored) {
            System.out.print("Error ignored!");
        }
        return env;
    }

}
