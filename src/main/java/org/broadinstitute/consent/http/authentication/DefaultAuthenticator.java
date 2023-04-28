package org.broadinstitute.consent.http.authentication;

import io.dropwizard.auth.Authenticator;
import java.util.Optional;
import org.broadinstitute.consent.http.models.AuthUser;

public class DefaultAuthenticator implements Authenticator<String, AuthUser> {

    @Override
    public Optional<AuthUser> authenticate(String s) {
        return Optional.of(new AuthUser(s));
    }
}
