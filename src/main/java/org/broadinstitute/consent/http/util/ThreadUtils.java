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
  public <T> ExecutorService getExecutorService(Class<T> clazz) {
    int cpuCount = Math.max(1, Runtime.getRuntime().availableProcessors() - 1);
    logWarn(
        String.format(
            "New thread pool requested for class: %s, size: %d", clazz.getSimpleName(), cpuCount));
    ExecutorService executorService = Executors.newFixedThreadPool(cpuCount);
    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                () -> {
                  logInfo("Shutting down %s executor service".formatted(clazz.getSimpleName()));
                  executorService.shutdown();
                }));
    return executorService;
  }
}
