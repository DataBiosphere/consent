package org.broadinstitute.consent.http.mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.api.client.http.HttpStatusCodes;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ForbiddenExceptionMapperTest {

  // Covers message.equals(GENERIC_MESSAGE) branch – no-arg constructor synthesises "HTTP 403
  // Forbidden".
  @Test
  void testMessagelessException() {
    ForbiddenExceptionMapper mapper = new ForbiddenExceptionMapper();
    try (Response response = mapper.toResponse(new ForbiddenException())) {
      assertEquals(HttpStatusCodes.STATUS_CODE_FORBIDDEN, response.getStatus());
      assertTrue(response.getEntity().toString().contains("Forbidden"));
    }
  }

  // Covers message == null branch.
  @Test
  void testNullMessage() {
    ForbiddenExceptionMapper mapper = new ForbiddenExceptionMapper();
    try (Response response = mapper.toResponse(new ForbiddenException((String) null))) {
      assertEquals(HttpStatusCodes.STATUS_CODE_FORBIDDEN, response.getStatus());
      assertEquals(
          "Forbidden",
          ((org.broadinstitute.consent.http.models.Error) response.getEntity()).message());
    }
  }

  // Covers message.isBlank() branch – empty string and whitespace-only string.
  @ParameterizedTest
  @ValueSource(strings = {"", " "})
  void testBlankMessage(String blank) {
    ForbiddenExceptionMapper mapper = new ForbiddenExceptionMapper();
    try (Response response = mapper.toResponse(new ForbiddenException(blank))) {
      assertEquals(HttpStatusCodes.STATUS_CODE_FORBIDDEN, response.getStatus());
      assertEquals(
          "Forbidden",
          ((org.broadinstitute.consent.http.models.Error) response.getEntity()).message());
    }
  }

  // Covers the else branch – a real message must be passed through unchanged.
  @Test
  void testExceptionWithMessage() {
    ForbiddenExceptionMapper mapper = new ForbiddenExceptionMapper();
    try (Response response =
        mapper.toResponse(new ForbiddenException("User does not have permission"))) {
      assertEquals(HttpStatusCodes.STATUS_CODE_FORBIDDEN, response.getStatus());
      assertTrue(response.getEntity().toString().contains("User does not have permission"));
    }
  }

  // Without an explicit type, content negotiation for a client Accept header incompatible with
  // JSON (e.g. text/xml) has no resource-method @Produces to fall back on here, since this
  // response never went through a matched resource - it can degrade to a 500 instead of the
  // intended 403 JSON body.
  @Test
  void testResponseMediaTypeIsExplicitlyJson() {
    ForbiddenExceptionMapper mapper = new ForbiddenExceptionMapper();
    try (Response response = mapper.toResponse(new ForbiddenException("nope"))) {
      assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
    }
  }
}
