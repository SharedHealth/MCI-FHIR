package org.sharedhealth.mci.web.launch;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sharedhealth.mci.web.controller.PatientController;

import static java.lang.System.getenv;
import static spark.Spark.port;

public class Application {

    private static final Logger logger = LogManager.getLogger(Application.class);

    public static void main(String[] args) {
        logger.info("MCI Java 8");
        String mci_port = getenv().get("MCI_PORT");
        port(Integer.parseInt(mci_port));

        new PatientController();
    }
}
