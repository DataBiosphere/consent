package org.broadinstitute.consent.http.models;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.Timestamp;
import org.junit.jupiter.api.Test;

class DarCollectionTest {

  private static final Timestamp SUBMITTED = Timestamp.valueOf("2026-01-01 00:00:00");

  @Test
  void getMostRecentDarBreaksASubmissionDateTieByTheLaterDar() {
    // Both id orders, since which DAR the map iterates first decides an unbroken tie.
    assertEquals(2, mostRecentDarId("a", 1, "b", 2));
    assertEquals(2, mostRecentDarId("a", 2, "b", 1));
  }

  private static Integer mostRecentDarId(
      String firstReferenceId, Integer firstId, String secondReferenceId, Integer secondId) {
    DarCollection collection = new DarCollection();
    collection.addDar(dar(firstReferenceId, firstId));
    collection.addDar(dar(secondReferenceId, secondId));
    return collection.getMostRecentDar().getId();
  }

  private static DataAccessRequest dar(String referenceId, Integer id) {
    DataAccessRequest dar = new DataAccessRequest();
    dar.setReferenceId(referenceId);
    dar.setId(id);
    dar.setSubmissionDate(SUBMITTED);
    return dar;
  }
}
