package org.sharedhealth.mci.web.task;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sharedhealth.mci.web.service.HealthIdService;

public class HealthIdReplenishTask implements Runnable {
    private final static Logger logger = LogManager.getLogger(HealthIdReplenishTask.class);

    HealthIdService healthIdService;

    public HealthIdReplenishTask(HealthIdService healthIdService) {
        this.healthIdService = healthIdService;
    }


    @Override
    public void run() {
        try {
            healthIdService.replenishIfNeeded();
        } catch (Exception e) {
            logger.error("Unable to replenish health ids.", e);
        }
    }
}
