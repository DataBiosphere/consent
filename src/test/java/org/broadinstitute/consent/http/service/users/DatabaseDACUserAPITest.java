package org.broadinstitute.consent.http.service.users;

import org.broadinstitute.consent.http.db.*;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.enumeration.VoteType;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.models.user.ValidateDelegationResponse;
import org.broadinstitute.consent.http.service.users.handler.UserHandlerAPI;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;
import org.skife.jdbi.v2.exceptions.UnableToExecuteStatementException;

import javax.ws.rs.NotFoundException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.when;


@RunWith(MockitoJUnitRunner.class)
public class DatabaseDACUserAPITest {

    private DatabaseDACUserAPI databaseDACUserAPI;

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

    private final String EMAIL = "test@gmail.com";

    private final String DISPLAY_NAME = "test";


    @Before
    public void setUp() throws URISyntaxException {
        MockitoAnnotations.initMocks(this);
        databaseDACUserAPI = new DatabaseDACUserAPI(dacUserDAO, userRoleDAO, electionDAO, voteDAO, dataSetAssociationDAO, userHandlerAPI, null);
    }

    @Test
    public void createDACUser() {
        User user = new User(null, EMAIL, DISPLAY_NAME, new Date(), null);
        when(dacUserDAO.insertDACUser(anyString(), anyString(), any(Date.class))).thenReturn(3);
        user.setUserId(3);
        UserRole role = new UserRole(1, UserRoles.RESEARCHER.getValue());
        List<UserRole> roles = new ArrayList<>(Arrays.asList(role));
        user.setRoles(roles);
        when(dacUserDAO.findDACUserById(3)).thenReturn(user);
        when(userRoleDAO.findRoleIdByName(UserRoles.RESEARCHER.getValue())).thenReturn(1);
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

    @Test
    public void describeUserByNonExistentEmail() {
        when(dacUserDAO.findDACUserByEmail(EMAIL)).thenReturn(null);
        try {
            databaseDACUserAPI.describeDACUserByEmail(EMAIL);
        } catch (NotFoundException e) {
            assertTrue(e.getMessage().equals("Could not find user for specified email : " + EMAIL));
        }
    }

    @Test
    public void describeUserByEmail() {
        User user = new User(1, EMAIL, DISPLAY_NAME, new Date(), null);
        when(dacUserDAO.findDACUserByEmail(EMAIL)).thenReturn(user);
        User foundUser = databaseDACUserAPI.describeDACUserByEmail(EMAIL);
        assertNotNull(foundUser);
    }

    @Test
    public void describeUserByNonExistentId() {
        int id = 1;
        when(dacUserDAO.findDACUserById(id)).thenReturn(null);
        try {
            databaseDACUserAPI.describeDACUserById(id);
        } catch (NotFoundException e) {
            assertTrue(e.getMessage().equals("Could not find user for specified id : " + id));
        }
    }

    @Test
    public void describeUserById() {
        User user = new User(1, EMAIL, DISPLAY_NAME, new Date(), null);
        when(dacUserDAO.findDACUserById(1)).thenReturn(user);
        User foundUser = databaseDACUserAPI.describeDACUserById(1);
        assertNotNull(foundUser);
    }

    @Test
    public void validateNeedsDelegationMemberTrue() {
        User user = new User(1, EMAIL, DISPLAY_NAME, new Date(), null);
        List<Integer> openElectionIdsForThisUser = new ArrayList<>(Arrays.asList(2));
        when(electionDAO.verifyOpenElections()).thenReturn(3);
        when(electionDAO.findNonDataSetOpenElectionIds(user.getUserId())).thenReturn(openElectionIdsForThisUser);
        when(voteDAO.findVoteCountForElections(openElectionIdsForThisUser, VoteType.DAC.getValue())).thenReturn(openElectionIdsForThisUser);
        when(dacUserDAO.getMembersApprovedToReplace(anyInt(), anyList())).thenReturn(new ArrayList<>(Arrays.asList(new User(5, EMAIL, DISPLAY_NAME, new Date(), null))));
        ValidateDelegationResponse response = databaseDACUserAPI.validateNeedsDelegation(user, UserRoles.MEMBER.getValue());
        assertNotNull(response);
        assertTrue(response.isNeedsDelegation());
        assertTrue(response.getDelegateCandidates().size() == 1);
    }

    @Test
    public void validateNeedsDelegationMemberTrueWithEmptyCandidates() {
        User user = new User(1, EMAIL, DISPLAY_NAME, new Date(), null);
        List<Integer> openElectionIdsForThisUser = new ArrayList<>(Arrays.asList(2));
        when(electionDAO.verifyOpenElections()).thenReturn(3);
        when(electionDAO.findNonDataSetOpenElectionIds(user.getUserId())).thenReturn(openElectionIdsForThisUser);
        when(voteDAO.findVoteCountForElections(openElectionIdsForThisUser, VoteType.DAC.getValue())).thenReturn(openElectionIdsForThisUser);
        when(dacUserDAO.getMembersApprovedToReplace(anyInt(), anyList())).thenReturn(new ArrayList<>());
        ValidateDelegationResponse response = databaseDACUserAPI.validateNeedsDelegation(user, UserRoles.MEMBER.getValue());
        assertNotNull(response);
        assertTrue(response.isNeedsDelegation());
        assertTrue(response.getDelegateCandidates().size() == 0);
    }

    @Test
    public void validateNeedsDelegationMemberFalse() {
        User user = new User(1, EMAIL, DISPLAY_NAME, new Date(), null);
        when(electionDAO.verifyOpenElections()).thenReturn(0);
        ValidateDelegationResponse response = databaseDACUserAPI.validateNeedsDelegation(user, UserRoles.MEMBER.getValue());
        assertNotNull(response);
        assertFalse(response.isNeedsDelegation());
        assertTrue(response.getDelegateCandidates().size() == 0);
    }

    @Test
    public void validateNeedsDelegationChairpersonFalse() {
        User user = new User(1, EMAIL, DISPLAY_NAME, new Date(), null);
        when(electionDAO.verifyOpenElections()).thenReturn(0);
        ValidateDelegationResponse response = databaseDACUserAPI.validateNeedsDelegation(user, UserRoles.CHAIRPERSON.getValue());
        assertNotNull(response);
        assertFalse(response.isNeedsDelegation());
        assertTrue(response.getDelegateCandidates().size() == 0);
    }

    @Test
    public void validateNeedsDelegationDataOwnerTrueWithEmptyCandidates() {
        User user = new User(1, EMAIL, DISPLAY_NAME, new Date(), null);
        List<Integer> associatedDataSetId = new ArrayList<>(Arrays.asList(2));
        List<Integer> dataOwnersPerDataSet = new ArrayList<>(Arrays.asList(1));
        when(dataSetAssociationDAO.getDataSetsIdOfDataOwnerNeedsApproval(user.getUserId())).thenReturn(associatedDataSetId);
        when(dataSetAssociationDAO.getCountOfDataOwnersPerDataSet(associatedDataSetId)).thenReturn(dataOwnersPerDataSet);
        when(dacUserDAO.getDataOwnersApprovedToReplace(user.getUserId())).thenReturn(new ArrayList<>());
        ValidateDelegationResponse response = databaseDACUserAPI.validateNeedsDelegation(user, UserRoles.DATAOWNER.getValue());
        assertNotNull(response);
        assertTrue(response.isNeedsDelegation());
        assertTrue(response.getDelegateCandidates().size() == 0);
    }



}
