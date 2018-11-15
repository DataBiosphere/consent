package org.broadinstitute.consent.http.service.users;

import org.broadinstitute.consent.http.db.*;
import org.broadinstitute.consent.http.db.mongo.MongoConsentDB;
import org.broadinstitute.consent.http.enumeration.Actions;
import org.broadinstitute.consent.http.enumeration.DACUserRoles;
import org.broadinstitute.consent.http.enumeration.VoteType;
import org.broadinstitute.consent.http.models.DACUser;
import org.broadinstitute.consent.http.models.DACUserRole;
import org.broadinstitute.consent.http.models.dto.PatchOperation;
import org.broadinstitute.consent.http.service.users.handler.UserHandlerAPI;
import org.broadinstitute.consent.http.service.users.handler.UserRoleHandlerException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import javax.ws.rs.NotAuthorizedException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DatabaseUserAPITest {

    @Mock
    DACUserDAO dacUserDAO;

    @Mock
    DACUserRoleDAO roleDAO;

    @Mock
    ElectionDAO electionDAO;

    @Mock
    VoteDAO voteDAO;

    @Mock
    DataSetAssociationDAO dataSetAssociationDAO;

    @Mock
    UserHandlerAPI userHandlerAPI;

    @Mock
    MongoConsentDB mongo;

    UserAPI userAPI;

    private final String EMAIL = "test@gmail.com";

    private final String INVALID_EMAIL = "invalid_email@gmail.com";

    private final String DISPLAY_NAME = "test";

    private final String DISPLAY_NAME_FIELD = "displayName";

    @Before
    public void setUp() throws URISyntaxException {
        MockitoAnnotations.initMocks(this);
        userAPI = new DatabaseUserAPI(dacUserDAO, roleDAO, electionDAO, voteDAO, dataSetAssociationDAO, userHandlerAPI, mongo, null);
    }


    @Test
    public void testCreateUserWithInvalidEmail() throws UserRoleHandlerException {
        DACUser dacUser = new DACUser(1, EMAIL, DISPLAY_NAME, new Date());
        try {
            userAPI.createUser(dacUser, INVALID_EMAIL);
        } catch (NotAuthorizedException e) {
            assertTrue(e.getMessage().equalsIgnoreCase("You don't have permission to update the specified user."));
        }
    }

    @Test
    public void testCreateUserWithInvalidRoles() throws UserRoleHandlerException {
        DACUser dacUser = new DACUser(1, EMAIL, DISPLAY_NAME, new Date());
        DACUserRole roleResearcher = new DACUserRole(1, DACUserRoles.RESEARCHER.getValue());
        DACUserRole roleMember = new DACUserRole(1, DACUserRoles.MEMBER.getValue());
        List<DACUserRole> roles = new ArrayList<>(Arrays.asList(roleResearcher, roleMember));
        dacUser.setRoles(roles);
        try {
            userAPI.createUser(dacUser, EMAIL);
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().equalsIgnoreCase("Invalid role: " + DACUserRoles.MEMBER.getValue() + ". Valid roles are: " + DACUserRoles.DATAOWNER.getValue() + " and " + DACUserRoles.RESEARCHER.getValue()));
        }
    }

    @Test
    public void testUpdateUserWithInvalidEmail() throws UserRoleHandlerException {
        DACUser dacUser = new DACUser(1, EMAIL, DISPLAY_NAME, new Date());
        try {
            userAPI.updateUser(dacUser, INVALID_EMAIL);
        } catch (NotAuthorizedException e) {
            assertTrue(e.getMessage().equalsIgnoreCase("You don't have permission to update the specified user."));
        }
    }

    @Test
    public void testUpdateUserWithInvalidRoles() throws UserRoleHandlerException {
        DACUser dacUser = new DACUser(1, EMAIL, DISPLAY_NAME, new Date());
        DACUserRole roleResearcher = new DACUserRole(1, DACUserRoles.RESEARCHER.getValue());
        DACUserRole roleMember = new DACUserRole(1, DACUserRoles.MEMBER.getValue());
        List<DACUserRole> roles = new ArrayList<>(Arrays.asList(roleResearcher, roleMember));
        dacUser.setRoles(roles);
        try {
            userAPI.updateUser(dacUser, EMAIL);
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().equalsIgnoreCase("Invalid role: " + DACUserRoles.MEMBER.getValue() + ". Valid roles are: " + DACUserRoles.DATAOWNER.getValue() + " and " + DACUserRoles.RESEARCHER.getValue()));
        }
    }


    @Test
    public void testUpdateUserRemoveDataOwnerWithDataSetAssociations() throws UserRoleHandlerException {
        DACUser dacUser = new DACUser(1, EMAIL, DISPLAY_NAME, new Date());
        DACUserRole roleResearcher = new DACUserRole(1, DACUserRoles.RESEARCHER.getValue());
        List<DACUserRole> roles = new ArrayList<>(Arrays.asList(roleResearcher));
        dacUser.setRoles(roles);
        DACUser existentUser = new DACUser(1, EMAIL, DISPLAY_NAME, new Date());
        DACUserRole dataOwnerRole = new DACUserRole(1, DACUserRoles.DATAOWNER.getValue());
        existentUser.setRoles(new ArrayList<>(Arrays.asList(dataOwnerRole)));
        when(dataSetAssociationDAO.getDataSetsIdOfDataOwnerNeedsApproval(anyInt())).thenReturn(Arrays.asList(1));
        when(dataSetAssociationDAO.getCountOfDataOwnersPerDataSet(anyList())).thenReturn(Arrays.asList(1));
        when(dacUserDAO.findDACUserById(1)).thenReturn(existentUser);
        when(dacUserDAO.findDACUserByEmail(EMAIL)).thenReturn(existentUser);
        when(roleDAO.findRolesByUserId(dacUser.getDacUserId())).thenReturn(Arrays.asList(dataOwnerRole));
        try {
            userAPI.updateUser(dacUser, EMAIL);
        } catch (UserRoleHandlerException e) {
            assertTrue(e.getMessage().equalsIgnoreCase("Role can not be removed. The specified user is the only data owner associated to a data set that needs approval."));
        }
    }


    @Test
    public void testUpdateUserRemoveDataOwnerWithOpenElections() throws UserRoleHandlerException {
        DACUser dacUser = new DACUser(1, EMAIL, DISPLAY_NAME, new Date());
        DACUserRole roleResearcher = new DACUserRole(1, DACUserRoles.RESEARCHER.getValue());
        List<DACUserRole> roles = new ArrayList<>(Arrays.asList(roleResearcher));
        dacUser.setRoles(roles);
        DACUser existentUser = new DACUser(1, EMAIL, DISPLAY_NAME, new Date());
        DACUserRole dataOwnerRole = new DACUserRole(1, DACUserRoles.DATAOWNER.getValue());
        existentUser.setRoles(new ArrayList<>(Arrays.asList(dataOwnerRole)));
        when(dataSetAssociationDAO.getDataSetsIdOfDataOwnerNeedsApproval(anyInt())).thenReturn(Arrays.asList(1));
        when(dataSetAssociationDAO.getCountOfDataOwnersPerDataSet(anyList())).thenReturn(Arrays.asList(6));
        when(electionDAO.findDataSetOpenElectionIds(any())).thenReturn(Arrays.asList(2));
        when(voteDAO.findVoteCountForElections(Arrays.asList(2), VoteType.DATA_OWNER.getValue())).thenReturn(Arrays.asList(1));
        when(dacUserDAO.findDACUserById(1)).thenReturn(existentUser);
        when(dacUserDAO.findDACUserByEmail(EMAIL)).thenReturn(existentUser);
        when(roleDAO.findRolesByUserId(dacUser.getDacUserId())).thenReturn(Arrays.asList(dataOwnerRole));
        try {
            userAPI.updateUser(dacUser, EMAIL);
        } catch (UserRoleHandlerException e) {
            assertTrue(e.getMessage().equalsIgnoreCase("Role can not be removed. There are open dataset elections for this user."));
        }
    }

    @Test
    public void testUpdateUserAddDataOwnerRole() throws UserRoleHandlerException {
        DACUser dacUser = new DACUser(1, EMAIL, DISPLAY_NAME, new Date());
        DACUserRole roleResearcher = new DACUserRole(1, DACUserRoles.DATAOWNER.getValue());
        List<DACUserRole> roles = new ArrayList<>(Arrays.asList(roleResearcher));
        dacUser.setRoles(roles);
        DACUser existentUser = new DACUser(1, EMAIL, DISPLAY_NAME, new Date());
        DACUserRole researcherRole = new DACUserRole(1, DACUserRoles.RESEARCHER.getValue());
        existentUser.setRoles(new ArrayList<>(Arrays.asList(researcherRole)));
        when(dacUserDAO.findDACUserByEmail(EMAIL)).thenReturn(existentUser);
        when(dacUserDAO.findDACUserById(1)).thenReturn(existentUser);
        userAPI.updateUser(dacUser, EMAIL);
        verify(roleDAO, times(1)).insertSingleUserRole(anyInt(), anyInt(), anyBoolean());
        verify(dacUserDAO, times(1)).updateDACUser(DISPLAY_NAME, 1);
    }

    @Test
    public void testUpdateDACUserSuccess() throws UserRoleHandlerException {
        DACUser existentDacUser = new DACUser(1, EMAIL, DISPLAY_NAME, new Date());
        DACUserRole roleResearcher = new DACUserRole(1, DACUserRoles.RESEARCHER.getValue());
        List<DACUserRole> roles = new ArrayList<>(Arrays.asList(roleResearcher));
        existentDacUser.setRoles(roles);
        DACUser dacUserToUpdate =  new DACUser(1, EMAIL, DISPLAY_NAME, new Date());
        DACUserRole roleDO =  new DACUserRole(2, DACUserRoles.DATAOWNER.getValue());
        List<DACUserRole> newRoles = new ArrayList<>(Arrays.asList(roleResearcher, roleDO));
        dacUserToUpdate.setRoles(newRoles);
        when(dacUserDAO.findDACUserByEmail(EMAIL)).thenReturn(existentDacUser);
        when(dacUserDAO.findDACUserById(1)).thenReturn(dacUserToUpdate);
        userAPI.updateUser(dacUserToUpdate, EMAIL);
        assertNotNull(dacUserToUpdate);
        assertNotNull(dacUserToUpdate.getRoles().size() == 2);

    }

    @Test
    public void testUpdateUserAddResearcherWithIncompatiblesRoles() throws UserRoleHandlerException {
        DACUser dacUser = new DACUser(1, EMAIL, DISPLAY_NAME, new Date());
        DACUserRole roleResearcher = new DACUserRole(1, DACUserRoles.RESEARCHER.getValue());
        List<DACUserRole> roles = new ArrayList<>(Arrays.asList(roleResearcher));
        dacUser.setRoles(roles);
        DACUser existentUser = new DACUser(1, EMAIL, DISPLAY_NAME, new Date());
        DACUserRole dataOwnerRole = new DACUserRole(1, DACUserRoles.MEMBER.getValue());
        existentUser.setRoles(new ArrayList<>(Arrays.asList(dataOwnerRole)));

        when(dacUserDAO.findDACUserById(1)).thenReturn(existentUser);
        when(dacUserDAO.findDACUserByEmail(EMAIL)).thenReturn(existentUser);
        when(roleDAO.findRolesByUserId(dacUser.getDacUserId())).thenReturn(Arrays.asList(dataOwnerRole));
        try {
            userAPI.updateUser(dacUser, EMAIL);
        } catch (UserRoleHandlerException e) {
            assertTrue(e.getMessage().equalsIgnoreCase("Role can not be added. Researcher  is incompatible with  Member and Chairperson role."));
        }
    }

    @Test
    public void testUpdateUserAddResearcherRole() throws UserRoleHandlerException {
        DACUser dacUser = new DACUser(1, EMAIL, DISPLAY_NAME, new Date());
        DACUserRole roleResearcher = new DACUserRole(1, DACUserRoles.RESEARCHER.getValue());
        List<DACUserRole> roles = new ArrayList<>(Arrays.asList(roleResearcher));
        dacUser.setRoles(roles);
        DACUser existentUser = new DACUser(1, EMAIL, DISPLAY_NAME, new Date());
        DACUserRole dataOwner = new DACUserRole(1, DACUserRoles.DATAOWNER.getValue());
        existentUser.setRoles(new ArrayList<>(Arrays.asList(dataOwner)));
        when(dacUserDAO.findDACUserByEmail(EMAIL)).thenReturn(existentUser);
        when(dacUserDAO.findDACUserById(1)).thenReturn(existentUser);
        userAPI.updateUser(dacUser, EMAIL);
        verify(dacUserDAO, times(1)).updateDACUser(DISPLAY_NAME, 1);
    }

    @Test
    public void testUpdatePartialUserDisplayNameInvalidOperation() throws UserRoleHandlerException {
        DACUser existentUser = new DACUser(1, EMAIL, DISPLAY_NAME, new Date());
        DACUserRole roleResearcher = new DACUserRole(1, DACUserRoles.RESEARCHER.getValue());
        List<DACUserRole> roles = new ArrayList<>(Arrays.asList(roleResearcher));
        existentUser.setRoles(roles);
        PatchOperation  patchOperation = new PatchOperation();
        patchOperation.setPath(DISPLAY_NAME_FIELD);
        patchOperation.setOp(Actions.ADD.getValue());
        patchOperation.setValue("newDisplayName");
        when(dacUserDAO.findDACUserByEmail(EMAIL)).thenReturn(existentUser);
        try {
            userAPI.updatePartialUser(new ArrayList<>(Arrays.asList(patchOperation)), EMAIL);
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().equalsIgnoreCase(patchOperation.getOp() + " operation is not supported for this field. The only supported operation for this field is replace."));
        }
    }

    @Test
    public void testUpdatePartialUserDisplayNameSuccess() throws UserRoleHandlerException {
        DACUser existentUser = new DACUser(1, EMAIL, DISPLAY_NAME, new Date());
        DACUserRole roleResearcher = new DACUserRole(1, DACUserRoles.RESEARCHER.getValue());
        List<DACUserRole> roles = new ArrayList<>(Arrays.asList(roleResearcher));
        existentUser.setRoles(roles);
        PatchOperation  patchOperation = new PatchOperation();
        patchOperation.setPath(DISPLAY_NAME_FIELD);
        patchOperation.setOp(Actions.REPLACE.getValue());
        patchOperation.setValue("newDisplayName");
        when(dacUserDAO.findDACUserByEmail(EMAIL)).thenReturn(existentUser);
        when(dacUserDAO.findDACUserById(1)).thenReturn(existentUser);
        when(roleDAO.findRolesByUserId(1)).thenReturn(roles);
        existentUser = userAPI.updatePartialUser(new ArrayList<>(Arrays.asList(patchOperation)), EMAIL);
        assertTrue(existentUser.getDisplayName().equals("newDisplayName"));
    }

    @Test
    public void testUpdatePartialUserInvalidPath() throws UserRoleHandlerException {
        DACUser existentUser = new DACUser(1, EMAIL, DISPLAY_NAME, new Date());
        PatchOperation  patchOperation = new PatchOperation();
        patchOperation.setPath("test");
        patchOperation.setOp(Actions.ADD.getValue());
        patchOperation.setValue("newDisplayName");
        when(dacUserDAO.findDACUserByEmail(EMAIL)).thenReturn(existentUser);
        try {
            userAPI.updatePartialUser(new ArrayList<>(Arrays.asList(patchOperation)), EMAIL);
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().equalsIgnoreCase("Invalid path."));
        }
    }

    @Test
    public void testUpdatePartialUserReplaceRoleSuccess() throws UserRoleHandlerException {
        DACUser dacUser = new DACUser(1, EMAIL, DISPLAY_NAME, new Date());
        DACUserRole roleResearcher = new DACUserRole(1, DACUserRoles.RESEARCHER.getValue());
        dacUser.setRoles(new ArrayList<>(Arrays.asList(roleResearcher)));
        PatchOperation  patchOperation = new PatchOperation();
        patchOperation.setPath("roles");
        patchOperation.setOp(Actions.REPLACE.getValue());
        patchOperation.setValue(DACUserRoles.DATAOWNER.getValue());
        when(dacUserDAO.findDACUserByEmail(EMAIL)).thenReturn(dacUser);
        when(dacUserDAO.findDACUserById(1)).thenReturn(dacUser);
        userAPI.updatePartialUser(new ArrayList<>(Arrays.asList(patchOperation)), EMAIL);
        verify(dacUserDAO, times(1)).updateDACUser(DISPLAY_NAME, 1);

    }

}
