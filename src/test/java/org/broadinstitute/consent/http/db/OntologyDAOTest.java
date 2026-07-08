package org.broadinstitute.consent.http.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.google.cloud.storage.BlobId;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import jakarta.ws.rs.core.StreamingOutput;
import java.io.FileInputStream;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.broadinstitute.consent.http.cloudstore.GCSService;
import org.broadinstitute.consent.http.configurations.StoreConfiguration;
import org.broadinstitute.consent.http.enumeration.OntologyType;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.models.DataUseBuilder;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.OntologyEntry;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.service.ontology.OntologyIndexService;
import org.broadinstitute.consent.http.service.ontology.OntologyReconciliationResult;
import org.broadinstitute.consent.http.service.ontology.OntologyTerm;
import org.broadinstitute.consent.http.util.gson.GsonUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OntologyDAOTest extends DAOTestHelper {

  private static final Instant FIXED_INSTANT = Instant.parse("2024-01-01T00:00:00Z");

  @Mock private GCSService gcsService;
  @Mock private StoreConfiguration storeConfiguration;
  private final Gson gson = GsonUtil.getInstance();

  private Collection<OntologyTerm> batchInsertTerms() throws Exception {
    when(storeConfiguration.getBucket()).thenReturn("my-bucket");
    when(gcsService.getDocument(any(BlobId.class)))
        .thenReturn(new FileInputStream("src/test/resources/duo.owl"));
    OntologyIndexService indexer = new OntologyIndexService(gcsService, storeConfiguration);
    Collection<OntologyTerm> terms = indexer.generateTerms(OntologyType.DUO);
    User user = createUser();
    ontologyDAO.batchInsertTerms(terms, user.getUserId());
    return terms;
  }

  @Test
  void testInsertTerms() throws Exception {
    Collection<OntologyTerm> terms = batchInsertTerms();
    int count = ontologyDAO.countTerms();
    assertEquals(terms.size(), count);
  }

  @Test
  void testDeleteTerms() throws Exception {
    Collection<OntologyTerm> terms = batchInsertTerms();
    int count = ontologyDAO.countTerms();
    assertEquals(terms.size(), count);
    ontologyDAO.deleteByOntology(OntologyType.DUO.name());
    int count2 = ontologyDAO.countTerms();
    assertEquals(0, count2);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "DUO_0000006", // normalized obo id
        " DUO_0000007 ", // normalized obo id with spaces
        "http://purl.obolibrary.org/obo/DUO_0000006" // full term_id
      })
  void testFindByIds(String id) throws Exception {
    batchInsertTerms();
    StreamingOutput output = ontologyDAO.findByTermIds(new String[] {id});
    JsonArray jsonArray = getJsonArrayFromStreamingOutput(output);
    assertEquals(1, jsonArray.size());
  }

  @Test
  void testFindByIdsMultiple() throws Exception {
    batchInsertTerms();
    StreamingOutput output = ontologyDAO.findByTermIds(new String[] {"DUO_0000006", "DUO_0000007"});
    JsonArray jsonArray = getJsonArrayFromStreamingOutput(output);
    assertEquals(2, jsonArray.size());
  }

  @Test
  void testFindByQuery() throws Exception {
    batchInsertTerms();
    StreamingOutput output = ontologyDAO.findByQuery("data use modifier", OntologyType.DUO, 5);
    JsonArray jsonArray = getJsonArrayFromStreamingOutput(output);
    assertEquals(5, jsonArray.size());
  }

  @Test
  // If invalid count is provided, default value of 20 should be used and results should be
  // returned.
  void testFindByQueryDefaultCount() throws Exception {
    batchInsertTerms();
    StreamingOutput output = ontologyDAO.findByQuery("data use modifier", OntologyType.DUO, 0);
    JsonArray jsonArray = getJsonArrayFromStreamingOutput(output);
    assertFalse(jsonArray.isEmpty());
  }

  @Test
  void testFindByQueryRanked() throws Exception {
    batchInsertTerms();
    StreamingOutput output = ontologyDAO.findByQuery("health medical biomedical", null, null);
    JsonArray jsonArray = getJsonArrayFromStreamingOutput(output);
    List<OntologyTerm> terms =
        jsonArray.asList().stream().map(t -> gson.fromJson(t, OntologyTerm.class)).toList();
    // In this example, DUO_0000006 contains the query terms more frequently than DUO_0000037, so it
    // should be ranked higher and returned first in the results.
    assertTrue(terms.getFirst().id().contains("DUO_0000006"));
    assertTrue(terms.get(1).id().contains("DUO_0000037"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"hea", "med", "bio"})
  void testFindByQueryPartial(String partial) throws Exception {
    batchInsertTerms();
    StreamingOutput output = ontologyDAO.findByQuery(partial, null, null);
    JsonArray jsonArray = getJsonArrayFromStreamingOutput(output);
    assertFalse(jsonArray.isEmpty());
    // In this example, DUO_0000006 and DUO_0000037 should be in all results
    String termIds =
        jsonArray.asList().stream()
            .map(t -> gson.fromJson(t, OntologyTerm.class))
            .map(OntologyTerm::id)
            .collect(Collectors.joining(" "));
    assertTrue(termIds.contains("DUO_0000006"));
    assertTrue(termIds.contains("DUO_0000037"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"hea", "med", "bio"})
  void testSanitizeForTsQuery(String partial) throws Exception {
    batchInsertTerms();
    // Test that special characters are sanitized and do not cause errors in the query
    String queryWithSpecialChars = partial + ":&|!";
    StreamingOutput output = ontologyDAO.findByQuery(queryWithSpecialChars, null, null);
    JsonArray jsonArray = getJsonArrayFromStreamingOutput(output);
    assertFalse(jsonArray.isEmpty());
  }

  @Test
  void testSanitizeForTsQueryNull() throws Exception {
    batchInsertTerms();
    StreamingOutput output = ontologyDAO.findByQuery(null, null, null);
    JsonArray jsonArray = getJsonArrayFromStreamingOutput(output);
    assertTrue(jsonArray.isEmpty());
  }

  @ParameterizedTest
  @ValueSource(strings = {" ", "", "\t", "\n", "@#$%", "!!", ":*", "&|!"})
  void testSanitizeForTsQueryNoAlphanumericContent(String partial) throws Exception {
    batchInsertTerms();
    StreamingOutput output = ontologyDAO.findByQuery(partial, null, null);
    JsonArray jsonArray = getJsonArrayFromStreamingOutput(output);
    assertTrue(jsonArray.isEmpty());
  }

  private static final String DOID_CANCER = "http://purl.obolibrary.org/obo/DOID_162";
  private static final String DOID_MISSING = "http://purl.obolibrary.org/obo/DOID_9999999";

  private void insertIndexedTerm(String id, String ontology, boolean usable, Integer userId) {
    jdbi.useHandle(
        handle ->
            handle
                .createUpdate(
                    """
                        INSERT INTO ontology_index (id, version, ontology, label, usable, create_user_id)
                        VALUES (:id, 'v1', :ontology, 'label', :usable, :userId)
                        """)
                .bind("id", id)
                .bind("ontology", ontology)
                .bind("usable", usable)
                .bind("userId", userId)
                .execute());
  }

  private Dataset insertDatasetWithDiseaseRestrictions(List<String> restrictions) {
    User user = createUser();
    Timestamp now = Timestamp.from(FIXED_INSTANT);
    DataUse dataUse =
        new DataUseBuilder().setGeneralUse(true).setDiseaseRestrictions(restrictions).build();
    Integer id =
        datasetDAO.insertDataset(
            "Name_" + randomAlphabetic(20),
            now,
            user.getUserId(),
            "Object ID_" + randomAlphabetic(20),
            dataUse.toString(),
            null);
    return datasetDAO.findDatasetById(id);
  }

  private DataAccessRequest insertDarWithOntologies(List<String> termIds) {
    User user = createUser();
    String darCode = "DAR-" + randomInt(1, 999999999);
    Integer collectionId =
        darCollectionDAO.insertDarCollection(darCode, user.getUserId(), Date.from(FIXED_INSTANT));
    DataAccessRequestData data = new DataAccessRequestData();
    data.setOntologies(
        termIds.stream()
            .map(
                termId -> {
                  OntologyEntry entry = new OntologyEntry();
                  entry.setId(termId);
                  entry.setLabel("label");
                  return entry;
                })
            .toList());
    String referenceId = UUID.randomUUID().toString();
    Date now = Date.from(FIXED_INSTANT);
    dataAccessRequestDAO.insertDataAccessRequest(
        collectionId, referenceId, user.getUserId(), now, now, now, data, randomAlphabetic(10));
    return dataAccessRequestDAO.findByReferenceId(referenceId);
  }

  private OntologyReconciliationResult findResult(
      List<OntologyReconciliationResult> results) {
    return results.stream().filter(r -> r.termId().equals(OntologyDAOTest.DOID_MISSING)).findFirst().orElse(null);
  }

  @Test
  void testReconcileMissingDatasetTerm() {
    User user = createUser();
    insertIndexedTerm(DOID_CANCER, OntologyType.DOID.name(), true, user.getUserId());
    Dataset dataset = insertDatasetWithDiseaseRestrictions(List.of(DOID_CANCER, DOID_MISSING));

    List<OntologyReconciliationResult> results = ontologyDAO.findReferencedTermsMissingFromIndex();

    // Only the un-indexed term is flagged; the indexed/usable term is not.
    assertEquals(1, results.size());
    OntologyReconciliationResult result = results.getFirst();
    assertEquals(DOID_MISSING, result.termId());
    assertEquals("MISSING_FROM_INDEX", result.issue());
    assertEquals(1L, result.referenceCount());
    assertEquals(1L, result.datasetRefs());
    assertEquals(0L, result.darRefs());
    assertTrue(result.referencedBy().contains("DATASET:" + dataset.getDatasetId()));
  }

  @Test
  void testReconcileMissingDarTerm() {
    DataAccessRequest dar = insertDarWithOntologies(List.of(DOID_MISSING));

    List<OntologyReconciliationResult> results = ontologyDAO.findReferencedTermsMissingFromIndex();

    assertEquals(1, results.size());
    OntologyReconciliationResult result = results.getFirst();
    assertEquals(DOID_MISSING, result.termId());
    assertEquals("MISSING_FROM_INDEX", result.issue());
    assertEquals(0L, result.datasetRefs());
    assertEquals(1L, result.darRefs());
    assertTrue(result.referencedBy().contains("DAR:" + dar.getReferenceId()));
  }

  @Test
  void testReconcilePresentButUnusableTerm() {
    User user = createUser();
    insertIndexedTerm(DOID_CANCER, OntologyType.DOID.name(), false, user.getUserId());
    insertDatasetWithDiseaseRestrictions(List.of(DOID_CANCER));

    List<OntologyReconciliationResult> results = ontologyDAO.findReferencedTermsMissingFromIndex();

    assertEquals(1, results.size());
    OntologyReconciliationResult result = results.getFirst();
    assertEquals(DOID_CANCER, result.termId());
    assertEquals("PRESENT_BUT_UNUSABLE", result.issue());
    assertEquals(OntologyType.DOID.name(), result.ontology());
  }

  @Test
  void testReconcileIndexedUsableTermNotFlagged() {
    User user = createUser();
    insertIndexedTerm(DOID_CANCER, OntologyType.DOID.name(), true, user.getUserId());
    insertDatasetWithDiseaseRestrictions(List.of(DOID_CANCER));
    insertDarWithOntologies(List.of(DOID_CANCER));

    List<OntologyReconciliationResult> results = ontologyDAO.findReferencedTermsMissingFromIndex();

    assertTrue(results.isEmpty());
  }

  @Test
  void testReconcileMatchingIsCaseInsensitive() {
    User user = createUser();
    // Indexed term is upper-cased; the referenced term is lower-cased. They should still match,
    // mirroring the normalization in findByTermIds, so the term is not flagged.
    insertIndexedTerm(DOID_CANCER.toUpperCase(), OntologyType.DOID.name(), true, user.getUserId());
    insertDatasetWithDiseaseRestrictions(List.of(DOID_CANCER.toLowerCase()));

    List<OntologyReconciliationResult> results = ontologyDAO.findReferencedTermsMissingFromIndex();

    assertTrue(results.isEmpty());
  }

  @Test
  void testReconcileAggregatesReferencesAcrossSources() {
    Dataset dataset = insertDatasetWithDiseaseRestrictions(List.of(DOID_MISSING));
    DataAccessRequest dar = insertDarWithOntologies(List.of(DOID_MISSING));

    List<OntologyReconciliationResult> results = ontologyDAO.findReferencedTermsMissingFromIndex();

    assertEquals(1, results.size());
    OntologyReconciliationResult result = findResult(results);
    assertEquals(2L, result.referenceCount());
    assertEquals(1L, result.datasetRefs());
    assertEquals(1L, result.darRefs());
    assertTrue(result.referencedBy().contains("DATASET:" + dataset.getDatasetId()));
    assertTrue(result.referencedBy().contains("DAR:" + dar.getReferenceId()));
  }

  @Test
  void testReconcileNoReferences() {
    assertTrue(ontologyDAO.findReferencedTermsMissingFromIndex().isEmpty());
  }
}
