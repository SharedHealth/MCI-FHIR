package org.sharedhealth.mci.web;

import org.cassandraunit.utils.EmbeddedCassandraServerHelper;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.contrib.java.lang.system.EnvironmentVariables;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class BaseIntegrationTest {
    @ClassRule
    public static final EnvironmentVariables environmentVariables
            = new EnvironmentVariables();

    @BeforeClass
    public static void baseSetup() throws Exception {
        EmbeddedCassandraServerHelper.startEmbeddedCassandra("cassandra-template.yaml");
        new TestMigrations(mockPropertySources()).migrate();
    }

    private static Map<String, String> mockPropertySources() {
        Map<String, String> env = new HashMap<>();

        try {
            InputStream inputStream = BaseIntegrationTest.class.getResourceAsStream("/test.properties");
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
