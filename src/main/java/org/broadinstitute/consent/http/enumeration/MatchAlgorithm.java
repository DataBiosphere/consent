package org.broadinstitute.consent.http.enumeration;

public enum MatchAlgorithm {
    V1("v1"),
    V2("v2");

    String version;

    MatchAlgorithm(String version) {
        this.version = version;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}
