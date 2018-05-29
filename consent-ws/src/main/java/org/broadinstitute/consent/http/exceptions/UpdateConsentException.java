package org.broadinstitute.consent.http.exceptions;

public class UpdateConsentException extends Exception {

    public UpdateConsentException() {
        super();
    }

    public UpdateConsentException(String message) {
        super(message);
    }

    public UpdateConsentException(String message, Throwable cause) {
        super(message, cause);
    }

    public UpdateConsentException(Throwable cause) {
        super(cause);
    }

    protected UpdateConsentException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
