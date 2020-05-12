package org.broadinstitute.consent.http.authentication;

import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.auth.Authenticator;
import org.broadinstitute.consent.http.models.AuthUser;

import java.util.List;
import java.util.Optional;

public class TestOAuthAuthenticator implements Authenticator<String, AuthUser> {

    private List<String> acceptableCredentials;

    public TestOAuthAuthenticator(List<String> acceptableCredentials) {
        this.acceptableCredentials = acceptableCredentials;
    }

    @Override
    public Optional<AuthUser> authenticate(String credentials) throws AuthenticationException {
        if (acceptableCredentials.contains(credentials)) {
            GoogleUser googleUser = new GoogleUser();
            googleUser.setEmail(credentials);
            googleUser.setName(credentials);
            AuthUser user = new AuthUser(googleUser);
            return Optional.of(user);
        }
        return Optional.empty();
    }

}
