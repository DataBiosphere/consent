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
    ElectionDAO electionDAO;

    @Mock
    VoteDAO voteDAO;

    @Mock
    DataSetAssociationDAO dataSetAssociationDAO;

    @Mock
    UserHandlerAPI userHandlerAPI;

    private UserAPI userAPI;

    private final String DISPLAY_NAME = "test";

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        userAPI = new DatabaseUserAPI(dacUserDAO, userRoleDAO, electionDAO, voteDAO, dataSetAssociationDAO, userHandlerAPI, null);
    }


    @Test(expected = BadRequestException.class)
    public void testCreateUserWithInvalidEmail() {
        DACUser dacUser = new DACUser(1, "", DISPLAY_NAME, new Date());
        UserRole researcher = new UserRole(UserRoles.RESEARCHER.getRoleId(), UserRoles.RESEARCHER.getRoleName());
        dacUser.setRoles(Collections.singletonList(researcher));
        userAPI.createUser(dacUser);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateUserWithInvalidRoles() {
        DACUser dacUser = new DACUser(1, "test@gmail.com", DISPLAY_NAME, new Date());
        UserRole roleResearcher = new UserRole(1, UserRoles.RESEARCHER.getRoleName());
        UserRole roleMember = new UserRole(1, UserRoles.MEMBER.getRoleName());
        List<UserRole> roles = new ArrayList<>(Arrays.asList(roleResearcher, roleMember));
        dacUser.setRoles(roles);
        userAPI.createUser(dacUser);
    }

}
