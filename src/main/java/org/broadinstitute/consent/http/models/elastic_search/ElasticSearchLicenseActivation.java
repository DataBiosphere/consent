package org.broadinstitute.consent.http.models.elastic_search;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * The result of asking the Elasticsearch cluster this deployment is configured against to start its
 * trial license.
 *
 * <p>Carries the license state from both sides of the call rather than only the resulting one. A
 * trial can be started once per cluster and cannot be undone, so "what this changed" is the part of
 * the answer worth keeping: {@code license_before} shows what was given up and {@code
 * license_after} what was obtained, and together with {@link #outcome()} they say whether this call
 * was the one that spent the trial.
 *
 * @param outcome what the call did — only {@link LicenseActivationOutcome#ACTIVATED} changed the
 *     cluster
 * @param detail human-readable expansion of the outcome, including the cluster's own words when it
 *     refused
 * @param licenseBefore the license state read immediately before the attempt
 * @param licenseAfter the license state read immediately after it; equal to {@code licenseBefore}
 *     when nothing was changed
 * @param notes caveats a reader needs, foremost that a trial is one-shot per cluster
 */
public record ElasticSearchLicenseActivation(
    LicenseActivationOutcome outcome,
    String detail,
    @SerializedName("license_before") ElasticSearchLicenseStatus licenseBefore,
    @SerializedName("license_after") ElasticSearchLicenseStatus licenseAfter,
    List<String> notes) {}
