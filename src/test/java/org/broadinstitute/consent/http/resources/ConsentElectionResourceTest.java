package org.broadinstitute.consent.http.resources;

import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.enumeration.ElectionStatus;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.enumeration.VoteType;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.Vote;
import org.broadinstitute.consent.http.service.AbstractElectionAPI;
import org.broadinstitute.consent.http.service.AbstractEmailNotifierAPI;
import org.broadinstitute.consent.http.service.AbstractVoteAPI;
import org.broadinstitute.consent.http.service.ConsentService;
import org.broadinstitute.consent.http.service.DacService;
import org.broadinstitute.consent.http.service.ElectionAPI;
import org.broadinstitute.consent.http.service.EmailNotifierAPI;
import org.broadinstitute.consent.http.service.VoteAPI;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@SuppressWarnings("deprecation")
@RunWith(PowerMockRunner.class)
@PrepareForTest({
        AbstractElectionAPI.class,
        AbstractVoteAPI.class,
        AbstractEmailNotifierAPI.class})
public class ConsentElectionResourceTest {

    @Mock
    ConsentService consentService;

    @Mock
    DacService dacService;

    @Mock
    ElectionAPI electionAPI;

    @Mock
    VoteAPI voteAPI;

    @Mock
    EmailNotifierAPI emailAPI;

    @Mock
    UriInfo info;

    @Mock
    UriBuilder builder;

    private AuthUser user = new AuthUser("auth.user@test.com");
    private ConsentElectionResource resource;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        PowerMockito.mockStatic(AbstractElectionAPI.class);
        PowerMockito.mockStatic(AbstractVoteAPI.class);
        PowerMockito.mockStatic(AbstractEmailNotifierAPI.class);

        when(builder.build()).thenReturn(URI.create("https://test.domain.org/some/path"));
        when(info.getRequestUriBuilder()).thenReturn(builder);

        Election election = getElection();
        when(electionAPI.createElection(any(Election.class), anyString(), any(ElectionType.class))).thenReturn(election);
        when(voteAPI.createVotes(anyInt(), any(ElectionType.class), anyBoolean())).thenReturn(getVotesForElection(election.getElectionId()));
        doNothing().when(electionAPI).deleteElection(anyString(), anyInt());
        doNothing().when(emailAPI).sendNewCaseMessageToList(anyList(), any(Election.class));

    }

    @Test
    public void testCreateConsentElection() {
        Election election = getElection();
        initResource();

        Response response = resource.createConsentElection(user, info, UUID.randomUUID().toString(), election);
        Assert.assertNotNull(response);
        Assert.assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
    }

    @Test
    public void testCreateConsentElection_failure() throws Exception {
        Election election = getElection();
        when(electionAPI.createElection(any(Election.class), anyString(), any(ElectionType.class))).thenThrow(new IllegalArgumentException());
        initResource();

        Response response = resource.createConsentElection(user, info, UUID.randomUUID().toString(), election);
        Assert.assertNotNull(response);
        Assert.assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    private void initResource() {
        when(AbstractElectionAPI.getInstance()).thenReturn(electionAPI);
        when(AbstractVoteAPI.getInstance()).thenReturn(voteAPI);
        when(AbstractEmailNotifierAPI.getInstance()).thenReturn(emailAPI);
        resource = new ConsentElectionResource(consentService, dacService);
    }

    private Election getElection() {
        Election election = new Election();
        election.setCreateDate(new Date());
        election.setElectionId(RandomUtils.nextInt(1, 100));
        election.setStatus(ElectionStatus.OPEN.getValue());
        election.setElectionType(ElectionType.TRANSLATE_DUL.getValue());
        return election;
    }

    @SuppressWarnings("ArraysAsListWithZeroOrOneArgument")
    private List<Vote> getVotesForElection(int electionId) {
        Vote vote = new Vote();
        vote.setVoteId(RandomUtils.nextInt(1, 100));
        vote.setCreateDate(new Date());
        vote.setElectionId(electionId);
        vote.setType(VoteType.DAC.getValue());
        return Arrays.asList(vote);
    }

}
