package org.broadinstitute.consent.http;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codahale.metrics.health.HealthCheckRegistry;
import com.google.inject.Guice;
import com.google.inject.Injector;
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
import org.broadinstitute.consent.http.authentication.AuthorizationHelper;
import org.broadinstitute.consent.http.authentication.DuosUserAuthenticator;
import org.broadinstitute.consent.http.authentication.OAuthAuthenticator;
import org.broadinstitute.consent.http.cloudstore.GCSService;
import org.broadinstitute.consent.http.configurations.ConsentConfiguration;
import org.broadinstitute.consent.http.configurations.ElasticSearchConfiguration;
import org.broadinstitute.consent.http.configurations.MailConfiguration;
import org.broadinstitute.consent.http.configurations.OidcConfiguration;
import org.broadinstitute.consent.http.configurations.ServicesConfiguration;
import org.broadinstitute.consent.http.db.DaaDAO;
import org.broadinstitute.consent.http.db.DarCollectionDAO;
import org.broadinstitute.consent.http.db.DataAccessRequestDAO;
import org.broadinstitute.consent.http.db.DatasetAuthorizationReaderDAO;
import org.broadinstitute.consent.http.db.DatasetDAO;
import org.broadinstitute.consent.http.db.DraftDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.db.FileStorageObjectDAO;
import org.broadinstitute.consent.http.db.LibraryCardDAO;
import org.broadinstitute.consent.http.db.OidcAuthorityDAO;
import org.broadinstitute.consent.http.db.SamDAO;
import org.broadinstitute.consent.http.db.StudyDAO;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.db.UserRoleDAO;
import org.broadinstitute.consent.http.db.VoteDAO;
import org.broadinstitute.consent.http.filters.ClaimsCache;
import org.broadinstitute.consent.http.mail.SendGridAPI;
import org.broadinstitute.consent.http.mail.freemarker.FreeMarkerTemplateHelper;
import org.broadinstitute.consent.http.matching.DataUseMatcherV4;
import org.broadinstitute.consent.http.matching.DataUseUtil;
import org.broadinstitute.consent.http.matching.TranslationUtil;
import org.broadinstitute.consent.http.models.support.TicketFactory;
import org.broadinstitute.consent.http.service.AcknowledgementService;
import org.broadinstitute.consent.http.service.CounterService;
import org.broadinstitute.consent.http.service.DACAutomationRuleService;
import org.broadinstitute.consent.http.service.DaaService;
import org.broadinstitute.consent.http.service.DacService;
import org.broadinstitute.consent.http.service.DarCollectionService;
import org.broadinstitute.consent.http.service.DataAccessRequestService;
import org.broadinstitute.consent.http.service.DatasetRegistrationService;
import org.broadinstitute.consent.http.service.DatasetService;
import org.broadinstitute.consent.http.service.ElasticSearchService;
import org.broadinstitute.consent.http.service.ElectionService;
import org.broadinstitute.consent.http.service.EmailService;
import org.broadinstitute.consent.http.service.FeatureFlagService;
import org.broadinstitute.consent.http.service.FileStorageObjectService;
import org.broadinstitute.consent.http.service.InstitutionService;
import org.broadinstitute.consent.http.service.LibraryCardService;
import org.broadinstitute.consent.http.service.MatchService;
import org.broadinstitute.consent.http.service.MetricsService;
import org.broadinstitute.consent.http.service.NihService;
import org.broadinstitute.consent.http.service.OidcService;
import org.broadinstitute.consent.http.service.OntologyService;
import org.broadinstitute.consent.http.service.SupportRequestService;
import org.broadinstitute.consent.http.service.UseRestrictionConverter;
import org.broadinstitute.consent.http.service.UserService;
import org.broadinstitute.consent.http.service.VoteService;
import org.broadinstitute.consent.http.service.dao.DaaServiceDAO;
import org.broadinstitute.consent.http.service.dao.DacServiceDAO;
import org.broadinstitute.consent.http.service.dao.DarCollectionServiceDAO;
import org.broadinstitute.consent.http.service.dao.DataAccessRequestServiceDAO;
import org.broadinstitute.consent.http.service.dao.DatasetServiceDAO;
import org.broadinstitute.consent.http.service.dao.DraftFileStorageServiceDAO;
import org.broadinstitute.consent.http.service.dao.DraftServiceDAO;
import org.broadinstitute.consent.http.service.dao.NihServiceDAO;
import org.broadinstitute.consent.http.service.dao.UserServiceDAO;
import org.broadinstitute.consent.http.service.dao.VoteServiceDAO;
import org.broadinstitute.consent.http.service.feature.InstitutionAndLibraryCardEnforcement;
import org.broadinstitute.consent.http.service.ontology.OntologyIndexService;
import org.broadinstitute.consent.http.service.sam.SamService;
import org.broadinstitute.consent.http.util.CountryValidator;
import org.broadinstitute.consent.http.util.HttpClientUtil;
import org.broadinstitute.consent.http.util.InstitutionUtil;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ConsentModuleTest extends AbstractTestHelper {

  private Environment environment;
  private Injector injector;

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
    injector = Guice.createInjector(new ConsentModule(config, environment));
  }

  @Test
  void testProvidesClient() {
    Client client = injector.getInstance(Client.class);
    assertNotNull(client);
    assertSame(client, injector.getInstance(Client.class));
  }

  @Test
  void testProvidesExecutorService() {
    ExecutorService executorService = injector.getInstance(ExecutorService.class);
    assertNotNull(executorService);
    assertFalse(executorService.isShutdown());
    assertSame(executorService, injector.getInstance(ExecutorService.class));
  }

  @Test
  void testSimpleProvidersSameInstance() {
    // Providers that return pre-built fields or config getters
    assertSame(injector.getInstance(Jdbi.class), injector.getInstance(Jdbi.class));
    assertSame(
        injector.getInstance(ElasticSearchConfiguration.class),
        injector.getInstance(ElasticSearchConfiguration.class));
    assertSame(
        injector.getInstance(MailConfiguration.class),
        injector.getInstance(MailConfiguration.class));
    assertSame(
        injector.getInstance(ServicesConfiguration.class),
        injector.getInstance(ServicesConfiguration.class));
    assertSame(
        injector.getInstance(HealthCheckRegistry.class),
        injector.getInstance(HealthCheckRegistry.class));
    assertSame(
        injector.getInstance(OidcConfiguration.class),
        injector.getInstance(OidcConfiguration.class));
    assertSame(
        injector.getInstance(UseRestrictionConverter.class),
        injector.getInstance(UseRestrictionConverter.class));
    assertSame(injector.getInstance(ElectionDAO.class), injector.getInstance(ElectionDAO.class));
    assertSame(injector.getInstance(VoteDAO.class), injector.getInstance(VoteDAO.class));
    assertSame(injector.getInstance(StudyDAO.class), injector.getInstance(StudyDAO.class));
    assertSame(injector.getInstance(DatasetDAO.class), injector.getInstance(DatasetDAO.class));
    assertSame(
        injector.getInstance(DatasetAuthorizationReaderDAO.class),
        injector.getInstance(DatasetAuthorizationReaderDAO.class));
    assertSame(injector.getInstance(DaaDAO.class), injector.getInstance(DaaDAO.class));
    assertSame(injector.getInstance(UserDAO.class), injector.getInstance(UserDAO.class));
    assertSame(injector.getInstance(UserRoleDAO.class), injector.getInstance(UserRoleDAO.class));
    assertSame(
        injector.getInstance(FileStorageObjectDAO.class),
        injector.getInstance(FileStorageObjectDAO.class));
    assertSame(
        injector.getInstance(LibraryCardDAO.class), injector.getInstance(LibraryCardDAO.class));
    assertSame(injector.getInstance(DraftDAO.class), injector.getInstance(DraftDAO.class));
    assertSame(
        injector.getInstance(DataAccessRequestDAO.class),
        injector.getInstance(DataAccessRequestDAO.class));
    assertSame(
        injector.getInstance(DarCollectionDAO.class), injector.getInstance(DarCollectionDAO.class));
  }

  @Test
  void testMemoizedProvidersReturnTheSameInstance() {
    // Infrastructure and DAO-layer singletons
    assertSame(
        injector.getInstance(OntologyService.class), injector.getInstance(OntologyService.class));
    assertSame(
        injector.getInstance(DatasetRegistrationService.class),
        injector.getInstance(DatasetRegistrationService.class));
    assertSame(
        injector.getInstance(InstitutionAndLibraryCardEnforcement.class),
        injector.getInstance(InstitutionAndLibraryCardEnforcement.class));
    assertSame(injector.getInstance(SamDAO.class), injector.getInstance(SamDAO.class));
    assertSame(
        injector.getInstance(HttpClientUtil.class), injector.getInstance(HttpClientUtil.class));
    assertSame(injector.getInstance(GCSService.class), injector.getInstance(GCSService.class));
    assertSame(
        injector.getInstance(OntologyIndexService.class),
        injector.getInstance(OntologyIndexService.class));
    assertSame(
        injector.getInstance(ElasticSearchService.class),
        injector.getInstance(ElasticSearchService.class));
    assertSame(
        injector.getInstance(DatasetServiceDAO.class),
        injector.getInstance(DatasetServiceDAO.class));
    assertSame(
        injector.getInstance(DarCollectionServiceDAO.class),
        injector.getInstance(DarCollectionServiceDAO.class));
    assertSame(
        injector.getInstance(DataAccessRequestServiceDAO.class),
        injector.getInstance(DataAccessRequestServiceDAO.class));
    assertSame(
        injector.getInstance(VoteServiceDAO.class), injector.getInstance(VoteServiceDAO.class));
    assertSame(
        injector.getInstance(DaaServiceDAO.class), injector.getInstance(DaaServiceDAO.class));
    assertSame(
        injector.getInstance(DacServiceDAO.class), injector.getInstance(DacServiceDAO.class));
    assertSame(
        injector.getInstance(NihServiceDAO.class), injector.getInstance(NihServiceDAO.class));
    assertSame(
        injector.getInstance(UserServiceDAO.class), injector.getInstance(UserServiceDAO.class));
    assertSame(
        injector.getInstance(ElectionService.class), injector.getInstance(ElectionService.class));
    assertSame(
        injector.getInstance(FeatureFlagService.class),
        injector.getInstance(FeatureFlagService.class));
    assertSame(
        injector.getInstance(MetricsService.class), injector.getInstance(MetricsService.class));
    assertSame(injector.getInstance(SamService.class), injector.getInstance(SamService.class));
    assertSame(
        injector.getInstance(OidcAuthorityDAO.class), injector.getInstance(OidcAuthorityDAO.class));
    assertSame(injector.getInstance(OidcService.class), injector.getInstance(OidcService.class));
    assertSame(
        injector.getInstance(SupportRequestService.class),
        injector.getInstance(SupportRequestService.class));
  }

  @Test
  void testMemoizedServiceProvidersReturnTheSameInstance() {
    // Business service singletons
    assertSame(
        injector.getInstance(FreeMarkerTemplateHelper.class),
        injector.getInstance(FreeMarkerTemplateHelper.class));
    assertSame(injector.getInstance(EmailService.class), injector.getInstance(EmailService.class));
    assertSame(injector.getInstance(SendGridAPI.class), injector.getInstance(SendGridAPI.class));
    assertSame(
        injector.getInstance(CounterService.class), injector.getInstance(CounterService.class));
    assertSame(
        injector.getInstance(DACAutomationRuleService.class),
        injector.getInstance(DACAutomationRuleService.class));
    assertSame(injector.getInstance(VoteService.class), injector.getInstance(VoteService.class));
    assertSame(injector.getInstance(MatchService.class), injector.getInstance(MatchService.class));
    assertSame(
        injector.getInstance(InstitutionService.class),
        injector.getInstance(InstitutionService.class));
    assertSame(
        injector.getInstance(LibraryCardService.class),
        injector.getInstance(LibraryCardService.class));
    assertSame(injector.getInstance(UserService.class), injector.getInstance(UserService.class));
    assertSame(
        injector.getInstance(DatasetService.class), injector.getInstance(DatasetService.class));
    assertSame(injector.getInstance(DaaService.class), injector.getInstance(DaaService.class));
    assertSame(injector.getInstance(DacService.class), injector.getInstance(DacService.class));
    assertSame(
        injector.getInstance(AcknowledgementService.class),
        injector.getInstance(AcknowledgementService.class));
    assertSame(injector.getInstance(NihService.class), injector.getInstance(NihService.class));
    assertSame(
        injector.getInstance(DataAccessRequestService.class),
        injector.getInstance(DataAccessRequestService.class));
    assertSame(
        injector.getInstance(FileStorageObjectService.class),
        injector.getInstance(FileStorageObjectService.class));
    assertSame(
        injector.getInstance(DarCollectionService.class),
        injector.getInstance(DarCollectionService.class));
    assertSame(
        injector.getInstance(DraftFileStorageServiceDAO.class),
        injector.getInstance(DraftFileStorageServiceDAO.class));
    assertSame(
        injector.getInstance(DraftServiceDAO.class), injector.getInstance(DraftServiceDAO.class));
    assertSame(
        injector.getInstance(AuthorizationHelper.class),
        injector.getInstance(AuthorizationHelper.class));
    assertSame(
        injector.getInstance(OAuthAuthenticator.class),
        injector.getInstance(OAuthAuthenticator.class));
    assertSame(
        injector.getInstance(DuosUserAuthenticator.class),
        injector.getInstance(DuosUserAuthenticator.class));
  }

  @Test
  void testNewSingletonProvidersReturnTheSameInstance() {
    // New providers added for injectable DI pattern
    assertSame(injector.getInstance(ClaimsCache.class), injector.getInstance(ClaimsCache.class));
    assertSame(
        injector.getInstance(TranslationUtil.class), injector.getInstance(TranslationUtil.class));
    assertSame(injector.getInstance(DataUseUtil.class), injector.getInstance(DataUseUtil.class));
    assertSame(
        injector.getInstance(DataUseMatcherV4.class), injector.getInstance(DataUseMatcherV4.class));
    assertSame(
        injector.getInstance(CountryValidator.class), injector.getInstance(CountryValidator.class));
    assertSame(
        injector.getInstance(InstitutionUtil.class), injector.getInstance(InstitutionUtil.class));
    assertSame(
        injector.getInstance(TicketFactory.class), injector.getInstance(TicketFactory.class));
  }

  @Test
  void testExecutorServiceShutsDownOnLifecycleStop() throws Exception {
    try (ExecutorService executorService = injector.getInstance(ExecutorService.class)) {
      assertFalse(executorService.isShutdown());
      findExecutorManaged().stop();
      assertTrue(executorService.isTerminated());
    }
  }

  @Test
  void testExecutorServiceShutdownNowWhenStopIsInterrupted() throws Exception {
    try (ExecutorService executorService = injector.getInstance(ExecutorService.class)) {
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
