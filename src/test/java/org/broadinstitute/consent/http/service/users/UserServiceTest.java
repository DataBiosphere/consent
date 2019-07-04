package org.broadinstitute.consent.http.service.users;

import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.db.DataSetAssociationDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.db.ResearcherPropertyDAO;
import org.broadinstitute.consent.http.db.UserRoleDAO;
import org.broadinstitute.consent.http.db.VoteDAO;
import org.broadinstitute.consent.http.db.mongo.MongoConsentDB;
import org.broadinstitute.consent.http.enumeration.Actions;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.enumeration.VoteType;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.models.dto.PatchOperation;
import org.broadinstitute.consent.http.service.users.handler.UserRoleHandlerException;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.ws.rs.NotAuthorizedException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyList;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class UserServiceTest {

    @Mock
    UserDAO userDAO;

    @Mock
    UserRoleDAO userRoleDAO;

    @Mock
    ElectionDAO electionDAO;

    @Mock
    VoteDAO voteDAO;

    @Mock
    DataSetAssociationDAO dataSetAssociationDAO;

    @Mock
    ResearcherPropertyDAO researcherPropertyDAO;

    @Mock
    MongoConsentDB mongoDB;

    private UserService userService;

    private final String EMAIL = "test@gmail.com";

    private final String INVALID_EMAIL = "invalid_email@gmail.com";

    private final String DISPLAY_NAME = "test";

    private final String DISPLAY_NAME_FIELD = "displayName";

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        userService = new UserService(userDAO, userRoleDAO,  electionDAO, voteDAO, dataSetAssociationDAO, researcherPropertyDAO, mongoDB);
    }


    @Test
    public void testCreateUserWithInvalidEmail() {
        User user = new User(1, EMAIL, DISPLAY_NAME, new Date());
        try {
            userService.createUser(user, INVALID_EMAIL);
        } catch (NotAuthorizedException e) {
            assertTrue(e.getMessage().equalsIgnoreCase("You don't have permission to update the specified user."));
        }
    }

    @Test
    public void testCreateUserWithInvalidRoles() {
        User user = new User(1, EMAIL, DISPLAY_NAME, new Date());
        UserRole roleResearcher = new UserRole(1, UserRoles.RESEARCHER.getValue());
        UserRole roleMember = new UserRole(1, UserRoles.MEMBER.getValue());
        List<UserRole> roles = new ArrayList<>(Arrays.asList(roleResearcher, roleMember));
        user.setRoles(roles);
        try {
            userService.createUser(user, EMAIL);
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().equalsIgnoreCase("Invalid role: " + UserRoles.MEMBER.getValue() + ". Valid roles are: " + UserRoles.DATAOWNER.getValue() + " and " + UserRoles.RESEARCHER.getValue()));
        }
    }

    @Test
    public void testUpdateUserWithInvalidEmail() throws UserRoleHandlerException {
        User user = new User(1, EMAIL, DISPLAY_NAME, new Date());
        try {
            userService.updateUser(user, INVALID_EMAIL);
        } catch (NotAuthorizedException e) {
            assertTrue(e.getMessage().equalsIgnoreCase("You don't have permission to update the specified user."));
        }
    }

    @Test
    public void testUpdateUserWithInvalidRoles() throws UserRoleHandlerException {
        User user = new User(1, EMAIL, DISPLAY_NAME, new Date());
        UserRole roleResearcher = new UserRole(1, UserRoles.RESEARCHER.getValue());
        UserRole roleMember = new UserRole(1, UserRoles.MEMBER.getValue());
        List<UserRole> roles = new ArrayList<>(Arrays.asList(roleResearcher, roleMember));
        user.setRoles(roles);
        try {
            userService.updateUser(user, EMAIL);
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().equalsIgnoreCase("Invalid role: " + UserRoles.MEMBER.getValue() + ". Valid roles are: " + UserRoles.DATAOWNER.getValue() + " and " + UserRoles.RESEARCHER.getValue()));
        }
    }


    @Test
    public void testUpdateUserRemoveDataOwnerWithDataSetAssociations() {
        User user = new User(1, EMAIL, DISPLAY_NAME, new Date());
        UserRole roleResearcher = new UserRole(1, UserRoles.RESEARCHER.getValue());
        List<UserRole> roles = Collections.singletonList(roleResearcher);
        user.setRoles(roles);
        User existentUser = new User(1, EMAIL, DISPLAY_NAME, new Date());
        UserRole dataOwnerRole = new UserRole(1, UserRoles.DATAOWNER.getValue());
        existentUser.setRoles(Collections.singletonList(dataOwnerRole));
        when(dataSetAssociationDAO.getDataSetsIdOfDataOwnerNeedsApproval(anyInt())).thenReturn(Collections.singletonList(1));
        when(dataSetAssociationDAO.getCountOfDataOwnersPerDataSet(anyList())).thenReturn(Collections.singletonList(1));
        when(userDAO.findDACUserById(1)).thenReturn(existentUser);
        when(userDAO.findDACUserByEmail(EMAIL)).thenReturn(existentUser);
        when(userRoleDAO.findRolesByUserId(user.getUserId())).thenReturn(Collections.singletonList(dataOwnerRole));
        try {
            userService.updateUser(user, EMAIL);
        } catch (UserRoleHandlerException e) {
            assertTrue(e.getMessage().equalsIgnoreCase("Role can not be removed. The specified user is the only data owner associated to a data set that needs approval."));
        }
    }


    @Test
    public void testUpdateUserRemoveDataOwnerWithOpenElections() {
        User user = new User(1, EMAIL, DISPLAY_NAME, new Date());
        UserRole roleResearcher = new UserRole(1, UserRoles.RESEARCHER.getValue());
        List<UserRole> roles = new ArrayList<>(Collections.singletonList(roleResearcher));
        user.setRoles(roles);
        User existentUser = new User(1, EMAIL, DISPLAY_NAME, new Date());
        UserRole dataOwnerRole = new UserRole(1, UserRoles.DATAOWNER.getValue());
        existentUser.setRoles(new ArrayList<>(Collections.singletonList(dataOwnerRole)));
        when(dataSetAssociationDAO.getDataSetsIdOfDataOwnerNeedsApproval(anyInt())).thenReturn(Collections.singletonList(1));
        when(dataSetAssociationDAO.getCountOfDataOwnersPerDataSet(anyList())).thenReturn(Collections.singletonList(6));
        when(electionDAO.findDataSetOpenElectionIds(any())).thenReturn(Collections.singletonList(2));
        when(voteDAO.findVoteCountForElections(Collections.singletonList(2), VoteType.DATA_OWNER.getValue())).thenReturn(Collections.singletonList(1));
        when(userDAO.findDACUserById(1)).thenReturn(existentUser);
        when(userDAO.findDACUserByEmail(EMAIL)).thenReturn(existentUser);
        when(userRoleDAO.findRolesByUserId(user.getUserId())).thenReturn(Collections.singletonList(dataOwnerRole));
        try {
            userService.updateUser(user, EMAIL);
        } catch (UserRoleHandlerException e) {
            assertTrue(e.getMessage().equalsIgnoreCase("Role can not be removed. There are open dataset elections for this user."));
        }
    }

    @Test
    @Ignore // TODO: Fix this with new test pattern.
    public void testUpdateUserAddDataOwnerRole() throws UserRoleHandlerException {
        User user = new User(1, EMAIL, DISPLAY_NAME, new Date());
        UserRole roleResearcher = new UserRole(1, UserRoles.DATAOWNER.getValue());
        List<UserRole> roles = new ArrayList<>(Collections.singletonList(roleResearcher));
        user.setRoles(roles);
        User existentUser = new User(1, EMAIL, DISPLAY_NAME, new Date());
        UserRole researcherRole = new UserRole(1, UserRoles.RESEARCHER.getValue());
        existentUser.setRoles(new ArrayList<>(Collections.singletonList(researcherRole)));
        when(userDAO.findDACUserByEmail(EMAIL)).thenReturn(existentUser);
        when(userDAO.findDACUserById(1)).thenReturn(existentUser);
        userService.updateUser(user, EMAIL);
        verify(userRoleDAO, times(1)).insertSingleUserRole(anyInt(), anyInt(), anyBoolean());
        verify(userDAO, times(1)).updateDACUser(DISPLAY_NAME, 1);
    }

    @Test
    public void testUpdateDACUserSuccess() throws UserRoleHandlerException {
        User existentUser = new User(1, EMAIL, DISPLAY_NAME, new Date());
        UserRole roleResearcher = new UserRole(1, UserRoles.RESEARCHER.getValue());
        List<UserRole> roles = new ArrayList<>(Collections.singletonList(roleResearcher));
        existentUser.setRoles(roles);
        User userToUpdate =  new User(1, EMAIL, DISPLAY_NAME, new Date());
        UserRole roleDO =  new UserRole(2, UserRoles.DATAOWNER.getValue());
        List<UserRole> newRoles = new ArrayList<>(Arrays.asList(roleResearcher, roleDO));
        userToUpdate.setRoles(newRoles);
        when(userDAO.findDACUserByEmail(EMAIL)).thenReturn(existentUser);
        when(userDAO.findDACUserById(1)).thenReturn(userToUpdate);
        userService.updateUser(userToUpdate, EMAIL);
        assertNotNull(userToUpdate);
        assertEquals(2, userToUpdate.getRoles().size());

    }

    @Test
    public void testUpdateUserAddResearcherWithIncompatiblesRoles() {
        User user = new User(1, EMAIL, DISPLAY_NAME, new Date());
        UserRole roleResearcher = new UserRole(1, UserRoles.RESEARCHER.getValue());
        List<UserRole> roles = new ArrayList<>(Collections.singletonList(roleResearcher));
        user.setRoles(roles);
        User existentUser = new User(1, EMAIL, DISPLAY_NAME, new Date());
        UserRole dataOwnerRole = new UserRole(1, UserRoles.MEMBER.getValue());
        existentUser.setRoles(new ArrayList<>(Collections.singletonList(dataOwnerRole)));

        when(userDAO.findDACUserById(1)).thenReturn(existentUser);
        when(userDAO.findDACUserByEmail(EMAIL)).thenReturn(existentUser);
        when(userRoleDAO.findRolesByUserId(user.getUserId())).thenReturn(Collections.singletonList(dataOwnerRole));
        try {
            userService.updateUser(user, EMAIL);
        } catch (UserRoleHandlerException e) {
            assertTrue(e.getMessage().equalsIgnoreCase("Role can not be added. Researcher  is incompatible with  Member and Chairperson role."));
        }
    }

    @Test
    public void testUpdateUserAddResearcherRole() throws UserRoleHandlerException {
        User user = new User(1, EMAIL, DISPLAY_NAME, new Date());
        UserRole roleResearcher = new UserRole(1, UserRoles.RESEARCHER.getValue());
        List<UserRole> roles = new ArrayList<>(Collections.singletonList(roleResearcher));
        user.setRoles(roles);
        User existentUser = new User(1, EMAIL, DISPLAY_NAME, new Date());
        UserRole dataOwner = new UserRole(1, UserRoles.DATAOWNER.getValue());
        existentUser.setRoles(new ArrayList<>(Collections.singletonList(dataOwner)));
        when(userDAO.findDACUserByEmail(EMAIL)).thenReturn(existentUser);
        when(userDAO.findDACUserById(1)).thenReturn(existentUser);
        userService.updateUser(user, EMAIL);
        verify(userDAO, times(1)).updateDACUser(DISPLAY_NAME, 1);
    }

    @Test
    public void testUpdatePartialUserDisplayNameInvalidOperation() throws UserRoleHandlerException {
        User existentUser = new User(1, EMAIL, DISPLAY_NAME, new Date());
        UserRole roleResearcher = new UserRole(1, UserRoles.RESEARCHER.getValue());
        List<UserRole> roles = new ArrayList<>(Collections.singletonList(roleResearcher));
        existentUser.setRoles(roles);
        PatchOperation  patchOperation = new PatchOperation();
        patchOperation.setPath(DISPLAY_NAME_FIELD);
        patchOperation.setOp(Actions.ADD.getValue());
        patchOperation.setValue("newDisplayName");
        when(userDAO.findDACUserByEmail(EMAIL)).thenReturn(existentUser);
        try {
            userService.updatePartialUser(new ArrayList<>(Collections.singletonList(patchOperation)), EMAIL);
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().equalsIgnoreCase(patchOperation.getOp() + " operation is not supported for this field. The only supported operation for this field is replace."));
        }
    }

    @Test
    public void testUpdatePartialUserDisplayNameSuccess() throws UserRoleHandlerException {
        User existentUser = new User(1, EMAIL, DISPLAY_NAME, new Date());
        UserRole roleResearcher = new UserRole(1, UserRoles.RESEARCHER.getValue());
        List<UserRole> roles = new ArrayList<>(Collections.singletonList(roleResearcher));
        existentUser.setRoles(roles);
        PatchOperation  patchOperation = new PatchOperation();
        patchOperation.setPath(DISPLAY_NAME_FIELD);
        patchOperation.setOp(Actions.REPLACE.getValue());
        patchOperation.setValue("newDisplayName");
        when(userDAO.findDACUserByEmail(EMAIL)).thenReturn(existentUser);
        when(userDAO.findDACUserById(1)).thenReturn(existentUser);
        when(userRoleDAO.findRolesByUserId(1)).thenReturn(roles);
        existentUser = userService.updatePartialUser(new ArrayList<>(Collections.singletonList(patchOperation)), EMAIL);
        assertEquals("newDisplayName", existentUser.getDisplayName());
    }

    @Test
    public void testUpdatePartialUserInvalidPath() throws UserRoleHandlerException {
        User existentUser = new User(1, EMAIL, DISPLAY_NAME, new Date());
        PatchOperation  patchOperation = new PatchOperation();
        patchOperation.setPath("test");
        patchOperation.setOp(Actions.ADD.getValue());
        patchOperation.setValue("newDisplayName");
        when(userDAO.findDACUserByEmail(EMAIL)).thenReturn(existentUser);
        try {
            userService.updatePartialUser(new ArrayList<>(Collections.singletonList(patchOperation)), EMAIL);
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().equalsIgnoreCase("Invalid path."));
        }
    }

    @Test
    public void testUpdatePartialUserReplaceRoleSuccess() throws UserRoleHandlerException {
        User user = new User(1, EMAIL, DISPLAY_NAME, new Date());
        UserRole roleResearcher = new UserRole(1, UserRoles.RESEARCHER.getValue());
        user.setRoles(new ArrayList<>(Collections.singletonList(roleResearcher)));
        PatchOperation  patchOperation = new PatchOperation();
        patchOperation.setPath("roles");
        patchOperation.setOp(Actions.REPLACE.getValue());
        patchOperation.setValue(UserRoles.DATAOWNER.getValue());
        when(userDAO.findDACUserByEmail(EMAIL)).thenReturn(user);
        when(userDAO.findDACUserById(1)).thenReturn(user);
        userService.updatePartialUser(new ArrayList<>(Collections.singletonList(patchOperation)), EMAIL);
        verify(userDAO, times(1)).updateDACUser(DISPLAY_NAME, 1);

    }

}
