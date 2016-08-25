package org.sharedhealth.mci.web;

import org.cassandraunit.utils.EmbeddedCassandraServerHelper;
import org.junit.AfterClass;
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
    public static void setupBaseIntegration() throws Exception {
        EmbeddedCassandraServerHelper.startEmbeddedCassandra("cassandra-template.yaml");
        new TestMigrations(mockPropertySources()).migrate();
    }

    @AfterClass
    public static void tearDownIntegration() throws Exception {
        EmbeddedCassandraServerHelper.cleanEmbeddedCassandra();
    }

    private static Map<String, String> mockPropertySources() {
        Map<String, String> env = new HashMap<>();

        try {
            InputStream inputStream = BaseIntegrationTest.class.getResourceAsStream("/test.properties");
            Properties properties = new Properties();
            properties.load(inputStream);

            setPathForProfileFolder(properties);

            for (Object key : properties.keySet()) {
                environmentVariables.set(key.toString(), properties.getProperty(key.toString()));
                env.put(key.toString(), properties.getProperty(key.toString()));
            }
        } catch (Exception ignored) {
            System.out.print("Error ignored!");
        }
        return env;
    }

    private static void setPathForProfileFolder(Properties properties) {
        String path = BaseIntegrationTest.class.getResource("/profiles/").getPath();
        properties.put("PROFILES_FOLDER_PATH", path);
    }

}
