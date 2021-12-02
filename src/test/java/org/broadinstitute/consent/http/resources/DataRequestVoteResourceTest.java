package org.broadinstitute.consent.http.resources;

import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.enumeration.VoteType;
import org.broadinstitute.consent.http.models.AuthUser;
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
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

public class DataRequestVoteResourceTest {
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
        user.setDacUserId(dacUserId);
        return user;
    }

    private Vote createMockVote(String voteType, Integer dacUserId) {
        Vote vote = new Vote();
        vote.setType(voteType);
        vote.setDacUserId(dacUserId);
        return vote;
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

        Vote vote = createMockVote("", 1);
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

        Vote vote = createMockVote(VoteType.DATA_OWNER.getValue(), 1);
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

        Vote vote = createMockVote("", 2);
        when(voteService.findVoteById(any())).thenReturn(vote);

        when(voteService.updateVoteById(any(), any())).thenReturn(vote);
        when(electionService.validateCollectDAREmailCondition(any())).thenReturn(false);

        initResource();
        Response response = resource.createDataRequestVote(authUser, uriInfo, "", 1, "");
        assertEquals(200, response.getStatus());
    }

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

        Vote vote = createMockVote("", 2);
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

        Vote vote = createMockVote(VoteType.DATA_OWNER.getValue(), 1);
        when(voteService.findVoteById(any())).thenReturn(vote);

        when(voteService.updateVoteById(any(), any())).thenReturn(vote);
        when(electionService.validateCollectDAREmailCondition(any())).thenReturn(true);
        doThrow(new IOException()).when(emailNotifierService).sendCollectMessage(any());
        //TODO: Check logger to verify "severe" status has been posted (issue DUOS-1515)

        initResource();
        Response response = resource.createDataRequestVote(authUser, uriInfo, "", 1, "");
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testCreateDataRequestVoteOtherError() {
        UserRole role = new UserRole(UserRoles.RESEARCHER.getRoleId(), UserRoles.RESEARCHER.getRoleName());
        User user = createMockUser(role, 1);
        when(userService.findUserByEmail(any())).thenReturn(user);

        Vote vote = createMockVote("", 1);
        when(voteService.findVoteById(any())).thenReturn(vote);

        doThrow(new RuntimeException()).when(voteService).updateVoteById(any(), any());

        initResource();
        Response response = resource.createDataRequestVote(authUser, uriInfo, "", 1, "");
        assertEquals(500, response.getStatus());
    }
}
