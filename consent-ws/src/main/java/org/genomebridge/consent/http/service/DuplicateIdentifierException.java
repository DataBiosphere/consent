package org.genomebridge.consent.http.service;

public class DuplicateIdentifierException extends Exception {

    private String id;

    public DuplicateIdentifierException(String id) {
        super(String.format("Identifier \"%s\" already names an expression in the database", id));
        this.id = id;
    }

    public String getId() { return id; }
}
