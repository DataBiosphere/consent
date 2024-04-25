package org.broadinstitute.consent.http.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doThrow;

import jakarta.ws.rs.core.Response;
import org.broadinstitute.consent.http.service.DataAccessRequestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DataRequestReportsResourceTest {

  @Mock
  private DataAccessRequestService darService;

  private DataRequestReportsResource resource;

  @BeforeEach
  void setUp() {
    resource = new DataRequestReportsResource(darService);
  }

  @Test
  void testDownloadApprovedDARsSuccess() throws Exception {
    Response response = resource.downloadApprovedDARs();
    assertEquals(200, response.getStatus());
  }

  @Test
  void testDownloadApprovedDARsError() throws Exception {
    doThrow(new RuntimeException()).when(darService).createApprovedDARDocument();
    Response response = resource.downloadApprovedDARs();
    assertEquals(500, response.getStatus());
  }

  @Test
  void testDownloadReviewedDARsSuccess() throws Exception {
    Response response = resource.downloadReviewedDARs();
    assertEquals(200, response.getStatus());
  }

  @Test
  void testDownloadReviewedDARsError() throws Exception {
    doThrow(new RuntimeException()).when(darService).createReviewedDARDocument();
    Response response = resource.downloadReviewedDARs();
    assertEquals(500, response.getStatus());
  }
}
