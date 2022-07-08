package org.broadinstitute.consent.http.models.supportticket;

public class SupportRequestComment {
    private String body;

    public SupportRequestComment(String body) {
        this.body = body;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }
}
