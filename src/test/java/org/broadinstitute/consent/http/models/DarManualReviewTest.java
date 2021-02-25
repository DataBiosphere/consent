package org.broadinstitute.consent.http.models;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.junit.Before;
import org.junit.Test;

public class DarManualReviewTest {

  private DataAccessRequest dar;

  @Before
  public void setUp() {
    dar = new DataAccessRequest();
    dar.setData(new DataAccessRequestData());
  }

  @Test
  public void testManualReviewFalse() {
    assertFalse(dar.requiresManualReview());
    // There are many fields we could check, but this one is enough to prove our logic.
    dar.getData().setDatasetIds(Collections.singletonList(1));
    assertFalse(dar.requiresManualReview());
  }

  @Test
  public void testManualReviewPoa() {
    dar.getData().setPoa(true);
    assertTrue(dar.requiresManualReview());
  }

  @Test
  public void testManualReviewPopulation() {
    dar.getData().setPopulation(true);
    assertTrue(dar.requiresManualReview());
  }

  @Test
  public void testManualReviewOther() {
    dar.getData().setOther(true);
    assertTrue(dar.requiresManualReview());
  }

  @Test
  public void testManualReviewOtherText() {
    dar.getData().setOtherText("true");
    assertTrue(dar.requiresManualReview());
  }

  @Test
  public void testManualReviewIllegalBehavior() {
    dar.getData().setIllegalBehavior(true);
    assertTrue(dar.requiresManualReview());
  }

  @Test
  public void testManualReviewIllegalAddiction() {
    dar.getData().setAddiction(true);
    assertTrue(dar.requiresManualReview());
  }

  @Test
  public void testManualReviewSexualDiseases() {
    dar.getData().setSexualDiseases(true);
    assertTrue(dar.requiresManualReview());
  }

  @Test
  public void testManualReviewStigmatizedDiseases() {
    dar.getData().setStigmatizedDiseases(true);
    assertTrue(dar.requiresManualReview());
  }

  @Test
  public void testManualReviewVulnerablePopulation() {
    dar.getData().setVulnerablePopulation(true);
    assertTrue(dar.requiresManualReview());
  }

  @Test
  public void testManualReviewPopulationMigration() {
    dar.getData().setPopulationMigration(true);
    assertTrue(dar.requiresManualReview());
  }

  @Test
  public void testManualReviewPsychiatricTraits() {
    dar.getData().setPsychiatricTraits(true);
    assertTrue(dar.requiresManualReview());
  }

  @Test
  public void testManualReviewNotHealth() {
    dar.getData().setNotHealth(true);
    assertTrue(dar.requiresManualReview());
  }

}
