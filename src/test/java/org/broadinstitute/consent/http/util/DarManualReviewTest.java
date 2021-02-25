package org.broadinstitute.consent.http.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

public class DarManualReviewTest {

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void testManualReviewFalse() {
    DataAccessRequest dar = new DataAccessRequest();
    assertFalse(dar.requiresManualReview());
    DataAccessRequestData data = new DataAccessRequestData();
    dar.setData(data);
    assertFalse(dar.requiresManualReview());
  }

  @Test
  public void testManualReviewPoa() {
    DataAccessRequest dar = new DataAccessRequest();
    DataAccessRequestData data = new DataAccessRequestData();
    data.setPoa(true);
    dar.setData(data);
    assertTrue(dar.requiresManualReview());
  }

  @Test
  public void testManualReviewPopulation() {
    DataAccessRequest dar = new DataAccessRequest();
    DataAccessRequestData data = new DataAccessRequestData();
    data.setPopulation(true);
    dar.setData(data);
    assertTrue(dar.requiresManualReview());
  }

  @Test
  public void testManualReviewOther() {
    DataAccessRequest dar = new DataAccessRequest();
    DataAccessRequestData data = new DataAccessRequestData();
    data.setOther(true);
    dar.setData(data);
    assertTrue(dar.requiresManualReview());
  }

  @Test
  public void testManualReviewOtherText() {
    DataAccessRequest dar = new DataAccessRequest();
    DataAccessRequestData data = new DataAccessRequestData();
    data.setOtherText("true");
    dar.setData(data);
    assertTrue(dar.requiresManualReview());
  }

  @Test
  public void testManualReviewIllegalBehavior() {
    DataAccessRequest dar = new DataAccessRequest();
    DataAccessRequestData data = new DataAccessRequestData();
    data.setIllegalBehavior(true);
    dar.setData(data);
    assertTrue(dar.requiresManualReview());
  }

  @Test
  public void testManualReviewIllegalAddiction() {
    DataAccessRequest dar = new DataAccessRequest();
    DataAccessRequestData data = new DataAccessRequestData();
    data.setAddiction(true);
    dar.setData(data);
    assertTrue(dar.requiresManualReview());
  }

  @Test
  public void testManualReviewSexualDiseases() {
    DataAccessRequest dar = new DataAccessRequest();
    DataAccessRequestData data = new DataAccessRequestData();
    data.setSexualDiseases(true);
    dar.setData(data);
    assertTrue(dar.requiresManualReview());
  }

  @Test
  public void testManualReviewStigmatizedDiseases() {
    DataAccessRequest dar = new DataAccessRequest();
    DataAccessRequestData data = new DataAccessRequestData();
    data.setStigmatizedDiseases(true);
    dar.setData(data);
    assertTrue(dar.requiresManualReview());
  }

  @Test
  public void testManualReviewVulnerablePopulation() {
    DataAccessRequest dar = new DataAccessRequest();
    DataAccessRequestData data = new DataAccessRequestData();
    data.setVulnerablePopulation(true);
    dar.setData(data);
    assertTrue(dar.requiresManualReview());
  }

  @Test
  public void testManualReviewPopulationMigration() {
    DataAccessRequest dar = new DataAccessRequest();
    DataAccessRequestData data = new DataAccessRequestData();
    data.setPopulationMigration(true);
    dar.setData(data);
    assertTrue(dar.requiresManualReview());
  }

  @Test
  public void testManualReviewPsychiatricTraits() {
    DataAccessRequest dar = new DataAccessRequest();
    DataAccessRequestData data = new DataAccessRequestData();
    data.setPsychiatricTraits(true);
    dar.setData(data);
    assertTrue(dar.requiresManualReview());
  }

  @Test
  public void testManualReviewNotHealth() {
    DataAccessRequest dar = new DataAccessRequest();
    DataAccessRequestData data = new DataAccessRequestData();
    data.setNotHealth(true);
    dar.setData(data);
    assertTrue(dar.requiresManualReview());
  }

}
