package org.broadinstitute.consent.http.enumeration;

public enum HeaderDAR {

    DAR_ID("DAR ID"),
    DATASET_NAME("Dataset name"),
    DATASET_ID("Dataset ID"),
    CONSENT_ID("Consent ID"),
    DATA_REQUESTER_NAME("Data requester name"),
    ORGANIZATION("Organization"),
    CODED_VERSION_SDUL("Coded version of sDUL"),
    CODED_VERSION_DAR("Coded version of DAR"),
    RESEARCH_PURPOSE("Research Purpose summary"),
    DATE_REQUEST_SUBMISSION("Date of request submission"),
    DATE_REQUEST_APPROVAL("Date of request approval"),
    DATE_REQUEST_RE_ATTESTATION("Date of request re-attestation"),
    DATE_REQUEST_APPROVAL_DISAPROVAL("Date of request approval/disapproval"),
    APPROVED_DISAPPROVED("Approved / disapproved"),
    RENEWAL_DATE("Renewal Date"),
    USERNAME("Username"),
    NAME("Name");


    private String value;

    HeaderDAR(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

}
