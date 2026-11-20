package org.broadinstitute.consent.http.filters;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ReaderInterceptorContext;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.broadinstitute.consent.http.exceptions.RequestTooLargeException;
import org.broadinstitute.consent.http.models.Error;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class UploadSizeLimitFilterTest {

  private final UploadSizeLimitFilter filter = new UploadSizeLimitFilter(10);

  // A declared length past the limit is refused before a byte of the body is read.
  @Test
  void testFilterRefusesADeclaredLengthPastTheLimit() {
    ContainerRequestContext request = request("11", InputStream.nullInputStream());

    filter.filter(request);

    ArgumentCaptor<Response> refusal = ArgumentCaptor.forClass(Response.class);
    verify(request).abortWith(refusal.capture());
    assertEquals(413, refusal.getValue().getStatus());
    assertEquals(
        new Error("Request is too large. Max size is: 0.00001 MB", 413),
        refusal.getValue().getEntity());
    verify(request, never()).getEntityStream();
  }

  // A body with no length, or one that understates it, stops at the limit as it's read.
  @Test
  void testFilterBoundsABodyThatDeclaresNoLength() {
    ContainerRequestContext request = request(null, new ByteArrayInputStream(new byte[11]));

    filter.filter(request);

    InputStream bounded = boundedStream(request);
    RequestTooLargeException e =
        assertThrows(RequestTooLargeException.class, bounded::readAllBytes);
    assertEquals("Request is too large. Max size is: 0.00001 MB", e.getMessage());
  }

  @Test
  void testFilterBoundsABodyThatUnderstatesItsLength() {
    ContainerRequestContext request = request("5", new ByteArrayInputStream(new byte[11]));

    filter.filter(request);

    assertThrows(RequestTooLargeException.class, boundedStream(request)::readAllBytes);
  }

  // A body at the limit passes through whole.
  @Test
  void testFilterPassesABodyAtTheLimit() throws IOException {
    byte[] body = "0123456789".getBytes();
    ContainerRequestContext request = request("10", new ByteArrayInputStream(body));

    filter.filter(request);

    verify(request, never()).abortWith(any());
    assertArrayEquals(body, boundedStream(request).readAllBytes());
  }

  @Test
  void testTheMessageGivesTheDefaultLimitInMegabytes() {
    ContainerRequestContext request = request("100000001", InputStream.nullInputStream());

    new UploadSizeLimitFilter(100_000_000L).filter(request);

    ArgumentCaptor<Response> refusal = ArgumentCaptor.forClass(Response.class);
    verify(request).abortWith(refusal.capture());
    assertEquals(
        new Error("Request is too large. Max size is: 100 MB", 413),
        refusal.getValue().getEntity());
  }

  // A read the bound stopped is answered with a 413, after the reader has cleaned up.
  @Test
  void testAReadTheStreamStoppedIsAnswered413() throws IOException {
    ReaderInterceptorContext context = mock(ReaderInterceptorContext.class);
    when(context.proceed())
        .thenThrow(new RequestTooLargeException("Request is too large. Max size is: 0.00001 MB"));

    WebApplicationException e =
        assertThrows(WebApplicationException.class, () -> filter.aroundReadFrom(context));
    assertEquals(413, e.getResponse().getStatus());
    assertEquals(
        new Error("Request is too large. Max size is: 0.00001 MB", 413),
        e.getResponse().getEntity());
  }

  private static ContainerRequestContext request(String declaredLength, InputStream body) {
    ContainerRequestContext request = mock(ContainerRequestContext.class);
    when(request.getHeaderString(HttpHeaders.CONTENT_LENGTH)).thenReturn(declaredLength);
    if (declaredLength == null || Long.parseLong(declaredLength) <= 10) {
      when(request.getEntityStream()).thenReturn(body);
    }
    return request;
  }

  private static InputStream boundedStream(ContainerRequestContext request) {
    ArgumentCaptor<InputStream> stream = ArgumentCaptor.forClass(InputStream.class);
    verify(request).setEntityStream(stream.capture());
    return stream.getValue();
  }
}
