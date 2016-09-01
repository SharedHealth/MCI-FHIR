package org.sharedhealth.mci.web.launch;

import com.datastax.driver.mapping.MappingManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sharedhealth.mci.web.config.MCICassandraConfig;
import org.sharedhealth.mci.web.config.MCIProperties;
import org.sharedhealth.mci.web.controller.GlobalExceptionHandler;
import org.sharedhealth.mci.web.controller.MCIRoutes;
import org.sharedhealth.mci.web.controller.PatientController;
import org.sharedhealth.mci.web.mapper.PatientMapper;
import org.sharedhealth.mci.web.model.IdentityStore;
import org.sharedhealth.mci.web.model.MciHealthIdStore;
import org.sharedhealth.mci.web.repository.PatientRepository;
import org.sharedhealth.mci.web.service.HealthIdService;
import org.sharedhealth.mci.web.service.IdentityProviderService;
import org.sharedhealth.mci.web.service.PatientService;
import org.sharedhealth.mci.web.task.HealthIdReplenishTask;
import org.sharedhealth.mci.web.validations.FhirPatientValidator;
import spark.Spark;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static java.lang.System.getenv;
import static spark.Spark.port;

public class Application {

    private static final Logger logger = LogManager.getLogger(Application.class);

    private static PatientRepository patientRepository;
    private static MCIProperties mciProperties;
    private static PatientMapper patientMapper;
    private static FhirPatientValidator fhirPatientValidator;
    private static IdentityStore identityStore;
    private static IdentityProviderService identityProviderService;
    private static MciHealthIdStore mciHealthIdStore;
    private static HealthIdService healthIdService;
    private static PatientService patientService;
    private static PatientController patientController;

    public static void main(String[] args) {
        logger.info("Starting MCI Registry");
        String mci_port = getenv().get("MCI_PORT");
        port(Integer.parseInt(mci_port));

        //handling exceptions thrown while processing requests
        new GlobalExceptionHandler();

        //instantiate all utilities here
        mciProperties = MCIProperties.getInstance();
        //instantiate all utilities here

        instantiateDao();

        instantiateMappers();

        fhirPatientValidator = new FhirPatientValidator(mciProperties);

        identityStore = new IdentityStore();
        mciHealthIdStore = new MciHealthIdStore();

        identityProviderService = new IdentityProviderService(identityStore);

        healthIdService = new HealthIdService
                (identityProviderService, mciHealthIdStore, mciProperties);

        patientService = new PatientService(patientMapper, healthIdService, patientRepository, fhirPatientValidator);
        //instantiate all services/mappers/ here

        instantiateControllers();

        //instantiate MCIRoutes with all controllers here
        new MCIRoutes(patientController);
        //instantiate MCIRoutes with all controllers here


        try {
            healthIdService.loadFromFile();
        } catch (IOException e) {
            logger.error("Unable to create health id file.");
            Spark.stop();
        }
        //instantiate a scheduler to replenish healthIds
        createHealthIdReplenishScheduler();
        //instantiate a scheduler to replenish healthIds
    }

    private static void instantiateControllers() {
        patientController = new PatientController(patientService);
    }

    private static void instantiateMappers() {
        patientMapper = new PatientMapper(mciProperties);
    }

    private static void instantiateDao() {
        MappingManager mappingManager = MCICassandraConfig.getInstance().getMappingManager();
        patientRepository = new PatientRepository(mappingManager);
    }

    private static void createHealthIdReplenishScheduler() {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(new HealthIdReplenishTask(healthIdService), mciProperties.getHealthIdReplenishInitialDelay(),
                mciProperties.getHealthIdReplenishDelay(), TimeUnit.MILLISECONDS);
    }

    public static MciHealthIdStore getMciHealthIdStore() {
        return mciHealthIdStore;
    }

    public static MCIProperties getMciProperties() {
        return mciProperties;
    }

    public static PatientController getPatientController() {
        return patientController;
    }

    public static PatientMapper getPatientMapper() {
        return patientMapper;
    }

    public static PatientRepository getPatientRepository() {
        return patientRepository;
    }

    public static PatientService getPatientService() {
        return patientService;
    }

    public static FhirPatientValidator getFhirPatientValidator() {
        return fhirPatientValidator;
    }

    public static HealthIdService getHealthIdService() {
        return healthIdService;
    }

    public static IdentityProviderService getIdentityProviderService() {
        return identityProviderService;
    }

    public static IdentityStore getIdentityStore() {
        return identityStore;
    }
}
