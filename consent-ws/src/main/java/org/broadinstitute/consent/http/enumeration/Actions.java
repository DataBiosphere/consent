package org.broadinstitute.consent.http.enumeration;


public enum Actions {

    REPLACE("replace"), ADD("add"), REMOVE("remove");

    private String value;

    Actions(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

}
