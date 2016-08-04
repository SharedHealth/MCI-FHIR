package org.sharedhealth.mci.web.launch;

import com.datastax.driver.mapping.MappingManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sharedhealth.mci.web.config.MCICassandraConfig;
import org.sharedhealth.mci.web.config.MCIProperties;
import org.sharedhealth.mci.web.controller.GlobalExceptionHandler;
import org.sharedhealth.mci.web.controller.PatientController;
import org.sharedhealth.mci.web.repository.PatientRepository;
import org.sharedhealth.mci.web.service.PatientService;

import static java.lang.System.getenv;
import static spark.Spark.port;

public class Application {

    private static final Logger logger = LogManager.getLogger(Application.class);

    public static void main(String[] args) {
        logger.info("Starting MCI Registry");
        String mci_port = getenv().get("MCI_PORT");
        port(Integer.parseInt(mci_port));

        new GlobalExceptionHandler();

        MappingManager mappingManager = MCICassandraConfig.getInstance().getMappingManager();
        PatientRepository patientRepository = new PatientRepository(mappingManager);
        PatientService patientService = new PatientService(patientRepository, MCIProperties.getInstance());
        new PatientController(patientService);
    }
}
