package org.broadinstitute.consent.http.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
  void testConstructor_null() {
    DarMetricsSummary summary = new DarMetricsSummary(null);
    assertNull(summary.updateDate());
    assertNull(summary.projectTitle());
    assertNull(summary.darCode());
    assertNull(summary.nonTechRus());
    assertNull(summary.referenceId());
    assertTrue(summary.expired());
  }

  @Test
  void testConstructor_updateDate() {
    DataAccessRequest dar = new DataAccessRequest();
    dar.setUpdateDate(Timestamp.from(Instant.now()));
    DarMetricsSummary summary = new DarMetricsSummary(dar);
    assertNotNull(summary.updateDate());
    assertNull(summary.projectTitle());
    assertNull(summary.darCode());
    assertNull(summary.nonTechRus());
    assertNull(summary.referenceId());
    assertTrue(summary.expired());
  }

  @Test
  void testConstructor_projectTitle() {
    DataAccessRequest dar = new DataAccessRequest();
    DataAccessRequestData data = new DataAccessRequestData();
    data.setProjectTitle(randomAlphabetic(10));
    dar.setData(data);
    DarMetricsSummary summary = new DarMetricsSummary(dar);
    assertEquals(dar.getUpdateDate(), summary.updateDate());
    assertEquals(dar.getData().getProjectTitle(), summary.projectTitle());
    assertEquals(dar.getDarCode(), summary.darCode());
    assertEquals(dar.getData().getNonTechRus(), summary.nonTechRus());
    assertEquals(dar.getReferenceId(), summary.referenceId());
    assertTrue(summary.expired());
  }

  @Test
  void testConstructor_darCode() {
    DataAccessRequest dar = new DataAccessRequest();
    dar.setDarCode(randomAlphabetic(10));
    DataAccessRequestData data = new DataAccessRequestData();
    dar.setData(data);
    DarMetricsSummary summary = new DarMetricsSummary(dar);
    assertEquals(dar.getUpdateDate(), summary.updateDate());
    assertEquals(dar.getData().getProjectTitle(), summary.projectTitle());
    assertEquals(dar.getDarCode(), summary.darCode());
    assertEquals(dar.getData().getNonTechRus(), summary.nonTechRus());
    assertEquals(dar.getReferenceId(), summary.referenceId());
    assertTrue(summary.expired());
  }

  @Test
  void testConstructor_nonTechRus() {
    DataAccessRequest dar = new DataAccessRequest();
    DataAccessRequestData data = new DataAccessRequestData();
    data.setNonTechRus(randomAlphabetic(10));
    dar.setData(data);
    DarMetricsSummary summary = new DarMetricsSummary(dar);
    assertEquals(dar.getUpdateDate(), summary.updateDate());
    assertEquals(dar.getData().getProjectTitle(), summary.projectTitle());
    assertEquals(dar.getDarCode(), summary.darCode());
    assertEquals(dar.getData().getNonTechRus(), summary.nonTechRus());
    assertEquals(dar.getReferenceId(), summary.referenceId());
    assertTrue(summary.expired());
  }

  @Test
  void testConstructor_referenceId() {
    DataAccessRequest dar = new DataAccessRequest();
    dar.setReferenceId(UUID.randomUUID().toString());
    DataAccessRequestData data = new DataAccessRequestData();
    dar.setData(data);
    DarMetricsSummary summary = new DarMetricsSummary(dar);
    assertEquals(dar.getUpdateDate(), summary.updateDate());
    assertEquals(dar.getData().getProjectTitle(), summary.projectTitle());
    assertEquals(dar.getDarCode(), summary.darCode());
    assertEquals(dar.getData().getNonTechRus(), summary.nonTechRus());
    assertEquals(dar.getReferenceId(), summary.referenceId());
    assertTrue(summary.expired());
  }

  @Test
  void testConstructor_notExpired() {
    DataAccessRequest dar = new DataAccessRequest();
    dar.setSubmissionDate(Timestamp.from(Instant.now()));
    DataAccessRequestData data = new DataAccessRequestData();
    dar.setData(data);
    DarMetricsSummary summary = new DarMetricsSummary(dar);
    assertEquals(dar.getUpdateDate(), summary.updateDate());
    assertEquals(dar.getData().getProjectTitle(), summary.projectTitle());
    assertEquals(dar.getDarCode(), summary.darCode());
    assertEquals(dar.getData().getNonTechRus(), summary.nonTechRus());
    assertEquals(dar.getReferenceId(), summary.referenceId());
    assertFalse(summary.expired());
  }

  @Test
  void testConstructor_isExpired() {
    DataAccessRequest dar = new DataAccessRequest();
    LocalDateTime oneYearAgo = LocalDateTime.now().minusYears(2);
    ZonedDateTime zonedDateTime = oneYearAgo.atZone(ZoneId.systemDefault());
    dar.setSubmissionDate(Timestamp.from(zonedDateTime.toInstant()));
    DataAccessRequestData data = new DataAccessRequestData();
    dar.setData(data);
    DarMetricsSummary summary = new DarMetricsSummary(dar);
    assertEquals(dar.getUpdateDate(), summary.updateDate());
    assertEquals(dar.getData().getProjectTitle(), summary.projectTitle());
    assertEquals(dar.getDarCode(), summary.darCode());
    assertEquals(dar.getData().getNonTechRus(), summary.nonTechRus());
    assertEquals(dar.getReferenceId(), summary.referenceId());
    assertTrue(summary.expired());
  }
}
