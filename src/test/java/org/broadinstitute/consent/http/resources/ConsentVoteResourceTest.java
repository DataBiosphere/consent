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
import org.mockito.MockitoAnnotations;

import javax.ws.rs.core.Response;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

public class ConsentVoteResourceTest {

    @Mock
    private VoteService voteService;
    @Mock
    private ElectionService electionService;
    @Mock
    private EmailNotifierService emailNotifierService;

    private ConsentVoteResource resource;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    private void initResource() {
        resource = new ConsentVoteResource(emailNotifierService, electionService, voteService);
    }

    @Test
    public void testCreateConsentVote() throws Exception {
        Consent consent = new Consent();
        consent.setConsentId(UUID.randomUUID().toString());
        Vote vote = new Vote();
        vote.setVoteId(RandomUtils.nextInt(100, 1000));
        vote.setVote(false);
        vote.setRationale("Test");
        when(voteService.updateVoteById(any(), any())).thenReturn(vote);
        when(electionService.validateCollectEmailCondition(any())).thenReturn(true);
        doNothing().when(emailNotifierService).sendCollectMessage(any());
        initResource();

        Response response = resource.firstVoteUpdate(vote, UUID.randomUUID().toString(), vote.getVoteId());
        assertEquals(200, response.getStatus());
    }

}
