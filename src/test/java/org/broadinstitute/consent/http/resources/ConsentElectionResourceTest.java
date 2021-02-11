package org.broadinstitute.consent.http.resources;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.enumeration.ElectionStatus;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.enumeration.VoteType;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DataUseBuilder;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.Vote;
import org.broadinstitute.consent.http.models.grammar.Everything;
import org.broadinstitute.consent.http.service.AbstractElectionAPI;
import org.broadinstitute.consent.http.service.AbstractVoteAPI;
import org.broadinstitute.consent.http.service.ConsentService;
import org.broadinstitute.consent.http.service.DacService;
import org.broadinstitute.consent.http.service.ElectionAPI;
import org.broadinstitute.consent.http.service.EmailNotifierService;
import org.broadinstitute.consent.http.service.UnknownIdentifierException;
import org.broadinstitute.consent.http.service.VoteAPI;
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

@SuppressWarnings("deprecation")
@RunWith(PowerMockRunner.class)
@PowerMockIgnore("jdk.internal.reflect.*")
@PrepareForTest({AbstractElectionAPI.class, AbstractVoteAPI.class})
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
    EmailNotifierService emailNotifierService;

    @Mock
    VoteService voteService;

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

        when(builder.build()).thenReturn(URI.create("https://test.domain.org/some/path"));
        when(info.getRequestUriBuilder()).thenReturn(builder);

        Election election = getElection();
        when(electionAPI.createElection(any(Election.class), anyString(), any(ElectionType.class))).thenReturn(election);
        when(voteService.createVotes(any(Election.class), any(ElectionType.class), anyBoolean())).thenReturn(getVotesForElection(election.getElectionId()));
        doNothing().when(emailNotifierService).sendNewCaseMessageToList(anyList(), any(Election.class));
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

    @Test
    public void testCreateConsentElectionForDac() throws UnknownIdentifierException {
        Election election = getElection();
        Dac dac = getDac();
        Consent consent = getConsent(dac.getDacId());
        when(consentService.getById(anyString())).thenReturn(consent);
        when(dacService.findById(anyInt())).thenReturn(dac);
        initResource();

        Response response = resource.createConsentElectionForDac(
                user,
                info,
                consent.getConsentId(),
                dac.getDacId(),
                election);
        Assert.assertNotNull(response);
        Assert.assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
    }

    @Test
    public void testCreateConsentElectionForDac_noConsent() throws UnknownIdentifierException {
        Election election = getElection();
        Dac dac = getDac();
        Consent consent = getConsent(dac.getDacId());
        when(consentService.getById(anyString())).thenThrow(new UnknownIdentifierException(""));
        initResource();

        Response response = resource.createConsentElectionForDac(
                user,
                info,
                consent.getConsentId(),
                dac.getDacId(),
                election);
        Assert.assertNotNull(response);
        Assert.assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @Test
    public void testCreateConsentElectionForDac_noDac() throws UnknownIdentifierException {
        Election election = getElection();
        Dac dac = getDac();
        Consent consent = getConsent(dac.getDacId());
        when(consentService.getById(anyString())).thenReturn(consent);
        when(dacService.findById(anyInt())).thenReturn(null);
        initResource();

        Response response = resource.createConsentElectionForDac(
                user,
                info,
                consent.getConsentId(),
                dac.getDacId(),
                election);
        Assert.assertNotNull(response);
        Assert.assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @Test
    public void testCreateConsentElectionForDac_consentDacMismatch() throws UnknownIdentifierException {
        Election election = getElection();
        Dac dac = getDac();
        Consent consent = getConsent(dac.getDacId() + 1);
        when(consentService.getById(anyString())).thenReturn(consent);
        when(dacService.findById(anyInt())).thenReturn(dac);
        initResource();

        Response response = resource.createConsentElectionForDac(
                user,
                info,
                consent.getConsentId(),
                dac.getDacId(),
                election);
        Assert.assertNotNull(response);
        Assert.assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    public void testCreateConsentElectionForDac_consentUpdateError() throws UnknownIdentifierException {
        Election election = getElection();
        Dac dac = getDac();
        Consent consent = getConsent(dac.getDacId());
        when(consentService.getById(anyString())).thenReturn(consent);
        when(dacService.findById(anyInt())).thenReturn(dac);
        doThrow(new RuntimeException()).when(consentService).updateConsentDac(anyString(), anyInt());
        initResource();

        Response response = resource.createConsentElectionForDac(
                user,
                info,
                consent.getConsentId(),
                dac.getDacId(),
                election);
        Assert.assertNotNull(response);
        Assert.assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
    }

    @Test
    public void testDeleteElection() {
        doNothing().when(electionAPI).deleteElection(anyString(), anyInt());
        initResource();

        Response response = resource.deleteElection(UUID.randomUUID().toString(), info, RandomUtils.nextInt(1, 10));
        Assert.assertNotNull(response);
        Assert.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

    private void initResource() {
        when(AbstractElectionAPI.getInstance()).thenReturn(electionAPI);
        when(AbstractVoteAPI.getInstance()).thenReturn(voteAPI);
        resource = new ConsentElectionResource(consentService, dacService, emailNotifierService, voteService);
    }

    private Consent getConsent(Integer dacId) {
        Consent consent = new Consent();
        consent.setConsentId(UUID.randomUUID().toString());
        consent.setCreateDate(new Timestamp(new Date().getTime()));
        consent.setDacId(dacId);
        consent.setDataUse(new DataUseBuilder().setGeneralUse(true).build());
        consent.setRequiresManualReview(false);
        consent.setDataUseLetter("");
        consent.setUseRestriction(new Everything());
        consent.setName("Name");
        return consent;
    }

    private Dac getDac() {
        Dac dac = new Dac();
        dac.setDacId(RandomUtils.nextInt(1, 100));
        dac.setName("Name");
        dac.setDescription("Description");
        dac.setCreateDate(new Date());
        return dac;
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
