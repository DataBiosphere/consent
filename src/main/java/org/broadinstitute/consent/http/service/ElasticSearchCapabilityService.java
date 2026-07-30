package org.broadinstitute.consent.http.service;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.inject.Inject;
import jakarta.ws.rs.HttpMethod;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.http.Header;
import org.apache.http.message.BasicHeader;
import org.broadinstitute.consent.http.configurations.ElasticSearchConfiguration;
import org.broadinstitute.consent.http.models.elastic_search.CapabilityVerdict;
import org.broadinstitute.consent.http.models.elastic_search.ElasticSearchCapability;
import org.broadinstitute.consent.http.models.elastic_search.ElasticSearchCapabilityReport;
import org.broadinstitute.consent.http.util.ConsentLogger;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseException;
import org.elasticsearch.client.RestClient;

/**
 * Inventories the security features of the Elasticsearch cluster this deployment is configured
 * against: version, edition, X-Pack Security, DLS, FLS, API keys, and {@code run_as}.
 *
 * <p><b>Every probe here targets Elasticsearch's X-Pack security APIs.</b> This service is scoped
 * to Elasticsearch deployments only; it does not detect or account for other search engines.
 *
 * <p><b>The default pass is non-destructive.</b> Nothing is created, updated, or deleted, so it is
 * safe to run anywhere — but DLS, FLS, and API keys cannot be *proven* without creating a role or a
 * key, so a read-only pass reports those with an {@code INFERRED_} verdict reasoned from the
 * license tier. Only {@code run_as} — whose probe is a header on a read-only request — and X-Pack
 * Security itself come back as observed fact.
 *
 * <p>The single exception to "read-only" in the strict HTTP sense is {@code POST
 * /_security/user/_has_privileges}, which is a POST only because it takes a request body; it
 * evaluates the caller's privileges and changes nothing.
 *
 * <p><b>Write probes turn those inferences into observations,</b> and must be asked for explicitly.
 * They mint a short-lived API key, create a role carrying a DLS query and an FLS grant, and then
 * use a key whose {@code role_descriptors} carry those same filters to check that the cluster
 * actually enforces them against the real dataset index — which is Epic D's exact mechanism, and
 * the one thing a license inference cannot tell you, since a Basic cluster accepts a key carrying a
 * DLS descriptor and only fails later at search time. Everything created is namespaced, expires
 * within {@value #PROBE_KEY_EXPIRATION}, and is torn down in a {@code finally} block.
 *
 * <p>Because each environment's deployment already holds its own cluster credential, running this
 * with write probes in each environment produces the measured per-environment record without anyone
 * needing cluster network access or a copy of a secret.
 *
 * <p>All calls go through the low-level {@link RestClient} using {@link Request}/{@link Response},
 * which is what {@code ElasticSearchSupport.createRestClient} builds. That this class works at all
 * is itself the answer to whether the bundled {@code elasticsearch-rest-client} can drive the
 * security APIs.
 */
public class ElasticSearchCapabilityService implements ConsentLogger {

  /** License tiers that include document- and field-level security. */
  private static final Set<String> DLS_FLS_LICENSES = Set.of("platinum", "enterprise", "trial");

  /** License tiers that include security, but not DLS/FLS. */
  private static final Set<String> SECURITY_ONLY_LICENSES = Set.of("basic", "standard", "gold");

  /** Cluster privileges worth knowing about before designing per-request credential minting. */
  private static final List<String> PROBED_CLUSTER_PRIVILEGES =
      List.of(
          "manage_security",
          "manage_api_key",
          "grant_api_key",
          "manage_own_api_key",
          "read_security",
          "monitor");

  // nosemgrep - a cluster setting name, not a key
  private static final String API_KEY_ENABLED_SETTING = "xpack.security.authc.api_key.enabled";
  private static final String VERSION_FIELD = "version";
  private static final String LICENSE_FIELD = "license";
  private static final String USERNAME_FIELD = "username";

  /**
   * The cluster-default security settings worth reporting: each one either gates a capability this
   * report covers, or describes the authentication posture a reader needs in order to interpret the
   * verdicts. Everything else in the {@code xpack.security.*} default namespace is tuning.
   */
  private static final Set<String> REPORTED_SECURITY_DEFAULTS =
      Set.of(
          "xpack.security.enabled",
          "xpack.security.dls_fls.enabled",
          API_KEY_ENABLED_SETTING,
          "xpack.security.authc.run_as.enabled",
          "xpack.security.authc.token.enabled",
          "xpack.security.authc.anonymous.username",
          "xpack.security.authc.anonymous.roles",
          "xpack.security.authc.reserved_realm.enabled",
          "xpack.security.audit.enabled",
          "xpack.security.operator_privileges.enabled",
          "xpack.security.fips_mode.enabled",
          "xpack.security.http.ssl.enabled",
          "xpack.security.transport.ssl.enabled");

  private static final String AUTHENTICATE_PATH = "/_security/_authenticate";
  // nosemgrep - an endpoint path, not a key
  private static final String API_KEY_PATH = "/_security/api_key";
  private static final String RUN_AS_HEADER = "es-security-runas-user";

  /**
   * A caller-supplied username goes into an HTTP header, and the transport does not validate header
   * values, so a value carrying CR or LF could append arbitrary requests to the one this service
   * sends on the deployment's own credential. Restricted to the characters an Elasticsearch
   * username actually uses; anything else is refused before it reaches the cluster.
   */
  private static final Pattern RUN_AS_USERNAME = Pattern.compile("[A-Za-z0-9._@+\\-]{1,255}");

  // nosemgrep - a capability label in the report, not a key
  private static final String API_KEYS = "API keys";
  private static final String DLS = "Document-level security (DLS)";
  private static final String FLS = "Field-level security (FLS)";
  private static final String RUN_AS = "run_as impersonation";

  /** How long a probe API key lives even if teardown never runs. */
  // nosemgrep - a duration, not a key
  private static final String PROBE_KEY_EXPIRATION = "10m";

  /** The field a probe FLS grant is scoped to; a real field of the dataset index. */
  private static final String FLS_GRANT_FIELD = "datasetIdentifier";

  /**
   * The role descriptor every probe key that does not need privileges is created with. Omitting
   * {@code role_descriptors} would instead give the key a snapshot of the deployment credential's
   * own permissions — on a cluster where that credential is broadly privileged, a live copy of it.
   * An empty descriptor grants nothing, and {@code GET /_security/_authenticate} needs nothing, so
   * the round trip is proven just as well by a key that can do nothing else.
   */
  // nosemgrep - an empty privilege grant, not a key
  private static final String PRIVILEGE_FREE_DESCRIPTOR =
      "{\"probe\":{\"cluster\":[],\"indices\":[]}}";

  /**
   * Builds a client that authenticates as an API key instead of the deployment's shared credential.
   * A seam rather than an inline {@code RestClient.builder} call so the write probes can be tested
   * without a live cluster.
   */
  public interface ApiKeyClientFactory {

    RestClient create(String encodedApiKey);
  }

  private final RestClient esClient;
  private final ElasticSearchConfiguration esConfig;
  private final ApiKeyClientFactory apiKeyClientFactory;

  @Inject
  public ElasticSearchCapabilityService(RestClient esClient, ElasticSearchConfiguration esConfig) {
    this(esClient, esConfig, defaultApiKeyClientFactory(esClient));
  }

