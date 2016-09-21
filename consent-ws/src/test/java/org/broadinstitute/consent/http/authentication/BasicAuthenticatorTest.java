package org.broadinstitute.consent.http.authentication;

import io.dropwizard.auth.basic.BasicCredentials;
import org.broadinstitute.consent.http.configurations.BasicAuthConfig;
import org.broadinstitute.consent.http.models.User;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

public class BasicAuthenticatorTest {

    @Mock
    BasicAuthConfig basicAuths;

    private BasicAuthenticator authenticator;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        authenticator = new BasicAuthenticator(basicAuths);
        List<BasicUser> users = new ArrayList<>();
        BasicUser user = new BasicUser();
        user.setPassword("testPassword");
        user.setUser("testUser");
        users.add(user);
        when(basicAuths.getUsers()).thenReturn(users);
    }

    @Test
    public void testAuthenticateSuccessful() throws Exception {
        BasicCredentials credentials = new BasicCredentials("testUser", "testPassword");
        Optional<User> user = authenticator.authenticate(credentials);
        assertTrue(user.get().getName().equals(basicAuths.getUsers().get(0).getUser()));
    }

    @Test
    public void testAuthenticateBadPassword() throws Exception {
        BasicCredentials credentials = new BasicCredentials("testUser", "wrongPassword");
        Optional<User> user = authenticator.authenticate(credentials);
        assertTrue(user.isPresent() == false);
    }

    @Test
    public void testAuthenticateBadUser() throws Exception {
        BasicCredentials credentials = new BasicCredentials("wrongUser", "testPassword");
        Optional<User> user = authenticator.authenticate(credentials);
        assertTrue(user.isPresent() == false);
    }

    @Test
    public void testAuthenticateBadUserAndPassword() throws Exception {
        BasicCredentials credentials = new BasicCredentials("wrongUser", "wrongPassword");
        Optional<User> user = authenticator.authenticate(credentials);
        assertTrue(user.isPresent() == false);
    }


}