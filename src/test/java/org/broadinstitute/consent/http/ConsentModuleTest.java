package org.broadinstitute.consent.http;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.dropwizard.configuration.YamlConfigurationFactory;
import io.dropwizard.core.setup.Environment;
import io.dropwizard.jackson.Jackson;
import io.dropwizard.jersey.validation.Validators;
import io.dropwizard.lifecycle.JettyManaged;
import io.dropwizard.lifecycle.Managed;
import io.dropwizard.testing.ResourceHelpers;
import jakarta.ws.rs.client.Client;
import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import org.broadinstitute.consent.http.configurations.ConsentConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ConsentModuleTest extends AbstractTestHelper {

  private Environment environment;
  private ConsentModule module;

  @BeforeEach
  void setUp() throws Exception {
    ConsentConfiguration config =
        new YamlConfigurationFactory<>(
                ConsentConfiguration.class,
                Validators.newValidator(),
                Jackson.newObjectMapper(),
                "dw")
            .build(new File(ResourceHelpers.resourceFilePath("consent-config.yml")));
    environment = new Environment("test");
    module = new ConsentModule(config, environment);
  }

  @Test
  void testProvidesClient() {
    assertNotNull(module.providesClient());
    Client client = module.providesClient();
    assertSame(client, module.providesClient());
  }

  @Test
  void testProvidesExecutorService() {
    ExecutorService executorService = module.providesExecutorService();
    assertNotNull(executorService);
    assertFalse(executorService.isShutdown());
    assertSame(executorService, module.providesExecutorService());
  }

  @Test
  void testSimpleProvidersSameInstance() {
    // Providers that return pre-built fields or config getters
    assertSame(module.providesJdbi(), module.providesJdbi());
    assertSame(
        module.providesElasticSearchConfiguration(), module.providesElasticSearchConfiguration());
    assertSame(module.providesMailConfiguration(), module.providesMailConfiguration());
    assertSame(module.providesServicesConfiguration(), module.providesServicesConfiguration());
    assertSame(module.providesHealthCheckRegistry(), module.providesHealthCheckRegistry());
    assertSame(module.providesOidcConfiguration(), module.providesOidcConfiguration());
    assertSame(module.providesUseRestrictionConverter(), module.providesUseRestrictionConverter());
    assertSame(module.providesElectionDAO(), module.providesElectionDAO());
    assertSame(module.providesVoteDAO(), module.providesVoteDAO());
    assertSame(module.providesStudyDAO(), module.providesStudyDAO());
    assertSame(module.providesDatasetDAO(), module.providesDatasetDAO());
    assertSame(
        module.providesDatasetAuthorizationReaderDAO(),
        module.providesDatasetAuthorizationReaderDAO());
    assertSame(module.providesDaaDAO(), module.providesDaaDAO());
    assertSame(module.providesUserDAO(), module.providesUserDAO());
    assertSame(module.providesUserRoleDAO(), module.providesUserRoleDAO());
    assertSame(module.providesFileStorageObjectDAO(), module.providesFileStorageObjectDAO());
    assertSame(module.providesLibraryCardDAO(), module.providesLibraryCardDAO());
    assertSame(module.providesDraftDAO(), module.providesDraftDAO());
    assertSame(module.providesDataAccessRequestDAO(), module.providesDataAccessRequestDAO());
    assertSame(module.providesDARCollectionDAO(), module.providesDARCollectionDAO());
  }

  @Test
  void testMemoizedProvidersReturnTheSameInstance() {
    // Infrastructure and DAO-layer singletons
    assertSame(module.providesOntologyService(), module.providesOntologyService());
    assertSame(
        module.providesDatasetRegistrationService(), module.providesDatasetRegistrationService());
    assertSame(
        module.providesInstitutionAndLibraryCardEnforcement(),
        module.providesInstitutionAndLibraryCardEnforcement());
    assertSame(module.providesSamDAO(), module.providesSamDAO());
    assertSame(module.providesHttpClientUtil(), module.providesHttpClientUtil());
    assertSame(module.providesGCSService(), module.providesGCSService());
    assertSame(module.providesOntologyIndexService(), module.providesOntologyIndexService());
    assertSame(module.providesElasticSearchService(), module.providesElasticSearchService());
    assertSame(module.providesDatasetServiceDAO(), module.providesDatasetServiceDAO());
    assertSame(module.providesDarCollectionServiceDAO(), module.providesDarCollectionServiceDAO());
    assertSame(
        module.providesDataAccessRequestServiceDAO(), module.providesDataAccessRequestServiceDAO());
    assertSame(module.providesVoteServiceDAO(), module.providesVoteServiceDAO());
    assertSame(module.providesDaaServiceDAO(), module.providesDaaServiceDAO());
    assertSame(module.providesDacServiceDAO(), module.providesDacServiceDAO());
    assertSame(module.providesNIHServiceDAO(), module.providesNIHServiceDAO());
    assertSame(module.providesUserServiceDAO(), module.providesUserServiceDAO());
    assertSame(module.providesElectionService(), module.providesElectionService());
    assertSame(module.providesFeatureFlagService(), module.providesFeatureFlagService());
    assertSame(module.providesMetricsService(), module.providesMetricsService());
    assertSame(module.providesSamService(), module.providesSamService());
    assertSame(module.providesOidcAuthorityDAO(), module.providesOidcAuthorityDAO());
    assertSame(module.providesOidcService(), module.providesOidcService());
    assertSame(module.providesSupportRequestService(), module.providesSupportRequestService());
  }

  @Test
  void testMemoizedServiceProvidersReturnTheSameInstance() {
    // Business service singletons
    assertSame(
        module.providesFreeMarkerTemplateHelper(), module.providesFreeMarkerTemplateHelper());
    assertSame(module.providesEmailService(), module.providesEmailService());
    assertSame(module.providesSendGridAPI(), module.providesSendGridAPI());
    assertSame(module.providesCounterService(), module.providesCounterService());
    assertSame(module.providesRuleService(), module.providesRuleService());
    assertSame(module.providesVoteService(), module.providesVoteService());
    assertSame(module.providesMatchService(), module.providesMatchService());
    assertSame(module.providesInstitutionService(), module.providesInstitutionService());
    assertSame(module.providesLibraryCardService(), module.providesLibraryCardService());
    assertSame(module.providesUserService(), module.providesUserService());
    assertSame(module.providesDatasetService(), module.providesDatasetService());
    assertSame(module.providesDaaService(), module.providesDaaService());
    assertSame(module.providesDacService(), module.providesDacService());
    assertSame(module.providesAcknowledgementService(), module.providesAcknowledgementService());
    assertSame(module.providesNihService(), module.providesNihService());
    assertSame(
        module.providesDataAccessRequestService(), module.providesDataAccessRequestService());
    assertSame(
        module.providesFileStorageObjectService(), module.providesFileStorageObjectService());
    assertSame(module.providesDarCollectionService(), module.providesDarCollectionService());
    assertSame(module.providesDraftFileStorageService(), module.providesDraftFileStorageService());
    assertSame(module.providesDraftServiceDAO(), module.providesDraftServiceDAO());
    assertSame(module.providesAuthorizationHelper(), module.providesAuthorizationHelper());
    assertSame(module.providesOAuthAuthenticator(), module.providesOAuthAuthenticator());
    assertSame(
        module.providesDuosUserOAuthAuthenticator(), module.providesDuosUserOAuthAuthenticator());
  }

  @Test
  void testNewSingletonProvidersReturnTheSameInstance() {
    // New providers added for injectable DI pattern
    assertSame(module.providesOntologyDAO(), module.providesOntologyDAO());
    assertSame(module.providesClaimsCache(), module.providesClaimsCache());
    assertSame(module.providesTranslationUtil(), module.providesTranslationUtil());
    assertSame(module.providesDataUseUtil(), module.providesDataUseUtil());
    assertSame(module.providesDataUseMatcherV4(), module.providesDataUseMatcherV4());
    assertSame(module.providesCountryValidator(), module.providesCountryValidator());
    assertSame(module.providesInstitutionUtil(), module.providesInstitutionUtil());
    assertSame(module.providesJsonSchemaUtil(), module.providesJsonSchemaUtil());
    assertSame(module.providesTicketFactory(), module.providesTicketFactory());
  }

  @Test
  void testExecutorServiceShutsDownOnLifecycleStop() throws Exception {
    try (ExecutorService executorService = module.providesExecutorService()) {
      assertFalse(executorService.isShutdown());
      findExecutorManaged().stop();
      assertTrue(executorService.isTerminated());
    }
  }

  @Test
  void testExecutorServiceShutdownNowWhenStopIsInterrupted() throws Exception {
    try (ExecutorService executorService = module.providesExecutorService()) {
      // Keep a task in flight so the executor is not yet terminated when stop() awaits it.
      CountDownLatch taskRunning = new CountDownLatch(1);
      executorService.submit(
          () -> {
            taskRunning.countDown();
            try {
              Thread.sleep(60_000);
            } catch (InterruptedException _) {
              Thread.currentThread().interrupt();
            }
          });
      assertTrue(taskRunning.await(5, java.util.concurrent.TimeUnit.SECONDS));

      Managed managed = findExecutorManaged();
      Thread.currentThread().interrupt();
      managed.stop();

      // The interrupt handler must force shutdown and restore the thread's interrupt flag.
      assertTrue(Thread.interrupted());
      assertTrue(executorService.isShutdown());
    }
  }

  /**
   * The module registers several lifecycle-managed objects (the JDBI data source, Jersey client
   * executors, and the shared executor shutdown hook). Find the module's own anonymous Managed.
   */
  private Managed findExecutorManaged() {
    return environment.lifecycle().getManagedObjects().stream()
        .filter(JettyManaged.class::isInstance)
        .map(JettyManaged.class::cast)
        .map(JettyManaged::getManaged)
        .filter(m -> m.getClass().getName().startsWith(ConsentModule.class.getName()))
        .findFirst()
        .orElseThrow();
  }
}
