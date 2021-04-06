package org.broadinstitute.consent.http.resources;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.ElectionReview;
import org.broadinstitute.consent.http.models.Vote;
import org.broadinstitute.consent.http.service.AbstractDataAccessRequestAPI;
import org.broadinstitute.consent.http.service.ConsentService;
import org.broadinstitute.consent.http.service.DataAccessRequestAPI;
import org.broadinstitute.consent.http.service.DataAccessRequestService;
import org.broadinstitute.consent.http.service.ElectionService;
import org.broadinstitute.consent.http.service.ReviewResultsService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Collections;
import java.util.UUID;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

@SuppressWarnings("deprecation")
@RunWith(PowerMockRunner.class)
@PowerMockIgnore("jdk.internal.reflect.*")
@PrepareForTest({
        AbstractDataAccessRequestAPI.class
})
public class ElectionReviewResourceTest {

    @Mock
    private ConsentService consentService;
    @Mock
    private DataAccessRequestAPI accessRequestAPI;
    @Mock
    private ElectionService electionService;
    @Mock
    private ReviewResultsService reviewResultsService;
    @Mock
    private DataAccessRequestService darService;

    private ElectionReviewResource resource;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        PowerMockito.mockStatic(AbstractDataAccessRequestAPI.class);
    }

    private void initResource() {
        when(AbstractDataAccessRequestAPI.getInstance()).thenReturn(accessRequestAPI);
        resource = new ElectionReviewResource(darService, consentService, electionService, reviewResultsService);
    }

    @Test
    public void testGetCollectElectionReview() {
        when(reviewResultsService.describeLastElectionReviewByReferenceIdAndType(any(), any())).thenReturn(new ElectionReview());
        initResource();
        ElectionReview response = resource.getCollectElectionReview(RandomStringUtils.random(10), RandomStringUtils.random(10));
        assertNotNull(response);
    }

    @Test
    public void testOpenElections() {
        when(reviewResultsService.openElections()).thenReturn(true);
        initResource();
        String response = resource.openElections();
        assertNotNull(response);
    }

    @Test
    public void testGetElectionReviewByElectionId() {
        when(reviewResultsService.describeElectionReviewByElectionId(any(), any())).thenReturn(new ElectionReview());
        initResource();
        ElectionReview response = resource.getElectionReviewByElectionId(RandomUtils.nextInt(100, 1000));
        assertNotNull(response);
    }

    @Test
    public void testGetAccessElectionReviewByReferenceId() {
        Election e = new Election();
        e.setElectionId(RandomUtils.nextInt(100, 1000));
        e.setReferenceId(UUID.randomUUID().toString());
        when(electionService.describeElectionById(any())).thenReturn(e);
        Election consentElection = new Election();
        consentElection.setElectionId(RandomUtils.nextInt(100, 1000));
        consentElection.setReferenceId(UUID.randomUUID().toString());
        when(electionService.getConsentElectionByDARElectionId(e.getElectionId())).thenReturn(consentElection);
        DataAccessRequest dar = new DataAccessRequest();
        DataAccessRequestData data = new DataAccessRequestData();
        data.setDatasetIds(Collections.singletonList(1));
        dar.setData(data);
        when(darService.findByReferenceId(any())).thenReturn(dar);
        when(consentService.getConsentFromDatasetID(any())).thenReturn(new Consent());
        when(reviewResultsService.describeElectionReviewByElectionId(any(), any())).thenReturn(new ElectionReview());
        when(reviewResultsService.describeAgreementVote(any())).thenReturn(Collections.singletonList(new Vote()));
        initResource();
        ElectionReview response = resource.getAccessElectionReviewByReferenceId(RandomUtils.nextInt(100, 1000), true);
        assertNotNull(response);
    }

    @Test
    public void testGetRPElectionReviewByReferenceId() {
        when(electionService.findRPElectionByElectionAccessId(any())).thenReturn(1);
        when(reviewResultsService.describeElectionReviewByElectionId(any(), any())).thenReturn(new ElectionReview());
        initResource();
        ElectionReview response = resource.getRPElectionReviewByReferenceId(RandomUtils.nextInt(100, 1000), true);
        assertNotNull(response);
    }

    @Test
    public void testGetElectionReviewByReferenceId() {
        when(reviewResultsService.describeElectionReviewByReferenceId(any())).thenReturn(new ElectionReview());
        initResource();
        ElectionReview response = resource.getElectionReviewByReferenceId(RandomStringUtils.random(10));
        assertNotNull(response);
    }

}

