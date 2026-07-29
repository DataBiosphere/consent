package org.broadinstitute.consent.http.models.elastic_search;

/**
 * Outcome of a single Elasticsearch security capability probe.
 *
 * <p>The distinction between the {@code INFERRED_*} values and the rest matters: a report produced
 * without write probes cannot prove any feature that requires creating something on the cluster (a
 * role, an API key), so those are reported as inferred from the license and cluster settings rather
 * than as observed fact. A write-probe run replaces them with observations.
 *
 * <p>The other distinction that matters is between the ways a feature can fail to be usable: {@link
 * #LICENSE_BLOCKED} is the cluster's answer, {@link #NOT_PERMITTED} is this credential's, and
 * {@link #UNKNOWN} means the probe reached no conclusion at all. Only the first is a fact about the
 * cluster, so only the first should carry weight in an architectural decision.
 */
public enum CapabilityVerdict {

  /** Observed to work. */
  SUPPORTED,

  /**
   * Observed not to work. Either the feature is absent — typically because security is disabled on
   * the cluster — or it was accepted and then silently not applied, which an enforcement probe
   * caught. The second case is the more serious: a filter that is accepted and ignored is worse
   * than one that is refused, because nothing can be built on it.
   */
  UNAVAILABLE,

  /** Present in the distribution but not included in the cluster's current license tier. */
  LICENSE_BLOCKED,

  /** Present and licensed, but the credential the application authenticates with may not use it. */
  NOT_PERMITTED,

  /** Expected to work based on license tier and cluster settings, but not proven by a probe. */
  INFERRED_SUPPORTED,

  /** Expected not to work based on license tier and cluster settings, but not proven by a probe. */
  INFERRED_UNAVAILABLE,

  /** The probe could not reach a conclusion. */
  UNKNOWN
}
