package org.broadinstitute.consent.http.mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.broadinstitute.consent.http.exceptions.RequestTooLargeException;
import org.broadinstitute.consent.http.models.Error;
import org.junit.jupiter.api.Test;

class RequestTooLargeExceptionMapperTest {

  private final RequestTooLargeExceptionMapper mapper = new RequestTooLargeExceptionMapper();

  @Test
  void testToResponseAnswersAStoppedBodyWith413() {
    Response response = mapper.toResponse(new RequestTooLargeException("Template is too large"));

    assertEquals(Response.Status.REQUEST_ENTITY_TOO_LARGE.getStatusCode(), response.getStatus());
    assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
    assertEquals(
        new Error(
            "Template is too large", Response.Status.REQUEST_ENTITY_TOO_LARGE.getStatusCode()),
        response.getEntity());
  }
}
