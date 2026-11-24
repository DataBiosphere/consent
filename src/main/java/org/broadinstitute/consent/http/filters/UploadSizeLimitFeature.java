package org.broadinstitute.consent.http.filters;

import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.DynamicFeature;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.FeatureContext;

/**
 * Gives each {@link UploadSizeLimited} endpoint a {@link UploadSizeLimitFilter} with its limit: the
 * endpoint's own, or the configured default.
 */
public class UploadSizeLimitFeature implements DynamicFeature {

  private final long defaultMaxBytes;

  public UploadSizeLimitFeature(long defaultMaxBytes) {
    this.defaultMaxBytes = defaultMaxBytes;
  }

  @Override
  public void configure(ResourceInfo resourceInfo, FeatureContext context) {
    UploadSizeLimited limited =
        resourceInfo.getResourceMethod().getAnnotation(UploadSizeLimited.class);
    if (limited != null) {
      long maxBytes = limited.maxBytes() > 0 ? limited.maxBytes() : defaultMaxBytes;
      context.register(new UploadSizeLimitFilter(maxBytes), Priorities.USER);
    }
  }
}
