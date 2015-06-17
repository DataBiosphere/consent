package org.genomebridge.consent.http.cloudstore;

public enum FileType {
    PDF ("application/pdf"),
    DOC ("application/msword");

    private final String value;
    public String getValue() {return value;}

    FileType(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
