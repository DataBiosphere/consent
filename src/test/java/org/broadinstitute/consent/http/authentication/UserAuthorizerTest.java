package org.broadinstitute.consent.http.authentication;

import org.broadinstitute.consent.http.db.UserRoleDAO;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.resources.Resource;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Collections;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

public class UserAuthorizerTest {

    private UserAuthorizer authorizer;
    @Mock
    UserRoleDAO userRoleDAO;
    @Mock
    AuthUser authorizedUser;
    @Mock
    AuthUser unauthorizedUser;

    @Before
    public void setUp() {
        openMocks(this);
        when(authorizedUser.getName()).thenReturn("Authorized User");
        when(unauthorizedUser.getName()).thenReturn("Unauthorized User");
        when(userRoleDAO.findRoleNamesByUserEmail("Authorized User")).thenReturn(Collections.singletonList(getChairpersonRole().getName()));
        when(userRoleDAO.findRoleNamesByUserEmail("Unauthorized User")).thenReturn(Collections.singletonList(getChairpersonRole().getName()));
        authorizer = new UserAuthorizer(userRoleDAO);
    }

    @Test
    public void testAuthorizeNotAuthorized() {
        assertFalse(authorizer.authorize(unauthorizedUser, Resource.MEMBER));
    }

    @Test
    public void testAuthorizeAuthorized() {
        assertTrue(authorizer.authorize(authorizedUser, Resource.CHAIRPERSON));
    }

    /* Helper Methods */

    private UserRole getChairpersonRole(){
        return new UserRole(1, "CHAIRPERSON");
    }

}