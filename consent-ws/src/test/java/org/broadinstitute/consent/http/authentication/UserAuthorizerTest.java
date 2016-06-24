package org.broadinstitute.consent.http.authentication;

import org.broadinstitute.consent.http.db.DACUserRoleDAO;
import org.broadinstitute.consent.http.models.DACUserRole;
import org.broadinstitute.consent.http.models.User;
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
    DACUserRoleDAO dacUserRoleDAO;
    @Mock
    User authorizedUser;
    @Mock
    User unauthorizedUser;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        when(authorizedUser.getName()).thenReturn("Authorized User");
        when(unauthorizedUser.getName()).thenReturn("Unauthorized User");
        when(dacUserRoleDAO.findRolesByUserEmail("Authorized User")).thenReturn(new ArrayList<>(Arrays.asList(getChairpersonRole())));
        when(dacUserRoleDAO.findRolesByUserEmail("Unauthorized User")).thenReturn(new ArrayList<>(Arrays.asList(getChairpersonRole())));
        authorizer = new UserAuthorizer(dacUserRoleDAO);
    }

    @Test
    public void testAuthorizeNotAuthorized() throws Exception {
        assertFalse(authorizer.authorize(unauthorizedUser, "MEMBER"));
    }

    @Test
    public void testAuthorizeAuthorized() throws Exception {
        assertTrue(authorizer.authorize(authorizedUser, "CHAIRPERSON"));
    }

    /* Helper Methods */

    private DACUserRole getChairpersonRole(){
        return new DACUserRole(1, "CHAIRPERSON", false);
    }

}