package org.broadinstitute.consent.http.models.support;

/**
 * Represents user creating a ticket to request support via Zendesk https://broadinstitute.zendesk.com/api/v2/requests.json
 */
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
