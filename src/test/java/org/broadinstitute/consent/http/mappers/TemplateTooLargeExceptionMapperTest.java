package org.broadinstitute.consent.http.mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.broadinstitute.consent.http.exceptions.TemplateTooLargeException;
import org.broadinstitute.consent.http.models.Error;
import org.junit.jupiter.api.Test;

class TemplateTooLargeExceptionMapperTest {

  private final TemplateTooLargeExceptionMapper mapper = new TemplateTooLargeExceptionMapper();

  @Test
  void testToResponseAnswersARefusalWith413() {
    // The mapped path: a refusal the resource method does not catch itself.
    Response response = mapper.toResponse(new TemplateTooLargeException("Template is too large"));

    assertEquals(Response.Status.REQUEST_ENTITY_TOO_LARGE.getStatusCode(), response.getStatus());
    assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
    assertEquals(
        new Error(
            "Template is too large", Response.Status.REQUEST_ENTITY_TOO_LARGE.getStatusCode()),
        response.getEntity());
  }
}
