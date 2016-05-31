package org.broadinstitute.consent.http.authentication;

import java.util.Base64;
import java.nio.charset.StandardCharsets;
import org.broadinstitute.consent.http.configurations.BasicAuthConfig;

public class BasicAuthentication implements BasicAuthenticationAPI {

    private BasicAuthConfig basicAuthentication;

    public BasicAuthentication(BasicAuthConfig basicAuthentication) {
        this.basicAuthentication = basicAuthentication;
    }

    @Override
    public void validateUser(String authHeader) throws Exception{
        String credential = authHeader.substring(6);
        String[] userPassword = decodeCredential(credential);
        if(!(userPassword[0].equals(basicAuthentication.getUser()) && userPassword[1].equals(basicAuthentication.getPassword()))) {
            unauthorized(credential);
        }

    }

    private static String[] decodeCredential(String encodedCredential) {
        final byte[] decodedBytes = Base64.getDecoder().decode(encodedCredential.getBytes());
        final String pair = new String(decodedBytes);
        return pair.split(":", 2);
    }

    private void unauthorized(String credential) throws Exception {
        throw new Exception("Provided user credential is either null or empty or does not have permissions to access this resource." + credential);
    }
}
