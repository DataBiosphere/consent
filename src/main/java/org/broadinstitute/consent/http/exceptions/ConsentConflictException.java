package org.broadinstitute.consent.http.exceptions;

public class ConsentConflictException extends Exception{
  public ConsentConflictException() {
    super();
  }

  public ConsentConflictException(String message) {
    super(message);
  }

  public ConsentConflictException(String message, Throwable cause) {
    super(message, cause);
  }

  public ConsentConflictException(Throwable cause) {
    super(cause);
  }

  public ConsentConflictException(String message, Throwable cause, boolean enableSuppresion, boolean writableStackTrace) {
    super(message, cause, enableSuppresion, writableStackTrace);
  }
}