  ElasticSearchCapabilityService(
      RestClient esClient,
      ElasticSearchConfiguration esConfig,
      ApiKeyClientFactory apiKeyClientFactory) {
    this.esClient = esClient;
    this.esConfig = esConfig;
    this.apiKeyClientFactory = apiKeyClientFactory;
  }

  /**
   * Points a second client at the same host the injected client uses, so an API key is exercised
   * against the same cluster — including when the deployment is configured by cloud ID, which the
   * injected client has already resolved to a host.
   */
  private static ApiKeyClientFactory defaultApiKeyClientFactory(RestClient esClient) {
    return encodedApiKey ->
        RestClient.builder(esClient.getNodes().get(0).getHost())
            .setDefaultHeaders(
                new Header[] {new BasicHeader("Authorization", "ApiKey " + encodedApiKey)})
            .build();
  }

  /**
   * Builds the capability report.
   *
   * @param runAsUser username to attempt impersonation against; when null the probe targets the
   *     authenticated user itself, which still proves whether the feature is licensed and enabled
   * @param writeProbes when true, additionally create and tear down a short-lived API key and role
   *     to convert the inferred DLS, FLS, and API-key verdicts into observed ones
   * @return the inventory for this deployment's cluster
   */
  public ElasticSearchCapabilityReport getCapabilityReport(String runAsUser, boolean writeProbes) {
    List<String> notes = new ArrayList<>();
    if (!writeProbes) {
      notes.add(
          "All probes were non-destructive. Nothing was created, modified, or deleted on the "
              + "cluster.");
    }

    ProbeResult root = probe(HttpMethod.GET, "/");
    if (root.status() == 0) {
      return unreachableReport(notes);
    }

    String version = string(root.body(), VERSION_FIELD, "number");
    String distribution =
        stringOrDefault(root.body(), "elasticsearch", VERSION_FIELD, "distribution");

    ProbeResult xpack = probe(HttpMethod.GET, "/_xpack");
    ProbeResult license = probe(HttpMethod.GET, "/_license");
    String licenseType = string(license.body(), LICENSE_FIELD, "type");
    String licenseStatus = string(license.body(), LICENSE_FIELD, "status");

    Map<String, String> securitySettings = securitySettings();
    Boolean securityEnabled = securityEnabled(xpack, securitySettings);

    ProbeResult authenticate = probe(HttpMethod.GET, AUTHENTICATE_PATH);
    boolean securityApiPresent = securityApiPresent(authenticate.status());
    String authenticatedUser = string(authenticate.body(), USERNAME_FIELD);
    List<String> roles = stringList(authenticate.body(), "roles");

    Map<String, Boolean> clusterPrivileges = securityApiPresent ? clusterPrivileges() : Map.of();

    boolean elasticCloud = esConfig.getCloudId() != null && !esConfig.getCloudId().trim().isEmpty();
    boolean writeProbesRan = writeProbes && securityApiPresent;
    notes.addAll(deploymentNotes(elasticCloud, securityApiPresent, writeProbes, writeProbesRan));

    WriteProbeOutcome writeProbeOutcome = writeProbesRan ? runWriteProbes(notes) : null;

    List<ElasticSearchCapability> capabilities =
        buildCapabilities(
            writeProbeOutcome,
            new ClusterObservations(
                securityEnabled,
                securityApiPresent,
                xpack,
                securitySettings,
                clusterPrivileges,
                licenseType,
                authenticatedUser,
                runAsUser));

    return new ElasticSearchCapabilityReport(
        string(root.body(), "cluster_name"),
        version,
        distribution,
        edition(
            elasticCloud,
            string(root.body(), VERSION_FIELD, "build_flavor"),
            xpack.status(),
            licenseType),
        licenseType,
        licenseStatus,
        elasticCloud,
        securityEnabled,
        authenticatedUser,
        roles,
        clusterPrivileges,
        securitySettings,
        writeProbesRan,
        capabilities,
        restClientCompatibility(version),
        recommendation(securityApiPresent, licenseType, writeProbeOutcome),
        notes);
  }

  /**
   * The advisory notes that depend only on the shape of the deployment and cluster, not on any
   * probe result — split out of {@link #getCapabilityReport} so its branching does not compound
   * with the rest of that method's own.
   */
  private List<String> deploymentNotes(
      boolean elasticCloud,
      boolean securityApiPresent,
      boolean writeProbes,
      boolean writeProbesRan) {
    List<String> notes = new ArrayList<>();
    if (elasticCloud) {
      notes.add(
          "This deployment is configured with a cloud ID, so the cluster is Elastic Cloud and "
              + "X-Pack Security is always present.");
    }
    if (!securityApiPresent) {
      notes.add(
          "The /_security API is not available on this cluster, so no security feature can be "
              + "exercised. Every security verdict below follows from that one fact.");
    } else if (!writeProbesRan) {
      notes.add(
          "DLS, FLS, and API-key verdicts are inferred from the license tier and cluster "
              + "settings. Re-run with writeProbes=true to create and tear down a short-lived key "
              + "and role and observe them instead.");
    }
    if (writeProbes && !writeProbesRan) {
      notes.add(
          "Write probes were requested but not run: they need the /_security API, which this "
              + "cluster did not answer as a security-enabled cluster would.");
    }
    return notes;
  }

  /**
   * What the read-only pass observed about the cluster, plus the impersonation target it was asked
   * to probe: the whole of what a capability verdict is reasoned from when no write probe settled
   * the question. Grouped so the verdict builder takes the cluster's state as one value.
   */
  private record ClusterObservations(
      Boolean securityEnabled,
      boolean securityApiPresent,
      ProbeResult xpack,
      Map<String, String> securitySettings,
      Map<String, Boolean> clusterPrivileges,
      String licenseType,
      String authenticatedUser,
      String runAsUser) {}

  /**
   * The five capability verdicts, each preferring an observed write-probe outcome over the inferred
   * read-only one — split out of {@link #getCapabilityReport} so its own branching does not
   * compound with the rest of that method's.
   */
  private List<ElasticSearchCapability> buildCapabilities(
      WriteProbeOutcome writeProbeOutcome, ClusterObservations observed) {
    List<ElasticSearchCapability> capabilities = new ArrayList<>();
    capabilities.add(
        securityCapability(
            observed.securityEnabled(), observed.securityApiPresent(), observed.xpack()));
    capabilities.add(
        writeProbeOutcome != null
            ? writeProbeOutcome.apiKeys()
            : apiKeyCapability(
                observed.securityApiPresent(),
                observed.securitySettings(),
                observed.clusterPrivileges()));
    capabilities.add(
        writeProbeOutcome != null
            ? writeProbeOutcome.dls()
            : dlsFlsCapability(
                DLS,
                observed.securityApiPresent(),
                observed.licenseType(),
                observed.securitySettings()));
    capabilities.add(
        writeProbeOutcome != null
            ? writeProbeOutcome.fls()
            : dlsFlsCapability(
                FLS,
                observed.securityApiPresent(),
                observed.licenseType(),
                observed.securitySettings()));
    capabilities.add(
        runAsCapability(
            observed.securityApiPresent(), observed.authenticatedUser(), observed.runAsUser()));
    return capabilities;
  }

  // ---------------------------------------------------------------------------
  // Capability evaluation
  // ---------------------------------------------------------------------------

