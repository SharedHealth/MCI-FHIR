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

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static java.lang.System.getenv;
import static spark.Spark.port;

public class Application {

    private static final Logger logger = LogManager.getLogger(Application.class);

    public static void main(String[] args) {
        logger.info("Starting MCI Registry");
        String mci_port = getenv().get("MCI_PORT");
        port(Integer.parseInt(mci_port));

        //handling exceptions thrown while processing requests
        new GlobalExceptionHandler();

        //instantiate all utilities here
        MCIProperties mciProperties = MCIProperties.getInstance();
        MappingManager mappingManager = MCICassandraConfig.getInstance().getMappingManager();
        //instantiate all utilities here

        //instantiate all DAOs here
        PatientRepository patientRepository = new PatientRepository(mappingManager);
        //instantiate all DAOs here

        //instantiate all services/mappers/ here
        PatientMapper patientMapper = new PatientMapper(mciProperties);

        FhirPatientValidator fhirPatientValidator = new FhirPatientValidator(mciProperties);
        IdentityStore identityStore = new IdentityStore();

        IdentityProviderService identityProviderService = new IdentityProviderService(identityStore);
        MciHealthIdStore mciHealthIdStore = new MciHealthIdStore();
        HealthIdService healthIdService = new HealthIdService(mappingManager, identityProviderService, mciHealthIdStore);
        PatientService patientService = new PatientService(patientMapper, healthIdService, patientRepository, fhirPatientValidator);
        //instantiate all services/mappers/ here

        //instantiate all controllers here
        PatientController patientController = new PatientController(patientService);
        //instantiate all controllers here

        //instantiate MCIRoutes with all controllers here
        new MCIRoutes(patientController);
        //instantiate MCIRoutes with all controllers here

        //instantiate a scheduler to replenish healthIds
        createHealthIdReplenishScheduler(mciProperties, healthIdService);
        //instantiate a scheduler to replenish healthIds
    }

    private static void createHealthIdReplenishScheduler(MCIProperties mciProperties, HealthIdService healthIdService) {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(new HealthIdReplenishTask(healthIdService), mciProperties.getHealthIdReplenishInitialDelay(),
                mciProperties.getHealthIdReplenishDelay(), TimeUnit.MILLISECONDS);
    }
}
