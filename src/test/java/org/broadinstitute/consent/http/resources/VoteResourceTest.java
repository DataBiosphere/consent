package org.broadinstitute.consent.http.resources;

import com.google.api.client.http.HttpStatusCodes;
import com.google.gson.Gson;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.Vote;
import org.broadinstitute.consent.http.service.UserService;
import org.broadinstitute.consent.http.service.VoteService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

public class VoteResourceTest {

  @Mock private UserService userService;

  @Mock private VoteService voteService;

  @Mock private AuthUser authUser;

  private final User user = new User();

  private final Vote vote = new Vote();

  private VoteResource resource;

  private final Gson gson = new Gson();

  @Before
  public void setUp() {
    openMocks(this);
  }

  private void initResource() {
    resource = new VoteResource(userService, voteService);
  }

  @Test
  public void testUpdateVotes_invalidJson() {
    initResource();
    Response response = resource.updateVotes(authUser, "{\"vote\": true, \"ID\":12345}");
    assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
  }

  @Test
  public void testUpdateVotes_nullIds() {
    initResource();
    Vote.VoteUpdate voteUpdate = new Vote.VoteUpdate();
    voteUpdate.setVote(true);
    voteUpdate.setRationale("example");

    Response response = resource.updateVotes(authUser, gson.toJson(voteUpdate, Vote.VoteUpdate.class));
    assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
  }

  @Test
  public void testUpdateVotes_noIds() {
    initResource();
    Vote.VoteUpdate voteUpdate = new Vote.VoteUpdate(true, "example", new ArrayList<>());
    Response response = resource.updateVotes(authUser, gson.toJson(voteUpdate, Vote.VoteUpdate.class));
    assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
  }


  @Test
  public void testUpdateVotes_noVotesForIds() {
    when(userService.findUserByEmail(any())).thenReturn(user);
    when(voteService.findVotesByIds(any())).thenReturn(Collections.emptyList());
    initResource();

    Vote.VoteUpdate voteUpdate = new Vote.VoteUpdate(true, "example", List.of(1));
    Response response = resource.updateVotes(authUser, gson.toJson(voteUpdate, Vote.VoteUpdate.class));
    assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
  }

  @Test
  public void testUpdateVotes_noVoteValue() {
    when(userService.findUserByEmail(any())).thenReturn(user);
    when(voteService.findVotesByIds(any())).thenReturn(List.of(vote));
    initResource();

    Vote.VoteUpdate voteUpdate = new Vote.VoteUpdate();
    voteUpdate.setRationale("example");
    voteUpdate.setVoteIds(List.of(1, 2, 3));

    Response response = resource.updateVotes(authUser, gson.toJson(voteUpdate, Vote.VoteUpdate.class));
    assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
  }

  @Test
  public void testUpdateVotes_invalidUser() {
    user.setDacUserId(1);
    vote.setDacUserId(2);
    when(userService.findUserByEmail(any())).thenReturn(user);
    when(voteService.findVotesByIds(any())).thenReturn(List.of(vote));
    initResource();

    Vote.VoteUpdate voteUpdate = new Vote.VoteUpdate(true, "example", List.of(1, 2, 3));
    Response response = resource.updateVotes(authUser, gson.toJson(voteUpdate, Vote.VoteUpdate.class));
    assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
  }

  @Test
  public void testUpdateVotes_closedElection() {
    user.setDacUserId(1);
    vote.setDacUserId(1);
    when(userService.findUserByEmail(any())).thenReturn(user);
    doThrow(new IllegalArgumentException()).when(voteService).findVotesByIds(any());
    initResource();

    Vote.VoteUpdate voteUpdate = new Vote.VoteUpdate(true, "example", List.of(1, 2, 3));
    Response response = resource.updateVotes(authUser, gson.toJson(voteUpdate, Vote.VoteUpdate.class));
    assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
  }

  @Test
  public void testUpdateVotesTrue() {
    user.setDacUserId(1);
    vote.setDacUserId(1);
    when(userService.findUserByEmail(any())).thenReturn(user);
    when(voteService.findVotesByIds(any())).thenReturn(List.of(vote));
    initResource();

    Vote.VoteUpdate voteUpdate = new Vote.VoteUpdate(true, "example", List.of(1, 2, 3));
    Response response = resource.updateVotes(authUser, gson.toJson(voteUpdate, Vote.VoteUpdate.class));
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }

  @Test
  public void testUpdateVotesFalse() {
    user.setDacUserId(1);
    vote.setDacUserId(1);
    when(userService.findUserByEmail(any())).thenReturn(user);
    when(voteService.findVotesByIds(any())).thenReturn(List.of(vote));
    initResource();

    Vote.VoteUpdate voteUpdate = new Vote.VoteUpdate(false, "example", List.of(1, 2, 3));
    Response response = resource.updateVotes(authUser, gson.toJson(voteUpdate, Vote.VoteUpdate.class));
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }

  @Test
  public void testUpdateVotes_noRationale() {
    user.setDacUserId(1);
    vote.setDacUserId(1);
    when(userService.findUserByEmail(any())).thenReturn(user);
    when(voteService.findVotesByIds(any())).thenReturn(List.of(vote));
    initResource();

    Vote.VoteUpdate voteUpdate = new Vote.VoteUpdate();
    voteUpdate.setVote(false);
    voteUpdate.setVoteIds(List.of(1, 2, 3));

    Response response = resource.updateVotes(authUser, gson.toJson(voteUpdate, Vote.VoteUpdate.class));
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }
}
