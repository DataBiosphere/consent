package org.broadinstitute.consent.http.configurations;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Optional {@code showcase} block in the application config. {@code publicLookupEnabled} is {@code
 * false} when the block or the field is missing, and then Consent does not register the anonymous
 * {@code POST /showcase/datasets} lookup.
 *
 * <p>Turn it on in an environment only after the pre-exposure checks in the S2.2 plan are done:
 * steward and AppSec approval of the returned fields, and a check in that environment that the
 * public rate limit keys on the caller's IP.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ShowcaseConfiguration {

  private boolean publicLookupEnabled = false;

  public boolean isPublicLookupEnabled() {
    return publicLookupEnabled;
  }

  public void setPublicLookupEnabled(boolean publicLookupEnabled) {
    this.publicLookupEnabled = publicLookupEnabled;
  }
}
