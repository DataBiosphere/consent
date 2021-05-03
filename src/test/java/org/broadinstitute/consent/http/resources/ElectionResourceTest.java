package org.broadinstitute.consent.http.resources;

import com.google.api.client.http.HttpStatusCodes;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.service.ElectionService;
import org.broadinstitute.consent.http.service.VoteService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@SuppressWarnings("FieldCanBeLocal")
public class ElectionResourceTest {

    private final int OK = HttpStatusCodes.STATUS_CODE_OK;
    private final int NOT_FOUND = HttpStatusCodes.STATUS_CODE_NOT_FOUND;
    private final int ERROR = HttpStatusCodes.STATUS_CODE_SERVER_ERROR;

    @Mock
    VoteService voteService;

    @Mock
    ElectionService electionService;

    private ElectionResource electionResource;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(voteService.findVotesByReferenceId(any())).thenReturn(Collections.emptyList());
        doNothing().when(voteService).advanceVotes(any(), anyBoolean(), anyString());
        when(electionService.checkDataOwnerToCloseElection(any())).thenReturn(false);
        doNothing().when(electionService).closeDataOwnerApprovalElection(any());
        when(electionService.updateElectionById(any(), any())).thenReturn(new Election());
        when(electionService.describeElectionById(any())).thenReturn(new Election());
        when(electionService.describeElectionByVoteId(any())).thenReturn(new Election());
        when(electionService.isDataSetElectionOpen()).thenReturn(true);
        when(electionService.getConsentElectionByDARElectionId(any())).thenReturn(new Election());
        electionResource = new ElectionResource(voteService, electionService);
    }

    @Test
    public void testAdvanceElection() {
        String referenceId = RandomStringUtils.random(10);
        Response response = electionResource.advanceElection(referenceId, "Yes");
        Assert.assertEquals(OK, response.getStatus());
    }

    @Test
    public void testAdvanceElectionError() {
        when(voteService.findVotesByReferenceId(anyString())).thenThrow(new NotFoundException());
        electionResource = new ElectionResource(voteService, electionService);
        String referenceId = RandomStringUtils.random(10);
        Response response = electionResource.advanceElection(referenceId, "Yes");
        Assert.assertEquals(NOT_FOUND, response.getStatus());
    }

    @Test
    public void testUpdateElection() {
        Response response = electionResource.updateElection(new Election(), randomInt());
        Assert.assertEquals(OK, response.getStatus());
    }

    @Test
    public void testUpdateElectionError() {
        when(electionService.updateElectionById(any(), anyInt())).thenThrow(new NotFoundException());
        electionResource = new ElectionResource(voteService, electionService);
        Response response = electionResource.updateElection(new Election(), randomInt());
        Assert.assertEquals(NOT_FOUND, response.getStatus());
    }

    @Test
    public void testDescribeElectionById() {
        Response response = electionResource.describeElectionById(randomInt());
        Assert.assertEquals(OK, response.getStatus());
    }

    @Test
    public void testDescribeElectionByIdError() {
        when(electionService.describeElectionById(anyInt())).thenThrow(new NotFoundException());
        electionResource = new ElectionResource(voteService, electionService);
        Response response = electionResource.describeElectionById(randomInt());
        Assert.assertEquals(NOT_FOUND, response.getStatus());
    }

    @Test
    public void testDescribeElectionByVoteId() {
        Response response = electionResource.describeElectionByVoteId(randomInt());
        Assert.assertEquals(OK, response.getStatus());
    }

    @Test
    public void testDescribeElectionByVoteIdError() {
        when(electionService.describeElectionByVoteId(anyInt())).thenThrow(new NotFoundException());
        electionResource = new ElectionResource(voteService, electionService);
        Response response = electionResource.describeElectionByVoteId(randomInt());
        Assert.assertEquals(NOT_FOUND, response.getStatus());
    }

    @Test
    public void testIsDataSetElectionOpen() {
        Response response = electionResource.isDataSetElectionOpen(null);
        Assert.assertEquals(OK, response.getStatus());
    }

    @Test
    public void testIsDataSetElectionOpenError() {
        when(electionService.isDataSetElectionOpen()).thenThrow(new NullPointerException());
        electionResource = new ElectionResource(voteService, electionService);
        Response response = electionResource.isDataSetElectionOpen(null);
        Assert.assertEquals(ERROR, response.getStatus());
    }

    @Test
    public void testDescribeVotesOnElection() {
        Response response = electionResource.describeVotesOnElection(randomInt());
        Assert.assertEquals(OK, response.getStatus());
    }

    @Test
    public void testDescribeVotesOnElectionError() {
        when(voteService.findVotesByElectionId(randomInt())).thenThrow(new NotFoundException());
        electionResource = new ElectionResource(voteService, electionService);
        Response response = electionResource.describeVotesOnElection(randomInt());
        Assert.assertEquals(NOT_FOUND, response.getStatus());
    }

    private static int randomInt() {
        return RandomUtils.nextInt(1, 10);
    }

}
