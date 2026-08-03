package org.broadinstitute.consent.http.models.elastic_search;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * The license state of the Elasticsearch cluster this deployment is configured against, and whether
 * a trial is still available on it.
 *
 * <p>Read-only, and the thing to read <em>before</em> activating a trial: a trial can be started
 * only once per cluster, so {@link #trialAvailable()} is the difference between an activation that
 * can still be spent and one that cannot.
 *
 * <p>{@link #dlsFlsLicensed()} answers the license question only. A cluster whose tier includes
 * DLS/FLS can still have it switched off cluster-wide by {@code xpack.security.dls_fls.enabled},
 * and a license this record calls sufficient is not on its own a promise that a filter will be
 * enforced — that is what {@link ElasticSearchCapabilityReport} is for.
 *
 * @param clusterName the cluster's own name, to confirm which cluster was reached
 * @param licenseType license tier reported by the cluster, e.g. {@code basic} or {@code trial}
 * @param licenseStatus whether that license is active
 * @param dlsFlsLicensed whether the tier includes document- and field-level security; null when the
 *     tier could not be read
 * @param trialAvailable whether the cluster is still eligible to start a trial; null when the
 *     cluster did not answer the eligibility check
 * @param detail human-readable summary of the state, including what it means for DLS/FLS
 * @param notes caveats a reader needs in order to interpret the fields above correctly
 */
public record ElasticSearchLicenseStatus(
    @SerializedName("cluster_name") String clusterName,
    @SerializedName("license_type") String licenseType,
    @SerializedName("license_status") String licenseStatus,
    @SerializedName("dls_fls_licensed") Boolean dlsFlsLicensed,
    @SerializedName("trial_available") Boolean trialAvailable,
    String detail,
    List<String> notes) {}
