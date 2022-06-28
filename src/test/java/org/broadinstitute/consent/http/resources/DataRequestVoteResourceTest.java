package org.broadinstitute.consent.http.resources;

import org.broadinstitute.consent.http.WithLogHandler;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.enumeration.VoteType;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.models.Vote;
import org.broadinstitute.consent.http.service.DataAccessRequestService;
import org.broadinstitute.consent.http.service.DatasetAssociationService;
import org.broadinstitute.consent.http.service.DatasetService;
import org.broadinstitute.consent.http.service.ElectionService;
import org.broadinstitute.consent.http.service.EmailNotifierService;
import org.broadinstitute.consent.http.service.UserService;
import org.broadinstitute.consent.http.service.VoteService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

public class DataRequestVoteResourceTest implements WithLogHandler {
    @Mock
    private EmailNotifierService emailNotifierService;
    @Mock
    private UserService userService;
    @Mock
    private DatasetService datasetService;
    @Mock
    private DataAccessRequestService dataAccessRequestService;
    @Mock
    private DatasetAssociationService datasetAssociationService;
    @Mock
    private ElectionService electionService;
    @Mock
    private VoteService voteService;
    @Mock
    private UriInfo uriInfo;
    @Mock
    private UriBuilder uriBuilder;
    @Mock
    private AuthUser authUser;
    @Mock
    private Election election;
    @Mock
    private Dataset dataSet;

    private DataRequestVoteResource resource;

    private void initResource() {
        resource = new DataRequestVoteResource(
                dataAccessRequestService, datasetAssociationService,
                emailNotifierService, voteService,
                datasetService, electionService, userService
        );
    }

    private User createMockUser(UserRole role, Integer dacUserId) {
        User user = new User();
        user.setRoles(List.of(role));
        user.setUserId(dacUserId);
        return user;
    }

    private Vote createMockVote(String voteType, Integer dacUserId, boolean voteValue) {
        Vote vote = new Vote();
        vote.setType(voteType);
        vote.setDacUserId(dacUserId);
        vote.setVote(voteValue);
        return vote;
    }

    private DataAccessRequest createMockDAR() {
        DataAccessRequest dar = new DataAccessRequest();
        DataAccessRequestData darData = new DataAccessRequestData();
        darData.setDatasetIds(List.of(1));
        darData.setDarCode("");
        dar.setData(darData);
        dar.setReferenceId("");
        return dar;
    }

    private void enableCreateDataOwnerElection(Vote vote, User user, boolean returnEmptyList) throws Exception {
        vote.setElectionId(1);
        if (returnEmptyList) {
            when(voteService.describeVoteByTypeAndElectionId(any(), any())).thenReturn(Collections.emptyList());
        } else {
            when(voteService.describeVoteByTypeAndElectionId(any(), any())).thenReturn(List.of(vote));
        }
        when(dataSet.getDataSetId()).thenReturn(1);
        when(datasetService.findNeedsApprovalDataSetByObjectId(any())).thenReturn(List.of(dataSet));
        Map<User, List<Dataset>> dataOwnerDataSet = new HashMap<>();
        dataOwnerDataSet.put(user, List.of(dataSet));
        when(datasetAssociationService.findDataOwnersWithAssociatedDataSets(any())).thenReturn(dataOwnerDataSet);
        when(electionService.createDataSetElections(any(), any())).thenReturn(List.of(election));
        when(voteService.createDataOwnersReviewVotes(election)).thenReturn(List.of(vote));
        UserRole adminRole = new UserRole(UserRoles.ADMIN.getRoleId(), UserRoles.ADMIN.getRoleName());
        User adminUser = createMockUser(adminRole, 2);
        when(userService.describeAdminUsersThatWantToReceiveMails()).thenReturn(List.of(adminUser));
        doNothing().when(emailNotifierService).sendAdminFlaggedDarApproved(any(), any(), any());
    }

    @Before
    public void setUp() throws Exception {
        openMocks(this);
        when(uriInfo.getRequestUriBuilder()).thenReturn(uriBuilder);
        when(uriBuilder.path(anyString())).thenReturn(uriBuilder);
        String requestId = UUID.randomUUID().toString();
        String uri = String.format("http://localhost:8180/api/dataRequest/%s/vote", requestId);
        when(uriBuilder.build(anyString())).thenReturn(new URI(uri));
    }

