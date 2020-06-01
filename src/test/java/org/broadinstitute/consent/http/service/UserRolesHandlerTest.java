package org.broadinstitute.consent.http.service;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.db.DACUserDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.db.UserRoleDAO;
import org.broadinstitute.consent.http.db.VoteDAO;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.enumeration.VoteType;
import org.broadinstitute.consent.http.models.DACUser;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.models.Vote;
import org.broadinstitute.consent.http.service.users.handler.UserRolesHandler;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class UserRolesHandlerTest {

    @Mock
    private DACUserDAO dacUserDAO;
    @Mock
    private ElectionDAO electionDAO;
    @Mock
    private VoteDAO voteDAO;
    @Mock
    private UserRoleDAO userRoleDAO;
    @Mock
    private DataAccessRequestService dataAccessRequestService;

    UserRolesHandler handler;


    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    private void initService() {
        handler = new UserRolesHandler(dacUserDAO, dataAccessRequestService, electionDAO, userRoleDAO, voteDAO);
    }

    @Test
    public void testUpdateToChairperson() {
        DACUser user = generateUser();
        when(userRoleDAO.findRolesByUserId(any())).thenReturn(Collections.singletonList(getResearcherRole()));
        initService();

        user.setRoles(Arrays.asList(getResearcherRole(), getChairpersonRole()));
        handler.updateRoles(user);
        verify(userRoleDAO, never()).insertSingleUserRole(any(), any());
    }

    @Test
    public void testUpdateToMember() {
        DACUser user = generateUser();
        when(userRoleDAO.findRolesByUserId(any())).thenReturn(Collections.singletonList(getResearcherRole()));
        initService();

        user.setRoles(Arrays.asList(getResearcherRole(), getMemberRole()));
        handler.updateRoles(user);
        verify(userRoleDAO, never()).insertSingleUserRole(any(), any());
    }

    @Test
    public void testAddRoles() {
        DACUser user = generateUser();
        when(userRoleDAO.findRolesByUserId(any())).thenReturn(Collections.singletonList(getResearcherRole()));
        initService();

        user.setRoles(Arrays.asList(getResearcherRole(), getAlumniRole(), getDataOwnerRole()));
        handler.updateRoles(user);
        verify(userRoleDAO, times(2)).insertUserRoles(any(), any());
        verify(userRoleDAO, never()).removeSingleUserRole(any(), any());
    }

    @Test
    public void testRemoveRoles() {
        DACUser user = generateUser();
        when(userRoleDAO.findRolesByUserId(any())).thenReturn(Arrays.asList(getResearcherRole(), getAlumniRole(), getDataOwnerRole()));
        initService();

        user.setRoles(Collections.singletonList(getResearcherRole()));
        handler.updateRoles(user);
        verify(userRoleDAO, never()).insertUserRoles(any(), any());
        verify(userRoleDAO, times(2)).removeSingleUserRole(any(), any());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMinimumAdmins() {
        DACUser user = generateUser();
        when(userRoleDAO.findRolesByUserId(any())).thenReturn(Collections.singletonList(getAdminRole()));
        initService();

        user.setRoles(Collections.singletonList(getResearcherRole()));
        handler.updateRoles(user);
    }

    @Test
    public void testRemoveDataOwnerRole() {
        DACUser user = generateUser();
        Vote doVote = generateDOVote(user);
        when(userRoleDAO.findRolesByUserId(any())).thenReturn(Collections.singletonList(getDataOwnerRole()));
        when(voteDAO.findVotesByUserId(any())).thenReturn(Collections.singletonList(doVote));
        when(electionDAO.findDataSetOpenElectionIds(any())).thenReturn(Collections.singletonList(doVote.getElectionId()));
        initService();

        user.setRoles(Collections.singletonList(getResearcherRole()));
        handler.updateRoles(user);
        verify(userRoleDAO, times(1)).insertUserRoles(any(), any());
        verify(userRoleDAO, times(1)).removeSingleUserRole(any(), any());
        verify(voteDAO, times(1)).removeVotesByIds(any());
    }

    @Test
    public void testRemoveResearcherRole() {
        DACUser user = generateUser();
        DataAccessRequest dar = generateDar(user);
        when(userRoleDAO.findRolesByUserId(any())).thenReturn(Collections.singletonList(getResearcherRole()));
        when(dataAccessRequestService.findAllDataAccessRequests()).thenReturn(Collections.singletonList(dar));
        initService();

        user.setRoles(Collections.singletonList(getAlumniRole()));
        handler.updateRoles(user);
        verify(userRoleDAO, times(1)).insertUserRoles(any(), any());
        verify(userRoleDAO, times(1)).removeSingleUserRole(any(), any());
        verify(electionDAO, times(2)).bulkCancelOpenElectionByReferenceIdAndType(any(), any());
        verify(dataAccessRequestService, times(1)).cancelDataAccessRequest(any());
    }

    @Test
    public void testContainsRole() {
        List<UserRole> roles = new ArrayList<>(Collections.singletonList(getChairpersonRole()));
        initService();

        boolean result = handler.containsRole(roles, CHAIRPERSON);
        assertTrue("This user is a chairperson ", result);
        result = handler.containsRole(roles, DATAOWNER);
        assertFalse("This user isn't a data owner", result);
    }

    @Test
    public void testContainsAnyRole() {
        initService();

        List<UserRole> roles = new ArrayList<>(Arrays.asList(getChairpersonRole(), getDataOwnerRole(), getAdminRole()));
        assertTrue("This user has admin role ", handler.containsAnyRole(roles, new String[]{ADMIN, RESEARCHER}));
        assertFalse("This user is not an alumni ", handler.containsAnyRole(roles, new String[]{ALUMNI, RESEARCHER}));
    }

    /**
     * Private helper methods
     **/

    private static final String CHAIRPERSON = UserRoles.CHAIRPERSON.getRoleName();
    private static final String MEMBER = UserRoles.MEMBER.getRoleName();
    private static final String ALUMNI = UserRoles.ALUMNI.getRoleName();
    private static final String DATAOWNER = UserRoles.DATAOWNER.getRoleName();
    private static final String RESEARCHER = UserRoles.RESEARCHER.getRoleName();
    private static final String ADMIN = UserRoles.ADMIN.getRoleName();

    private DataAccessRequest generateDar(DACUser user) {
        DataAccessRequest dar = new DataAccessRequest();
        DataAccessRequestData data = new DataAccessRequestData();
        data.setUserId(user.getDacUserId());
        dar.setReferenceId(UUID.randomUUID().toString());
        dar.setData(data);
        return dar;
    }

    private Vote generateDOVote(DACUser user) {
        Vote doVote = new Vote();
        doVote.setVote(true);
        doVote.setType(VoteType.DATA_OWNER.getValue());
        doVote.setDacUserId(user.getDacUserId());
        doVote.setElectionId(RandomUtils.nextInt(1, 100));
        return doVote;
    }

    private DACUser generateUser() {
        return new DACUser(
                RandomUtils.nextInt(1, 1000),
                RandomStringUtils.random(10, true, false),
                RandomStringUtils.random(10, true, false),
                new Date()
        );
    }

    private UserRole getMemberRole() {
        return new UserRole(1, MEMBER);
    }

    private UserRole getChairpersonRole() {
        return new UserRole(2, CHAIRPERSON);
    }

    private UserRole getAlumniRole() {
        return new UserRole(3, ALUMNI);
    }

    private UserRole getAdminRole() {
        return new UserRole(4, ADMIN);
    }

    private UserRole getResearcherRole() {
        return new UserRole(5, RESEARCHER);
    }

    private UserRole getDataOwnerRole() {
        return new UserRole(6, DATAOWNER);
    }

}