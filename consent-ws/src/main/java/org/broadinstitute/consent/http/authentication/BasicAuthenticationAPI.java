package org.broadinstitute.consent.http.authentication;


public interface BasicAuthenticationAPI {

    public void validateUser(String authHeader) throws Exception;

}
