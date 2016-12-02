package org.sharedhealth.mci.web.launch;

import com.datastax.driver.mapping.MappingManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ehcache.CacheManager;
import org.ehcache.config.CacheConfiguration;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.expiry.Duration;
import org.ehcache.expiry.Expirations;
import org.sharedhealth.mci.web.config.MCICassandraConfig;
import org.sharedhealth.mci.web.config.MCIProperties;
import org.sharedhealth.mci.web.controller.GlobalExceptionHandler;
import org.sharedhealth.mci.web.controller.MCIRoutes;
import org.sharedhealth.mci.web.controller.PatientController;
import org.sharedhealth.mci.web.mapper.FHIRBundleMapper;
import org.sharedhealth.mci.web.mapper.MCIPatientMapper;
import org.sharedhealth.mci.web.model.IdentityStore;
import org.sharedhealth.mci.web.model.MciHealthIdStore;
import org.sharedhealth.mci.web.repository.MasterDataRepository;
import org.sharedhealth.mci.web.repository.PatientRepository;
import org.sharedhealth.mci.web.security.TokenAuthenticationFilter;
import org.sharedhealth.mci.web.security.UserInfo;
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
    public static final String IDENTITY_PROVIDER_CACHE = "identityProviderUserCache";

    private static final int ENOENT_NO_SUCH_FILE_OR_DIR_ERROR_CODE = 2;
    private static final int ENOENT_BAD_FILE_FORMAT_ERROR_CODE = 59;

    private static PatientRepository patientRepository;
    private static MasterDataRepository masterDataRepository;
    private static MCIProperties mciProperties;
    private static MCIPatientMapper mciPatientMapper;
    private static FHIRBundleMapper fhirBundleMapper;
    private static FhirPatientValidator fhirPatientValidator;
    private static IdentityStore identityStore;
    private static IdentityProviderService identityProviderService;
    private static MciHealthIdStore mciHealthIdStore;
    private static HealthIdService healthIdService;
    private static PatientService patientService;
    private static PatientController patientController;
    private static TokenAuthenticationFilter authenticationFilter;
    private static CacheManager cacheManager;

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

        try {
            fhirPatientValidator = new FhirPatientValidator(mciProperties);
        } catch (Exception e) {
            logger.error("Unable to create FHIR Patient Validator", e);
            Spark.stop();
            System.exit(ENOENT_NO_SUCH_FILE_OR_DIR_ERROR_CODE);
        }

        identityStore = new IdentityStore();
        mciHealthIdStore = MciHealthIdStore.getInstance();

        identityProviderService = new IdentityProviderService(identityStore);

        healthIdService = new HealthIdService
                (identityProviderService, mciHealthIdStore, mciProperties);

        patientService = new PatientService(mciPatientMapper, fhirBundleMapper, healthIdService, patientRepository, fhirPatientValidator);
        //instantiate all services/mappers/ here

        instantiateControllers();

        //instantiate MCIRoutes with all controllers here
        CacheConfiguration<String, UserInfo> configuration = CacheConfigurationBuilder
                .newCacheConfigurationBuilder(String.class, UserInfo.class, ResourcePoolsBuilder.heap(500))
                .withExpiry(Expirations.timeToLiveExpiration(Duration.of(2 * 60, TimeUnit.SECONDS)))
                .withExpiry(Expirations.timeToIdleExpiration(Duration.of(2 * 60, TimeUnit.SECONDS)))
                .build();
        cacheManager = CacheManagerBuilder.newCacheManagerBuilder()
                .withCache(IDENTITY_PROVIDER_CACHE, configuration)
                .build(true);

        authenticationFilter = new TokenAuthenticationFilter(identityProviderService, cacheManager.getCache(IDENTITY_PROVIDER_CACHE, String.class, UserInfo.class));
        new MCIRoutes(patientController, authenticationFilter);
        //instantiate MCIRoutes with all controllers here

        try {
            healthIdService.loadFromFile();
        } catch (IOException e) {
            logger.error("Unable to create health id file.");
            Spark.stop();
            System.exit(ENOENT_BAD_FILE_FORMAT_ERROR_CODE);
        }
        //instantiate a scheduler to replenish healthIds
        createHealthIdReplenishScheduler();
        //instantiate a scheduler to replenish healthIds
    }

    private static void instantiateControllers() {
        patientController = new PatientController(patientService);
    }

    private static void instantiateMappers() {
        mciPatientMapper = new MCIPatientMapper(mciProperties, masterDataRepository);
        fhirBundleMapper = new FHIRBundleMapper();
    }

    private static void instantiateDao() {
        MappingManager mappingManager = MCICassandraConfig.getInstance().getMappingManager();
        patientRepository = new PatientRepository(mappingManager);
        masterDataRepository = new MasterDataRepository(mappingManager);
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

    public static MCIPatientMapper getMciPatientMapper() {
        return mciPatientMapper;
    }

    public static FHIRBundleMapper getFHIRBundleMapper() {
        return fhirBundleMapper;
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

    public static CacheManager getCacheManager() {
        return cacheManager;
    }
}
