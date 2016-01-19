package org.broadinstitute.consent.http.models.dto;

public enum DefaultErrorMessage {
    INTERNAL_SERVER_ERROR("Internal Server Error. Please, try again later or contact Support."),
    NOT_FOUND("The requested entity wasn't found.");

    private DefaultErrorMessage(String message) {
        this.message = message;
    }

    // Internal state
    private String message;

    public String getMessage() {
        return message;
    }
}