package org.broadinstitute.consent.http.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import com.google.api.client.http.HttpStatusCodes;
import com.google.gson.Gson;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.Vote;
import org.broadinstitute.consent.http.service.ElectionService;
import org.broadinstitute.consent.http.service.UserService;
import org.broadinstitute.consent.http.service.VoteService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VoteResourceTest {

  @Mock
  private UserService userService;

  @Mock
  private VoteService voteService;

  @Mock
  private ElectionService electionService;

  @Mock
  private AuthUser authUser;

  private final User user = new User();

  private final Vote vote = new Vote();

  private VoteResource resource;

  private final Gson gson = new Gson();

  private void initResource() {
    resource = new VoteResource(userService, voteService, electionService);
  }

  @Test
  void testUpdateVotes_invalidJson() {
    initResource();
    Response response = resource.updateVotes(authUser, "{\"vote\": true, \"ID\":12345}");
    assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
  }

  @Test
  void testUpdateVotes_nullIds() {
    initResource();
    Vote.VoteUpdate voteUpdate = new Vote.VoteUpdate();
    voteUpdate.setVote(true);
    voteUpdate.setRationale("example");

    Response response = resource.updateVotes(authUser,
        gson.toJson(voteUpdate, Vote.VoteUpdate.class));
    assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
  }

  @Test
  void testUpdateVotes_noIds() {
    initResource();
    Vote.VoteUpdate voteUpdate = new Vote.VoteUpdate(true, "example", new ArrayList<>());
    Response response = resource.updateVotes(authUser,
        gson.toJson(voteUpdate, Vote.VoteUpdate.class));
    assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
  }


  @Test
  void testUpdateVotes_noVotesForIds() {
    when(voteService.findVotesByIds(any())).thenReturn(Collections.emptyList());
    initResource();

    Vote.VoteUpdate voteUpdate = new Vote.VoteUpdate(true, "example", List.of(1));
    Response response = resource.updateVotes(authUser,
        gson.toJson(voteUpdate, Vote.VoteUpdate.class));
    assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
  }

  @Test
  void testUpdateVotes_noVoteValue() {
    initResource();

    Vote.VoteUpdate voteUpdate = new Vote.VoteUpdate();
    voteUpdate.setRationale("example");
    voteUpdate.setVoteIds(List.of(1, 2, 3));

    Response response = resource.updateVotes(authUser,
        gson.toJson(voteUpdate, Vote.VoteUpdate.class));
    assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
  }

  @Test
  void testUpdateVotes_invalidUser() {
    user.setUserId(1);
    vote.setUserId(2);
    when(userService.findUserByEmail(any())).thenReturn(user);
    when(voteService.findVotesByIds(any())).thenReturn(List.of(vote));
    initResource();

    Vote.VoteUpdate voteUpdate = new Vote.VoteUpdate(true, "example", List.of(1, 2, 3));
    Response response = resource.updateVotes(authUser,
        gson.toJson(voteUpdate, Vote.VoteUpdate.class));
    assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
  }

  @Test
  void testUpdateVotes_closedElection() {
    user.setUserId(1);
    vote.setUserId(1);
    doThrow(new IllegalArgumentException()).when(voteService).findVotesByIds(any());
    initResource();

    Vote.VoteUpdate voteUpdate = new Vote.VoteUpdate(true, "example", List.of(1, 2, 3));
    Response response = resource.updateVotes(authUser,
        gson.toJson(voteUpdate, Vote.VoteUpdate.class));
    assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
  }

  @Test
  void testUpdateVotes_allMemberVotes() {
    user.setUserId(1);
    vote.setUserId(1);
    vote.setType("DAC");
    vote.setVote(true);
    Vote voteTwo = new Vote();
    voteTwo.setVote(true);
    voteTwo.setType("DAC");
    voteTwo.setUserId(1);
    Vote.VoteUpdate voteUpdate = new Vote.VoteUpdate(true, "example", List.of(1, 2, 3));
    when(voteService.findVotesByIds(anyList())).thenReturn(List.of(vote, voteTwo));
    when(userService.findUserByEmail(any())).thenReturn(user);
    when(voteService.updateVotesWithValue(anyList(), anyBoolean(), anyString()))
        .thenReturn(List.of(vote));
    initResource();

    Response response = resource.updateVotes(authUser,
        gson.toJson(voteUpdate, Vote.VoteUpdate.class));
    assertEquals(Status.OK.getStatusCode(), response.getStatus());
  }

  @Test
  void testUpdateVotes_allYes_allRP() {
    user.setUserId(1);
    vote.setUserId(1);
    vote.setType("Chairperson");
    vote.setVote(true);
    Vote voteTwo = new Vote();
    voteTwo.setVote(true);
    voteTwo.setType("Chairperson");
    voteTwo.setUserId(1);
    Election election = new Election();
    Election electionTwo = new Election();
    election.setElectionType("RP");
    electionTwo.setElectionType("RP");
    Vote.VoteUpdate voteUpdate = new Vote.VoteUpdate(true, "example", List.of(1, 2, 3));
    when(voteService.findVotesByIds(anyList())).thenReturn(List.of(vote, voteTwo));
    when(userService.findUserByEmail(any())).thenReturn(user);
    when(voteService.updateVotesWithValue(anyList(), anyBoolean(), anyString()))
        .thenReturn(List.of(vote));

    initResource();

    Response response = resource.updateVotes(authUser,
        gson.toJson(voteUpdate, Vote.VoteUpdate.class));
    assertEquals(Status.OK.getStatusCode(), response.getStatus());
  }

  @Test
  void testUpdateVotes_allNo_allRP() {
    user.setUserId(1);
    vote.setUserId(1);
    vote.setType("Chairperson");
    vote.setVote(false);
    Vote voteTwo = new Vote();
    voteTwo.setVote(false);
    voteTwo.setType("Chairperson");
    voteTwo.setUserId(1);
    Vote.VoteUpdate voteUpdate = new Vote.VoteUpdate(false, "example", List.of(1, 2, 3));
    when(voteService.findVotesByIds(anyList())).thenReturn(List.of(vote, voteTwo));
    when(userService.findUserByEmail(any())).thenReturn(user);
    when(voteService.updateVotesWithValue(anyList(), anyBoolean(), anyString()))
        .thenReturn(List.of(vote));
    initResource();

    Response response = resource.updateVotes(authUser,
        gson.toJson(voteUpdate, Vote.VoteUpdate.class));
    assertEquals(Status.OK.getStatusCode(), response.getStatus());
  }

  @Test
  void testUpdateVotes_allYes_allDataAccess_AllCards() {
    user.setUserId(1);
    vote.setUserId(1);
    vote.setVoteId(1);
    vote.setType("Chairperson");
    vote.setVote(true);
    Vote voteTwo = new Vote();
    voteTwo.setType("Chairperson");
    voteTwo.setVoteId(2);
    voteTwo.setUserId(1);
    voteTwo.setVote(true);
    Election election = new Election();
    election.setElectionId(1);
    Vote.VoteUpdate voteUpdate = new Vote.VoteUpdate(true, "example", List.of(1, 2, 3));
    when(voteService.findVotesByIds(anyList())).thenReturn(List.of(vote, voteTwo));
    when(userService.findUserByEmail(any())).thenReturn(user);
    when(voteService.updateVotesWithValue(anyList(), anyBoolean(), anyString()))
        .thenReturn(List.of(vote, voteTwo));
    when(electionService.findElectionsByVoteIdsAndType(anyList(), anyString()))
        .thenReturn(List.of(election));
    when(electionService.findElectionsWithCardHoldingUsersByElectionIds(anyList()))
        .thenReturn(List.of(election));

    initResource();

    Response response = resource.updateVotes(authUser,
        gson.toJson(voteUpdate, Vote.VoteUpdate.class));
    assertEquals(Status.OK.getStatusCode(), response.getStatus());
  }

  @Test
  void testUpdateVotes_allNo_allDataAccess_AllCards() {
    user.setUserId(1);
    vote.setUserId(1);
    vote.setVoteId(1);
    vote.setType("Chairperson");
    vote.setVote(false);
    Vote voteTwo = new Vote();
    voteTwo.setType("Chairperson");
    voteTwo.setVoteId(2);
    voteTwo.setUserId(1);
    voteTwo.setVote(false);
    Election election = new Election();
    election.setElectionId(1);
    Vote.VoteUpdate voteUpdate = new Vote.VoteUpdate(false, "example", List.of(1, 2, 3));
    when(voteService.findVotesByIds(anyList())).thenReturn(List.of(vote, voteTwo));
    when(userService.findUserByEmail(any())).thenReturn(user);
    when(voteService.updateVotesWithValue(anyList(), anyBoolean(), anyString()))
        .thenReturn(List.of(vote, voteTwo));
    initResource();

    Response response = resource.updateVotes(authUser,
        gson.toJson(voteUpdate, Vote.VoteUpdate.class));
    assertEquals(Status.OK.getStatusCode(), response.getStatus());
  }

  @Test
  void testUpdateVotes_allYes_allDataAccess_NotAllCards() {
    user.setUserId(1);
    vote.setUserId(1);
    vote.setVoteId(1);
    vote.setType("Chairperson");
    vote.setVote(true);
    Vote voteTwo = new Vote();
    voteTwo.setType("Chairperson");
    voteTwo.setVoteId(2);
    voteTwo.setUserId(1);
    voteTwo.setVote(true);
    Election election = new Election();
    election.setElectionId(1);
    Election electionTwo = new Election();
    election.setElectionId(2);
    Vote.VoteUpdate voteUpdate = new Vote.VoteUpdate(true, "example", List.of(1, 2, 3));
    when(voteService.findVotesByIds(anyList())).thenReturn(List.of(vote, voteTwo));
    when(userService.findUserByEmail(any())).thenReturn(user);
    when(electionService.findElectionsByVoteIdsAndType(anyList(), anyString()))
        .thenReturn(List.of(election, electionTwo));
    when(electionService.findElectionsWithCardHoldingUsersByElectionIds(anyList()))
        .thenReturn(List.of(election));
    initResource();

    Response response = resource.updateVotes(authUser,
        gson.toJson(voteUpdate, Vote.VoteUpdate.class));
    assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
  }

  @Test
  void testUpdateVotes_noRationale() {
    user.setUserId(1);
    vote.setUserId(1);
    when(userService.findUserByEmail(any())).thenReturn(user);
    when(voteService.findVotesByIds(any())).thenReturn(List.of(vote));
    initResource();

    Vote.VoteUpdate voteUpdate = new Vote.VoteUpdate();
    voteUpdate.setVote(false);
    voteUpdate.setVoteIds(List.of(1, 2, 3));

    Response response = resource.updateVotes(authUser,
        gson.toJson(voteUpdate, Vote.VoteUpdate.class));
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }

  @Test
  void testUpdateVoteRationale_InvalidJson() {
    String invalidJson = "[]";
    initResource();

    Response response = resource.updateVoteRationale(authUser, invalidJson);
    assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
  }

  @Test
  void testUpdateVoteRationale_EmptyVoteIds() {
    Vote.RationaleUpdate update = new Vote.RationaleUpdate();
    update.setRationale("Rationale");
    initResource();

    Response response = resource.updateVoteRationale(authUser, new Gson().toJson(update));
    assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
  }

  @Test
  void testUpdateVoteRationale_NoVotesFound() {
    user.setUserId(1);
    when(userService.findUserByEmail(any())).thenReturn(user);
    when(voteService.findVotesByIds(any())).thenReturn(List.of());
    Vote.RationaleUpdate update = new Vote.RationaleUpdate();
    update.setVoteIds(List.of(1));
    update.setRationale("Rationale");
    initResource();

    Response response = resource.updateVoteRationale(authUser, new Gson().toJson(update));
    assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
  }

  @Test
  void testUpdateVoteRationale_UserNotOwnerOfVotes() {
    user.setUserId(1);
    vote.setUserId(2);
    when(userService.findUserByEmail(any())).thenReturn(user);
    when(voteService.findVotesByIds(any())).thenReturn(List.of(vote));
    Vote.RationaleUpdate update = new Vote.RationaleUpdate();
    update.setVoteIds(List.of(1));
    update.setRationale("Rationale");
    initResource();

    Response response = resource.updateVoteRationale(authUser, new Gson().toJson(update));
    assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
  }

  @Test
  void testUpdateVoteRationale_Success() {
    user.setUserId(1);
    vote.setUserId(1);
    when(userService.findUserByEmail(any())).thenReturn(user);
    when(voteService.findVotesByIds(any())).thenReturn(List.of(vote));
    when(voteService.updateRationaleByVoteIds(any(), any())).thenReturn(List.of(vote));
    Vote.RationaleUpdate update = new Vote.RationaleUpdate();
    update.setVoteIds(List.of(1));
    update.setRationale("Rationale");
    initResource();

    Response response = resource.updateVoteRationale(authUser, new Gson().toJson(update));
    assertEquals(Status.OK.getStatusCode(), response.getStatus());
  }
}
