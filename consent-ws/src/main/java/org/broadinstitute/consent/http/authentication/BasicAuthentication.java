package org.broadinstitute.consent.http.authentication;

import java.util.Base64;
import org.broadinstitute.consent.http.configurations.UserConfiguration;
import java.nio.charset.StandardCharsets;

public class BasicAuthentication implements BasicAuthenticationAPI{

    UserConfiguration userConfiguration;

    public BasicAuthentication(UserConfiguration userConfiguration){
        this.userConfiguration = userConfiguration;
    }

    @Override
    public void validateUser(String authHeader) throws Exception{
        String credential = authHeader.substring(6);
        String[] userPassword = decodeCredential(credential);
        String password = Base64.getEncoder().encodeToString(userPassword[1].getBytes(StandardCharsets.UTF_8));
        if(!(userPassword[0].equals(userConfiguration.getUser()) && password.equals(userConfiguration.getPassword()))){
            unauthorized(credential);
        }

    }

    private static String[] decodeCredential(String encodedCredential) {
        final byte[] decodedBytes = Base64.getDecoder().decode(encodedCredential.getBytes());
        final String pair = new String(decodedBytes);
        final String[] userDetails = pair.split(":", 2);
        return userDetails;
    }

    private void unauthorized(String credential) throws Exception {
        throw new Exception("Provided user credential is either null or empty or does not have permissions to access this resource." + credential);
    }

}
