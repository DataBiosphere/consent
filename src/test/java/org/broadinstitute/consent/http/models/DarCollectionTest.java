package org.broadinstitute.consent.http.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

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

  @Test
  void getMostRecentDarPrefersASubmittedDarOverADraft() {
    DarCollection collection = new DarCollection();
    DataAccessRequest draft = dar("a", 1);
    draft.setSubmissionDate(null);
    collection.addDar(draft);
    collection.addDar(dar("b", 2));
    collection.addDar(dar("c", 3));
    // No NullPointerException from comparing a draft's missing submission date.
    assertEquals(3, collection.getMostRecentDar().getId());
  }

  @Test
  void getMostRecentDarOfOnlyDraftsReturnsADraft() {
    DarCollection collection = new DarCollection();
    DataAccessRequest first = dar("a", 1);
    first.setSubmissionDate(null);
    DataAccessRequest second = dar("b", 2);
    second.setSubmissionDate(null);
    collection.addDar(first);
    collection.addDar(second);
    assertNull(collection.getMostRecentDar().getSubmissionDate());
    assertEquals(2, collection.getMostRecentDar().getId());
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
