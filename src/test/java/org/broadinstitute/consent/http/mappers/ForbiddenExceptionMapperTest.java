package org.broadinstitute.consent.http.mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.api.client.http.HttpStatusCodes;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

class ForbiddenExceptionMapperTest {

  @Test
  void testMessagelessException() {
    ForbiddenExceptionMapper mapper = new ForbiddenExceptionMapper();
    try (Response response = mapper.toResponse(new ForbiddenException())) {
      assertEquals(HttpStatusCodes.STATUS_CODE_FORBIDDEN, response.getStatus());
      assertTrue(response.getEntity().toString().contains("Forbidden"));
    }
  }

  @Test
  void testExceptionWithMessage() {
    ForbiddenExceptionMapper mapper = new ForbiddenExceptionMapper();
    try (Response response =
        mapper.toResponse(new ForbiddenException("User does not have permission"))) {
      assertEquals(HttpStatusCodes.STATUS_CODE_FORBIDDEN, response.getStatus());
      assertTrue(response.getEntity().toString().contains("User does not have permission"));
    }
  }
}