  private ElasticSearchCapability securityCapability(
      Boolean securityEnabled, boolean securityApiPresent, ProbeResult xpack) {
    boolean enabled = Boolean.TRUE.equals(securityEnabled);
    CapabilityVerdict verdict =
        enabled && securityApiPresent ? CapabilityVerdict.SUPPORTED : CapabilityVerdict.UNAVAILABLE;
    // The two signals can disagree — a cluster can report the feature as enabled while the
    // /_security API is unreachable, say behind a proxy that strips it — and when they do, the
    // detail has to say which, rather than repeating the verdict's own conclusion.
    String detail;
    if (enabled && securityApiPresent) {
      detail = "X-Pack Security is enabled and the /_security API responds.";
    } else if (enabled) {
      detail =
          "X-Pack Security reports itself enabled, but the /_security API did not respond as a "
              + "security-enabled cluster would, so no security feature could be exercised.";
    } else {
      detail = "X-Pack Security is not enabled on this cluster.";
    }
    return new ElasticSearchCapability(
        "X-Pack Security",
        verdict,
        detail,
        "GET /_xpack (status %d), GET %s".formatted(xpack.status(), AUTHENTICATE_PATH));
  }

  private ElasticSearchCapability apiKeyCapability(
      boolean securityApiPresent, Map<String, String> settings, Map<String, Boolean> privileges) {
    if (!securityApiPresent) {
      return new ElasticSearchCapability(
          API_KEYS,
          CapabilityVerdict.UNAVAILABLE,
          "Security is disabled, so API keys cannot be issued.",
          "GET " + AUTHENTICATE_PATH);
    }
    // nosemgrep - a cluster setting name, not a key
    if ("false".equals(settings.get(API_KEY_ENABLED_SETTING))) {
      return new ElasticSearchCapability(
          API_KEYS,
          CapabilityVerdict.UNAVAILABLE,
          "API keys are explicitly disabled by cluster setting.",
          "xpack.security.authc.api_key.enabled=false");
    }
    // API keys are a Basic-tier feature, so a security-enabled cluster is expected to support
    // them regardless of license. What actually gates Consent is whether the shared credential
    // may mint them.
    boolean canMint =
        Boolean.TRUE.equals(privileges.get("manage_api_key"))
            || Boolean.TRUE.equals(privileges.get("grant_api_key"));
    if (!privileges.isEmpty() && !canMint) {
      return new ElasticSearchCapability(
          API_KEYS,
          CapabilityVerdict.NOT_PERMITTED,
          "The cluster supports API keys, but the credential this deployment authenticates with "
              + "holds neither manage_api_key nor grant_api_key, so it cannot mint per-request keys.",
          "POST /_security/user/_has_privileges");
    }
    return new ElasticSearchCapability(
        API_KEYS,
        CapabilityVerdict.INFERRED_SUPPORTED,
        "Security is enabled and API keys are a Basic-tier feature, so key creation is expected "
            + "to work. Not proven: creating a key is a write.",
        "xpack.security.authc.api_key.enabled=%s; POST /_security/user/_has_privileges"
            .formatted(settings.getOrDefault(API_KEY_ENABLED_SETTING, "not-set")));
  }

  private ElasticSearchCapability dlsFlsCapability(
      String name, boolean securityApiPresent, String licenseType, Map<String, String> settings) {
    if (!securityApiPresent) {
      return new ElasticSearchCapability(
          name,
          CapabilityVerdict.UNAVAILABLE,
          "Security is disabled, so no role can carry a DLS query or an FLS grant.",
          "GET " + AUTHENTICATE_PATH);
    }
    String license = licenseType == null ? "" : licenseType.toLowerCase();
    String dlsFlsSetting = settings.getOrDefault("xpack.security.dls_fls.enabled", "not-set");
    if (DLS_FLS_LICENSES.contains(license)) {
      return new ElasticSearchCapability(
          name,
          CapabilityVerdict.INFERRED_SUPPORTED,
          "The '%s' license includes DLS/FLS and xpack.security.dls_fls.enabled=%s."
              .formatted(licenseType, dlsFlsSetting),
          "GET /_license; GET /_cluster/settings");
    }
    if (SECURITY_ONLY_LICENSES.contains(license)) {
      return new ElasticSearchCapability(
          name,
          CapabilityVerdict.LICENSE_BLOCKED,
          "The '%s' license does not include DLS/FLS. A role or API key carrying a DLS query is "
                  .formatted(licenseType)
              + "rejected, and note that a key with a DLS role descriptor is accepted at creation "
              + "and only fails at search time.",
          "GET /_license");
    }
    return new ElasticSearchCapability(
        name,
        CapabilityVerdict.UNKNOWN,
        "License tier '%s' could not be mapped to a DLS/FLS entitlement.".formatted(licenseType),
        "GET /_license");
  }

  /**
   * The one security feature that can be proven without a write: {@code run_as} is requested with a
   * header on an otherwise read-only call.
   */
  private ElasticSearchCapability runAsCapability(
      boolean securityApiPresent, String authenticatedUser, String requestedUser) {
    if (!securityApiPresent) {
      return new ElasticSearchCapability(
          RUN_AS,
          CapabilityVerdict.UNAVAILABLE,
          "Security is disabled, so impersonation is not available.",
          "GET " + AUTHENTICATE_PATH);
    }
    String target =
        requestedUser != null && !requestedUser.isBlank() ? requestedUser : authenticatedUser;
    if (target == null || target.isBlank()) {
      return new ElasticSearchCapability(
          RUN_AS,
          CapabilityVerdict.UNKNOWN,
          "No target user was available to attempt impersonation against.",
          "GET " + AUTHENTICATE_PATH);
    }
    if (!RUN_AS_USERNAME.matcher(target).matches()) {
      // The target ends up in a request header, so it is checked before it is sent rather than
      // relying on the transport to reject it.
      return new ElasticSearchCapability(
          RUN_AS,
          CapabilityVerdict.UNKNOWN,
          "The requested run_as username is not a valid Elasticsearch username, so no request was "
              + "sent. Expected only letters, digits, and the characters . _ @ + -",
          "no request issued");
    }

    ProbeResult result =
        probe(HttpMethod.GET, AUTHENTICATE_PATH, null, Map.of(RUN_AS_HEADER, target));
    String evidence =
        "GET %s with %s: %s -> %d"
            .formatted(AUTHENTICATE_PATH, RUN_AS_HEADER, target, result.status());

    if (result.status() == 200) {
      String resolved = string(result.body(), USERNAME_FIELD);
      if (target.equals(resolved)) {
        return new ElasticSearchCapability(
            RUN_AS,
            CapabilityVerdict.SUPPORTED,
            "The cluster honoured the run_as header and resolved the request to '%s'."
                .formatted(resolved),
            evidence);
      }
      return new ElasticSearchCapability(
          RUN_AS,
          CapabilityVerdict.UNKNOWN,
          "The run_as header was accepted but the request still resolved to '%s'."
              .formatted(resolved),
          evidence);
    }
    if (result.status() == 403) {
      return new ElasticSearchCapability(
          RUN_AS,
          refusalVerdict(result),
          "Impersonation was refused: " + reason(result.body()),
          evidence);
    }
    return new ElasticSearchCapability(
        RUN_AS,
        CapabilityVerdict.UNKNOWN,
        "Impersonation probe returned an unexpected status. " + reason(result.body()),
        evidence);
  }

  // ---------------------------------------------------------------------------
  // Write probes
  // ---------------------------------------------------------------------------

