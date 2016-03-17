package org.broadinstitute.consent.http.authentication;


public interface GoogleAuthenticationAPI {

    void validateAccessToken(String authHeader) throws Exception;


}