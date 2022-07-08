package org.broadinstitute.consent.http.models.supportticket;

public class SupportRequester {

    private String name;
    private String email;

    public SupportRequester(String name, String email) {
        this.name = name;
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