  /** What a write-probe pass established, replacing the inferred verdicts for those features. */
  private record WriteProbeOutcome(
      ElasticSearchCapability apiKeys, ElasticSearchCapability dls, ElasticSearchCapability fls) {

    /** Whether DLS came back usable, which is the pivot the Epic D / Epic E decision turns on. */
    boolean dlsUsable() {
      return dls.verdict() == CapabilityVerdict.SUPPORTED;
    }

    /**
     * Whether the probes were stopped by the credential's privileges rather than by the cluster's
     * capability. In that case the DLS and FLS verdicts say nothing about what the cluster can do,
     * so nothing about Epic D can be concluded from them — a distinction that matters because the
     * shared service credential is unlikely to hold {@code manage_security}.
     */
    boolean credentialBlocked() {
      return dls.verdict() == CapabilityVerdict.NOT_PERMITTED;
    }
  }

  /**
   * Creates and then removes a short-lived API key and role in order to observe what the read-only
   * probes can only infer. Every resource is namespaced {@code duos-capability-probe-*} / {@code
   * duos_dlsfls_probe_*}, keys expire in {@value #PROBE_KEY_EXPIRATION} even if teardown never
   * runs, and teardown is in a {@code finally} so it also fires when a probe throws.
   *
   * <p>Ordered cheapest-first so a credential that cannot mint keys at all fails on the first step
   * rather than part-way through.
   */
  private WriteProbeOutcome runWriteProbes(List<String> notes) {
    // Unique per run rather than per millisecond: two admins probing at once must not generate the
    // same role name, or the second overwrites the first's role and both try to delete it.
    String stamp =
        "%d-%s".formatted(System.currentTimeMillis(), UUID.randomUUID().toString().substring(0, 8));
    String index = probeIndex();
    List<String> createdKeyIds = new ArrayList<>();
    String roleName = "duos_dlsfls_probe_" + stamp;
    boolean roleCreated = false;

    logInfo("Elasticsearch capability write probes starting against index " + index);

    try {
      ElasticSearchCapability apiKeys = apiKeyRoundTripProbe(stamp, createdKeyIds);
      // Run regardless of the key probe's outcome: role acceptance answers the license question on
      // its own, and is the only DLS/FLS signal available to a credential that cannot mint keys.
      RoleAcceptance acceptance = dlsFlsRoleProbe(roleName, index);
      roleCreated = acceptance.created();

      ElasticSearchCapability dls = acceptance.capability(DLS);
      ElasticSearchCapability fls = acceptance.capability(FLS);

      // Enforcement needs a key to search with and documents to compare against; without either,
      // the verdicts stand at role acceptance and the report has to say so rather than imply more.
      if (apiKeys.verdict() != CapabilityVerdict.SUPPORTED) {
        notes.add(
            "No usable probe key, so DLS and FLS enforcement could not be observed end to end; "
                + "those verdicts reflect whether the cluster accepted a role carrying the filters, "
                + "not whether it applies them.");
      } else {
        long baseline = documentCount(index);
        if (baseline < 0) {
          notes.add(
              "Index '%s' was not readable, so DLS and FLS enforcement could not be observed "
                      .formatted(index)
                  + "end to end; the verdicts reflect role acceptance only.");
        } else if (baseline == 0) {
          notes.add(
              "Index '%s' is empty, so DLS and FLS enforcement could not be observed end to end; "
                      .formatted(index)
                  + "the verdicts reflect role acceptance only.");
        } else {
          Optional<ElasticSearchCapability> dlsObserved =
              dlsEnforcementProbe(stamp, index, baseline, createdKeyIds);
          if (dlsObserved.isEmpty()) {
            notes.add(
                "The DLS enforcement check could not be run to a conclusion, so the DLS verdict "
                    + "reflects role acceptance only.");
          }
          dls = dlsObserved.orElse(dls);

          Optional<ElasticSearchCapability> flsObserved =
              flsProjectionProbe(stamp, index, createdKeyIds);
          if (flsObserved.isEmpty()) {
            notes.add(
                "The FLS projection check returned no document fields to inspect (the index may "
                    + "not store _source, or its documents may not carry '%s'), so the FLS verdict "
                        .formatted(FLS_GRANT_FIELD)
                    + "reflects role acceptance only.");
          }
          fls = flsObserved.orElse(fls);
        }
      }
      notes.add(writeProbeNote(createdKeyIds.size(), roleCreated));
      return new WriteProbeOutcome(apiKeys, dls, fls);
    } finally {
      tearDownProbeResources(createdKeyIds, roleCreated ? roleName : null, notes);
    }
  }

  /**
   * Describes what the pass actually did. Worth getting exactly right: a run in which the cluster
   * refused everything created nothing, and a note claiming otherwise would misrepresent both what
   * happened to the cluster and what the verdicts mean.
   */
  private String writeProbeNote(int keysCreated, boolean roleCreated) {
    if (keysCreated == 0 && !roleCreated) {
      return "Write probes were attempted but the cluster accepted nothing, so nothing was created "
          + "or removed. Where the refusals were on privilege grounds, the verdicts below describe "
          + "this deployment's credential rather than the cluster's capability — cluster_privileges "
          + "shows what it is missing.";
    }
    return "Write probes ran: %d short-lived API key(s)%s were created under the "
            .formatted(keysCreated, roleCreated ? " and one probe role" : "")
        + "duos-capability-probe / duos_dlsfls_probe names and removed again. The DLS, FLS, and "
        + "API-key verdicts below are observed rather than inferred.";
  }

  /**
   * The plain API-key lifecycle: mint a key, authenticate as it, and let teardown invalidate it.
   * Authenticating is the part that matters — a key that cannot authenticate is no use as a
   * per-request credential.
   */
  private ElasticSearchCapability apiKeyRoundTripProbe(String stamp, List<String> createdKeyIds) {
    KeyCreation key =
        createProbeKey("duos-capability-probe-" + stamp, PRIVILEGE_FREE_DESCRIPTOR, createdKeyIds);
    String evidence = "POST %s -> %d".formatted(API_KEY_PATH, key.status());

    if (!created(key.response())) {
      return new ElasticSearchCapability(
          API_KEYS,
          refusalVerdict(key.response()),
          "The cluster refused to create an API key: " + reason(key.response().body()),
          evidence);
    }
    if (key.encoded() == null) {
      return new ElasticSearchCapability(
          API_KEYS,
          CapabilityVerdict.UNKNOWN,
          "The key was created but the response carried neither an encoded form nor an id and "
              + "secret to build one from.",
          evidence);
    }

    ProbeResult asKey = probeAsApiKey(key.encoded(), HttpMethod.GET, AUTHENTICATE_PATH);
    if (asKey.status() == 200) {
      return new ElasticSearchCapability(
          API_KEYS,
          CapabilityVerdict.SUPPORTED,
          "Observed: a key was created, authenticated as '%s', and invalidated."
              .formatted(string(asKey.body(), USERNAME_FIELD)),
          evidence + "; GET %s as the key -> 200".formatted(AUTHENTICATE_PATH));
    }
    return new ElasticSearchCapability(
        API_KEYS,
        CapabilityVerdict.UNKNOWN,
        "The key was created but could not authenticate: " + reason(asKey.body()),
        evidence + "; GET %s as the key -> %d".formatted(AUTHENTICATE_PATH, asKey.status()));
  }

