package org.broadinstitute.consent.http.authentication;

import org.broadinstitute.consent.http.db.UserRoleDAO;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.resources.Resource;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

public class UserAuthorizerTest {

    UserAuthorizer authorizer;
    @Mock
    UserRoleDAO userRoleDAO;
    @Mock
    AuthUser authorizedUser;
    @Mock
    AuthUser unauthorizedUser;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        when(authorizedUser.getName()).thenReturn("Authorized User");
        when(unauthorizedUser.getName()).thenReturn("Unauthorized User");
        when(userRoleDAO.findRolesByUserEmail("Authorized User")).thenReturn(new ArrayList<>(Arrays.asList(getChairpersonRole())));
        when(userRoleDAO.findRolesByUserEmail("Unauthorized User")).thenReturn(new ArrayList<>(Arrays.asList(getChairpersonRole())));
        authorizer = new UserAuthorizer(userRoleDAO);
    }

    @Test
    public void testAuthorizeNotAuthorized() throws Exception {
        assertFalse(authorizer.authorize(unauthorizedUser, Resource.MEMBER));
    }

    @Test
    public void testAuthorizeAuthorized() throws Exception {
        assertTrue(authorizer.authorize(authorizedUser, Resource.CHAIRPERSON));
    }

    /* Helper Methods */

    private UserRole getChairpersonRole(){
        return new UserRole(1, "CHAIRPERSON", false);
    }

}