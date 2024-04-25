package org.broadinstitute.consent.http.util;

import io.sentry.Sentry;
import io.sentry.SentryEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface ConsentLogger {

  @SuppressWarnings("rawtypes")
  default Logger getLogger(Class clazz) {
    return LoggerFactory.getLogger(clazz);
  }

  /**
   * Logs an exception to the console and to Sentry
   *
   * @param e Exception
   */
  default void logException(Exception e) {
    getLogger(this.getClass()).error(e.getMessage());
    Sentry.captureEvent(new SentryEvent(e));
  }

  /**
   * Logs a throwable to the console and to Sentry
   *
   * @param t Throwable
   */
  default void logThrowable(Throwable t) {
    getLogger(this.getClass()).error(t.getMessage());
    Sentry.captureEvent(new SentryEvent(t));
  }

  /**
   * Logs a message and exception to the console and to Sentry
   *
   * @param message The message
   * @param e       Exception
   */
  default void logException(String message, Exception e) {
    getLogger(this.getClass()).error(message + e.getMessage());
    Sentry.captureEvent(new SentryEvent(e));
  }

  /**
   * Logs a warning message to the console
   *
   * @param message Error Message
   */
  default void logWarn(String message) {
    getLogger(this.getClass()).warn(message);
  }

  /**
   * Logs a warning message and throwable to the console
   *
   * @param message Error Message
   * @param t       Exception
   */
  default void logWarn(String message, Throwable t) {
    getLogger(this.getClass()).warn(message, t);
  }

  /**
   * Logs an info message to the console
   *
   * @param message Error Message
   */
  default void logInfo(String message) {
    getLogger(this.getClass()).info(message);
  }

  /**
   * Logs a debug message to the console
   *
   * @param message Error Message
   */
  default void logDebug(String message) {
    getLogger(this.getClass()).debug(message);
  }
}