  /** Whether the cluster accepted a role carrying a DLS query and an FLS grant, and why not. */
  private record RoleAcceptance(
      boolean created, CapabilityVerdict verdict, String detail, String evidence) {

    ElasticSearchCapability capability(String name) {
      return new ElasticSearchCapability(name, verdict, detail, evidence);
    }
  }

  /**
   * Creating a role that carries both {@code query} and {@code field_security} is the cleanest
   * license check there is: a cluster whose license excludes DLS/FLS rejects it outright with an
   * explicit message, where an API key carrying the same descriptors would be accepted and only
   * fail later at search time.
   */
  private RoleAcceptance dlsFlsRoleProbe(String roleName, String index) {
    String path = "/_security/role/" + roleName;
    String body =
        """
        {"indices":[{"names":["%s"],"privileges":["read"],\
        "query":{"term":{"accessPolicy.probe":"capability-probe"}},\
        "field_security":{"grant":["%s"],"except":[]}}]}"""
            .formatted(index, FLS_GRANT_FIELD);

    ProbeResult result = probe(HttpMethod.PUT, path, body, Map.of());
    String evidence =
        "PUT %s carrying query and field_security -> %d".formatted(path, result.status());

    if (created(result)) {
      return new RoleAcceptance(
          true,
          CapabilityVerdict.SUPPORTED,
          "Observed: the cluster accepted a role carrying both a DLS query and an FLS grant.",
          evidence);
    }
    CapabilityVerdict verdict = refusalVerdict(result);
    String detail =
        verdict == CapabilityVerdict.LICENSE_BLOCKED
            ? "The license does not permit it: " + reason(result.body())
            : "The role was rejected: " + reason(result.body());
    return new RoleAcceptance(false, verdict, detail, evidence);
  }

  /**
   * The end-to-end check, and the one that exercises Epic D's exact mechanism: a per-request API
   * key whose {@code role_descriptors} carry the DLS filter. A {@code match_none} filter must
   * return zero of the {@code baseline} documents the shared credential can see. Anything else
   * means the filter was accepted but not enforced — a far worse outcome than an honest refusal,
   * and the whole reason this probe exists.
   */
  private Optional<ElasticSearchCapability> dlsEnforcementProbe(
      String stamp, String index, long baseline, List<String> createdKeyIds) {
    String descriptor =
        """
        {"dls_probe":{"indices":[{"names":["%s"],"privileges":["read"],\
        "query":{"match_none":{}}}]}}"""
            .formatted(index);
    EnforcementAttempt attempt =
        attemptEnforcement(
            new EnforcementRequest(
                DLS,
                "DLS",
                "duos-capability-probe-dls-" + stamp,
                descriptor,
                "a DLS role_descriptor",
                "a match_none DLS key",
                index),
            createdKeyIds);
    if (attempt.settled() != null) {
      return Optional.of(attempt.settled());
    }
    if (attempt.search() == null) {
      return Optional.empty();
    }

    long visible = hitCount(attempt.search());
    String evidence =
        "%s; %d of %d documents visible".formatted(attempt.evidence(), visible, baseline);
    if (visible < 0) {
      // Do not read an unrecognised response shape as a failure to enforce: "not enforced" is the
      // most serious verdict this report can return, and it has to mean what it says.
      return Optional.of(
          new ElasticSearchCapability(
              DLS,
              CapabilityVerdict.UNKNOWN,
              "The search succeeded but its response carried no hit total, so whether the filter "
                  + "was enforced could not be established.",
              evidence));
    }
    if (visible == 0) {
      return Optional.of(
          new ElasticSearchCapability(
              DLS,
              CapabilityVerdict.SUPPORTED,
              "Proven end to end: a match_none DLS key returned 0 of %d documents."
                  .formatted(baseline),
              evidence));
    }
    return Optional.of(
        new ElasticSearchCapability(
            DLS,
            CapabilityVerdict.UNAVAILABLE,
            "Not enforced: a match_none DLS key still returned %d of %d documents. The filter was "
                    .formatted(visible, baseline)
                + "accepted but had no effect, so DLS cannot be relied on here.",
            evidence));
  }

  /**
   * The FLS counterpart: a key granting exactly one field must return documents carrying only that
   * field. Role acceptance alone does not establish that the projection is applied.
   */
  private Optional<ElasticSearchCapability> flsProjectionProbe(
      String stamp, String index, List<String> createdKeyIds) {
    String descriptor =
        """
        {"fls_probe":{"indices":[{"names":["%s"],"privileges":["read"],\
        "field_security":{"grant":["%s"]}}]}}"""
            .formatted(index, FLS_GRANT_FIELD);
    EnforcementAttempt attempt =
        attemptEnforcement(
            new EnforcementRequest(
                FLS,
                "FLS",
                "duos-capability-probe-fls-" + stamp,
                descriptor,
                "an FLS role_descriptor",
                "a key granting only '%s'".formatted(FLS_GRANT_FIELD),
                index),
            createdKeyIds);
    if (attempt.settled() != null) {
      return Optional.of(attempt.settled());
    }
    if (attempt.search() == null) {
      return Optional.empty();
    }

    Set<String> fields = firstHitSourceFields(attempt.search());
    if (fields.isEmpty()) {
      return Optional.empty();
    }
    if (fields.equals(Set.of(FLS_GRANT_FIELD))) {
      return Optional.of(
          new ElasticSearchCapability(
              FLS,
              CapabilityVerdict.SUPPORTED,
              "Proven end to end: documents came back carrying only the granted '%s' field."
                  .formatted(FLS_GRANT_FIELD),
              attempt.evidence()));
    }
    return Optional.of(
        new ElasticSearchCapability(
            FLS,
            CapabilityVerdict.UNAVAILABLE,
            "Not enforced: a key granting only '%s' returned documents carrying %s. The grant was "
                    .formatted(FLS_GRANT_FIELD, fields)
                + "accepted but had no effect, so FLS cannot be relied on here.",
            attempt.evidence()));
  }

  /**
   * What an enforcement attempt reached before interpretation: either a verdict the attempt itself
   * settled — a refusal at key creation, or a failure at search time — or a successful search
   * response for the caller to read the filter's effect out of, or neither, when the attempt could
   * not be run to a conclusion and the caller must fall back to role acceptance.
   */
  private record EnforcementAttempt(
      ElasticSearchCapability settled, JsonObject search, String evidence) {

    static EnforcementAttempt settled(ElasticSearchCapability capability) {
      return new EnforcementAttempt(capability, null, null);
    }

    static EnforcementAttempt inconclusive() {
      return new EnforcementAttempt(null, null, null);
    }

    static EnforcementAttempt searched(JsonObject body, String evidence) {
      return new EnforcementAttempt(null, body, evidence);
    }
  }

  /**
   * Everything {@link #attemptEnforcement} needs to mint a probe key and search through it, bundled
   * so the method itself takes a request and the list of created key ids to reconcile, rather than
   * one parameter per fact about the attempt.
   *
   * @param descriptorLabel how the {@code role_descriptors} block is described in evidence
   * @param keyLabel how the key is described in evidence, e.g. {@code a match_none DLS key}
   */
  private record EnforcementRequest(
      String name,
      String shortName,
      String keyName,
      String descriptor,
      String descriptorLabel,
      String keyLabel,
      String index) {}

