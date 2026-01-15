package org.broadinstitute.consent.http.rules;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.Dataset;
import org.junit.jupiter.api.Test;

class SOApprovalForNewDARRequiredTest {
  @Test
  void ensureRuleReturnsFalse() {
    SOApprovalForNewDARRequired rule = new SOApprovalForNewDARRequired();
    assertFalse(rule.compare(new Dataset(), new DataAccessRequest()));
  }
}
