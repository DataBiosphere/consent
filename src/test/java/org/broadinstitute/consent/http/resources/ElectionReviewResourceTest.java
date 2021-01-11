package org.broadinstitute.consent.http.resources;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.ElectionReview;
import org.broadinstitute.consent.http.models.Vote;
import org.broadinstitute.consent.http.service.AbstractConsentAPI;
import org.broadinstitute.consent.http.service.AbstractDataAccessRequestAPI;
import org.broadinstitute.consent.http.service.AbstractElectionAPI;
import org.broadinstitute.consent.http.service.AbstractReviewResultsAPI;
import org.broadinstitute.consent.http.service.ConsentAPI;
import org.broadinstitute.consent.http.service.DataAccessRequestAPI;
import org.broadinstitute.consent.http.service.DataAccessRequestService;
import org.broadinstitute.consent.http.service.ElectionAPI;
import org.broadinstitute.consent.http.service.ReviewResultsAPI;
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
        AbstractConsentAPI.class,
        AbstractDataAccessRequestAPI.class,
        AbstractElectionAPI.class,
        AbstractReviewResultsAPI.class
})
public class ElectionReviewResourceTest {

    @Mock
    private ConsentAPI consentAPI;
    @Mock
    private DataAccessRequestAPI accessRequestAPI;
    @Mock
    private ElectionAPI electionAPI;
    @Mock
    private ReviewResultsAPI reviewResultsAPI;
    @Mock
    private DataAccessRequestService darService;

    private ElectionReviewResource resource;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        PowerMockito.mockStatic(AbstractConsentAPI.class);
        PowerMockito.mockStatic(AbstractDataAccessRequestAPI.class);
        PowerMockito.mockStatic(AbstractElectionAPI.class);
        PowerMockito.mockStatic(AbstractReviewResultsAPI.class);
    }

    private void initResource() {
        when(AbstractConsentAPI.getInstance()).thenReturn(consentAPI);
        when(AbstractDataAccessRequestAPI.getInstance()).thenReturn(accessRequestAPI);
        when(AbstractElectionAPI.getInstance()).thenReturn(electionAPI);
        when(AbstractReviewResultsAPI.getInstance()).thenReturn(reviewResultsAPI);
        resource = new ElectionReviewResource(darService);
    }

    @Test
    public void testGetCollectElectionReview() {
        when(reviewResultsAPI.describeLastElectionReviewByReferenceIdAndType(any(), any())).thenReturn(new ElectionReview());
        initResource();
        ElectionReview response = resource.getCollectElectionReview(RandomStringUtils.random(10), RandomStringUtils.random(10));
        assertNotNull(response);
    }

    @Test
    public void testOpenElections() {
        when(reviewResultsAPI.openElections()).thenReturn(true);
        initResource();
        String response = resource.openElections();
        assertNotNull(response);
    }

    @Test
    public void testGetElectionReviewByElectionId() {
        when(reviewResultsAPI.describeElectionReviewByElectionId(any(), any())).thenReturn(new ElectionReview());
        initResource();
        ElectionReview response = resource.getElectionReviewByElectionId(RandomUtils.nextInt(100, 1000));
        assertNotNull(response);
    }

    @Test
    public void testGetAccessElectionReviewByReferenceId() {
        Election e = new Election();
        e.setElectionId(RandomUtils.nextInt(100, 1000));
        e.setReferenceId(UUID.randomUUID().toString());
        when(electionAPI.describeElectionById(any())).thenReturn(e);
        Election consentElection = new Election();
        consentElection.setElectionId(RandomUtils.nextInt(100, 1000));
        consentElection.setReferenceId(UUID.randomUUID().toString());
        when(electionAPI.getConsentElectionByDARElectionId(e.getElectionId())).thenReturn(consentElection);
        DataAccessRequest dar = new DataAccessRequest();
        DataAccessRequestData data = new DataAccessRequestData();
        data.setDatasetIds(Collections.singletonList(1));
        dar.setData(data);
        when(darService.findByReferenceId(any())).thenReturn(dar);
        when(consentAPI.getConsentFromDatasetID(any())).thenReturn(new Consent());
        when(reviewResultsAPI.describeElectionReviewByElectionId(any(), any())).thenReturn(new ElectionReview());
        when(reviewResultsAPI.describeAgreementVote(any())).thenReturn(Collections.singletonList(new Vote()));
        initResource();
        ElectionReview response = resource.getAccessElectionReviewByReferenceId(RandomUtils.nextInt(100, 1000), true);
        assertNotNull(response);
    }

    @Test
    public void testGetRPElectionReviewByReferenceId() {
        when(electionAPI.findRPElectionByElectionAccessId(any())).thenReturn(1);
        when(reviewResultsAPI.describeElectionReviewByElectionId(any(), any())).thenReturn(new ElectionReview());
        initResource();
        ElectionReview response = resource.getRPElectionReviewByReferenceId(RandomUtils.nextInt(100, 1000), true);
        assertNotNull(response);
    }

    @Test
    public void testGetElectionReviewByReferenceId() {
        when(reviewResultsAPI.describeElectionReviewByReferenceId(any())).thenReturn(new ElectionReview());
        initResource();
        ElectionReview response = resource.getElectionReviewByReferenceId(RandomStringUtils.random(10));
        assertNotNull(response);
    }

}

