package org.broadinstitute.consent.http.filters;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.ext.Provider;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.broadinstitute.consent.http.exceptions.TemplateTooLargeException;
import org.broadinstitute.consent.http.mappers.TemplateTooLargeExceptionMapper;
import org.broadinstitute.consent.http.service.studytemplate.StudyTemplateValidationService;

/**
 * Bounds a template upload at the request, which the validator's limit cannot do: a resource method
 * taking a multipart parameter is called only after Jersey has read the whole entity, so by then
 * the body has already been spooled whatever its size. The validator still owns the limit on the
 * file itself, and this owns only the boundary — the point past which no more of the request is
 * read.
 *
 * <p>Two guards, because either alone leaves a way through. A {@code Content-Length} past the cap
 * is refused before a byte of the body is read, which is the honest oversize upload. A body sent
 * chunked, or under a {@code Content-Length} that lies, has no length to check, so the entity
 * stream is bounded as well and stops the read at the cap. That second refusal is raised while
 * Jersey is reading the body, and reaches the client as a 413 through {@link
 * TemplateTooLargeExceptionMapper} rather than from the resource method; whether Jersey routes
 * every reader-phase failure there is best-effort, but the read stops at the cap either way.
 */
@Provider
@TemplateSizeLimited
@Priority(Priorities.USER)
public class TemplateSizeLimitFilter implements ContainerRequestFilter {

  /**
   * What the multipart envelope adds around the file: the boundaries, the part headers, and the
   * epilogue. Generous, since the file limit is what a client is told about and this only has to
   * leave a template of the largest allowed size room to arrive.
   */
  private static final long ENVELOPE_ALLOWANCE = 8 * 1024;

  static final long MAX_REQUEST_BYTES =
      StudyTemplateValidationService.MAX_TEMPLATE_BYTES + ENVELOPE_ALLOWANCE;

  @Override
  public void filter(ContainerRequestContext requestContext) {
    if (declaredLength(requestContext) > MAX_REQUEST_BYTES) {
      requestContext.abortWith(
          TemplateTooLargeExceptionMapper.tooLarge(
              StudyTemplateValidationService.TOO_LARGE_MESSAGE));
      return;
    }
    requestContext.setEntityStream(
        new BoundedEntityStream(requestContext.getEntityStream(), MAX_REQUEST_BYTES));
  }

  /**
   * Read as a long rather than through {@code getLength()}, whose int parse turns the header of a
   * multi-gigabyte body — the one this most needs to catch — into an unknown length.
   */
  private static long declaredLength(ContainerRequestContext requestContext) {
    String header = requestContext.getHeaderString(HttpHeaders.CONTENT_LENGTH);
    if (header == null || header.isBlank()) {
      return -1;
    }
    try {
      return Long.parseLong(header.trim());
    } catch (NumberFormatException e) {
      return -1;
    }
  }

  /** Refuses to hand out more than the cap, so a body with no declared length is bounded too. */
  private static final class BoundedEntityStream extends FilterInputStream {

    private final long limit;
    private long read;

    private BoundedEntityStream(InputStream in, long limit) {
      super(in);
      this.limit = limit;
    }

    @Override
    public int read() throws IOException {
      int value = super.read();
      if (value != -1) {
        count(1);
      }
      return value;
    }

    @Override
    public int read(byte[] buffer, int offset, int length) throws IOException {
      int count = super.read(buffer, offset, length);
      if (count > 0) {
        count(count);
      }
      return count;
    }

    @Override
    public long skip(long requested) throws IOException {
      long skipped = super.skip(requested);
      if (skipped > 0) {
        count(skipped);
      }
      return skipped;
    }

    private void count(long bytes) {
      read += bytes;
      if (read > limit) {
        throw new TemplateTooLargeException(StudyTemplateValidationService.TOO_LARGE_MESSAGE);
      }
    }
  }
}