  /**
   * The half the DLS and FLS enforcement checks share: mint a key carrying the filter under test,
   * then search the real index through it. Only the reading of a successful response differs
   * between the two, so only that is left to the callers.
   */
  private EnforcementAttempt attemptEnforcement(
      EnforcementRequest request, List<String> createdKeyIds) {
    KeyCreation key = createProbeKey(request.keyName(), request.descriptor(), createdKeyIds);
    if (!created(key.response())) {
      // On a license-blocked cluster the key may be refused here rather than at search time.
      return EnforcementAttempt.settled(
          new ElasticSearchCapability(
              request.name(),
              refusalVerdict(key.response()),
              "A key carrying %s was refused: ".formatted(request.descriptorLabel())
                  + reason(key.response().body()),
              "POST %s with %s -> %d"
                  .formatted(API_KEY_PATH, request.descriptorLabel(), key.status())));
    }
    if (key.encoded() == null) {
      return EnforcementAttempt.inconclusive();
    }

    String searchPath = "/%s/_search?size=1".formatted(request.index());
    ProbeResult search = probeAsApiKey(key.encoded(), HttpMethod.GET, searchPath);
    String evidence =
        "GET %s through %s -> %d".formatted(searchPath, request.keyLabel(), search.status());
    return search.status() == 200
        ? EnforcementAttempt.searched(search.body(), evidence)
        : EnforcementAttempt.settled(
            searchFailure(request.name(), request.shortName(), search, evidence));
  }

  /**
   * A probe key's creation response paired with its encoded form, which is null when the cluster
   * refused the key or returned nothing to build one from.
   */
  private record KeyCreation(ProbeResult response, String encoded) {

    int status() {
      return response.status();
    }
  }

  /**
   * Creates a short-lived probe key and records its id so teardown removes it. Every probe key goes
   * through here, so the namespaced name, the expiry, and the teardown registration cannot be
   * forgotten on one path and not another.
   */
  private KeyCreation createProbeKey(
      String keyName, String roleDescriptors, List<String> createdKeyIds) {
    String body =
        "{\"name\":\"%s\",\"expiration\":\"%s\",\"role_descriptors\":%s}"
            .formatted(keyName, PROBE_KEY_EXPIRATION, roleDescriptors);
    ProbeResult created = probe(HttpMethod.POST, API_KEY_PATH, body, Map.of());
    if (!created(created)) {
      return new KeyCreation(created, null);
    }
    String keyId = string(created.body(), "id");
    if (keyId != null) {
      createdKeyIds.add(keyId);
    }
    return new KeyCreation(created, encodedApiKey(created.body()));
  }

  /**
   * Removes everything the write probes created. Best effort by design: the keys expire on their
   * own within {@value #PROBE_KEY_EXPIRATION}, so a failure here leaves nothing durable behind —
   * but it is reported in the notes rather than swallowed, because an operator should not have to
   * read server logs to find out something was left on the cluster.
   */
  private void tearDownProbeResources(
      List<String> createdKeyIds, String roleName, List<String> notes) {
    for (String keyId : createdKeyIds) {
      ProbeResult result =
          probe(HttpMethod.DELETE, API_KEY_PATH, "{\"ids\":[\"%s\"]}".formatted(keyId), Map.of());
      if (result.status() != 200) {
        logWarn(
            "Failed to invalidate probe API key %s (status %d)".formatted(keyId, result.status()));
        notes.add(
            "Probe API key %s could not be invalidated (status %d); it expires on its own within %s."
                .formatted(keyId, result.status(), PROBE_KEY_EXPIRATION));
      }
    }
    if (roleName == null) {
      return;
    }
    ProbeResult result = probe(HttpMethod.DELETE, "/_security/role/" + roleName, null, Map.of());
    if (result.status() != 200) {
      logWarn("Failed to delete probe role %s (status %d)".formatted(roleName, result.status()));
      notes.add(
          "Probe role %s could not be deleted (status %d) and must be removed by hand."
              .formatted(roleName, result.status()));
    }
  }

  /**
   * The single index every probe scopes itself to: the real dataset index, so that a DLS or FLS
   * check measures the index Epic D would actually filter. {@code datasetIndexName} is
   * {@code @NotNull} in the configuration, so the fallback is only reached by a hand-built
   * configuration — and it is deliberately a name no real index uses, so a probe can never widen
   * its own scope to a live index it was not pointed at.
   */
  private String probeIndex() {
    String index = esConfig.getDatasetIndexName();
    return index == null || index.isBlank() ? "duos-capability-probe-index" : index;
  }

  /**
   * Classifies a failed search through a probe key. The distinction is the whole value of the
   * probe: a licence refusal means the feature is unavailable on this cluster, whereas a privilege
   * refusal or a transport failure says nothing whatever about the feature — and an API key is
   * limited by its owning credential's privileges, so a key that reads a restricted index is
   * refused for reasons that have nothing to do with DLS or FLS.
   */
  private ElasticSearchCapability searchFailure(
      String name, String shortName, ProbeResult search, String evidence) {
    if (search.status() == 0) {
      return new ElasticSearchCapability(
          name,
          CapabilityVerdict.UNKNOWN,
          "The key was accepted but the search could not be completed, so enforcement was not "
              + "observed. This is a transport failure, not a finding about the cluster.",
          evidence);
    }
    String reason = reason(search.body());
    if (licenseRefusal(reason)) {
      return new ElasticSearchCapability(
          name,
          CapabilityVerdict.LICENSE_BLOCKED,
          "Refused at search time on licensing grounds, which is exactly how a license that "
              + "excludes %s fails — the key is accepted at creation and only rejected when used: "
                  .formatted(shortName)
              + reason,
          evidence);
    }
    if (search.status() == 401 || search.status() == 403) {
      return new ElasticSearchCapability(
          name,
          CapabilityVerdict.NOT_PERMITTED,
          "The key was accepted but the search was refused on privilege rather than licensing "
              + "grounds, so %s itself was not established: ".formatted(shortName)
              + reason,
          evidence);
    }
    return new ElasticSearchCapability(
        name,
        CapabilityVerdict.UNKNOWN,
        "The search through the probe key returned an unexpected status, so enforcement was not "
            + "observed: "
            + reason,
        evidence);
  }

  /** Distinguishes "the license forbids this" from "this credential may not do it". */
  private CapabilityVerdict refusalVerdict(ProbeResult result) {
    if (licenseRefusal(reason(result.body()))) {
      return CapabilityVerdict.LICENSE_BLOCKED;
    }
    if (result.status() == 401 || result.status() == 403) {
      return CapabilityVerdict.NOT_PERMITTED;
    }
    return CapabilityVerdict.UNKNOWN;
  }

  /**
   * Whether a refusal was the cluster's licence talking rather than the credential's privileges.
   * The single place that decision is made, so every probe classifies a refusal the same way — the
   * distinction the whole report turns on, since only a licence refusal says anything about the
   * cluster.
   */
  private static boolean licenseRefusal(String reason) {
    String lowered = reason.toLowerCase();
    return lowered.contains("non-compliant") || lowered.contains(LICENSE_FIELD);
  }

  /** Whether the cluster created what was asked of it; PUT role returns 200 or 201. */
  private static boolean created(ProbeResult result) {
    return result.status() == 200 || result.status() == 201;
  }

