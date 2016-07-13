package org.sharedhealth.mci.web.launch;

import org.apache.log4j.Logger;

public class Main {
    private static final Logger logger = Logger.getLogger(Main.class);

    public static void main(String[] args) {
        System.out.println("MCI-Java 8");
        logger.info("MCI-Java 8");
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
