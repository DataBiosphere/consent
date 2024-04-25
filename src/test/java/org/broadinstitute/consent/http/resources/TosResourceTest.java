package org.broadinstitute.consent.http.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.google.api.client.http.HttpStatusCodes;
import jakarta.ws.rs.core.Response;
import org.broadinstitute.consent.http.service.sam.SamService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TosResourceTest {

  @Mock
  private SamService service;

  private TosResource resource;

  private void initResource() {
    resource = new TosResource(service);
  }

  @Test
  void testGetToSText() throws Exception {
    String mockText = "Plain Text";
    when(service.getToSText()).thenReturn(mockText);
    initResource();

    Response response = resource.getToSText();
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    assertEquals(mockText, response.getEntity().toString());
  }

  @Test
  void testGetDuosToSText() throws Exception {
    String mockText = "Sample Terra ToS";
    when(service.getToSText()).thenReturn(mockText);
    initResource();

    Response response = resource.getDUOSToSText();
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    assertTrue(response.getEntity().toString().contains("DUOS"));
  }
}
