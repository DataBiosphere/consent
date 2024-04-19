package org.broadinstitute.consent.http.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.api.client.http.HttpStatusCodes;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DatasetAssociationsResourceTest {

  private DatasetAssociationsResource resource;

  private void initResource() {
    resource = new DatasetAssociationsResource();
  }

  @Test
  void testAssociateDatasetWithUsers() {
    initResource();
    Response response = resource.associateDatasetWithUsers();
    assertEquals(HttpStatusCodes.STATUS_CODE_NO_CONTENT, response.getStatus());
  }

  @Test
  void testGetDatasetAssociations() {
    initResource();
    Response response = resource.getDatasetAssociations();
    assertEquals(HttpStatusCodes.STATUS_CODE_NO_CONTENT, response.getStatus());
  }

  @Test
  void testUpdateDatasetAssociations() {
    initResource();
    Response response = resource.updateDatasetAssociations();
    assertEquals(HttpStatusCodes.STATUS_CODE_NO_CONTENT, response.getStatus());
  }

}
