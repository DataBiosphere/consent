package org.broadinstitute.consent.http.enumeration;


public enum Actions {

    REPLACE("replace"), ADD("add"), REMOVE("remove"), CREATE("create");

    private String value;

    Actions(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

}
