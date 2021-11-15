package org.broadinstitute.consent.http.resources;

import org.broadinstitute.consent.http.service.DataAccessRequestService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import javax.ws.rs.core.Response;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.MockitoAnnotations.openMocks;

public class DataRequestReportsResourceTest {
    @Mock
    private DataAccessRequestService darService;

    private DataRequestReportsResource resource;

    @Before
    public void setUp() {
        openMocks(this);
        resource = new DataRequestReportsResource(darService);
    }

    @Test
    public void testDownloadApprovedDARsSuccess() throws Exception {
        Response response = resource.downloadApprovedDARs();
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testDownloadApprovedDARsError() throws Exception {
        doThrow(new RuntimeException()).when(darService).createApprovedDARDocument();
        Response response = resource.downloadApprovedDARs();
        assertEquals(500, response.getStatus());
    }

    @Test
    public void testDownloadReviewedDARsSuccess() throws Exception {
        Response response = resource.downloadReviewedDARs();
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testDownloadReviewedDARsError() throws Exception {
        doThrow(new RuntimeException()).when(darService).createReviewedDARDocument();
        Response response = resource.downloadReviewedDARs();
        assertEquals(500, response.getStatus());
    }
}
