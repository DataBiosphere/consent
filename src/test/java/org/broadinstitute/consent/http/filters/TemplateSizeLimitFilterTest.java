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
  void testFilterBoundsABodyThatDeclaresNoLength() {
    // Chunked, or lying: there is no length to refuse on, so the stream itself has to stop.
    InputStream bounded =
        boundedStreamOver(new byte[(int) TemplateSizeLimitFilter.MAX_REQUEST_BYTES + 1], null);

    TemplateTooLargeException e =
        assertThrows(TemplateTooLargeException.class, bounded::readAllBytes);
    assertEquals(StudyTemplateValidationService.TOO_LARGE_MESSAGE, e.getMessage());
  }

  @Test
  void testFilterBoundsABodyThatUnderstatesItsLength() {
    InputStream bounded =
        boundedStreamOver(new byte[(int) TemplateSizeLimitFilter.MAX_REQUEST_BYTES + 1], "10");

    assertThrows(TemplateTooLargeException.class, bounded::readAllBytes);
  }

  @Test
  void testTheBoundedStreamPassesABodyWithinTheCapThrough() throws IOException {
    byte[] body = "1,study\n2,Greg".getBytes();
    InputStream bounded = boundedStreamOver(body, Integer.toString(body.length));

    assertEquals(new String(body), new String(bounded.readAllBytes()));
    assertEquals(-1, bounded.read());
  }

  @Test
  void testFilterTreatsAHeaderItCannotReadAsNoLength() {
    // Blank, or not a number: there is nothing to refuse on, so only the stream bound applies.
    for (String header : new String[] {"   ", "not-a-number"}) {
      ContainerRequestContext request = requestOfDeclaredLength(header);
      when(request.getEntityStream()).thenReturn(InputStream.nullInputStream());

      filter.filter(request);

      verify(request, never()).abortWith(any());
      verify(request).setEntityStream(any());
    }
  }

  @Test
  void testTheBoundedStreamCountsABodyReadOneByteAtATime() {
    InputStream bounded = boundedStreamOver(oversizeBody(), null);

    assertThrows(TemplateTooLargeException.class, () -> readEveryByte(bounded));
  }

  @Test
  void testTheBoundedStreamCountsBytesSkippedRatherThanRead() {
    // Skipping past the cap consumes the body just as reading it does.
    InputStream bounded = boundedStreamOver(oversizeBody(), null);

    assertThrows(
        TemplateTooLargeException.class,
        () -> bounded.skip(TemplateSizeLimitFilter.MAX_REQUEST_BYTES + 1));
  }

  @Test
  void testTheBoundedStreamPassesASkipWithinTheCapThrough() throws IOException {
    byte[] body = "1,study\n2,Greg".getBytes();
    InputStream bounded = boundedStreamOver(body, Integer.toString(body.length));

    assertEquals(2, bounded.skip(2));
    assertEquals("study", new String(bounded.readNBytes(5)));

    // Nothing left to skip, so nothing is counted against the cap.
    bounded.readAllBytes();
    assertEquals(0, bounded.skip(1));
  }

  private static void readEveryByte(InputStream stream) throws IOException {
    while (stream.read() != -1) {
      // Counted a byte at a time, which is the read the array overload does not cover.
    }
  }

  private static byte[] oversizeBody() {
    return new byte[(int) TemplateSizeLimitFilter.MAX_REQUEST_BYTES + 1];
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
