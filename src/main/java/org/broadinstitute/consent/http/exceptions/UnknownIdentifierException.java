package org.broadinstitute.consent.http.exceptions;

public class UnknownIdentifierException extends Exception {

    private final String id;

    public UnknownIdentifierException(String id) {
        super(String.format("Unknown identifier \"%s\"", id));
        this.id = id;
    }

    public String getId() {
        return id;
    }

}
