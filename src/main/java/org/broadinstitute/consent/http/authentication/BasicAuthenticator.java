package org.broadinstitute.consent.http.authentication;

import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.auth.Authenticator;
import io.dropwizard.auth.basic.BasicCredentials;
import org.broadinstitute.consent.http.configurations.BasicAuthConfig;
import org.broadinstitute.consent.http.models.AuthUser;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;


public class BasicAuthenticator implements Authenticator<BasicCredentials, AuthUser>  {

    BasicAuthConfig basicAuthentication;

    public BasicAuthenticator(BasicAuthConfig basicAuthentication) {
        this.basicAuthentication = basicAuthentication;
    }

    @Override
    public Optional<AuthUser> authenticate(BasicCredentials credentials) throws AuthenticationException {
        Map<String, String> users = basicAuthentication.getUsers().stream().collect(
                Collectors.toMap(BasicUser::getUser, BasicUser::getPassword));

        if (users.containsKey(credentials.getUsername()) &&
                users.get(credentials.getUsername()).equals(credentials.getPassword())) {
            return Optional.of(new AuthUser(credentials.getUsername()));
        }
        return Optional.empty();
    }

}
