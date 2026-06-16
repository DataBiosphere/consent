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
  void testMemoizedProvidersReturnTheSameInstance() {
    assertSame(module.providesOntologyService(), module.providesOntologyService());
    assertSame(
        module.providesDatasetRegistrationService(), module.providesDatasetRegistrationService());
    assertSame(
        module.providesInstitutionAndLibraryCardEnforcement(),
        module.providesInstitutionAndLibraryCardEnforcement());
    assertSame(module.providesSamDAO(), module.providesSamDAO());
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
