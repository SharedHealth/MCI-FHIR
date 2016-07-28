package org.sharedhealth.mci.web;

import org.cassandraunit.utils.EmbeddedCassandraServerHelper;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.extensions.cpsuite.ClasspathSuite;
import org.junit.runner.RunWith;
import org.sharedhealth.mci.web.launch.Application;
import spark.Spark;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@RunWith(ClasspathSuite.class)
@ClasspathSuite.ClassnameFilters({".*IT"})
public class IntegrationTestSuite {
    @ClassRule
    public static final EnvironmentVariables environmentVariables
            = new EnvironmentVariables();

    @BeforeClass
    public static void setupBaseIntegration() throws Exception {
        EmbeddedCassandraServerHelper.startEmbeddedCassandra("cassandra-template.yaml");
        new TestMigrations(mockPropertySources()).migrate();
        Application.main(null);
        Spark.awaitInitialization();
    }

    @AfterClass
    public static void tearDownIntegration() throws Exception {
        Spark.stop();
        EmbeddedCassandraServerHelper.cleanEmbeddedCassandra();
    }

    private static Map<String, String> mockPropertySources() {
        Map<String, String> env = new HashMap<>();

        try {
            InputStream inputStream = IntegrationTestSuite.class.getResourceAsStream("/test.properties");
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
