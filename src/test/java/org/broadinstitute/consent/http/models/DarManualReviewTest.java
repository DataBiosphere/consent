package org.broadinstitute.consent.http.models;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DarManualReviewTest {

    private DataAccessRequest dar;

    @BeforeEach
    public void setUp() {
        dar = new DataAccessRequest();
        dar.setData(new DataAccessRequestData());
    }

    @Test
    public void testManualReviewFalse() {
        Assertions.assertFalse(dar.requiresManualReview());
        // There are many fields we could check, but this one is enough to prove our logic.
        dar.addDatasetId(1);
        Assertions.assertFalse(dar.requiresManualReview());
    }

    @Test
    public void testManualReviewPoa() {
        dar.getData().setPoa(true);
        Assertions.assertTrue(dar.requiresManualReview());
    }

    @Test
    public void testManualReviewPopulation() {
        dar.getData().setPopulation(true);
        Assertions.assertTrue(dar.requiresManualReview());
    }

    @Test
    public void testManualReviewOther() {
        dar.getData().setOther(true);
        Assertions.assertTrue(dar.requiresManualReview());
    }

    @Test
    public void testManualReviewOtherTextTrue() {
        dar.getData().setOtherText("true");
        Assertions.assertTrue(dar.requiresManualReview());
    }

    @Test
    public void testManualReviewOtherTextFalse() {
        dar.getData().setOtherText("");
        Assertions.assertFalse(dar.requiresManualReview());
    }

    @Test
    public void testManualReviewIllegalBehavior() {
        dar.getData().setIllegalBehavior(true);
        Assertions.assertTrue(dar.requiresManualReview());
    }

    @Test
    public void testManualReviewIllegalAddiction() {
        dar.getData().setAddiction(true);
        Assertions.assertTrue(dar.requiresManualReview());
    }

    @Test
    public void testManualReviewSexualDiseases() {
        dar.getData().setSexualDiseases(true);
        Assertions.assertTrue(dar.requiresManualReview());
    }

    @Test
    public void testManualReviewStigmatizedDiseases() {
        dar.getData().setStigmatizedDiseases(true);
        Assertions.assertTrue(dar.requiresManualReview());
    }

    @Test
    public void testManualReviewVulnerablePopulation() {
        dar.getData().setVulnerablePopulation(true);
        Assertions.assertTrue(dar.requiresManualReview());
    }

    @Test
    public void testManualReviewPopulationMigration() {
        dar.getData().setPopulationMigration(true);
        Assertions.assertTrue(dar.requiresManualReview());
    }

    @Test
    public void testManualReviewPsychiatricTraits() {
        dar.getData().setPsychiatricTraits(true);
        Assertions.assertTrue(dar.requiresManualReview());
    }

    @Test
    public void testManualReviewNotHealth() {
        dar.getData().setNotHealth(true);
        Assertions.assertTrue(dar.requiresManualReview());
    }

}
