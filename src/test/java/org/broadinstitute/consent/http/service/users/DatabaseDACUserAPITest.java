package org.broadinstitute.consent.http.service.users;

import org.broadinstitute.consent.http.db.DACUserDAO;
import org.broadinstitute.consent.http.db.UserRoleDAO;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.service.UserService;
import org.broadinstitute.consent.http.service.users.handler.UserRolesHandler;
import org.jdbi.v3.core.statement.UnableToExecuteStatementException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;


@RunWith(MockitoJUnitRunner.class)
public class DatabaseDACUserAPITest {

    private DatabaseDACUserAPI databaseDACUserAPI;

    @Mock
    DACUserDAO dacUserDAO;

    @Mock
    UserRoleDAO userRoleDAO;

    @Mock
    UserRolesHandler userRolesHandler;

    @Mock
    UserService userService;

    private final String EMAIL = "test@gmail.com";

    private final String DISPLAY_NAME = "test";


    @Before
    public void setUp() throws URISyntaxException {
        MockitoAnnotations.initMocks(this);
        databaseDACUserAPI = new DatabaseDACUserAPI(dacUserDAO, userRoleDAO, userRolesHandler, userService);
    }

    @Test
    public void createDACUser() {
        User user = new User(null, EMAIL, DISPLAY_NAME, new Date(), null);
        when(dacUserDAO.insertDACUser(anyString(), anyString(), any(Date.class))).thenReturn(3);
        user.setDacUserId(3);
        UserRole role = new UserRole(1, UserRoles.RESEARCHER.getRoleName());
        List<UserRole> roles = new ArrayList<>(Arrays.asList(role));
        user.setRoles(roles);
        when(dacUserDAO.findDACUserById(3)).thenReturn(user);
        when(userRoleDAO.findRoleIdByName(UserRoles.RESEARCHER.getRoleName())).thenReturn(1);
        when(userRoleDAO.findRolesByUserId(3)).thenReturn(roles);
        user = databaseDACUserAPI.createDACUser(user);
        assertTrue(user != null);
        assertTrue(user.getDisplayName() != null);
    }

    @Test
    public void createDACUserWithExistentEmail() {
        User user = new User(null, EMAIL, DISPLAY_NAME, new Date(), null);
        when(dacUserDAO.insertDACUser(anyString(), anyString(), any(Date.class))).thenThrow(UnableToExecuteStatementException.class);
        try {
            databaseDACUserAPI.createDACUser(user);
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().equals("Email should be unique."));
        }
    }

    @Test
    public void createDACUserWithoutDisplayName() {
        User user = new User(null, EMAIL, null, new Date(), null);
        try {
            databaseDACUserAPI.createDACUser(user);
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().equals("Display Name can't be null. The user needs a name to display."));
        }
    }

    @Test
    public void createDACUserWithoutEmail() {
        User user = new User(null, null, DISPLAY_NAME, new Date(), null);
        try {
            databaseDACUserAPI.createDACUser(user);
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().equals("The user needs a valid email to be able to login."));
        }
    }

}
