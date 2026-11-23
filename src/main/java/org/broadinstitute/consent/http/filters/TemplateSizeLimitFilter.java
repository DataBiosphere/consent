package org.broadinstitute.consent.http.filters;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.ext.Provider;
import org.broadinstitute.consent.http.mappers.TemplateTooLargeExceptionMapper;
import org.broadinstitute.consent.http.service.studytemplate.StudyTemplateValidationService;
import org.broadinstitute.consent.http.util.ContentLength;

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
 * Jersey is reading the body, after MIMEPull has deleted what it wrote to temporary disk, and is
 * answered with a 413 by {@code RequestTooLargeExceptionMapper}.
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
  private static final long ENVELOPE_ALLOWANCE = 8L * 1024;

  static final long MAX_REQUEST_BYTES =
      StudyTemplateValidationService.MAX_TEMPLATE_BYTES + ENVELOPE_ALLOWANCE;

  @Override
  public void filter(ContainerRequestContext requestContext) {
    if (ContentLength.declared(requestContext.getHeaderString(HttpHeaders.CONTENT_LENGTH))
        > MAX_REQUEST_BYTES) {
      requestContext.abortWith(
          TemplateTooLargeExceptionMapper.tooLarge(
              StudyTemplateValidationService.TOO_LARGE_MESSAGE));
      return;
    }
    requestContext.setEntityStream(
        new BoundedEntityStream(
            requestContext.getEntityStream(),
            MAX_REQUEST_BYTES,
            StudyTemplateValidationService.TOO_LARGE_MESSAGE));
  }
}
