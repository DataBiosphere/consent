package org.broadinstitute.consent.http.resources;

import com.google.api.client.http.HttpStatusCodes;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.Vote;
import org.broadinstitute.consent.http.service.UserService;
import org.broadinstitute.consent.http.service.VoteService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

public class VoteResourceTest {

  @Mock private UserService userService;

  @Mock private VoteService voteService;

  @Mock private AuthUser authUser;

  private User user = new User();

  private Vote vote = new Vote();

  private VoteResource resource;

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
    Response response = resource.updateVotes(authUser, true, "rationale", "{\"ID\":12345}");
    assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
  }

  @Test
  public void testUpdateVotes_noIds() {
    initResource();
    Response response = resource.updateVotes(authUser, true, "rationale", "[]");
    assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
  }

  @Test
  public void testUpdateVotes_noVotesForIds() {
    when(userService.findUserByEmail(any())).thenReturn(user);
    when(voteService.findVotesByIds(any())).thenReturn(Collections.emptyList());
    initResource();
    Response response = resource.updateVotes(authUser, true, "rationale", "[1]");
    assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
  }

  @Test
  public void testUpdateVotes_invalidUser() {
    user.setDacUserId(1);
    vote.setDacUserId(2);
    when(userService.findUserByEmail(any())).thenReturn(user);
    when(voteService.findVotesByIds(any())).thenReturn(List.of(vote));
    initResource();

    Response response = resource.updateVotes(authUser, true, "rationale", "[1, 2, 3]");
    assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
  }

  @Test
  public void testUpdateVotes() {
    user.setDacUserId(1);
    vote.setDacUserId(1);
    when(userService.findUserByEmail(any())).thenReturn(user);
    when(voteService.findVotesByIds(any())).thenReturn(List.of(vote));
    initResource();

    Response response = resource.updateVotes(authUser, true, "rationale", "[1, 2, 3]");
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }
}
