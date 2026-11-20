package org.broadinstitute.consent.http.filters;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.ext.ReaderInterceptorContext;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.broadinstitute.consent.http.exceptions.RequestTooLargeException;
import org.broadinstitute.consent.http.mappers.TemplateTooLargeExceptionMapper;

/**
 * Refuses to hand out more than a limit, so a request body with no declared length (or one that
 * understates it) is bounded too. Past the limit, a read throws {@link RequestTooLargeException}.
 */
final class BoundedEntityStream extends FilterInputStream {

  private final long limit;
  private final String message;
  private long read;

  BoundedEntityStream(InputStream in, long limit, String message) {
    super(in);
    this.limit = limit;
    this.message = message;
  }

  /**
   * Reads the entity, answering a read this stream stopped with a 413. It has to be answered here,
   * around the reader: Jersey wraps an {@link IOException} from a reader in a {@code
   * ProcessingException} that no exception mapper sees, and the client would get a 500. By the time
   * the exception gets here, MIMEPull has already deleted the temporary files of a multipart body.
   */
  static Object readOrRefuse(ReaderInterceptorContext context) throws IOException {
    try {
      return context.proceed();
    } catch (RequestTooLargeException e) {
      throw new WebApplicationException(TemplateTooLargeExceptionMapper.tooLarge(e.getMessage()));
    }
  }

  /** Bytes read again after a reset would be counted twice, so marks are refused. */
  @Override
  public boolean markSupported() {
    return false;
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

  private void count(long bytes) throws RequestTooLargeException {
    read += bytes;
    if (read > limit) {
      throw new RequestTooLargeException(message);
    }
  }
}
