package org.broadinstitute.consent.http.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;

/**
 * Result of an atomic bulk pre-authorization operation between researchers and a DAA (the SO-driven
 * "Approve All" / "Remove All" actions).
 *
 * <p>Because the underlying operation runs in a single database transaction it is all-or-nothing
 * with respect to failure: a mid-batch error rolls the whole batch back and surfaces as an
 * exception rather than a partial result, so {@code errors} is always empty on a returned summary.
 * {@code applied} counts only the relationships that actually changed; requested items that were
 * already in the desired state (re-adding an existing pre-authorization, or removing one that was
 * never present) are no-ops counted under {@code skipped}, so {@code applied + skipped ==
 * requested} and {@code applied} may be less than {@code requested}.
 */
public class DaaBulkRelationResult {

  @JsonProperty private Integer requested;

  @JsonProperty private Integer applied;

  @JsonProperty private Integer skipped;

  @JsonProperty private List<String> errors;

  public DaaBulkRelationResult() {}

  public DaaBulkRelationResult(
      Integer requested, Integer applied, Integer skipped, List<String> errors) {
    this.requested = requested;
    this.applied = applied;
    this.skipped = skipped;
    setErrors(errors);
  }

  /** Convenience factory for the atomic happy path where every requested item was applied. */
  public static DaaBulkRelationResult allApplied(int requested) {
    return new DaaBulkRelationResult(requested, requested, 0, List.of());
  }

  public Integer getRequested() {
    return requested;
  }

  public void setRequested(Integer requested) {
    this.requested = requested;
  }

  public Integer getApplied() {
    return applied;
  }

  public void setApplied(Integer applied) {
    this.applied = applied;
  }

  public Integer getSkipped() {
    return skipped;
  }

  public void setSkipped(Integer skipped) {
    this.skipped = skipped;
  }

  public List<String> getErrors() {
    return errors;
  }

  /**
   * Normalizes {@code null} to an empty list and defensively copies, preserving the class invariant
   * that {@code errors} is always a non-null list callers can safely read.
   */
  public void setErrors(List<String> errors) {
    this.errors = errors == null ? new ArrayList<>() : new ArrayList<>(errors);
  }
}
