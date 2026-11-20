package org.broadinstitute.consent.http.models;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Timestamp;
import org.junit.jupiter.api.Test;

class SubmissionApprovalTest {

  private static DataAccessRequest dar(String referenceId, Long submissionDate) {
    DataAccessRequest dar = new DataAccessRequest();
    dar.setReferenceId(referenceId);
    dar.setSubmissionDate(submissionDate == null ? null : new Timestamp(submissionDate));
    return dar;
  }

  @Test
  void isEmptyOnlyWithoutBothValues() {
    assertTrue(new SubmissionApproval(null, null).isEmpty());
    assertFalse(new SubmissionApproval("ref", null).isEmpty());
    assertFalse(new SubmissionApproval(null, 1L).isEmpty());
  }

  @Test
  void matchesTheNamedSubmission() {
    assertTrue(new SubmissionApproval("ref", 5L).matches(dar("ref", 5L)));
  }

  @Test
  void doesNotMatchAnotherSubmission() {
    assertFalse(new SubmissionApproval("other", 5L).matches(dar("ref", 5L)));
    // A submission is named by both values; a date alone could match another DAR.
    assertFalse(new SubmissionApproval(null, 5L).matches(dar("ref", 5L)));
    assertFalse(new SubmissionApproval("ref", 4L).matches(dar("ref", 5L)));
    assertFalse(new SubmissionApproval("ref", null).matches(dar("ref", 5L)));
    assertFalse(new SubmissionApproval("ref", 5L).matches(dar("ref", null)));
  }
}
