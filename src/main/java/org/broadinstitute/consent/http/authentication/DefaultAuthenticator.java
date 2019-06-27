package org.broadinstitute.consent.http.authentication;

import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.auth.Authenticator;
import org.broadinstitute.consent.http.models.AuthUser;

import java.util.Optional;

public class DefaultAuthenticator implements Authenticator<String, AuthUser> {

    @Override
    public Optional<AuthUser> authenticate(String s) throws AuthenticationException {
        return Optional.of(new AuthUser(s));
    }
}
