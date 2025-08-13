package org.broadinstitute.consent.http.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ThreadUtils implements ConsentLogger {

  /**
   * Creates a new ExecutorService with a fixed thread pool size based on the number of available
   * processors. It also registers a shutdown hook to cleanly shut down the executor service when
   * the JVM exits.
   *
   * @return A new ExecutorService instance.
   */
  public ExecutorService getExecutorService(Class clazz) {
    ExecutorService executorService = Executors.newFixedThreadPool(
        Runtime.getRuntime().availableProcessors());
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      logInfo("Shutting down  %s executor service".formatted(clazz.getSimpleName()));
      executorService.shutdown();
    }));
    return executorService;
  }
}