  /** The {@code encoded} form of a created key, or one built from its id and secret. */
  private static String encodedApiKey(JsonObject created) {
    String encoded = string(created, "encoded");
    if (encoded != null) {
      return encoded;
    }
    String id = string(created, "id");
    String secret = string(created, "api_key");
    if (id == null || secret == null) {
      return null;
    }
    return Base64.getEncoder().encodeToString((id + ":" + secret).getBytes(StandardCharsets.UTF_8));
  }

  /** The shared credential's view of how many documents the index holds, or -1 if unreadable. */
  private long documentCount(String index) {
    ProbeResult result = probe(HttpMethod.GET, "/%s/_count".formatted(index));
    if (result.status() != 200 || !result.body().has("count")) {
      return -1;
    }
    return result.body().get("count").getAsLong();
  }

  private static long hitCount(JsonObject searchBody) {
    JsonObject hits = object(searchBody, "hits");
    if (hits == null || !hits.has("total")) {
      return -1;
    }
    JsonElement total = hits.get("total");
    if (total.isJsonObject() && total.getAsJsonObject().has("value")) {
      return total.getAsJsonObject().get("value").getAsLong();
    }
    return total.isJsonPrimitive() ? total.getAsLong() : -1;
  }

  private static Set<String> firstHitSourceFields(JsonObject searchBody) {
    JsonObject hits = object(searchBody, "hits");
    if (hits == null || !hits.has("hits") || !hits.get("hits").isJsonArray()) {
      return Set.of();
    }
    var array = hits.getAsJsonArray("hits");
    if (array.isEmpty() || !array.get(0).isJsonObject()) {
      return Set.of();
    }
    JsonObject source = object(array.get(0).getAsJsonObject(), "_source");
    return source == null ? Set.of() : source.keySet();
  }

  /** Issues a request authenticated as an API key rather than the deployment's own credential. */
  private ProbeResult probeAsApiKey(String encodedApiKey, String method, String path) {
    try (RestClient keyClient = apiKeyClientFactory.create(encodedApiKey)) {
      return toProbeResult(keyClient.performRequest(new Request(method, path)));
    } catch (ResponseException e) {
      return toProbeResult(e.getResponse());
    } catch (IOException | RuntimeException e) {
      logWarn(
          "Elasticsearch API-key probe failed for %s %s: %s"
              .formatted(method, path, e.getMessage()));
      return new ProbeResult(0, new JsonObject());
    }
  }

  // ---------------------------------------------------------------------------
  // Derived summary values
  // ---------------------------------------------------------------------------

  /**
   * X-Pack has shipped in every default Elasticsearch distribution since 6.3, so a missing {@code
   * /_xpack} endpoint means an OSS build rather than merely a cluster with security switched off —
   * a distinction worth keeping, because a security-disabled default distribution can have security
   * enabled by configuration whereas an OSS build cannot. {@code build_flavor} says which directly
   * when the cluster reports it.
   */
  private String edition(
      boolean elasticCloud, String buildFlavor, int xpackStatus, String licenseType) {
    if (elasticCloud) {
      return "Elastic Cloud (X-Pack always present)";
    }
    if ("oss".equalsIgnoreCase(buildFlavor) || xpackStatus == 400 || xpackStatus == 404) {
      return "OSS (no X-Pack endpoint)";
    }
    return licenseType == null ? "unknown" : licenseType;
  }

  /**
   * Reports whether the REST client this application ships can drive the security APIs. The
   * low-level client is a version-agnostic HTTP transport with no typed request model, so the only
   * real compatibility axis is major-version skew.
   */
  private String restClientCompatibility(String clusterVersion) {
    String clientVersion = RestClient.class.getPackage().getImplementationVersion();
    if (clientVersion == null || clusterVersion == null) {
      return "Could not determine the client or cluster version at runtime. This report was itself "
          + "produced through RestClient.performRequest, so the transport reaches /_security.";
    }
    String clientMajor = clientVersion.split("\\.")[0];
    String clusterMajor = clusterVersion.split("\\.")[0];
    if (clientMajor.equals(clusterMajor)) {
      return "Compatible: elasticsearch-rest-client %s matches cluster major %s. Security calls go "
              .formatted(clientVersion, clusterMajor)
          + "through RestClient.performRequest(Request) with no typed-client dependency.";
    }
    return "Major-version skew: client %s vs cluster %s. The low-level client is version agnostic "
            .formatted(clientVersion, clusterVersion)
        + "over HTTP, but confirm the skew is within the supported range.";
  }

  private String recommendation(
      boolean securityApiPresent, String licenseType, WriteProbeOutcome writeProbeOutcome) {
    // An observation outranks the license inference in either direction: a cluster that actually
    // enforced DLS settles the question, and one that refused on licensing grounds settles it just
    // as firmly. A refusal on *privilege* grounds settles nothing about the cluster, so that case
    // falls through to the license reading rather than being read as a verdict against Epic D.
    if (writeProbeOutcome != null && securityApiPresent) {
      if (writeProbeOutcome.dlsUsable()) {
        return "Epic D (native DLS/FLS) is viable on this cluster, and this was observed rather "
            + "than inferred: a probe role and API key carrying DLS/FLS descriptors were accepted "
            + "and enforced. Keep Epic E in scope only if another environment cannot support "
            + "DLS/FLS.";
      }
      if (writeProbeOutcome.credentialBlocked()) {
        return "Inconclusive from the write probes: this deployment's credential is not permitted "
            + "to create a role or an API key, so the DLS and FLS verdicts describe the credential "
            + "rather than the cluster (see cluster_privileges). Re-run with a credential holding "
            + "manage_security and manage_api_key to settle it. On the license alone: "
            + licenseBasedRecommendation(licenseType);
      }
      return "Epic E (compatibility fallback). Write probes were run against this cluster and DLS "
          + "was not usable: %s Epic D would need that resolved first."
              .formatted(writeProbeOutcome.dls().detail());
    }
    if (!securityApiPresent) {
      return "Epic E (compatibility fallback) is the only path available on this cluster. "
          + "Security is not enabled, so DLS, FLS, API keys, and run_as cannot be used at all. "
          + "Epic D stays blocked until X-Pack Security is enabled here.";
    }
    return licenseBasedRecommendation(licenseType);
  }

  /** What the license tier alone implies, used when no write probe settled the question. */
  private String licenseBasedRecommendation(String licenseType) {
    String license = licenseType == null ? "" : licenseType.toLowerCase();
    if (DLS_FLS_LICENSES.contains(license)) {
      return "Epic D (native DLS/FLS) is viable on this cluster: security is enabled and the '%s' "
              .formatted(licenseType)
          + "license includes DLS/FLS. Confirm with a write probe in a non-production environment "
          + "before committing, and keep Epic E in scope only if another environment cannot "
          + "support DLS/FLS.";
    }
    if (SECURITY_ONLY_LICENSES.contains(license)) {
      return "Epic E (compatibility fallback). Security is enabled but the '%s' license excludes "
              .formatted(licenseType)
          + "DLS/FLS. Epic D would require a Platinum or Enterprise license — a separate infra "
          + "decision that should precede any commitment to it.";
    }
    return "Inconclusive. The license tier could not be mapped to a DLS/FLS entitlement; inspect "
        + "the license and settings in this report before recording a decision.";
  }

  // ---------------------------------------------------------------------------
  // Probes
  // ---------------------------------------------------------------------------

