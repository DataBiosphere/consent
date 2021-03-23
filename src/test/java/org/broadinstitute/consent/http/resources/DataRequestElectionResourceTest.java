package org.broadinstitute.consent.http.resources;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.OK;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.io.File;
import java.net.URI;
import java.util.Collections;
import java.util.UUID;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.service.AbstractVoteAPI;
import org.broadinstitute.consent.http.service.DataAccessRequestService;
import org.broadinstitute.consent.http.service.ElectionService;
import org.broadinstitute.consent.http.service.EmailNotifierService;
import org.broadinstitute.consent.http.service.SummaryService;
import org.broadinstitute.consent.http.service.VoteAPI;
import org.broadinstitute.consent.http.service.VoteService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@SuppressWarnings("deprecation")
@RunWith(PowerMockRunner.class)
@PowerMockIgnore("jdk.internal.reflect.*")
@PrepareForTest({
        AbstractVoteAPI.class
})
public class DataRequestElectionResourceTest {

    @Mock
    private DataAccessRequestService darService;
    @Mock
    private ElectionService electionService;
    @Mock
    private VoteAPI voteAPI;
    @Mock
    private EmailNotifierService emailNotifierService;
    @Mock
    private SummaryService summaryService;
    @Mock
    private UriInfo uriInfo;
    @Mock
    private UriBuilder uriBuilder;
    @Mock
    private VoteService voteService;

    private DataRequestElectionResource resource;

    @Before
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);
        PowerMockito.mockStatic(AbstractVoteAPI.class);
        when(AbstractVoteAPI.getInstance()).thenReturn(voteAPI);
        when(uriInfo.getRequestUriBuilder()).thenReturn(uriBuilder);
        when(uriBuilder.path(Mockito.anyString())).thenReturn(uriBuilder);
        String requestId = UUID.randomUUID().toString();
        String url = String.format("http://localhost:8180/api/dataRequest/%s/election", requestId);
        when(uriBuilder.build(anyString())).thenReturn(new URI(url));
    }

    private void initResource() {
        resource = new DataRequestElectionResource(darService, emailNotifierService, summaryService, voteService, electionService);
    }

    @Test
    public void testCreateDataRequestElection() throws Exception {
        when(darService.findByReferenceId(any())).thenReturn(new DataAccessRequest());
        when(electionService.createElection(any(), any(), any())).thenReturn(new Election());
        when(voteService.createVotes(any(Election.class), any(), any())).thenReturn(Collections.emptyList());
        doNothing().when(emailNotifierService).sendNewCaseMessageToList(any(), any());
        initResource();
        Response response = resource.createDataRequestElection(
                uriInfo,
                new Election(),
                UUID.randomUUID().toString()
        );
        Assert.assertEquals(CREATED.getStatusCode(), response.getStatus());
    }

    @Test
    public void testCreateDataRequestElectionWithResearchPurpose() throws Exception {
        Election election = new Election();
        election.setElectionId(RandomUtils.nextInt(1, 100));
        when(darService.findByReferenceId(any())).thenReturn(new DataAccessRequest());
        when(electionService.createElection(any(), any(), any())).thenReturn(election);
        when(voteService.createVotes(any(Election.class), any(), any())).thenReturn(Collections.emptyList());
        doNothing().when(emailNotifierService).sendNewCaseMessageToList(any(), any());
        initResource();
        Response response = resource.createDataRequestElection(
                uriInfo,
                new Election(),
                UUID.randomUUID().toString()
        );
        Assert.assertEquals(CREATED.getStatusCode(), response.getStatus());
    }

    @Test
    public void retrieveElectionWithInvalidDataRequestId() {
        when(electionService.describeDataRequestElection(any())).thenThrow(new NotFoundException());
        initResource();
        Response response = resource.describe(UUID.randomUUID().toString());
        Assert.assertEquals(NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @Test
    public void testDataRequestElectionWithInvalidDataRequest() throws Exception {
        // should return 404 because the data request id does not exist
        when(darService.findByReferenceId(any())).thenThrow(new NotFoundException());
        when(electionService.createElection(any(), any(), any())).thenThrow(new NotFoundException());
        initResource();
        Response response = resource.createDataRequestElection(
                uriInfo,
                new Election(),
                UUID.randomUUID().toString()
        );
        Assert.assertEquals(NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @Test
    public void testCreateDataRequestElectionWithInvalidStatus() throws Exception {
        // should return 400 bad request because status is invalid
        when(darService.findByReferenceId(any())).thenReturn(new DataAccessRequest());
        when(electionService.createElection(any(), any(), any())).thenThrow(new IllegalArgumentException());
        initResource();
        Response response = resource.createDataRequestElection(
                uriInfo,
                new Election(),
                UUID.randomUUID().toString()
        );
        Assert.assertEquals(BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    public void testDeleteElection() {
        doNothing().when(electionService).deleteElection(any(), any());
        initResource();
        Response response = resource.deleteElection(
                UUID.randomUUID().toString(),
                RandomUtils.nextInt(1, 100),
                uriInfo
        );
        Assert.assertEquals(OK.getStatusCode(), response.getStatus());
    }

    @Test
    public void testDeleteElectionNotFound() {
        doThrow(new NotFoundException()).when(electionService).deleteElection(any(), any());
        initResource();
        Response response = resource.deleteElection(
                UUID.randomUUID().toString(),
                RandomUtils.nextInt(1, 100),
                uriInfo
        );
        Assert.assertEquals(NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @Test
    public void testDeleteElectionBadRequest() {
        doThrow(new IllegalArgumentException()).when(electionService).deleteElection(any(), any());
        initResource();
        Response response = resource.deleteElection(
                UUID.randomUUID().toString(),
                RandomUtils.nextInt(1, 100),
                uriInfo
        );
        Assert.assertEquals(BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    public void testDescribeDataSetVotes() throws Exception {
        File file = File.createTempFile("test", "txt");
        when(summaryService.describeDataSetElectionsVotesForDar(any())).thenReturn(file);
        initResource();
        Response response = resource.describeDataSetVotes(UUID.randomUUID().toString());
        Assert.assertEquals(OK.getStatusCode(), response.getStatus());
    }

    @Test
    public void testDescribeDataSetVotesNoFile() {
        when(summaryService.describeDataSetElectionsVotesForDar(any())).thenReturn(null);
        initResource();
        Response response = resource.describeDataSetVotes(UUID.randomUUID().toString());
        Assert.assertEquals(OK.getStatusCode(), response.getStatus());
    }

}
