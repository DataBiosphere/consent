package org.broadinstitute.consent.http.models;

/**
 * Names the submission a signing official approves: the one they reviewed. The approval is refused
 * when the request's current submission differs.
 *
 * @param referenceId The approved DAR's reference id
 * @param submissionDate The approved DAR's submission date, in milliseconds from the epoch
 */
public record SubmissionApproval(String referenceId, Long submissionDate) {

  /** Clients that predate this record send an empty body or {}. */
  public boolean isEmpty() {
    return referenceId == null && submissionDate == null;
  }

  /**
   * Whether the DAR is the named submission: both values must match. Dates compare in milliseconds,
   * as clients get them.
   */
  public boolean matches(DataAccessRequest dar) {
    return referenceId != null
        && referenceId.equals(dar.getReferenceId())
        && submissionDate != null
        && dar.getSubmissionDate() != null
        && submissionDate == dar.getSubmissionDate().getTime();
  }
}
