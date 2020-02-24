package org.broadinstitute.consent.http.service.validate;

import org.broadinstitute.consent.http.configurations.ServicesConfiguration;
import org.broadinstitute.consent.http.models.validate.ValidateResponse;
import org.glassfish.jersey.client.ClientProperties;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;

public class UseRestrictionValidator extends AbstractUseRestrictionValidatorAPI{

    private Client client;
    private String validateUrl;

    public static void initInstance(Client client, ServicesConfiguration config) {
        AbstractUseRestrictionValidatorAPI.UseRestrictionValidatorAPIHolder.setInstance(new UseRestrictionValidator(client, config));
    }

    private UseRestrictionValidator(Client client, ServicesConfiguration config){
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

    @Override
    public void setClient(Client client) {
        this.client = client;
    }

}