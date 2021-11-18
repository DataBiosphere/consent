package org.broadinstitute.consent.http.resources;


import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.Vote;
import org.broadinstitute.consent.http.service.ElectionService;
import org.broadinstitute.consent.http.service.EmailNotifierService;
import org.broadinstitute.consent.http.service.VoteService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.UUID;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

public class ConsentVoteResourceTest {

    @Mock
    private VoteService voteService;
    @Mock
    private ElectionService electionService;
    @Mock
    private EmailNotifierService emailNotifierService;

    private ConsentVoteResource resource;

    private Vote createMockVote() {
        Vote vote = new Vote();
        vote.setVoteId(RandomUtils.nextInt(100, 1000));
        vote.setVote(false);
        vote.setRationale("Test");
        return vote;
    }

    private Consent createMockConsent() {
        Consent consent = new Consent();
        consent.setConsentId(UUID.randomUUID().toString());
        return consent;
    }

    private class LogHandler extends Handler {
        Level lastLevel = Level.FINEST;

        public Level checkLevel() {
            return lastLevel;
        }

        public void publish(LogRecord record) {
            lastLevel = record.getLevel();
        }

        public void close(){}
        public void flush(){}
    }

    @Before
    public void setUp() {
        openMocks(this);
    }

    private void initResource() {
        resource = new ConsentVoteResource(emailNotifierService, electionService, voteService);
    }

    @Test
    public void testCreateConsentVoteSuccess() throws Exception {
        Vote vote = createMockVote();
        Consent consent = createMockConsent();

        when(voteService.updateVoteById(any(), any())).thenReturn(vote);
        when(electionService.validateCollectEmailCondition(any())).thenReturn(true);
        doNothing().when(emailNotifierService).sendCollectMessage(any());
        initResource();

        Response response = resource.firstVoteUpdate(vote, consent.getConsentId(), vote.getVoteId());
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testCreateConsentVoteCollectMessageError() throws Exception {
        Vote vote = createMockVote();
        Consent consent = createMockConsent();

        Logger logger = Logger.getLogger(ConsentVoteResource.class.getName());
        LogHandler handler = new LogHandler();
        handler.setLevel(Level.ALL);
        logger.setUseParentHandlers(false);
        logger.addHandler(handler);
        logger.setLevel(Level.ALL);

        when(voteService.updateVoteById(any(), any())).thenReturn(vote);
        when(electionService.validateCollectEmailCondition(any())).thenReturn(true);
        doThrow(new IOException()).when(emailNotifierService).sendCollectMessage(any());
        initResource();

        Response response = resource.firstVoteUpdate(vote, consent.getConsentId(), vote.getVoteId());
        assertEquals(Level.SEVERE, handler.checkLevel());
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testCreateConsentVoteOtherError() throws Exception {
        Vote vote = createMockVote();
        Consent consent = createMockConsent();

        doThrow(new RuntimeException()).when(voteService).updateVoteById(any(), any());
        initResource();

        Response response = resource.firstVoteUpdate(vote, consent.getConsentId(), vote.getVoteId());
        assertEquals(500, response.getStatus());
    }

    @Test
    public void testUpdateConsentVoteSuccess() throws Exception {
        Vote vote = createMockVote();
        Consent consent = createMockConsent();

        when(voteService.updateVote(any(), any(), any())).thenReturn(vote);
        initResource();

        Response response = resource.updateConsentVote(vote, consent.getConsentId(), vote.getVoteId());
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testUpdateConsentVoteError() throws Exception {
        Vote vote = createMockVote();
        Consent consent = createMockConsent();

        doThrow(new RuntimeException()).when(voteService).updateVote(any(), any(), any());
        initResource();

        Response response = resource.updateConsentVote(vote, consent.getConsentId(), vote.getVoteId());
        assertEquals(500, response.getStatus());
    }

    @Test
    public void testDescribe() throws Exception {
        Vote vote = createMockVote();
        Consent consent = createMockConsent();

        when(voteService.findVoteById(any())).thenReturn(vote);
        initResource();

        Vote fromGet = resource.describe(consent.getConsentId(), vote.getVoteId());
        assertEquals(vote, fromGet);
    }

    @Test
    public void testDeleteVoteSuccess() throws Exception {
        Vote vote = createMockVote();
        Consent consent = createMockConsent();

        initResource();

        Response response = resource.deleteVote(consent.getConsentId(), vote.getVoteId());
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testDeleteVoteError() throws Exception {
        Vote vote = createMockVote();
        Consent consent = createMockConsent();

        doThrow(new RuntimeException()).when(voteService).deleteVote(any(), any());
        initResource();

        Response response = resource.deleteVote(consent.getConsentId(), vote.getVoteId());
        assertEquals(500, response.getStatus());
    }

    @Test
    public void testDeleteVotesSuccess() throws Exception {
        Consent consent = createMockConsent();

        initResource();

        Response response = resource.deleteVotes(consent.getConsentId());
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testDeleteVotesError() throws Exception {
        Consent consent = createMockConsent();

        doThrow(new RuntimeException()).when(voteService).deleteVotes(any());
        initResource();

        Response response = resource.deleteVotes(consent.getConsentId());
        assertEquals(500, response.getStatus());
    }

    @Test
    public void testOptions() throws Exception {
        Consent consent = createMockConsent();

        initResource();

        Response response = resource.options(consent.getConsentId());
        assertEquals(200, response.getStatus());
    }
}