  /** Reads the caller's own cluster privileges. A POST, but an evaluation rather than a write. */
  private Map<String, Boolean> clusterPrivileges() {
    String body =
        ("{\"cluster\":[%s],\"index\":[{\"names\":[\"%s\"],"
                + "\"privileges\":[\"read\",\"view_index_metadata\"]}]}")
            .formatted(
                PROBED_CLUSTER_PRIVILEGES.stream()
                    .map("\"%s\""::formatted)
                    .collect(Collectors.joining(",")),
                probeIndex());

    ProbeResult result = probe(HttpMethod.POST, "/_security/user/_has_privileges", body, Map.of());
    if (result.status() != 200 || !result.body().has("cluster")) {
      return Map.of();
    }
    Map<String, Boolean> privileges = new LinkedHashMap<>();
    JsonObject cluster = result.body().getAsJsonObject("cluster");
    for (String privilege : PROBED_CLUSTER_PRIVILEGES) {
      if (cluster.has(privilege)) {
        privileges.put(privilege, cluster.get(privilege).getAsBoolean());
      }
    }
    return privileges;
  }

  /**
   * Returns the cluster's security-relevant settings.
   *
   * <p>A cluster reports around fifty {@code xpack.security.*} defaults, nearly all of which —
   * cache TTLs, thread-pool sizes, SSL handshake timeouts, hashing algorithms — say nothing about
   * whether a capability is available, and including them buries the handful that do. So defaults
   * are filtered to {@link #REPORTED_SECURITY_DEFAULTS}, while any {@code xpack.security.*} value
   * an operator has explicitly set as a persistent or transient override is always reported: the
   * fact that someone configured it is itself worth seeing.
   */
  private Map<String, String> securitySettings() {
    ProbeResult result =
        probe(HttpMethod.GET, "/_cluster/settings?include_defaults=true&flat_settings=true");
    if (result.status() != 200) {
      return Map.of();
    }
    Map<String, String> settings = new TreeMap<>();
    // Later sections win: a persistent or transient override beats the default.
    for (String section : List.of("defaults", "persistent", "transient")) {
      if (!result.body().has(section) || !result.body().get(section).isJsonObject()) {
        continue;
      }
      boolean isDefault = "defaults".equals(section);
      for (Map.Entry<String, JsonElement> entry :
          result.body().getAsJsonObject(section).entrySet()) {
        if (entry.getValue().isJsonPrimitive()
            && isReportableSecuritySetting(entry.getKey(), isDefault)) {
          settings.put(entry.getKey(), entry.getValue().getAsString());
        }
      }
    }
    return settings;
  }

  private boolean isReportableSecuritySetting(String key, boolean isDefault) {
    if (!key.startsWith("xpack.security") && !key.contains("dls_fls")) {
      return false;
    }
    if (!isDefault) {
      // Explicitly configured, so report it whatever it is — except the audit-logfile settings,
      // which are voluminous and describe log formatting rather than capability.
      return !key.contains("audit.logfile");
    }
    return REPORTED_SECURITY_DEFAULTS.contains(key);
  }

  private Boolean securityEnabled(ProbeResult xpack, Map<String, String> settings) {
    if (xpack.status() == 200) {
      JsonObject security = object(xpack.body(), "features", "security");
      if (security != null && security.has("enabled")) {
        return security.get("enabled").getAsBoolean();
      }
    }
    String setting = settings.get("xpack.security.enabled");
    return setting == null ? null : Boolean.parseBoolean(setting);
  }

  /** A 400/404/405 from the security API means security is off rather than merely restricted. */
  private boolean securityApiPresent(int authenticateStatus) {
    return authenticateStatus == 200 || authenticateStatus == 401 || authenticateStatus == 403;
  }

  private ElasticSearchCapabilityReport unreachableReport(List<String> notes) {
    logWarn("Elasticsearch capability probe could not reach the cluster");
    notes.add("The cluster could not be reached, so no capability could be determined.");
    return new ElasticSearchCapabilityReport(
        null,
        null,
        null,
        "unknown",
        null,
        null,
        null,
        null,
        null,
        List.of(),
        Map.of(),
        Map.of(),
        false,
        List.of(
            new ElasticSearchCapability(
                "Cluster reachability",
                CapabilityVerdict.UNKNOWN,
                "The configured Elasticsearch cluster did not respond to GET /.",
                "GET /")),
        "Not assessed — the cluster was unreachable.",
        "Not assessed — the cluster was unreachable.",
        notes);
  }

  private ProbeResult probe(String method, String path) {
    return probe(method, path, null, Map.of());
  }

  private ProbeResult probe(String method, String path, String body, Map<String, String> headers) {
    Request request = new Request(method, path);
    if (body != null) {
      request.setJsonEntity(body);
    }
    if (!headers.isEmpty()) {
      RequestOptions.Builder options = RequestOptions.DEFAULT.toBuilder();
      headers.forEach(options::addHeader);
      request.setOptions(options.build());
    }
    try {
      return toProbeResult(esClient.performRequest(request));
    } catch (ResponseException e) {
      // A non-2xx is data here, not a failure: the status is frequently the finding itself.
      return toProbeResult(e.getResponse());
    } catch (IOException | RuntimeException e) {
      logWarn(
          "Elasticsearch capability probe failed for %s %s: %s"
              .formatted(method, path, e.getMessage()));
      return new ProbeResult(0, new JsonObject());
    }
  }

  private ProbeResult toProbeResult(Response response) {
    int status = response.getStatusLine().getStatusCode();
    try {
      if (response.getEntity() == null) {
        return new ProbeResult(status, new JsonObject());
      }
      String content =
          new String(response.getEntity().getContent().readAllBytes(), StandardCharsets.UTF_8);
      JsonElement parsed = JsonParser.parseString(content);
      return new ProbeResult(
          status, parsed.isJsonObject() ? parsed.getAsJsonObject() : new JsonObject());
    } catch (Exception e) {
      return new ProbeResult(status, new JsonObject());
    }
  }

  // ---------------------------------------------------------------------------
  // JSON helpers
  // ---------------------------------------------------------------------------

  private static JsonObject object(JsonObject source, String... path) {
    JsonObject current = source;
    for (String key : path) {
      if (current == null || !current.has(key) || !current.get(key).isJsonObject()) {
        return null;
      }
      current = current.getAsJsonObject(key);
    }
    return current;
  }

  private static String string(JsonObject source, String... path) {
    if (path.length == 0) {
      return null;
    }
    JsonObject parent =
        path.length == 1 ? source : object(source, Arrays.copyOf(path, path.length - 1));
    String key = path[path.length - 1];
    if (parent == null || !parent.has(key) || !parent.get(key).isJsonPrimitive()) {
      return null;
    }
    return parent.get(key).getAsString();
  }

  private static String stringOrDefault(JsonObject source, String fallback, String... path) {
    String value = string(source, path);
    return value == null ? fallback : value;
  }

  private static List<String> stringList(JsonObject source, String key) {
    if (source == null || !source.has(key) || !source.get(key).isJsonArray()) {
      return List.of();
    }
    List<String> values = new ArrayList<>();
    source.getAsJsonArray(key).forEach(element -> values.add(element.getAsString()));
    return values;
  }

  private static String reason(JsonObject body) {
    JsonObject error = object(body, "error");
    if (error != null && error.has("reason")) {
      return error.get("reason").getAsString();
    }
    return "no reason reported by the cluster";
  }

  /** A probe's HTTP status paired with its parsed body; a status of 0 means transport failure. */
  private record ProbeResult(int status, JsonObject body) {}
}
