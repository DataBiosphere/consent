package org.broadinstitute.consent.http.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.api.client.http.HttpStatusCodes;
import jakarta.ws.rs.core.Response;
import org.broadinstitute.consent.http.util.JsonSchemaUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SchemaResourceTest {


  private final JsonSchemaUtil jsonSchemaUtil = new JsonSchemaUtil();

  @Test
  void testGetDatasetRegistrationSchemaV1() {
    SchemaResource resource = new SchemaResource();

    Response response = resource.getDatasetRegistrationSchemaV1();
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());

    Object body = response.getEntity();
    assertEquals(jsonSchemaUtil.getDatasetRegistrationSchemaV1(), body.toString());
  }

}
