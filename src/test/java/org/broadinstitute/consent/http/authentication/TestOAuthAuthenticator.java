package org.broadinstitute.consent.http.authentication;

import io.dropwizard.auth.Authenticator;
import org.broadinstitute.consent.http.models.AuthUser;

import java.util.List;
import java.util.Optional;

public class TestOAuthAuthenticator implements Authenticator<String, AuthUser> {

    private final List<String> acceptableCredentials;

    public TestOAuthAuthenticator(List<String> acceptableCredentials) {
        this.acceptableCredentials = acceptableCredentials;
    }

    @Override
    public Optional<AuthUser> authenticate(String credentials) {
        if (acceptableCredentials.contains(credentials)) {
            GenericUser genericUser = new GenericUser();
            genericUser.setEmail(credentials);
            genericUser.setName(credentials);
            AuthUser user = new AuthUser(genericUser);
            return Optional.of(user);
        }
        return Optional.empty();
    }

}
