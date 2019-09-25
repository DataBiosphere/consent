package org.broadinstitute.consent.http.service.users;

import org.broadinstitute.consent.http.db.DACUserDAO;
import org.broadinstitute.consent.http.db.DataSetAssociationDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.db.UserRoleDAO;
import org.broadinstitute.consent.http.db.VoteDAO;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.DACUser;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.service.users.handler.UserHandlerAPI;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.ws.rs.NotAuthorizedException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertTrue;

public class DatabaseUserAPITest {

    @Mock
    DACUserDAO dacUserDAO;

    @Mock
    UserRoleDAO userRoleDAO;

    @Mock
    ElectionDAO electionDAO;

    @Mock
    VoteDAO voteDAO;

    @Mock
    DataSetAssociationDAO dataSetAssociationDAO;

    @Mock
    UserHandlerAPI userHandlerAPI;

    private UserAPI userAPI;

    private final String EMAIL = "test@gmail.com";

    private final String INVALID_EMAIL = "invalid_email@gmail.com";

    private final String DISPLAY_NAME = "test";

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        userAPI = new DatabaseUserAPI(dacUserDAO, userRoleDAO, electionDAO, voteDAO, dataSetAssociationDAO, userHandlerAPI, null);
    }


    @Test
    public void testCreateUserWithInvalidEmail() {
        DACUser dacUser = new DACUser(1, EMAIL, DISPLAY_NAME, new Date());
        try {
            userAPI.createUser(dacUser, INVALID_EMAIL);
        } catch (NotAuthorizedException e) {
            assertTrue(e.getMessage().equalsIgnoreCase("You don't have permission to update the specified user."));
        }
    }

    @Test
    public void testCreateUserWithInvalidRoles() {
        DACUser dacUser = new DACUser(1, EMAIL, DISPLAY_NAME, new Date());
        UserRole roleResearcher = new UserRole(1, UserRoles.RESEARCHER.getRoleName());
        UserRole roleMember = new UserRole(1, UserRoles.MEMBER.getRoleName());
        List<UserRole> roles = new ArrayList<>(Arrays.asList(roleResearcher, roleMember));
        dacUser.setRoles(roles);
        try {
            userAPI.createUser(dacUser, EMAIL);
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().equalsIgnoreCase("Invalid role: " + UserRoles.MEMBER.getRoleName() + ". Valid roles are: " + UserRoles.DATAOWNER.getRoleName() + " and " + UserRoles.RESEARCHER.getRoleName()));
        }
    }

}
