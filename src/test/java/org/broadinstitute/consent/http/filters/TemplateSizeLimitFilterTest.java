package org.broadinstitute.consent.http.filters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.broadinstitute.consent.http.exceptions.TemplateTooLargeException;
import org.broadinstitute.consent.http.models.Error;
import org.broadinstitute.consent.http.service.studytemplate.StudyTemplateValidationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TemplateSizeLimitFilterTest {

  private final TemplateSizeLimitFilter filter = new TemplateSizeLimitFilter();

  @Test
  void testFilterRefusesADeclaredLengthPastTheCap() {
    ContainerRequestContext request =
        requestOfDeclaredLength(Long.toString(TemplateSizeLimitFilter.MAX_REQUEST_BYTES + 1));

    filter.filter(request);

    ArgumentCaptor<Response> refusal = ArgumentCaptor.forClass(Response.class);
    verify(request).abortWith(refusal.capture());
    assertEquals(
        Response.Status.REQUEST_ENTITY_TOO_LARGE.getStatusCode(), refusal.getValue().getStatus());
    assertEquals(
        new Error(
            StudyTemplateValidationService.TOO_LARGE_MESSAGE,
            Response.Status.REQUEST_ENTITY_TOO_LARGE.getStatusCode()),
        refusal.getValue().getEntity());
    // Refused on the header alone: the body was never asked for, which is the point of the guard.
    verify(request, never()).getEntityStream();
  }

  @Test
  void testFilterRefusesALengthNoIntCouldHold() {
    // The multi-gigabyte upload the guard exists for, whose header overflows an int parse.
    ContainerRequestContext request = requestOfDeclaredLength("5000000000");

    filter.filter(request);

    verify(request).abortWith(any(Response.class));
  }

  @Test
  void testFilterAdmitsADeclaredLengthWithinTheCap() {
    ContainerRequestContext request =
        requestOfDeclaredLength(Long.toString(TemplateSizeLimitFilter.MAX_REQUEST_BYTES));
    when(request.getEntityStream()).thenReturn(InputStream.nullInputStream());

    filter.filter(request);

    verify(request, never()).abortWith(any());
    verify(request).setEntityStream(any());
  }

  @Test
  void testFilterBoundsABodyThatDeclaresNoLength() throws IOException {
    // Chunked, or lying: there is no length to refuse on, so the stream itself has to stop.
    InputStream bounded =
        boundedStreamOver(new byte[(int) TemplateSizeLimitFilter.MAX_REQUEST_BYTES + 1], null);

    TemplateTooLargeException e =
        assertThrows(TemplateTooLargeException.class, () -> bounded.readAllBytes());
    assertEquals(StudyTemplateValidationService.TOO_LARGE_MESSAGE, e.getMessage());
  }

  @Test
  void testFilterBoundsABodyThatUnderstatesItsLength() throws IOException {
    InputStream bounded =
        boundedStreamOver(new byte[(int) TemplateSizeLimitFilter.MAX_REQUEST_BYTES + 1], "10");

    assertThrows(TemplateTooLargeException.class, () -> bounded.readAllBytes());
  }

  @Test
  void testTheBoundedStreamPassesABodyWithinTheCapThrough() throws IOException {
    byte[] body = "1,study\n2,Greg".getBytes();
    InputStream bounded = boundedStreamOver(body, Integer.toString(body.length));

    assertEquals(new String(body), new String(bounded.readAllBytes()));
    assertEquals(-1, bounded.read());
  }

  /** The stream the filter installs over a body, read back for the assertions above. */
  private InputStream boundedStreamOver(byte[] body, String declaredLength) {
    ContainerRequestContext request = requestOfDeclaredLength(declaredLength);
    when(request.getEntityStream()).thenReturn(new ByteArrayInputStream(body));

    filter.filter(request);

    ArgumentCaptor<InputStream> installed = ArgumentCaptor.forClass(InputStream.class);
    verify(request).setEntityStream(installed.capture());
    return installed.getValue();
  }

  private ContainerRequestContext requestOfDeclaredLength(String declaredLength) {
    ContainerRequestContext request = mock(ContainerRequestContext.class);
    when(request.getHeaderString(HttpHeaders.CONTENT_LENGTH)).thenReturn(declaredLength);
    return request;
  }
}
