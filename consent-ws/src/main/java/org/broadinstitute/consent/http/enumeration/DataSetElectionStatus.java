package org.broadinstitute.consent.http.enumeration;

public enum DataSetElectionStatus {

    DS_APPROVED("Approved by Data Owner(s)."), DS_DENIED("Denied by Data Owner(s)."), DS_PENDING("Needs Data Owner(s) approval."), APPROVAL_NOT_NEEDED("Approval not needed.");

    private String value;

    DataSetElectionStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static String getValue(String value) {
        for (ElectionStatus e : ElectionStatus.values()) {
            if (e.getValue().equalsIgnoreCase(value)) {
                return e.getValue();
            }
        }
        return null;
    }

}
