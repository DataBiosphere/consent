package org.broadinstitute.consent.http.authentication;

import com.google.common.base.Optional;
import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.auth.Authenticator;
import io.dropwizard.auth.basic.BasicCredentials;
import org.broadinstitute.consent.http.configurations.BasicAuthConfig;
import org.broadinstitute.consent.http.models.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.stream.Collectors;


public class BasicAuthenticator implements Authenticator<BasicCredentials, User>  {

    BasicAuthConfig basicAuthentication;

    private static final Logger logger = LoggerFactory.getLogger(BasicAuthenticator.class);

    public BasicAuthenticator(BasicAuthConfig basicAuthentication) {
        this.basicAuthentication = basicAuthentication;
    }

    @Override
    public Optional<User> authenticate(BasicCredentials credentials) throws AuthenticationException {
        Map<String, String> users = basicAuthentication.getUsers().stream().collect(
                Collectors.toMap(BasicUser::getUser, BasicUser::getPassword));

        if (users.containsKey(credentials.getUsername()) &&
                users.get(credentials.getUsername()).equals(credentials.getPassword())) {
            return Optional.of(new User(credentials.getUsername()));
        }
        return Optional.absent();
    }

}
