package org.broadinstitute.consent.http.models;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DarManualReviewTest {

  private DataAccessRequest dar;

  @BeforeEach
  void setUp() {
    dar = new DataAccessRequest();
    dar.setData(new DataAccessRequestData());
  }

  @Test
  void testManualReviewFalse() {
    assertFalse(dar.requiresManualReview());
    // There are many fields we could check, but this one is enough to prove our logic.
    dar.addDatasetId(1);
    assertFalse(dar.requiresManualReview());
  }

  @Test
  void testManualReviewPoa() {
    dar.getData().setPoa(true);
    assertTrue(dar.requiresManualReview());
  }

  @Test
  void testManualReviewPopulation() {
    dar.getData().setPopulation(true);
    assertTrue(dar.requiresManualReview());
  }

  @Test
  void testManualReviewOther() {
    dar.getData().setOther(true);
    assertTrue(dar.requiresManualReview());
  }

  @Test
  void testManualReviewOtherTextTrue() {
    dar.getData().setOtherText("true");
    assertTrue(dar.requiresManualReview());
  }

  @Test
  void testManualReviewOtherTextFalse() {
    dar.getData().setOtherText("");
    assertFalse(dar.requiresManualReview());
  }

  @Test
  void testManualReviewIllegalBehavior() {
    dar.getData().setIllegalBehavior(true);
    assertTrue(dar.requiresManualReview());
  }

  @Test
  void testManualReviewIllegalAddiction() {
    dar.getData().setAddiction(true);
    assertTrue(dar.requiresManualReview());
  }

  @Test
  void testManualReviewSexualDiseases() {
    dar.getData().setSexualDiseases(true);
    assertTrue(dar.requiresManualReview());
  }

  @Test
  void testManualReviewStigmatizedDiseases() {
    dar.getData().setStigmatizedDiseases(true);
    assertTrue(dar.requiresManualReview());
  }

  @Test
  void testManualReviewVulnerablePopulation() {
    dar.getData().setVulnerablePopulation(true);
    assertTrue(dar.requiresManualReview());
  }

  @Test
  void testManualReviewPopulationMigration() {
    dar.getData().setPopulationMigration(true);
    assertTrue(dar.requiresManualReview());
  }

  @Test
  void testManualReviewPsychiatricTraits() {
    dar.getData().setPsychiatricTraits(true);
    assertTrue(dar.requiresManualReview());
  }

  @Test
  void testManualReviewNotHealth() {
    dar.getData().setNotHealth(true);
    assertTrue(dar.requiresManualReview());
  }

}
