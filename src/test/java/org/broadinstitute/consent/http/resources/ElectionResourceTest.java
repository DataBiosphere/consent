package org.broadinstitute.consent.http.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import com.google.api.client.http.HttpStatusCodes;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import java.util.Collections;
import java.util.UUID;
import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.service.ElectionService;
import org.broadinstitute.consent.http.service.VoteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

@SuppressWarnings("FieldCanBeLocal")
public class ElectionResourceTest {

  private final int OK = HttpStatusCodes.STATUS_CODE_OK;
  private final int NOT_FOUND = HttpStatusCodes.STATUS_CODE_NOT_FOUND;

  private final AuthUser authUser = new AuthUser("test@test.com");

  @Mock
  VoteService voteService;

  @Mock
  ElectionService electionService;

  private ElectionResource electionResource;

  @BeforeEach
  public void setUp() {
    openMocks(this);
    when(voteService.findVotesByReferenceId(any())).thenReturn(Collections.emptyList());
    doNothing().when(voteService).advanceVotes(any(), anyBoolean(), anyString());
    when(electionService.checkDataOwnerToCloseElection(any())).thenReturn(false);
    doNothing().when(electionService).closeDataOwnerApprovalElection(any());
    when(electionService.updateElectionById(any(), any())).thenReturn(new Election());
    when(electionService.describeElectionById(any())).thenReturn(new Election());
    when(electionService.describeElectionByVoteId(any())).thenReturn(new Election());
    electionResource = new ElectionResource(voteService, electionService);
  }

  @Test
  public void testAdvanceElection() {
    String referenceId = UUID.randomUUID().toString();
    Response response = electionResource.advanceElection(referenceId, "Yes");
    assertEquals(OK, response.getStatus());
  }

  @Test
  public void testAdvanceElectionError() {
    when(voteService.findVotesByReferenceId(anyString())).thenThrow(new NotFoundException());
    electionResource = new ElectionResource(voteService, electionService);
    String referenceId = UUID.randomUUID().toString();
    Response response = electionResource.advanceElection(referenceId, "Yes");
    assertEquals(NOT_FOUND, response.getStatus());
  }

  @Test
  public void testUpdateElection() {
    Response response = electionResource.updateElection(new Election(), randomInt());
    assertEquals(OK, response.getStatus());
  }

  @Test
  public void testUpdateElectionError() {
    when(electionService.updateElectionById(any(), anyInt())).thenThrow(new NotFoundException());
    electionResource = new ElectionResource(voteService, electionService);
    Response response = electionResource.updateElection(new Election(), randomInt());
    assertEquals(NOT_FOUND, response.getStatus());
  }

  @Test
  public void testDescribeElectionById() {
    Response response = electionResource.describeElectionById(randomInt());
    assertEquals(OK, response.getStatus());
  }

  @Test
  public void testDescribeElectionByIdError() {
    when(electionService.describeElectionById(anyInt())).thenThrow(new NotFoundException());
    electionResource = new ElectionResource(voteService, electionService);
    Response response = electionResource.describeElectionById(randomInt());
    assertEquals(NOT_FOUND, response.getStatus());
  }

  @Test
  public void testDescribeElectionByVoteId() {
    Response response = electionResource.describeElectionByVoteId(randomInt());
    assertEquals(OK, response.getStatus());
  }

  @Test
  public void testDescribeElectionByVoteIdError() {
    when(electionService.describeElectionByVoteId(anyInt())).thenThrow(new NotFoundException());
    electionResource = new ElectionResource(voteService, electionService);
    Response response = electionResource.describeElectionByVoteId(randomInt());
    assertEquals(NOT_FOUND, response.getStatus());
  }

  @Test
  public void testDescribeVotesOnElection() {
    Response response = electionResource.describeVotesOnElection(authUser, randomInt());
    assertEquals(OK, response.getStatus());
  }

  @Test
  public void testDescribeVotesOnElectionError() {
    when(voteService.findVotesByElectionId(any())).thenThrow(new NotFoundException());
    electionResource = new ElectionResource(voteService, electionService);
    Response response = electionResource.describeVotesOnElection(authUser, any());
    assertEquals(NOT_FOUND, response.getStatus());
  }

  private static int randomInt() {
    return RandomUtils.nextInt(1, 10);
  }

}
