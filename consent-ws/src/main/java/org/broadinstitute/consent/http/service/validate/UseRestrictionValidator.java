package org.broadinstitute.consent.http.service.validate;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import org.broadinstitute.consent.http.configurations.ServicesConfiguration;
import org.broadinstitute.consent.http.models.validate.ValidateResponse;
import org.broadinstitute.consent.http.service.AbstractConsentAPI;
import org.broadinstitute.consent.http.service.AbstractDataAccessRequestAPI;
import org.broadinstitute.consent.http.service.AbstractDataSetAPI;
import org.broadinstitute.consent.http.service.ConsentAPI;
import org.broadinstitute.consent.http.service.DataAccessRequestAPI;
import org.broadinstitute.consent.http.service.DataSetAPI;
import org.glassfish.jersey.client.ClientProperties;

public class UseRestrictionValidator extends AbstractUseRestrictionValidatorAPI{

    private Client client;
    private ConsentAPI consentAPI;
    private DataAccessRequestAPI dataAccessAPI;
    private DataSetAPI dsAPI;
    private String validateUrl;

    public static void initInstance(Client client, ServicesConfiguration config) {
        AbstractUseRestrictionValidatorAPI.UseRestrictionValidatorAPIHolder.setInstance(new UseRestrictionValidator(client, config));
    }

    private UseRestrictionValidator(Client client, ServicesConfiguration config){
        this.client = ClientBuilder.newClient();
        this.dataAccessAPI = AbstractDataAccessRequestAPI.getInstance();
        this.consentAPI = AbstractConsentAPI.getInstance();
        this.dsAPI = AbstractDataSetAPI.getInstance();
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
        }else{
            throw new IllegalArgumentException("There was an error posting the use restriction" + (res.readEntity(ValidateResponse.class)).getErrors().toString());
        }
    }

    @Override
    public void setClient(Client client) {
        this.client = client;
    }
}