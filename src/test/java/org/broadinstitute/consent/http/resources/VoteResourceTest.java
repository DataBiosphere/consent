package org.broadinstitute.consent.http.resources;

import com.google.api.client.http.HttpStatusCodes;
import com.google.gson.Gson;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.Vote;
import org.broadinstitute.consent.http.models.VoteUpdateInfo;
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
    Response response = resource.updateVotes2(authUser, "{\"vote\": true, \"ID\":12345}");
    assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
  }

  @Test
  public void testUpdateVotes_noIds() {
    initResource();
    VoteUpdateInfo voteUpdateInfo = new VoteUpdateInfo(true, "example", new ArrayList<>());
    Response response = resource.updateVotes2(authUser, gson.toJson(voteUpdateInfo, VoteUpdateInfo.class));
    assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
  }


  @Test
  public void testUpdateVotes_noVotesForIds() {
    when(userService.findUserByEmail(any())).thenReturn(user);
    when(voteService.findVotesByIds(any())).thenReturn(Collections.emptyList());
    initResource();

    VoteUpdateInfo voteUpdateInfo = new VoteUpdateInfo(true, "example", List.of(1));
    Response response = resource.updateVotes2(authUser, gson.toJson(voteUpdateInfo, VoteUpdateInfo.class));
    assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
  }

  @Test
  public void testUpdateVotes_invalidUser() {
    user.setDacUserId(1);
    vote.setDacUserId(2);
    when(userService.findUserByEmail(any())).thenReturn(user);
    when(voteService.findVotesByIds(any())).thenReturn(List.of(vote));
    initResource();

    VoteUpdateInfo voteUpdateInfo = new VoteUpdateInfo(true, "example", List.of(1, 2, 3));
    Response response = resource.updateVotes2(authUser, gson.toJson(voteUpdateInfo, VoteUpdateInfo.class));
    assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
  }

  @Test
  public void testUpdateVotes_closedElection() {
    user.setDacUserId(1);
    vote.setDacUserId(2);
    when(userService.findUserByEmail(any())).thenReturn(user);
    doThrow(new IllegalArgumentException()).when(voteService).findVotesByIds(any());
    initResource();

    VoteUpdateInfo voteUpdateInfo = new VoteUpdateInfo(true, "example", List.of(1, 2, 3));
    Response response = resource.updateVotes2(authUser, gson.toJson(voteUpdateInfo, VoteUpdateInfo.class));
    assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
  }

  @Test
  public void testUpdateVotes() {
    user.setDacUserId(1);
    vote.setDacUserId(1);
    when(userService.findUserByEmail(any())).thenReturn(user);
    when(voteService.findVotesByIds(any())).thenReturn(List.of(vote));
    initResource();

    VoteUpdateInfo voteUpdateInfo = new VoteUpdateInfo(true, "example", List.of(1, 2, 3));
    Response response = resource.updateVotes2(authUser, gson.toJson(voteUpdateInfo, VoteUpdateInfo.class));
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }
}
