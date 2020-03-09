package org.broadinstitute.consent.http.resources;


import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.Vote;
import org.broadinstitute.consent.http.service.AbstractElectionAPI;
import org.broadinstitute.consent.http.service.AbstractEmailNotifierAPI;
import org.broadinstitute.consent.http.service.AbstractVoteAPI;
import org.broadinstitute.consent.http.service.ElectionAPI;
import org.broadinstitute.consent.http.service.EmailNotifierAPI;
import org.broadinstitute.consent.http.service.VoteAPI;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.ws.rs.core.Response;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@SuppressWarnings("deprecation")
@RunWith(PowerMockRunner.class)
@PrepareForTest({
        AbstractVoteAPI.class,
        AbstractElectionAPI.class,
        AbstractEmailNotifierAPI.class
})
public class ConsentVoteResourceTest {

    @Mock
    private VoteAPI voteAPI;
    @Mock
    private ElectionAPI electionAPI;
    @Mock
    private EmailNotifierAPI emailAPI;

    private ConsentVoteResource resource;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        PowerMockito.mockStatic(AbstractVoteAPI.class);
        PowerMockito.mockStatic(AbstractElectionAPI.class);
        PowerMockito.mockStatic(AbstractEmailNotifierAPI.class);
    }

    private void initResource() {
        when(AbstractVoteAPI.getInstance()).thenReturn(voteAPI);
        when(AbstractElectionAPI.getInstance()).thenReturn(electionAPI);
        when(AbstractEmailNotifierAPI.getInstance()).thenReturn(emailAPI);
        resource = new ConsentVoteResource();
    }

    @Test
    public void testCreateConsentVote() throws Exception {
        Consent consent = new Consent();
        consent.setConsentId(UUID.randomUUID().toString());
        Vote vote = new Vote();
        vote.setVoteId(RandomUtils.nextInt(100, 1000));
        vote.setVote(false);
        vote.setRationale("Test");
        when(voteAPI.firstVoteUpdate(any(), any())).thenReturn(vote);
        when(electionAPI.validateCollectEmailCondition(any())).thenReturn(true);
        doNothing().when(emailAPI).sendCollectMessage(any());
        initResource();

        Response response = resource.firstVoteUpdate(vote, UUID.randomUUID().toString(), vote.getVoteId());
        assertEquals(200, response.getStatus());
    }

}