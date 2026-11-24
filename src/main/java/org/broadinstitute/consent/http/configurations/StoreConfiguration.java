package org.broadinstitute.consent.http.configurations;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@JsonIgnoreProperties(ignoreUnknown = true)
public class StoreConfiguration {

  /**
   * Path to a service account key file. Optional: when blank, {@code GCSService} uses Application
   * Default Credentials instead (the local development path). Deployed environments always set it.
   */
  public String password;

  @NotNull public String endpoint;

  @NotNull public String bucket;

  /**
   * The largest document an upload may store, in bytes. Uploads stream to the bucket and stop past
   * it, whatever size the client declares. Defaults to 25 MB. Bounded so a support attachment, read
   * into one array, fits.
   */
  @Min(1)
  @Max(2_000_000_000L)
  private long maxUploadBytes = 25_000_000L;

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getEndpoint() {
    return endpoint;
  }

  public void setEndpoint(String endpoint) {
    this.endpoint = endpoint;
  }

  public String getBucket() {
    return bucket;
  }

  public void setBucket(String bucket) {
    this.bucket = bucket;
  }

  public long getMaxUploadBytes() {
    return maxUploadBytes;
  }

  public void setMaxUploadBytes(long maxUploadBytes) {
    this.maxUploadBytes = maxUploadBytes;
  }
}
