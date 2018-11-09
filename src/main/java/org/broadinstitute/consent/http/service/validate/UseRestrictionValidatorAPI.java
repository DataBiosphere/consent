package org.broadinstitute.consent.http.service.validate;


import javax.ws.rs.client.Client;

public interface UseRestrictionValidatorAPI {

    void validateUseRestriction(String useRestriction) throws IllegalArgumentException;

    void validateConsentUseRestriction();

    void validateDARUseRestriction();

    void setClient(Client client);
}
