package org.broadinstitute.consent.http.filters;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.broadinstitute.consent.http.exceptions.RequestTooLargeException;

/**
 * Refuses to hand out more than a limit, so a request body with no declared length (or one that
 * understates it) is bounded too. Past the limit, a read throws {@link RequestTooLargeException},
 * which {@code RequestTooLargeExceptionMapper} answers with a 413.
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

  /** Bytes read again after a reset would be counted twice, so marks are refused. */
  @Override
  public boolean markSupported() {
    return false;
  }

  /** Does nothing: the wrapped stream must not keep a mark this stream can't count. */
  @Override
  public void mark(int readLimit) {
    // Marks are not supported.
  }

  @Override
  public void reset() throws IOException {
    throw new IOException("mark/reset not supported");
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
