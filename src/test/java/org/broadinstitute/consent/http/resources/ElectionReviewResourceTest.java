package org.broadinstitute.consent.http.resources;

import org.broadinstitute.consent.http.service.ReviewResultsService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

public class ElectionReviewResourceTest {

    @Mock
    private ReviewResultsService reviewResultsService;
    private ElectionReviewResource resource;

    @Before
    public void setUp() {
        openMocks(this);
    }

    private void initResource() {
        resource = new ElectionReviewResource(reviewResultsService);
    }

    @Test
    public void testOpenElections() {
        when(reviewResultsService.openElections()).thenReturn(true);
        initResource();
        String response = resource.openElections();
        assertNotNull(response);
    }

}
