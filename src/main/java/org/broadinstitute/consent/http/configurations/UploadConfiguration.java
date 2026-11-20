package org.broadinstitute.consent.http.configurations;

import jakarta.validation.constraints.Min;

/** Limits on upload requests. */
public class UploadConfiguration {

  /**
   * The largest upload request, in bytes, before Jersey reads it: what a request may write to
   * temporary disk. An endpoint can set its own ({@code UploadSizeLimited#maxBytes}). Defaults to
   * 100 MB, room for several files under the per-file limit.
   */
  @Min(1)
  private long maxRequestBytes = 100_000_000L;

  public long getMaxRequestBytes() {
    return maxRequestBytes;
  }

  public void setMaxRequestBytes(long maxRequestBytes) {
    this.maxRequestBytes = maxRequestBytes;
  }
}
