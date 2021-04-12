package org.broadinstitute.consent.http.service;

import com.google.inject.Inject;
import org.broadinstitute.consent.http.configurations.ServicesConfiguration;
import org.broadinstitute.consent.http.models.validate.ValidateResponse;
import org.glassfish.jersey.client.ClientProperties;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;

public class UseRestrictionValidator {
    private Client client;
    private final String validateUrl;

    @Inject
    public UseRestrictionValidator(Client client, ServicesConfiguration config) {
        this.client = ClientBuilder.newClient();
        client.property(ClientProperties.CONNECT_TIMEOUT, 10000);
        client.property(ClientProperties.READ_TIMEOUT,    10000);
        this.validateUrl = config.getValidateUseRestrictionURL();
    }

    public void validateUseRestriction(String useRestriction) throws IllegalArgumentException {
        Response res = client.target(validateUrl).request("application/json").post(Entity.json(useRestriction));
        if (res.getStatus() == Response.Status.OK.getStatusCode()) {
            ValidateResponse entity = res.readEntity(ValidateResponse.class);
            if (!entity.isValid()) {
                throw new IllegalArgumentException(entity.getErrors().toString());
            }
        } else {
            throw new IllegalArgumentException("There was an error posting the use restriction" + (res.readEntity(String.class)));
        }
    }

    public void setClient(Client client) {
        this.client = client;
    }

}
