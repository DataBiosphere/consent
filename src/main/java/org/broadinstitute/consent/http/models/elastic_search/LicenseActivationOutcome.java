package org.broadinstitute.consent.http.models.elastic_search;

/**
 * What a request to activate the Elasticsearch trial license did.
 *
 * <p>Only {@link #ACTIVATED} changed the cluster. The other four values all mean "nothing was
 * changed", and they are kept apart because the reason matters to whoever asked: a cluster that is
 * already licensed for DLS/FLS needs no action, a cluster whose trial is spent cannot be made to
 * offer one again, a refusal is the cluster's own answer, and {@link #UNKNOWN} means the question
 * was never settled.
 *
 * <p>The distinction is also what makes the activation step safe to repeat: only the first call on
 * a given cluster can be {@link #ACTIVATED}, and a caller can tell that it was the first.
 */
public enum LicenseActivationOutcome {

  /** The trial license was started by this call. The cluster's license tier changed. */
  ACTIVATED,

  /**
   * The cluster's license already includes document- and field-level security, so no trial was
   * started. Nothing was changed.
   */
  ALREADY_LICENSED,

  /**
   * The cluster reports that it is no longer eligible for a trial — a trial can be started only
   * once per cluster, and this one's has been used. Nothing was changed, and nothing can be:
   * obtaining a DLS/FLS-capable license on this cluster is a licensing decision, not an API call.
   */
  TRIAL_UNAVAILABLE,

  /** The cluster was asked to start a trial and refused. Nothing was changed. */
  REFUSED,

  /**
   * The cluster could not be reached, or answered in a way that left the license state unreadable.
   * Whether anything changed is not known from this response alone — read the license back before
   * concluding anything.
   */
  UNKNOWN
}