    @Test
    public void testCreateDataRequestVoteSuccess() {
        UserRole role = new UserRole(UserRoles.RESEARCHER.getRoleId(), UserRoles.RESEARCHER.getRoleName());
        User user = createMockUser(role, 1);
        when(userService.findUserByEmail(any())).thenReturn(user);

        Vote vote = createMockVote("", 1, true);
        when(voteService.findVoteById(any())).thenReturn(vote);

        when(voteService.updateVoteById(any(), any())).thenReturn(vote);
        when(electionService.validateCollectDAREmailCondition(any())).thenReturn(false);

        initResource();
        Response response = resource.createDataRequestVote(authUser, uriInfo, "", 1, "");
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testCreateDataRequestVoteSuccessWithEmails() throws Exception {
        UserRole role = new UserRole(UserRoles.RESEARCHER.getRoleId(), UserRoles.RESEARCHER.getRoleName());
        User user = createMockUser(role, 1);
        when(userService.findUserByEmail(any())).thenReturn(user);

        Vote vote = createMockVote(VoteType.DATA_OWNER.getValue(), 1, true);
        when(voteService.findVoteById(any())).thenReturn(vote);

        when(voteService.updateVoteById(any(), any())).thenReturn(vote);
        when(electionService.validateCollectDAREmailCondition(any())).thenReturn(true);
        doNothing().when(emailNotifierService).sendCollectMessage(any());

        initResource();
        Response response = resource.createDataRequestVote(authUser, uriInfo, "", 1, "");
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testCreateDataRequestVoteSuccessAsAdmin() {
        UserRole role = new UserRole(UserRoles.ADMIN.getRoleId(), UserRoles.ADMIN.getRoleName());
        User user = createMockUser(role, 1);
        when(userService.findUserByEmail(any())).thenReturn(user);

        Vote vote = createMockVote("", 2, true);
        when(voteService.findVoteById(any())).thenReturn(vote);

        when(voteService.updateVoteById(any(), any())).thenReturn(vote);
        when(electionService.validateCollectDAREmailCondition(any())).thenReturn(false);

        initResource();
        Response response = resource.createDataRequestVote(authUser, uriInfo, "", 1, "");
        assertEquals(200, response.getStatus());
    }

    // The next 3 tests are for the private validateUserAndVoteId method.
    // They can be assumed to test all other uses of that method as well.

    @Test
    public void testCreateDataRequestVoteUserNotFound() {
        doThrow(new NotFoundException()).when(userService).findUserByEmail(any());

        initResource();
        Response response = resource.createDataRequestVote(authUser, uriInfo, "", 1, "");
        assertEquals(404, response.getStatus());
    }

    @Test
    public void testCreateDataRequestVoteVoteNotFound() {
        UserRole role = new UserRole(UserRoles.RESEARCHER.getRoleId(), UserRoles.RESEARCHER.getRoleName());
        User user = createMockUser(role, 1);
        when(userService.findUserByEmail(any())).thenReturn(user);

        doThrow(new NotFoundException()).when(voteService).findVoteById(any());

        initResource();
        Response response = resource.createDataRequestVote(authUser, uriInfo, "", 1, "");
        assertEquals(404, response.getStatus());
    }

    @Test
    public void testCreateDataRequestVoteNoPermission() {
        UserRole role = new UserRole(UserRoles.RESEARCHER.getRoleId(), UserRoles.RESEARCHER.getRoleName());
        User user = createMockUser(role, 1);
        when(userService.findUserByEmail(any())).thenReturn(user);

        Vote vote = createMockVote("", 2, true);
        when(voteService.findVoteById(any())).thenReturn(vote);

        initResource();
        Response response = resource.createDataRequestVote(authUser, uriInfo, "", 1, "");
        assertEquals(403, response.getStatus());
    }

    @Test
    public void testCreateDataRequestVoteEmailError() throws Exception {
        UserRole role = new UserRole(UserRoles.RESEARCHER.getRoleId(), UserRoles.RESEARCHER.getRoleName());
        User user = createMockUser(role, 1);
        when(userService.findUserByEmail(any())).thenReturn(user);

        Vote vote = createMockVote("", 1, true);
        when(voteService.findVoteById(any())).thenReturn(vote);

        LogHandler handler = createLogHandler(DataRequestVoteResource.class.getName());
        when(voteService.updateVoteById(any(), any())).thenReturn(vote);
        when(electionService.validateCollectDAREmailCondition(any())).thenReturn(true);
        doThrow(new IOException()).when(emailNotifierService).sendCollectMessage(any());

        initResource();
        Response response = resource.createDataRequestVote(authUser, uriInfo, "", 1, "");
        assertEquals(Level.SEVERE, handler.checkLevel());
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testCreateDataRequestVoteOtherError() {
        UserRole role = new UserRole(UserRoles.RESEARCHER.getRoleId(), UserRoles.RESEARCHER.getRoleName());
        User user = createMockUser(role, 1);
        when(userService.findUserByEmail(any())).thenReturn(user);

        Vote vote = createMockVote("", 1, true);
        when(voteService.findVoteById(any())).thenReturn(vote);

        doThrow(new RuntimeException()).when(voteService).updateVoteById(any(), any());

        initResource();
        Response response = resource.createDataRequestVote(authUser, uriInfo, "", 1, "");
        assertEquals(500, response.getStatus());
    }

    @Test
    public void testSubmitFinalAccessVoteSuccess() throws Exception {
        UserRole role = new UserRole(UserRoles.RESEARCHER.getRoleId(), UserRoles.RESEARCHER.getRoleName());
        User user = createMockUser(role, 1);
        when(userService.findUserByEmail(any())).thenReturn(user);

        Vote vote = createMockVote("", 1, true);
        when(voteService.findVoteById(any())).thenReturn(vote);

        when(electionService.submitFinalAccessVoteDataRequestElection(any(), any())).thenReturn(election);
        when(voteService.updateVoteById(any(), any())).thenReturn(vote);
        DataAccessRequest dar = createMockDAR();
        when(dataAccessRequestService.findByReferenceId(any())).thenReturn(dar);

        initResource();
        String json = "{\"electionId\":1,\"vote\":true}";
        Response response = resource.submitFinalAccessVote(authUser, "", 1, json);
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testSubmitFinalAccessVoteSuccessAsAdmin() throws Exception {
        UserRole role = new UserRole(UserRoles.ADMIN.getRoleId(), UserRoles.ADMIN.getRoleName());
        User user = createMockUser(role, 1);
        when(userService.findUserByEmail(any())).thenReturn(user);

        Vote vote = createMockVote("", 2, true);
        when(voteService.findVoteById(any())).thenReturn(vote);

        when(electionService.submitFinalAccessVoteDataRequestElection(any(), any())).thenReturn(election);
        when(voteService.updateVoteById(any(), any())).thenReturn(vote);
        DataAccessRequest dar = createMockDAR();
        when(dataAccessRequestService.findByReferenceId(any())).thenReturn(dar);

        initResource();
        String json = "{\"electionId\":1,\"vote\":true}";
        Response response = resource.submitFinalAccessVote(authUser, "", 1, json);
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testSubmitFinalAccessVoteSuccessFinalYes() throws Exception {
        UserRole role = new UserRole(UserRoles.RESEARCHER.getRoleId(), UserRoles.RESEARCHER.getRoleName());
        User user = createMockUser(role, 1);
        when(userService.findUserByEmail(any())).thenReturn(user);

        Vote vote = createMockVote(VoteType.FINAL.getValue(), 1, true);
        when(voteService.findVoteById(any())).thenReturn(vote);

        when(electionService.submitFinalAccessVoteDataRequestElection(any(), any())).thenReturn(election);
        when(voteService.updateVoteById(any(), any())).thenReturn(vote);
        DataAccessRequest dar = createMockDAR();
        when(dataAccessRequestService.findByReferenceId(any())).thenReturn(dar);

        enableCreateDataOwnerElection(vote, user, true);

        initResource();
        String json = "{\"electionId\":1,\"vote\":true}";
        Response response = resource.submitFinalAccessVote(authUser, "", 1, json);
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testSubmitFinalAccessVoteSuccessAgreementYes() throws Exception {
        UserRole role = new UserRole(UserRoles.RESEARCHER.getRoleId(), UserRoles.RESEARCHER.getRoleName());
        User user = createMockUser(role, 1);
        when(userService.findUserByEmail(any())).thenReturn(user);

        Vote vote = createMockVote(VoteType.AGREEMENT.getValue(), 1, true);
        when(voteService.findVoteById(any())).thenReturn(vote);

        when(electionService.submitFinalAccessVoteDataRequestElection(any(), any())).thenReturn(election);
        when(voteService.updateVoteById(any(), any())).thenReturn(vote);
        DataAccessRequest dar = createMockDAR();
        when(dataAccessRequestService.findByReferenceId(any())).thenReturn(dar);

        enableCreateDataOwnerElection(vote, user, false);

        initResource();
        String json = "{\"electionId\":1,\"vote\":true}";
        Response response = resource.submitFinalAccessVote(authUser, "", 1, json);
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testSubmitFinalAccessVoteError() throws Exception {
        UserRole role = new UserRole(UserRoles.RESEARCHER.getRoleId(), UserRoles.RESEARCHER.getRoleName());
        User user = createMockUser(role, 1);
        when(userService.findUserByEmail(any())).thenReturn(user);

        Vote vote = createMockVote("", 1, true);
        when(voteService.findVoteById(any())).thenReturn(vote);

        doThrow(new RuntimeException()).when(electionService).submitFinalAccessVoteDataRequestElection(any(), any());

        initResource();
        String json = "{\"electionId\":1,\"vote\":true}";
        Response response = resource.submitFinalAccessVote(authUser, "", 1, json);
        assertEquals(500, response.getStatus());
    }

    @Test
    public void testUpdateDataRequestVoteSuccess() {
        UserRole role = new UserRole(UserRoles.RESEARCHER.getRoleId(), UserRoles.RESEARCHER.getRoleName());
        User user = createMockUser(role, 1);
        when(userService.findUserByEmail(any())).thenReturn(user);

        Vote vote = createMockVote("", 1, true);
        when(voteService.findVoteById(any())).thenReturn(vote);
        when(voteService.updateVote(any(), any(), any())).thenReturn(vote);

        initResource();
        Response response = resource.updateDataRequestVote(authUser, "", 1, "");
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testUpdateDataRequestVoteError() {
        doThrow(new RuntimeException()).when(userService).findUserByEmail(any());

        initResource();
        Response response = resource.updateDataRequestVote(authUser, "", 1, "");
        assertEquals(500, response.getStatus());
    }

    @Test
    public void testDescribeSuccess() {
        Vote vote = createMockVote("", 1, true);
        when(voteService.findVoteById(any())).thenReturn(vote);

        initResource();
        Response response = resource.describe(authUser, "", 1);
        assertEquals(200, response.getStatus());
        assertEquals(vote, response.getEntity());
    }

    @Test
    public void testDescribeError() {
        doThrow(new RuntimeException()).when(voteService).findVoteById(any());

        initResource();
        Response response = resource.describe(authUser, "", 1);
        assertEquals(500, response.getStatus());
    }

    @Test
    public void testDescribeDataOwnerVoteSuccess() {
        Vote vote = createMockVote("", 1, true);
        when(voteService.describeDataOwnerVote(any(), any())).thenReturn(vote);

        initResource();
        Response response = resource.describeDataOwnerVote(authUser, "", 1);
        assertEquals(200, response.getStatus());
        assertEquals(vote, response.getEntity());
    }

    @Test
    public void testDescribeDataOwnerVoteError() {
        doThrow(new RuntimeException()).when(voteService).describeDataOwnerVote(any(), any());

        initResource();
        Response response = resource.describeDataOwnerVote(authUser, "", 1);
        assertEquals(500, response.getStatus());
    }

    @Test
    public void testDescribeAllVotesSuccess() {
        Vote vote = createMockVote("", 1, true);
        when(voteService.describeVotes(any())).thenReturn(List.of(vote));

        initResource();
        Response response = resource.describeAllVotes(authUser, "");
        assertEquals(200, response.getStatus());
        assertEquals(List.of(vote), response.getEntity());
    }

    @Test
    public void testDescribeAllVotesError() {
        doThrow(new RuntimeException()).when(voteService).describeVotes(any());

        initResource();
        Response response = resource.describeAllVotes(authUser, "");
        assertEquals(500, response.getStatus());
    }

    @Test
    public void testDeleteVoteSuccess() {
        UserRole role = new UserRole(UserRoles.ADMIN.getRoleId(), UserRoles.ADMIN.getRoleName());
        User user = createMockUser(role, 1);
        when(userService.findUserByEmail(any())).thenReturn(user);

        Vote vote = createMockVote("", 1, true);
        when(voteService.findVoteById(any())).thenReturn(vote);

        doNothing().when(voteService).deleteVote(any(), any());

        initResource();
        Response response = resource.deleteVote(authUser, "", 1);
        assertEquals(200, response.getStatus());
        assertEquals("Vote was deleted", response.getEntity());
    }

    @Test
    public void testDeleteVoteError() {
        UserRole role = new UserRole(UserRoles.ADMIN.getRoleId(), UserRoles.ADMIN.getRoleName());
        User user = createMockUser(role, 1);
        when(userService.findUserByEmail(any())).thenReturn(user);

        Vote vote = createMockVote("", 1, true);
        when(voteService.findVoteById(any())).thenReturn(vote);

        doThrow(new RuntimeException()).when(voteService).deleteVote(any(), any());

        initResource();
        Response response = resource.deleteVote(authUser, "", 1);
        assertEquals(500, response.getStatus());
    }

    @Test
    public void testDeleteVotesSuccess() throws Exception {
        doNothing().when(voteService).deleteVotes(any());

        initResource();
        Response response = resource.deleteVotes(authUser, "");
        assertEquals(200, response.getStatus());
        assertEquals("Votes for specified id have been deleted", response.getEntity());
    }

    @Test
    public void testDeleteVotesBadRequest() {
        initResource();
        Response response = resource.deleteVotes(authUser, null);
        assertEquals(400, response.getStatus());
    }

    @Test
    public void testDeleteVotesServiceError() throws Exception {
        doThrow(new RuntimeException()).when(voteService).deleteVotes(any());

        initResource();
        Response response = resource.deleteVotes(authUser, "");
        assertEquals(500, response.getStatus());
    }
}
