package org.broadinstitute.consent.http.service.users;

import org.broadinstitute.consent.http.db.DACUserDAO;
import org.broadinstitute.consent.http.db.UserRoleDAO;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.service.UserService;
import org.broadinstitute.consent.http.service.users.handler.UserRolesHandler;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.ws.rs.BadRequestException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class DatabaseUserAPITest {

    @Mock
    DACUserDAO dacUserDAO;

    @Mock
    UserRoleDAO userRoleDAO;

    @Mock
    UserRolesHandler userRolesHandler;

    @Mock
    UserService userService;

    private UserAPI userAPI;

    private final String DISPLAY_NAME = "test";

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        userAPI = new DatabaseUserAPI(dacUserDAO, userRoleDAO, userRolesHandler, userService);
    }


    @Test(expected = BadRequestException.class)
    public void testCreateUserWithInvalidEmail() {
        User user = new User(1, "", DISPLAY_NAME, new Date());
        UserRole researcher = new UserRole(UserRoles.RESEARCHER.getRoleId(), UserRoles.RESEARCHER.getRoleName());
        user.setRoles(Collections.singletonList(researcher));
        userAPI.createUser(user);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateUserWithInvalidRoles() {
        User user = new User(1, "test@gmail.com", DISPLAY_NAME, new Date());
        UserRole roleResearcher = new UserRole(1, UserRoles.RESEARCHER.getRoleName());
        UserRole roleMember = new UserRole(1, UserRoles.MEMBER.getRoleName());
        List<UserRole> roles = new ArrayList<>(Arrays.asList(roleResearcher, roleMember));
        user.setRoles(roles);
        userAPI.createUser(user);
    }

}
