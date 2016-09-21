package org.broadinstitute.consent.http.service.users;

import org.broadinstitute.consent.http.db.*;
import org.broadinstitute.consent.http.enumeration.DACUserRoles;
import org.broadinstitute.consent.http.enumeration.VoteType;
import org.broadinstitute.consent.http.models.DACUser;
import org.broadinstitute.consent.http.models.DACUserRole;
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
    DACUserRoleDAO roleDAO;

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
        databaseDACUserAPI = new DatabaseDACUserAPI(dacUserDAO, roleDAO, electionDAO, voteDAO, dataSetAssociationDAO, userHandlerAPI, null);
    }

    @Test
    public void createDACUser() {
        DACUser user = new DACUser(null, EMAIL, DISPLAY_NAME, new Date());
        when(dacUserDAO.insertDACUser(anyString(), anyString(), any(Date.class))).thenReturn(3);
        user.setDacUserId(3);
        DACUserRole role = new DACUserRole(1, DACUserRoles.RESEARCHER.getValue());
        List<DACUserRole> roles = new ArrayList<>(Arrays.asList(role));
        user.setRoles(roles);
        when(dacUserDAO.findDACUserById(3)).thenReturn(user);
        when(roleDAO.findRoleIdByName(DACUserRoles.RESEARCHER.getValue())).thenReturn(1);
        when(roleDAO.findRolesByUserId(3)).thenReturn(roles);
        user = databaseDACUserAPI.createDACUser(user);
        assertTrue(user != null);
        assertTrue(user.getDisplayName() != null);
    }

    @Test
    public void createDACUserWithExistentEmail() {
        DACUser user = new DACUser(null, EMAIL, DISPLAY_NAME, new Date());
        when(dacUserDAO.insertDACUser(anyString(), anyString(), any(Date.class))).thenThrow(UnableToExecuteStatementException.class);
        try {
            databaseDACUserAPI.createDACUser(user);
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().equals("Email should be unique."));
        }
    }

    @Test
    public void createDACUserWithoutDisplayName() {
        DACUser user = new DACUser(null, EMAIL, null, new Date());
        try {
            databaseDACUserAPI.createDACUser(user);
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().equals("Display Name can't be null. The user needs a name to display."));
        }
    }

    @Test
    public void createDACUserWithoutEmail() {
        DACUser user = new DACUser(null, null, DISPLAY_NAME, new Date());
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
        DACUser dacUser = new DACUser(1, EMAIL, DISPLAY_NAME, new Date());
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
        DACUser dacUser = new DACUser(1, EMAIL, DISPLAY_NAME, new Date());
        when(dacUserDAO.findDACUserById(1)).thenReturn(dacUser);
        DACUser user = databaseDACUserAPI.describeDACUserById(1);
        assertNotNull(user);
    }

    @Test
    public void validateNeedsDelegationMemberTrue() {
        DACUser dacUser = new DACUser(1, EMAIL, DISPLAY_NAME, new Date());
        List<Integer> openElectionIdsForThisUser = new ArrayList<>(Arrays.asList(2));
        when(electionDAO.verifyOpenElections()).thenReturn(3);
        when(electionDAO.findNonDataSetOpenElectionIds(dacUser.getDacUserId())).thenReturn(openElectionIdsForThisUser);
        when(voteDAO.findVoteCountForElections(openElectionIdsForThisUser, VoteType.DAC.getValue())).thenReturn(openElectionIdsForThisUser);
        when(dacUserDAO.getMembersApprovedToReplace(anyInt(), anyList())).thenReturn(new ArrayList<>(Arrays.asList(new DACUser(5, EMAIL, DISPLAY_NAME, new Date()))));
        ValidateDelegationResponse response = databaseDACUserAPI.validateNeedsDelegation(dacUser, DACUserRoles.MEMBER.getValue());
        assertNotNull(response);
        assertTrue(response.isNeedsDelegation());
        assertTrue(response.getDelegateCandidates().size() == 1);
    }

    @Test
    public void validateNeedsDelegationMemberTrueWithEmptyCandidates() {
        DACUser dacUser = new DACUser(1, EMAIL, DISPLAY_NAME, new Date());
        List<Integer> openElectionIdsForThisUser = new ArrayList<>(Arrays.asList(2));
        when(electionDAO.verifyOpenElections()).thenReturn(3);
        when(electionDAO.findNonDataSetOpenElectionIds(dacUser.getDacUserId())).thenReturn(openElectionIdsForThisUser);
        when(voteDAO.findVoteCountForElections(openElectionIdsForThisUser, VoteType.DAC.getValue())).thenReturn(openElectionIdsForThisUser);
        when(dacUserDAO.getMembersApprovedToReplace(anyInt(), anyList())).thenReturn(new ArrayList<>());
        ValidateDelegationResponse response = databaseDACUserAPI.validateNeedsDelegation(dacUser, DACUserRoles.MEMBER.getValue());
        assertNotNull(response);
        assertTrue(response.isNeedsDelegation());
        assertTrue(response.getDelegateCandidates().size() == 0);
    }

    @Test
    public void validateNeedsDelegationMemberFalse() {
        DACUser dacUser = new DACUser(1, EMAIL, DISPLAY_NAME, new Date());
        when(electionDAO.verifyOpenElections()).thenReturn(0);
        ValidateDelegationResponse response = databaseDACUserAPI.validateNeedsDelegation(dacUser, DACUserRoles.MEMBER.getValue());
        assertNotNull(response);
        assertFalse(response.isNeedsDelegation());
        assertTrue(response.getDelegateCandidates().size() == 0);
    }

    @Test
    public void validateNeedsDelegationChairpersonFalse() {
        DACUser dacUser = new DACUser(1, EMAIL, DISPLAY_NAME, new Date());
        when(electionDAO.verifyOpenElections()).thenReturn(0);
        ValidateDelegationResponse response = databaseDACUserAPI.validateNeedsDelegation(dacUser, DACUserRoles.CHAIRPERSON.getValue());
        assertNotNull(response);
        assertFalse(response.isNeedsDelegation());
        assertTrue(response.getDelegateCandidates().size() == 0);
    }

    @Test
    public void validateNeedsDelegationDataOwnerTrueWithEmptyCandidates() {
        DACUser dacUser = new DACUser(1, EMAIL, DISPLAY_NAME, new Date());
        List<Integer> associatedDataSetId = new ArrayList<>(Arrays.asList(2));
        List<Integer> dataOwnersPerDataSet = new ArrayList<>(Arrays.asList(1));
        when(dataSetAssociationDAO.getDataSetsIdOfDataOwnerNeedsApproval(dacUser.getDacUserId())).thenReturn(associatedDataSetId);
        when(dataSetAssociationDAO.getCountOfDataOwnersPerDataSet(associatedDataSetId)).thenReturn(dataOwnersPerDataSet);
        when(dacUserDAO.getDataOwnersApprovedToReplace(dacUser.getDacUserId())).thenReturn(new ArrayList<>());
        ValidateDelegationResponse response = databaseDACUserAPI.validateNeedsDelegation(dacUser, DACUserRoles.DATAOWNER.getValue());
        assertNotNull(response);
        assertTrue(response.isNeedsDelegation());
        assertTrue(response.getDelegateCandidates().size() == 0);
    }



}
