package org.sharedhealth.mci.web.launch;

import org.apache.log4j.Logger;

import static java.lang.System.getenv;
import static spark.Spark.get;
import static spark.Spark.port;

public class Application {
    private static final Logger logger = Logger.getLogger(Application.class);

    public static void main(String[] args) {
        logger.info("MCI Java 8");
        String mci_port = getenv().get("MCI_PORT");
        port(Integer.parseInt(mci_port));
        get("/api/", (request, response) -> "MCI Java 8");
    }
}
