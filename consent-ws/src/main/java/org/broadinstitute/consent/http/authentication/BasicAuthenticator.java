package org.broadinstitute.consent.http.authentication;

import com.google.common.base.Optional;
import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.auth.Authenticator;
import io.dropwizard.auth.basic.BasicCredentials;
import org.broadinstitute.consent.http.configurations.BasicAuthConfig;
import org.broadinstitute.consent.http.models.User;


public class BasicAuthenticator implements Authenticator<BasicCredentials, User>  {

    BasicAuthConfig basicAuthentication;

    public BasicAuthenticator(BasicAuthConfig basicAuthentication) {
        this.basicAuthentication = basicAuthentication;
    }

    @Override
    public Optional<User> authenticate(BasicCredentials credentials) throws AuthenticationException {
        if (basicAuthentication.getUser().equals(credentials.getUsername()) &&
                basicAuthentication.getPassword().equals(credentials.getPassword())) {
            return Optional.of(new User(credentials.getUsername()));
        }
        if (basicAuthentication.getUser().equals(credentials.getUsername())) {
            throw new AuthenticationException("Provided user credential is either null or empty or does not have permissions to access this resource.");
        }
        return Optional.absent();
    }

}
