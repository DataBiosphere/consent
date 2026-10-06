package org.broadinstitute.consent.http.db;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.OntologyEntry;
import org.broadinstitute.consent.http.models.TermDarCount;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.service.ontology.OntologyTerm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DarTermMetricsDAOTest extends DAOTestHelper {

  private static final Instant FROM = startOf(LocalDate.of(2026, 1, 1));
  private static final Instant TO = startOf(LocalDate.of(2027, 1, 1));
  private static final Date IN_RANGE = Date.from(startOf(LocalDate.of(2026, 6, 1)));
  private static final String CANCER = "http://purl.obolibrary.org/obo/MONDO_0004992";
  private static final String DIABETES = "http://purl.obolibrary.org/obo/MONDO_0005015";
  private static final String ASTHMA = "http://purl.obolibrary.org/obo/MONDO_0004979";

  private DarTermMetricsDAO dao;
  private User user;

  @BeforeEach
  void setUpDao() {
    dao = jdbi.onDemand(DarTermMetricsDAO.class);
    user = createUser();
  }

  @Test
  void ranksTermsByTheDarsCitingThemCountingEachDarOnce() {
    dar(IN_RANGE, null, term(CANCER, "cancer"), term(CANCER, "cancer"));
    dar(IN_RANGE, null, term(CANCER.toUpperCase() + " ", "cancer"), term(DIABETES, "diabetes"));
    dar(IN_RANGE, null, term(DIABETES, "diabetes"), term(ASTHMA, "asthma"));
    dar(IN_RANGE, null, term(CANCER, "cancer"));

    assertEquals(
        List.of(
            new TermDarCount(CANCER, "cancer", 3),
            new TermDarCount(DIABETES, "diabetes", 2),
            new TermDarCount(ASTHMA, "asthma", 1)),
        dao.findTopTerms(FROM, TO, 10));
  }

  @Test
  void countsOnlyOriginalDarsSubmittedInTheRangeThatArentCanceledOrArchived() {
    String parent = dar(IN_RANGE, null, term(CANCER, "cancer"));
    dar(Date.from(TO), null, term(DIABETES, "diabetes"));
    dar(IN_RANGE, "Canceled", term(DIABETES, "diabetes"));
    dar(IN_RANGE, "archived", term(DIABETES, "diabetes"));
    progressReport(parent, term(ASTHMA, "asthma"));

    assertEquals(List.of(new TermDarCount(CANCER, "cancer", 1)), dao.findTopTerms(FROM, TO, 10));
  }

  @Test
  void prefersTheIndexedLabelAndKeepsTheLimit() {
    OntologyTerm indexed = new OntologyTerm(DIABETES, "v1", "MONDO");
    indexed.setLabel("diabetes mellitus");
    indexed.setUsable(true);
    ontologyDAO.batchInsertTerms(List.of(indexed), user.getUserId());
    dar(IN_RANGE, null, term(DIABETES, "diabetes"), term(CANCER, "cancer"));
    dar(IN_RANGE, null, term(DIABETES, "diabetes"));

    assertEquals(
        List.of(new TermDarCount(DIABETES, "diabetes mellitus", 2)), dao.findTopTerms(FROM, TO, 1));
  }

  @Test
  void countsATermCitedByIriAndByOboIdOnceAndBreaksTiesByLabel() {
    OntologyTerm indexed = new OntologyTerm(CANCER, "v1", "MONDO");
    indexed.setLabel("malignant neoplasm");
    indexed.setOboId("MONDO_0004992");
    indexed.setUsable(true);
    ontologyDAO.batchInsertTerms(List.of(indexed), user.getUserId());
    dar(IN_RANGE, null, term(CANCER, "cancer"));
    dar(IN_RANGE, null, term("mondo:0004992", "cancer"));
    dar(IN_RANGE, null, term(DIABETES, "diabetes"), term(ASTHMA, "asthma"));
    dar(IN_RANGE, null, term(DIABETES, "diabetes"), term(ASTHMA, "asthma"));

    assertEquals(
        List.of(
            new TermDarCount(ASTHMA, "asthma", 2),
            new TermDarCount(DIABETES, "diabetes", 2),
            new TermDarCount(CANCER, "malignant neoplasm", 2)),
        dao.findTopTerms(FROM, TO, 10));
  }

  @Test
  void matchesAnIriVariantToTheIndexedOboId() {
    OntologyTerm indexed = new OntologyTerm(CANCER, "v1", "MONDO");
    indexed.setLabel("malignant neoplasm");
    indexed.setOboId("MONDO_0004992");
    indexed.setUsable(true);
    ontologyDAO.batchInsertTerms(List.of(indexed), user.getUserId());
    dar(IN_RANGE, null, term(CANCER.replace("http:", "https:"), "cancer"));

    assertEquals(
        List.of(new TermDarCount(CANCER, "malignant neoplasm", 1)), dao.findTopTerms(FROM, TO, 10));
  }

  @Test
  void countsAnUnindexedTermCitedByIriAndByCurieOnce() {
    dar(IN_RANGE, null, term(CANCER, "cancer"));
    dar(IN_RANGE, null, term(CANCER, "cancer"));
    dar(IN_RANGE, null, term("MONDO:0004992", "cancer"));

    assertEquals(List.of(new TermDarCount(CANCER, "cancer", 3)), dao.findTopTerms(FROM, TO, 10));
  }

  @Test
  void ignoresTheLabelOfAnUnusableIndexedTerm() {
    OntologyTerm obsolete = new OntologyTerm(ASTHMA, "v1", "MONDO");
    obsolete.setLabel("obsolete asthma");
    obsolete.setUsable(false);
    ontologyDAO.batchInsertTerms(List.of(obsolete), user.getUserId());
    dar(IN_RANGE, null, term(ASTHMA, "asthma"));

    assertEquals(List.of(new TermDarCount(ASTHMA, "asthma", 1)), dao.findTopTerms(FROM, TO, 10));
  }

  private static Instant startOf(LocalDate date) {
    return date.atStartOfDay(ZoneId.systemDefault()).toInstant();
  }

  private static OntologyEntry term(String id, String label) {
    OntologyEntry entry = new OntologyEntry();
    entry.setId(id);
    entry.setLabel(label);
    return entry;
  }

  private String dar(Date submitted, String status, OntologyEntry... terms) {
    DataAccessRequestData data = new DataAccessRequestData();
    data.setOntologies(Arrays.asList(terms));
    data.setStatus(status);
    Integer collectionId =
        darCollectionDAO.insertDarCollection(
            "DAR-" + UUID.randomUUID(), user.getUserId(), submitted);
    String referenceId = UUID.randomUUID().toString();
    dataAccessRequestDAO.insertDataAccessRequest(
        collectionId, referenceId, user.getUserId(), submitted, submitted, submitted, data, "era");
    return referenceId;
  }

  private void progressReport(String parent, OntologyEntry... terms) {
    DataAccessRequestData data = new DataAccessRequestData();
    data.setOntologies(Arrays.asList(terms));
    var parentDar = dataAccessRequestDAO.findByReferenceId(parent);
    String child = UUID.randomUUID().toString();
    dataAccessRequestDAO.insertProgressReport(
        parentDar.getId(), parentDar.getCollectionId(), child, user.getUserId(), data, "era");
    jdbi.useHandle(
        handle ->
            handle
                .createUpdate(
                    "UPDATE data_access_request SET submission_date = :at WHERE reference_id = :id")
                .bind("at", IN_RANGE)
                .bind("id", child)
                .execute());
  }
}
