package org.broadinstitute.consent.http.models.elastic_search;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import java.util.Map;

/**
 * The Elasticsearch security feature inventory for the cluster this deployment is configured
 * against.
 *
 * <p>Because each environment runs its own Consent deployment pointed at its own cluster, calling
 * the endpoint that produces this report in dev, staging, and production yields the per-environment
 * record that Ticket A-1 requires.
 *
 * @param clusterName the cluster's own name, to confirm which cluster was reached
 * @param version Elasticsearch version, e.g. {@code 9.3.3}
 * @param distribution the distribution reported by the cluster, e.g. {@code elasticsearch}
 * @param edition OSS, a license tier, or Elastic Cloud
 * @param licenseType license tier reported by the cluster
 * @param licenseStatus whether that license is active
 * @param elasticCloud whether the deployment is configured with a cloud ID
 * @param securityEnabled whether X-Pack Security is on
 * @param authenticatedUser the principal the shared credential resolves to
 * @param authenticatedUserRoles that principal's roles
 * @param clusterPrivileges cluster privileges the shared credential holds, from a read-only check
 * @param securitySettings the cluster's {@code xpack.security.*} settings
 * @param writeProbesRun whether write probes ran; when false the DLS, FLS, and API-key verdicts are
 *     inferred from the license tier rather than observed, which is the first thing a reader of
 *     this report needs to know
 * @param capabilities the capability inventory itself
 * @param restClientCompatibility whether the bundled REST client can drive the security APIs
 * @param recommendation which implementation path the findings point to
 * @param notes caveats a reader needs in order to interpret the report correctly
 */
public record ElasticSearchCapabilityReport(
    @SerializedName("cluster_name") String clusterName,
    String version,
    String distribution,
    String edition,
    @SerializedName("license_type") String licenseType,
    @SerializedName("license_status") String licenseStatus,
    @SerializedName("elastic_cloud") Boolean elasticCloud,
    @SerializedName("security_enabled") Boolean securityEnabled,
    @SerializedName("authenticated_user") String authenticatedUser,
    @SerializedName("authenticated_user_roles") List<String> authenticatedUserRoles,
    @SerializedName("cluster_privileges") Map<String, Boolean> clusterPrivileges,
    @SerializedName("security_settings") Map<String, String> securitySettings,
    @SerializedName("write_probes_run") Boolean writeProbesRun,
    List<ElasticSearchCapability> capabilities,
    @SerializedName("rest_client_compatibility") String restClientCompatibility,
    String recommendation,
    List<String> notes) {}
