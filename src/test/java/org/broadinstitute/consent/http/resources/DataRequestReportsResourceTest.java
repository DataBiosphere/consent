package org.broadinstitute.consent.http.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.MockitoAnnotations.openMocks;

import javax.ws.rs.core.Response;
import org.broadinstitute.consent.http.service.DataAccessRequestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

public class DataRequestReportsResourceTest {
    @Mock
    private DataAccessRequestService darService;

    private DataRequestReportsResource resource;

    @BeforeEach
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
