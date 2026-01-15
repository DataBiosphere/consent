package org.broadinstitute.consent.http.rules;

import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.Dataset;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

class AutoOpenDARForAllMembersTest {
  @Test
  void ensureRuleReturnsFalse() {
    AutoOpenDARForAllMembers rule = new AutoOpenDARForAllMembers();
    assertFalse(rule.compare(new Dataset(), new DataAccessRequest()));
  }
}
