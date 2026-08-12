package org.broadinstitute.consent.http.enumeration;

import com.google.gson.annotations.SerializedName;

/**
 * Which Signing Official authorization model applies to a dataset, derived from whether the
 * dataset's DAC has the REQUIRE_SO_DAR_APPROVAL automation rule enabled. Surfaced on indexed
 * datasets so clients do not have to resolve DAC rules themselves.
 *
 * <p>The wire values are pinned with {@link SerializedName} so renaming a constant cannot silently
 * change the published contract.
 */
public enum SoApprovalModel {
  /** The SO named in each DAR must approve that request before the DAC reviews it. */
  @SerializedName("PER_DAR")
  PER_DAR,

  /** The SO authorizes researchers in advance; no per-request SO approval is needed. */
  @SerializedName("PRE_AUTHORIZED")
  PRE_AUTHORIZED
}
