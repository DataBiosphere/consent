package org.broadinstitute.consent.http.authentication;

import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.auth.Authenticator;
import org.broadinstitute.consent.http.models.User;

import java.util.Optional;

public class DefaultAuthenticator implements Authenticator<String, User> {

    @Override
    public Optional<User> authenticate(String s) throws AuthenticationException {
        return Optional.of(new User(s));
    }
}
