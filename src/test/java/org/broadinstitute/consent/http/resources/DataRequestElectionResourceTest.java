package org.broadinstitute.consent.http.resources;

import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.service.AbstractDataAccessRequestAPI;
import org.broadinstitute.consent.http.service.AbstractElectionAPI;
import org.broadinstitute.consent.http.service.AbstractEmailNotifierAPI;
import org.broadinstitute.consent.http.service.AbstractSummaryAPI;
import org.broadinstitute.consent.http.service.AbstractVoteAPI;
import org.broadinstitute.consent.http.service.DataAccessRequestAPI;
import org.broadinstitute.consent.http.service.ElectionAPI;
import org.broadinstitute.consent.http.service.EmailNotifierAPI;
import org.broadinstitute.consent.http.service.SummaryAPI;
import org.broadinstitute.consent.http.service.VoteAPI;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.io.File;
import java.net.URI;
import java.util.Collections;
import java.util.UUID;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.OK;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@SuppressWarnings("deprecation")
@RunWith(PowerMockRunner.class)
@PrepareForTest({
        AbstractElectionAPI.class,
        AbstractVoteAPI.class,
        AbstractEmailNotifierAPI.class,
        AbstractDataAccessRequestAPI.class,
        AbstractSummaryAPI.class
})
public class DataRequestElectionResourceTest {

    @Mock
    private ElectionAPI electionAPI;
    @Mock
    private VoteAPI voteAPI;
    @Mock
    private EmailNotifierAPI emailAPI;
    @Mock
    private DataAccessRequestAPI darApi;
    @Mock
    private SummaryAPI summaryAPI;
    @Mock
    private UriInfo uriInfo;
    @Mock
    private UriBuilder uriBuilder;

    private AuthUser authUser = new AuthUser("test@test.com");
    private DataRequestElectionResource resource;

    @Before
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);
        PowerMockito.mockStatic(AbstractElectionAPI.class);
        PowerMockito.mockStatic(AbstractVoteAPI.class);
        PowerMockito.mockStatic(AbstractEmailNotifierAPI.class);
        PowerMockito.mockStatic(AbstractDataAccessRequestAPI.class);
        PowerMockito.mockStatic(AbstractSummaryAPI.class);
        when(AbstractElectionAPI.getInstance()).thenReturn(electionAPI);
        when(AbstractVoteAPI.getInstance()).thenReturn(voteAPI);
        when(AbstractEmailNotifierAPI.getInstance()).thenReturn(emailAPI);
        when(AbstractDataAccessRequestAPI.getInstance()).thenReturn(darApi);
        when(AbstractSummaryAPI.getInstance()).thenReturn(summaryAPI);

        when(uriInfo.getRequestUriBuilder()).thenReturn(uriBuilder);
        when(uriBuilder.path(Mockito.anyString())).thenReturn(uriBuilder);
        String requestId = UUID.randomUUID().toString();
        String url = String.format("http://localhost:8180/api/dataRequest/%s/election", requestId);
        when(uriBuilder.build(anyString())).thenReturn(new URI(url));
    }

    private void initResource() {
        resource = new DataRequestElectionResource();
    }

    @Test
    public void testCreateDataRequestElection() throws Exception {
        when(electionAPI.createElection(any(), any(), any())).thenReturn(new Election());
        when(darApi.getField(any(), any())).thenReturn(null);
        when(voteAPI.createVotes(any(), any(), any())).thenReturn(Collections.emptyList());
        doNothing().when(emailAPI).sendNewCaseMessageToList(any(), any());
        initResource();
        Response response = resource.createDataRequestElection(
                authUser,
                uriInfo,
                new Election(),
                UUID.randomUUID().toString()
        );
        Assert.assertEquals(CREATED.getStatusCode(), response.getStatus());
    }

    @Test
    public void retrieveElectionWithInvalidDataRequestId() {
        when(electionAPI.describeDataRequestElection(any())).thenThrow(new NotFoundException());
        initResource();
        Response response = resource.describe(UUID.randomUUID().toString());
        Assert.assertEquals(NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @Test
    public void testDataRequestElectionWithInvalidDataRequest() throws Exception {
        // should return 404 because the data request id does not exist
        when(electionAPI.createElection(any(), any(), any())).thenThrow(new NotFoundException());
        initResource();
        Response response = resource.createDataRequestElection(
                authUser,
                uriInfo,
                new Election(),
                UUID.randomUUID().toString()
        );
        Assert.assertEquals(NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @Test
    public void testCreateDataRequestElectionWithInvalidStatus() throws Exception {
        // should return 400 bad request because status is invalid
        when(electionAPI.createElection(any(), any(), any())).thenThrow(new IllegalArgumentException());
        initResource();
        Response response = resource.createDataRequestElection(
                authUser,
                uriInfo,
                new Election(),
                UUID.randomUUID().toString()
        );
        Assert.assertEquals(BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    public void testDeleteElection() {
        doNothing().when(electionAPI).deleteElection(any(), any());
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
        doThrow(new NotFoundException()).when(electionAPI).deleteElection(any(), any());
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
        doThrow(new IllegalArgumentException()).when(electionAPI).deleteElection(any(), any());
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
        when(summaryAPI.describeDataSetElectionsVotesForDar(any())).thenReturn(file);
        initResource();
        Response response = resource.describeDataSetVotes(UUID.randomUUID().toString());
        Assert.assertEquals(OK.getStatusCode(), response.getStatus());
    }

    @Test
    public void testDescribeDataSetVotesNoFile() {
        when(summaryAPI.describeDataSetElectionsVotesForDar(any())).thenReturn(null);
        initResource();
        Response response = resource.describeDataSetVotes(UUID.randomUUID().toString());
        Assert.assertEquals(OK.getStatusCode(), response.getStatus());
    }

}
