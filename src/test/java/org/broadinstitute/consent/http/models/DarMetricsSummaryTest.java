package org.broadinstitute.consent.http.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;
import org.broadinstitute.consent.http.AbstractTestHelper;
import org.junit.jupiter.api.Test;

class DarMetricsSummaryTest extends AbstractTestHelper {

  @Test
  void testConstructor_nullCollectionSummary() {
    DarMetricsSummary summary = new DarMetricsSummary(null);
    assertNull(summary.updateDate());
    assertNull(summary.projectTitle());
    assertNull(summary.darCode());
    assertNull(summary.nonTechRus());
    assertNull(summary.referenceId());
    assertTrue(summary.expired());
  }

  @Test
  void testConstructor_emptyCollectionSummary() {
    DarCollectionSummary darCollectionSummary = new DarCollectionSummary();
    DarMetricsSummary summary = new DarMetricsSummary(darCollectionSummary);
    assertNull(summary.updateDate());
    assertNull(summary.projectTitle());
    assertNull(summary.darCode());
    assertNull(summary.nonTechRus());
    assertNull(summary.referenceId());
    // No submissionDate set: isExpired() defaults to false
    assertFalse(summary.expired());
  }

  @Test
  void testConstructor_updateDate() {
    DarCollectionSummary darCollectionSummary = new DarCollectionSummary();
    darCollectionSummary.setUpdateDate(Timestamp.from(Instant.now()));

    DarMetricsSummary summary = new DarMetricsSummary(darCollectionSummary);
    assertEntityEquivalence(darCollectionSummary, summary, false);
  }

  @Test
  void testConstructor_projectTitle() {
    DarCollectionSummary darCollectionSummary = new DarCollectionSummary();
    darCollectionSummary.setName(randomAlphabetic(10));

    DarMetricsSummary summary = new DarMetricsSummary(darCollectionSummary);
    assertEntityEquivalence(darCollectionSummary, summary, false);
  }

  @Test
  void testConstructor_darCode() {
    DarCollectionSummary darCollectionSummary = new DarCollectionSummary();
    darCollectionSummary.setDarCode(randomAlphabetic(10));

    DarMetricsSummary summary = new DarMetricsSummary(darCollectionSummary);
    assertEntityEquivalence(darCollectionSummary, summary, false);
  }

  @Test
  void testConstructor_nonTechRus() {
    DarCollectionSummary darCollectionSummary = new DarCollectionSummary();
    darCollectionSummary.setNonTechRus(randomAlphabetic(10));

    DarMetricsSummary summary = new DarMetricsSummary(darCollectionSummary);
    assertEntityEquivalence(darCollectionSummary, summary, false);
  }

  @Test
  void testConstructor_referenceId() {
    DarCollectionSummary darCollectionSummary = new DarCollectionSummary();
    darCollectionSummary.setLatestReferenceId(UUID.randomUUID().toString());

    DarMetricsSummary summary = new DarMetricsSummary(darCollectionSummary);
    assertEntityEquivalence(darCollectionSummary, summary, false);
  }

  @Test
  void testConstructor_notExpired() {
    DarCollectionSummary darCollectionSummary = new DarCollectionSummary();
    darCollectionSummary.setSubmissionDate(Timestamp.from(Instant.now()));

    DarMetricsSummary summary = new DarMetricsSummary(darCollectionSummary);
    assertEntityEquivalence(darCollectionSummary, summary, false);
  }

  @Test
  void testConstructor_isExpired() {
    DarCollectionSummary darCollectionSummary = new DarCollectionSummary();
    LocalDateTime twoYearsAgo = LocalDateTime.now().minusYears(2);
    ZonedDateTime zonedDateTime = twoYearsAgo.atZone(ZoneId.systemDefault());
    darCollectionSummary.setSubmissionDate(Timestamp.from(zonedDateTime.toInstant()));

    DarMetricsSummary summary = new DarMetricsSummary(darCollectionSummary);
    assertEntityEquivalence(darCollectionSummary, summary, true);
  }

  private void assertEntityEquivalence(
      DarCollectionSummary darCollectionSummary,
      DarMetricsSummary summary,
      boolean expectedExpired) {
    assertEquals(darCollectionSummary.getUpdateDate(), summary.updateDate());
    assertEquals(darCollectionSummary.getName(), summary.projectTitle());
    assertEquals(darCollectionSummary.getDarCode(), summary.darCode());
    assertEquals(darCollectionSummary.getNonTechRus(), summary.nonTechRus());
    assertEquals(darCollectionSummary.getLatestReferenceId(), summary.referenceId());
    assertEquals(expectedExpired, summary.expired());
  }
}
