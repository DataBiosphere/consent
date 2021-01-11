package org.broadinstitute.consent.http.resources;

import com.google.api.client.http.HttpStatusCodes;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.service.AbstractElectionAPI;
import org.broadinstitute.consent.http.service.ElectionAPI;
import org.broadinstitute.consent.http.service.VoteService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import java.util.Collections;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@SuppressWarnings("FieldCanBeLocal")
@RunWith(PowerMockRunner.class)
@PowerMockIgnore("jdk.internal.reflect.*")
@PrepareForTest({AbstractElectionAPI.class})
public class ElectionResourceTest {

    private final int OK = HttpStatusCodes.STATUS_CODE_OK;
    private final int NOT_FOUND = HttpStatusCodes.STATUS_CODE_NOT_FOUND;
    private final int ERROR = HttpStatusCodes.STATUS_CODE_SERVER_ERROR;

    @Mock
    VoteService voteService;

    @Mock
    ElectionAPI electionAPI;

    private ElectionResource electionResource;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        PowerMockito.mockStatic(AbstractElectionAPI.class);
        when(voteService.findVotesByReferenceId(any())).thenReturn(Collections.emptyList());
        doNothing().when(voteService).advanceVotes(any(), anyBoolean(), anyString());
        when(electionAPI.checkDataOwnerToCloseElection(any())).thenReturn(false);
        doNothing().when(electionAPI).closeDataOwnerApprovalElection(any());
        when(electionAPI.updateElectionById(any(), any())).thenReturn(new Election());
        when(electionAPI.describeElectionById(any())).thenReturn(new Election());
        when(electionAPI.describeElectionByVoteId(any())).thenReturn(new Election());
        when(electionAPI.isDataSetElectionOpen()).thenReturn(true);
        when(electionAPI.getConsentElectionByDARElectionId(any())).thenReturn(new Election());
        when(AbstractElectionAPI.getInstance()).thenReturn(electionAPI);
        electionResource = new ElectionResource(voteService);
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
        electionResource = new ElectionResource(voteService);
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
        when(electionAPI.updateElectionById(any(), anyInt())).thenThrow(new NotFoundException());
        electionResource = new ElectionResource(voteService);
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
        when(electionAPI.describeElectionById(anyInt())).thenThrow(new NotFoundException());
        electionResource = new ElectionResource(voteService);
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
        when(electionAPI.describeElectionByVoteId(anyInt())).thenThrow(new NotFoundException());
        electionResource = new ElectionResource(voteService);
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
        when(electionAPI.isDataSetElectionOpen()).thenThrow(new NullPointerException());
        electionResource = new ElectionResource(voteService);
        Response response = electionResource.isDataSetElectionOpen(null);
        Assert.assertEquals(ERROR, response.getStatus());
    }

    @Test
    public void testDescribeConsentElectionByDARElectionId() {
        Response response = electionResource.describeConsentElectionByDARElectionId(randomInt());
        Assert.assertEquals(OK, response.getStatus());
    }

    @Test
    public void testDescribeConsentElectionByDARElectionIdError() {
        when(electionAPI.getConsentElectionByDARElectionId(anyInt())).thenThrow(new NotFoundException());
        electionResource = new ElectionResource(voteService);
        Response response = electionResource.describeConsentElectionByDARElectionId(randomInt());
        Assert.assertEquals(NOT_FOUND, response.getStatus());
    }

    private static int randomInt() {
        return RandomUtils.nextInt(1, 10);
    }

}
