package org.broadinstitute.consent.http.service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

final class DashboardServiceSupport {

  private DashboardServiceSupport() {}

  static <T> T join(CompletableFuture<T> future) {
    try {
      return future.join();
    } catch (CompletionException e) {
      if (e.getCause() instanceof RuntimeException runtimeException) {
        throw runtimeException;
      }
      throw e;
    }
  }
}
