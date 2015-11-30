package org.broadinstitute.consent.http.service;

public class UnknownIdentifierException extends Exception {

    private String id;

    public UnknownIdentifierException(String id) {
        super(String.format("Unknown identifier \"%s\"", id));
        this.id = id;
    }

    public String getId() {
        return id;
    }

}
