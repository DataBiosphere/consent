package org.broadinstitute.consent.http.filters;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.ext.ReaderInterceptor;
import jakarta.ws.rs.ext.ReaderInterceptorContext;
import java.io.IOException;
import java.math.BigDecimal;
import org.broadinstitute.consent.http.mappers.TemplateTooLargeExceptionMapper;
import org.broadinstitute.consent.http.util.ContentLength;

/**
 * Bounds an upload request before Jersey reads it. A multipart body is written to temporary disk as
 * Jersey parses it, before the resource method runs, so the per-file limit applied while a file
 * streams to storage comes too late to protect the disk.
 *
 * <p>Two guards, as for templates ({@link TemplateSizeLimitFilter}): a {@code Content-Length} past
 * the limit is refused before a byte is read, and the entity stream is bounded, so a chunked body,
 * or one whose length understates it, stops at the limit. Either way the client gets a 413.
 */
public class UploadSizeLimitFilter implements ContainerRequestFilter, ReaderInterceptor {

  private final long maxBytes;
  private final String message;

  UploadSizeLimitFilter(long maxBytes) {
    this.maxBytes = maxBytes;
    this.message =
        "Request is too large. Max size is: %s MB"
            .formatted(BigDecimal.valueOf(maxBytes, 6).stripTrailingZeros().toPlainString());
  }

  long maxBytes() {
    return maxBytes;
  }

  @Override
  public void filter(ContainerRequestContext requestContext) {
    if (ContentLength.declared(requestContext.getHeaderString(HttpHeaders.CONTENT_LENGTH))
        > maxBytes) {
      requestContext.abortWith(TemplateTooLargeExceptionMapper.tooLarge(message));
      return;
    }
    requestContext.setEntityStream(
        new BoundedEntityStream(requestContext.getEntityStream(), maxBytes, message));
  }

  @Override
  public Object aroundReadFrom(ReaderInterceptorContext context) throws IOException {
    return BoundedEntityStream.readOrRefuse(context);
  }
}
