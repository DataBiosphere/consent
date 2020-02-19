package org.broadinstitute.consent.http.service.users;

import org.broadinstitute.consent.http.db.DACUserDAO;
import org.broadinstitute.consent.http.db.UserRoleDAO;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.DACUser;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.service.users.handler.UserHandlerAPI;
import org.jdbi.v3.core.statement.UnableToExecuteStatementException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;

import javax.ws.rs.NotFoundException;
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
    UserHandlerAPI userHandlerAPI;

    private final String EMAIL = "test@gmail.com";

    private final String DISPLAY_NAME = "test";


    @Before
    public void setUp() throws URISyntaxException {
        MockitoAnnotations.initMocks(this);
        databaseDACUserAPI = new DatabaseDACUserAPI(dacUserDAO, userRoleDAO, userHandlerAPI, null);
    }

    @Test
    public void createDACUser() {
        DACUser user = new DACUser(null, EMAIL, DISPLAY_NAME, new Date(), null);
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
        DACUser user = new DACUser(null, EMAIL, DISPLAY_NAME, new Date(), null);
        when(dacUserDAO.insertDACUser(anyString(), anyString(), any(Date.class))).thenThrow(UnableToExecuteStatementException.class);
        try {
            databaseDACUserAPI.createDACUser(user);
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().equals("Email should be unique."));
        }
    }

    @Test
    public void createDACUserWithoutDisplayName() {
        DACUser user = new DACUser(null, EMAIL, null, new Date(), null);
        try {
            databaseDACUserAPI.createDACUser(user);
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().equals("Display Name can't be null. The user needs a name to display."));
        }
    }

    @Test
    public void createDACUserWithoutEmail() {
        DACUser user = new DACUser(null, null, DISPLAY_NAME, new Date(), null);
        try {
            databaseDACUserAPI.createDACUser(user);
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().equals("The user needs a valid email to be able to login."));
        }
    }

    @Test
    public void describeUserByNonExistentEmail() {
        when(dacUserDAO.findDACUserByEmail(EMAIL)).thenReturn(null);
        try {
            databaseDACUserAPI.describeDACUserByEmail(EMAIL);
        } catch (NotFoundException e) {
            assertTrue(e.getMessage().equals("Could not find dacUser for specified email : " + EMAIL));
        }
    }

    @Test
    public void describeUserByEmail() {
        DACUser dacUser = new DACUser(1, EMAIL, DISPLAY_NAME, new Date(), null);
        when(dacUserDAO.findDACUserByEmail(EMAIL)).thenReturn(dacUser);
        DACUser user = databaseDACUserAPI.describeDACUserByEmail(EMAIL);
        assertNotNull(user);
    }

    @Test
    public void describeUserByNonExistentId() {
        int id = 1;
        when(dacUserDAO.findDACUserById(id)).thenReturn(null);
        try {
            databaseDACUserAPI.describeDACUserById(id);
        } catch (NotFoundException e) {
            assertTrue(e.getMessage().equals("Could not find dacUser for specified id : " + id));
        }
    }

    @Test
    public void describeUserById() {
        DACUser dacUser = new DACUser(1, EMAIL, DISPLAY_NAME, new Date(), null);
        when(dacUserDAO.findDACUserById(1)).thenReturn(dacUser);
        DACUser user = databaseDACUserAPI.describeDACUserById(1);
        assertNotNull(user);
    }

}
