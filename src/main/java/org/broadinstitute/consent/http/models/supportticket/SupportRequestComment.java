package org.broadinstitute.consent.http.models.supportticket;

public class SupportRequestComment {
    private String body;

    public SupportRequestComment(String description, String url) {
        this.body = description + "\n\n------------------\nSubmitted from: " + url;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }
}
