package org.broadinstitute.consent.http.db;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.ws.rs.core.MediaType;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.broadinstitute.consent.http.enumeration.AuditActions;
import org.broadinstitute.consent.http.enumeration.FileCategory;
import org.broadinstitute.consent.http.models.DaaAudit;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DarDatasetDaaSnapshot;
import org.broadinstitute.consent.http.models.DataAccessAgreement;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.DataUseBuilder;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.DatasetDaaMapping;
import org.broadinstitute.consent.http.models.DatasetDaaSnapshotDetail;
import org.broadinstitute.consent.http.models.LibraryCard;
import org.broadinstitute.consent.http.models.User;
import org.junit.jupiter.api.Test;

class DaaDAOTest extends DAOTestHelper {

  private static final int NON_EXISTENT_ID = Integer.MAX_VALUE;

  private Integer createUserId() {
    return createUser().getUserId();
  }

  @Test
  void testInsert() {
    Integer userId = createUserId();
    Integer dacId =
        dacDAO.createDac(randomAlphabetic(5), randomAlphabetic(5), "", createUser().getUserId());
    Integer daaId = daaDAO.createDaa(userId, Instant.now(), userId, Instant.now(), dacId);
    assertNotNull(daaId);
    // Assert that CREATE audit records are created.
    List<DaaAudit> daaAudits = daaDAO.findAuditsByDaaId(daaId);
    assertNotNull(daaAudits);
    assertFalse(daaAudits.isEmpty());
    assertTrue(daaAudits.stream().anyMatch(a -> a.action().equals(AuditActions.CREATE)));
  }

  @Test
  void testInsertMultipleDaasOneDacId() {
    Integer userId = createUserId();
    Integer dacId =
        dacDAO.createDac(randomAlphabetic(5), randomAlphabetic(5), "", createUser().getUserId());
    Integer daaId1 = daaDAO.createDaa(userId, Instant.now(), userId, Instant.now(), dacId);
    Integer daaId2 = daaDAO.createDaa(userId, Instant.now(), userId, Instant.now(), dacId);
    Integer daaId3 = daaDAO.createDaa(userId, Instant.now(), userId, Instant.now(), dacId);
    assertNotNull(daaId1);
    assertNotNull(daaId2);
    assertNotNull(daaId3);
    DataAccessAgreement daa1 = daaDAO.findById(daaId1);
    DataAccessAgreement daa2 = daaDAO.findById(daaId2);
    DataAccessAgreement daa3 = daaDAO.findById(daaId3);
    assertEquals(daa1.getInitialDacId(), daa2.getInitialDacId());
    assertEquals(daa2.getInitialDacId(), daa3.getInitialDacId());
    assertEquals(daa1.getInitialDacId(), daa3.getInitialDacId());
  }

  @Test
  void testFindAllOneDaa() {
    Integer userId = createUserId();
    Integer dacId =
        dacDAO.createDac(randomAlphabetic(5), randomAlphabetic(5), "", createUser().getUserId());
    Integer daaId = daaDAO.createDaa(userId, Instant.now(), userId, Instant.now(), dacId);
    assertNotNull(daaId);
    List<DataAccessAgreement> daas = daaDAO.findAll();
    assertNotNull(daas);
    assertEquals(1, daas.size());
  }

  @Test
  void testFindAllMultipleDaas() {
    Integer userId = createUserId();
    Integer dacId =
        dacDAO.createDac(randomAlphabetic(5), randomAlphabetic(5), "", createUser().getUserId());
    Integer daaId1 = daaDAO.createDaa(userId, Instant.now(), userId, Instant.now(), dacId);
    Integer daaId2 = daaDAO.createDaa(userId, Instant.now(), userId, Instant.now(), dacId);
    Integer daaId3 = daaDAO.createDaa(userId, Instant.now(), userId, Instant.now(), dacId);
    assertNotNull(daaId1);
    assertNotNull(daaId2);
    assertNotNull(daaId3);
    List<DataAccessAgreement> daas = daaDAO.findAll();
    assertNotNull(daas);
    assertEquals(3, daas.size());
  }

  @Test
  void testFindAllNoDaas() {
    List<DataAccessAgreement> daas = daaDAO.findAll();
    assertNotNull(daas);
    assertEquals(0, daas.size());
  }

  @Test
  void testFindById() {
    Integer userId = createUserId();
    Integer dacId =
        dacDAO.createDac(randomAlphabetic(5), randomAlphabetic(5), "", createUser().getUserId());
    Integer daaId1 = daaDAO.createDaa(userId, Instant.now(), userId, Instant.now(), dacId);
    Integer daaId2 = daaDAO.createDaa(userId, Instant.now(), userId, Instant.now(), dacId);
    assertNotNull(daaId1);
    assertNotNull(daaId2);
    DataAccessAgreement daa1 = daaDAO.findById(daaId1);
    assertNotNull(daa1);
    assertEquals(daa1.getDaaId(), daaId1);
    DataAccessAgreement daa2 = daaDAO.findById(daaId2);
    assertNotNull(daa2);
    assertEquals(daa2.getDaaId(), daaId2);
  }

  @Test
  void testFindByIdInvalid() {
    DataAccessAgreement daa3 = daaDAO.findById(NON_EXISTENT_ID);
    assertNull(daa3);
  }

  @Test
  void testFindByDacId() {
    Integer userId = createUserId();
    Integer dacId =
        dacDAO.createDac(randomAlphabetic(5), randomAlphabetic(5), "", createUser().getUserId());
    Integer dacId2 =
        dacDAO.createDac(randomAlphabetic(5), randomAlphabetic(5), "", createUser().getUserId());
    Integer daaId1 = daaDAO.createDaa(userId, Instant.now(), userId, Instant.now(), dacId);
    daaDAO.createDaa(userId, Instant.now(), userId, Instant.now(), dacId2);
    assertNotNull(daaId1);
    DataAccessAgreement daa1 = daaDAO.findByDacId(dacId);
    assertNotNull(daa1);
    assertEquals(daa1.getInitialDacId(), dacId);
    DataAccessAgreement daa2 = daaDAO.findByDacId(dacId2);
    assertNotNull(daa2);
    assertEquals(daa2.getInitialDacId(), dacId2);
    DataAccessAgreement daa3 = daaDAO.findByDacId(NON_EXISTENT_ID);
    assertNull(daa3);
  }

  @Test
  void testCreateDaaDacRelation() {
    Integer userId = createUserId();
    Integer dacId =
        dacDAO.createDac(randomAlphabetic(5), randomAlphabetic(5), "", createUser().getUserId());
    Integer dacId2 =
        dacDAO.createDac(randomAlphabetic(5), randomAlphabetic(5), "", createUser().getUserId());
    Integer dacId3 =
        dacDAO.createDac(randomAlphabetic(5), randomAlphabetic(5), "", createUser().getUserId());
    Integer daaId1 = daaDAO.createDaa(userId, Instant.now(), userId, Instant.now(), dacId);
    daaDAO.createDaa(userId, Instant.now(), userId, Instant.now(), dacId2);
    assertNotNull(daaId1);
    DataAccessAgreement daa1 = daaDAO.findByDacId(dacId);
    assertNotNull(daa1);
    assertEquals(daa1.getInitialDacId(), dacId);
    DataAccessAgreement daa2 = daaDAO.findByDacId(dacId2);
    assertNotNull(daa2);
    assertEquals(daa2.getInitialDacId(), dacId2);

    daaDAO.createDacDaaRelation(dacId, daa1.getDaaId(), userId);
    daaDAO.createDacDaaRelation(dacId2, daa2.getDaaId(), userId);
    daaDAO.createDacDaaRelation(dacId3, daa2.getDaaId(), userId);

    // Assert new relations exist
    DataAccessAgreement testDAA1 = daaDAO.findById(daa1.getDaaId());
    assertTrue(dacIdIn(testDAA1, dacId));
    assertFalse(dacIdIn(testDAA1, dacId2));
    assertFalse(dacIdIn(testDAA1, dacId3));
    DataAccessAgreement testDAA2 = daaDAO.findById(daa2.getDaaId());
    assertFalse(dacIdIn(testDAA2, dacId));
    assertTrue(dacIdIn(testDAA2, dacId2));
    assertTrue(dacIdIn(testDAA2, dacId3));

    // Assert that ADD audit records are created.
    List<DaaAudit> daa1Audits = daaDAO.findAuditsByDaaId(daa1.getDaaId());
    assertNotNull(daa1Audits);
    assertFalse(daa1Audits.isEmpty());
    assertTrue(daa1Audits.stream().anyMatch(a -> a.action().equals(AuditActions.ADD)));

    List<DaaAudit> daa2Audits = daaDAO.findAuditsByDaaId(daa2.getDaaId());
    assertNotNull(daa2Audits);
    assertFalse(daa2Audits.isEmpty());
    assertTrue(daa2Audits.stream().anyMatch(a -> a.action().equals(AuditActions.ADD)));
  }

  @Test
  void testDeleteDaaDacRelation() {
    Integer userId = createUserId();
    Integer dacId1 =
        dacDAO.createDac(randomAlphabetic(5), randomAlphabetic(5), "", createUser().getUserId());
    Integer dacId2 =
        dacDAO.createDac(randomAlphabetic(5), randomAlphabetic(5), "", createUser().getUserId());
    Integer dacId3 =
        dacDAO.createDac(randomAlphabetic(5), randomAlphabetic(5), "", createUser().getUserId());

    Integer daaId1 = daaDAO.createDaa(userId, Instant.now(), userId, Instant.now(), dacId1);
    assertNotNull(daaId1);
    Integer daaId2 = daaDAO.createDaa(userId, Instant.now(), userId, Instant.now(), dacId2);
    assertNotNull(daaId2);
    DataAccessAgreement daa1 = daaDAO.findById(daaId1);
    assertNotNull(daa1);
    assertEquals(daa1.getInitialDacId(), dacId1);
    DataAccessAgreement daa2 = daaDAO.findById(daaId2);
    assertNotNull(daa2);
    assertEquals(daa2.getInitialDacId(), dacId2);

    daaDAO.createDacDaaRelation(dacId1, daaId1, userId);
    daaDAO.createDacDaaRelation(dacId2, daaId2, userId);
    daaDAO.createDacDaaRelation(dacId3, daaId2, userId);

    // Delete DAC-DAA Relations
    daaDAO.deleteDacDaaRelation(daaId1, dacId1, userId);
    daaDAO.deleteDacDaaRelation(daaId2, dacId2, userId);

    // Assert that only 2 of the 3 DAC-DAA relations are removed
    DataAccessAgreement testDAA1 = daaDAO.findById(daa1.getDaaId());
    // DAA 1 should not have any DACs
    assertNotNull(testDAA1);
    assertNull(testDAA1.getDacs());
    // DAA 2 should have 1 DAC
    DataAccessAgreement testDAA2 = daaDAO.findById(daa2.getDaaId());
    assertNotNull(testDAA2);
    assertNotNull(testDAA2.getDacs());
    assertFalse(testDAA2.getDacs().isEmpty());
    assertFalse(testDAA2.getDacs().stream().anyMatch(dac -> dac.getDacId().equals(dacId1)));
    assertFalse(testDAA2.getDacs().stream().anyMatch(dac -> dac.getDacId().equals(dacId2)));
    assertTrue(testDAA2.getDacs().stream().anyMatch(dac -> dac.getDacId().equals(dacId3)));

    // Assert that REMOVE audit records are created.
    List<DaaAudit> daa1Audits = daaDAO.findAuditsByDaaId(daaId1);
    assertNotNull(daa1Audits);
    assertFalse(daa1Audits.isEmpty());
    assertTrue(daa1Audits.stream().anyMatch(a -> a.action().equals(AuditActions.REMOVE)));

    List<DaaAudit> daa2Audits = daaDAO.findAuditsByDaaId(daaId2);
    assertNotNull(daa2Audits);

    assertFalse(daa2Audits.isEmpty());
    assertTrue(daa2Audits.stream().anyMatch(a -> a.action().equals(AuditActions.REMOVE)));
  }

  @Test
  void testFindWithFileStorageObject() {
    Integer userId = createUserId();
    Integer dacId =
        dacDAO.createDac(randomAlphabetic(5), randomAlphabetic(5), "", createUser().getUserId());
    Integer daaId = daaDAO.createDaa(userId, Instant.now(), userId, Instant.now(), dacId);
    Integer fsoId =
        fileStorageObjectDAO.insertNewFile(
            randomAlphabetic(10),
            FileCategory.DATA_ACCESS_AGREEMENT.getValue(),
            randomAlphabetic(10),
            MediaType.TEXT_PLAIN_TYPE.getType(),
            daaId.toString(),
            userId,
            Instant.now());
    DataAccessAgreement daa = daaDAO.findById(daaId);
    assertNotNull(daa);
    assertNotNull(daa.getFile());
    assertEquals(fsoId, daa.getFile().getFileStorageObjectId());
  }

  @Test
  void testFindWithDacs() {
    Integer userId = createUser().getUserId();
    Integer dacId =
        dacDAO.createDac(
            randomAlphabetic(5), "Dac 1", randomAlphabetic(15), createUser().getUserId());
    Integer dacId2 =
        dacDAO.createDac(
            randomAlphabetic(5), "Dac 2", randomAlphabetic(15), createUser().getUserId());
    Integer dacId3 =
        dacDAO.createDac(
            randomAlphabetic(5), "Dac 3", randomAlphabetic(15), createUser().getUserId());
    Integer daaId = daaDAO.createDaa(userId, Instant.now(), userId, Instant.now(), dacId);
    daaDAO.createDacDaaRelation(dacId, daaId, userId);
    daaDAO.createDacDaaRelation(dacId2, daaId, userId);
    daaDAO.createDacDaaRelation(dacId3, daaId, userId);
    DataAccessAgreement daa = daaDAO.findById(daaId);

    assertNotNull(daa);
    assertNotNull(daa.getDacs());
    assertEquals(3, daa.getDacs().size());
  }

  @Test
  void testFindWithDacs_excludesSoftDeletedDac() {
    Integer userId = createUser().getUserId();
    Integer dacId1 =
        dacDAO.createDac(
            randomAlphabetic(5), "Dac 1", randomAlphabetic(15), createUser().getUserId());
    Integer dacId2 =
        dacDAO.createDac(
            randomAlphabetic(5), "Dac 2", randomAlphabetic(15), createUser().getUserId());
    Integer dacId3 =
        dacDAO.createDac(
            randomAlphabetic(5), "Dac 3", randomAlphabetic(15), createUser().getUserId());
    Integer daaId = daaDAO.createDaa(userId, Instant.now(), userId, Instant.now(), dacId1);
    daaDAO.createDacDaaRelation(dacId1, daaId, userId);
    daaDAO.createDacDaaRelation(dacId2, daaId, userId);
    daaDAO.createDacDaaRelation(dacId3, daaId, userId);

    // Baseline: all 3 DACs appear
    DataAccessAgreement daaBeforeDelete = daaDAO.findById(daaId);
    assertNotNull(daaBeforeDelete.getDacs());
    assertEquals(3, daaBeforeDelete.getDacs().size());

    // Soft-delete one DAC
    dacDAO.deleteDac(dacId3, userId);

    // Only the 2 non-deleted DACs should appear in the dacs list
    DataAccessAgreement daaAfterDelete = daaDAO.findById(daaId);
    assertNotNull(daaAfterDelete);
    assertNotNull(daaAfterDelete.getDacs());
    assertEquals(2, daaAfterDelete.getDacs().size());
    List<Integer> remainingDacIds = daaAfterDelete.getDacs().stream().map(Dac::getDacId).toList();
    assertTrue(remainingDacIds.contains(dacId1));
    assertTrue(remainingDacIds.contains(dacId2));
    assertFalse(remainingDacIds.contains(dacId3));
  }

  @Test
  void testFindById_excludesSoftDeletedDac() {
    Integer userId = createUser().getUserId();
    Integer dacId = dacDAO.createDac(randomAlphabetic(5), randomAlphabetic(5), "", userId);
    Integer daaId = daaDAO.createDaa(userId, Instant.now(), userId, Instant.now(), dacId);
    daaDAO.createDacDaaRelation(dacId, daaId, userId);

    // Before soft-deletion the DAC is visible in the DAA
    DataAccessAgreement daaBefore = daaDAO.findById(daaId);
    assertNotNull(daaBefore);
    assertNotNull(daaBefore.getDacs());
    assertFalse(daaBefore.getDacs().isEmpty());
    assertTrue(dacIdIn(daaBefore, dacId));

    // Soft-delete the DAC
    dacDAO.deleteDac(dacId, userId);

    // The soft-deleted DAC must not appear in the DAA's dacs list
    DataAccessAgreement daaAfter = daaDAO.findById(daaId);
    assertNotNull(daaAfter);
    assertNull(daaAfter.getDacs());
  }

  @Test
  void testFindAll_excludesSoftDeletedDac() {
    Integer userId = createUser().getUserId();
    Integer dacId1 = dacDAO.createDac(randomAlphabetic(5), randomAlphabetic(5), "", userId);
    Integer dacId2 = dacDAO.createDac(randomAlphabetic(5), randomAlphabetic(5), "", userId);
    Integer daaId = daaDAO.createDaa(userId, Instant.now(), userId, Instant.now(), dacId1);
    daaDAO.createDacDaaRelation(dacId1, daaId, userId);
    daaDAO.createDacDaaRelation(dacId2, daaId, userId);

    // Before deletion: both DACs appear in the DAA's dacs list
    List<DataAccessAgreement> daasBeforeDelete = daaDAO.findAll();
    assertEquals(1, daasBeforeDelete.size());
    assertNotNull(daasBeforeDelete.getFirst().getDacs());
    assertEquals(2, daasBeforeDelete.getFirst().getDacs().size());

    // Soft-delete one DAC
    dacDAO.deleteDac(dacId2, userId);

    // After deletion: only the non-deleted DAC appears in the DAA's dacs list
    List<DataAccessAgreement> daasAfterDelete = daaDAO.findAll();
    assertEquals(1, daasAfterDelete.size());
    DataAccessAgreement daaAfter = daasAfterDelete.getFirst();
    assertNotNull(daaAfter.getDacs());
    assertEquals(1, daaAfter.getDacs().size());
    assertTrue(dacIdIn(daaAfter, dacId1));
    assertFalse(dacIdIn(daaAfter, dacId2));
  }

  @Test
  void testFindDaaDatasetIdsByUserId() {
    // Testing the case of a user requesting DAR access to a dataset.
    // That user must have an LC with a DAA associated to the same DAC that the dataset is
    // associated to.
    User signingOfficial = createUser();
    User user = createUserWithInstitution();
    LibraryCard lc = createRandomLibraryCard(user);
    Dac dac1 = createRandomDac();
    Dac dac2 = createRandomDac();
    DataAccessAgreement daa = createRandomDataAccessAgreement(user, dac1);
    // Associate the DAC to the Data Access Agreeement:
    daaDAO.createDacDaaRelation(dac1.getDacId(), daa.getDaaId(), user.getUserId());
    // Associate the user's Library Card to the Data Access Agreement:
    libraryCardDAO.createLibraryCardDaaRelation(
        lc.getUserId(), signingOfficial.getUserId(), lc.getId(), daa.getDaaId());
    // Create two datasets associated to the DAC and DAA
    Dataset dataset1 = createRandomDataset(user, dac1);
    Dataset dataset2 = createRandomDataset(user, dac1);
    // Create a third dataset that should not be returned
    Dataset dataset3 = createRandomDataset(user, dac2);

    List<Integer> datasetIds = daaDAO.findDaaDatasetIdsByUserId(user.getUserId());
    assertFalse(datasetIds.isEmpty());
    assertEquals(2, datasetIds.size());
    assertTrue(datasetIds.contains(dataset1.getDatasetId()));
    assertTrue(datasetIds.contains(dataset2.getDatasetId()));
    assertFalse(datasetIds.contains(dataset3.getDatasetId()));
  }

  @Test
  void testFindDaaDatasetIdsByUserIdNullUser() {
    List<Integer> datasetIds = daaDAO.findDaaDatasetIdsByUserId(null);
    assertTrue(datasetIds.isEmpty());
  }

  @Test
  void testFindByDarReferenceId() {
    // This test requires a good deal of model setup: DAR, Dataset, DAC, and DataAccessAgreements
    // We'll create a single DAR with 2 datasets, each one in a separate DAC with separate DAAs
    // and a third dac/dataset/daa that should not be found.
    Integer dataSubmitterId = createUserId();
    User dataSubmitter = userDAO.findUserById(dataSubmitterId);

    // DAC/Dataset/DAA 1
    Integer dac1Id =
        dacDAO.createDac(randomAlphabetic(5), randomAlphabetic(5), "", createUser().getUserId());
    Dac dac1 = dacDAO.findById(dac1Id);
    Integer daaId1 =
        daaDAO.createDaa(dataSubmitterId, Instant.now(), dataSubmitterId, Instant.now(), dac1Id);
    daaDAO.createDacDaaRelation(dac1Id, daaId1, dataSubmitterId);
    DataAccessAgreement daa1 = daaDAO.findById(daaId1);
    Dataset d1 = createRandomDataset(dataSubmitter, dac1);

    // Dac/Dataset/DAA 2
    Integer dacId2 =
        dacDAO.createDac(randomAlphabetic(5), randomAlphabetic(5), "", createUser().getUserId());
    Dac dac2 = dacDAO.findById(dacId2);
    Integer daaId2 =
        daaDAO.createDaa(dataSubmitterId, Instant.now(), dataSubmitterId, Instant.now(), dacId2);
    daaDAO.createDacDaaRelation(dacId2, daaId2, dataSubmitterId);
    DataAccessAgreement daa2 = daaDAO.findById(daaId2);
    Dataset d2 = createRandomDataset(dataSubmitter, dac2);

    // Dac/Dataset/DAA 3 which should not be in the returned results
    Integer dacId3 =
        dacDAO.createDac(randomAlphabetic(5), randomAlphabetic(5), "", createUser().getUserId());
    Dac dac3 = dacDAO.findById(dacId3);
    Integer daaId3 =
        daaDAO.createDaa(dataSubmitterId, Instant.now(), dataSubmitterId, Instant.now(), dacId3);
    daaDAO.createDacDaaRelation(dacId3, daaId3, dataSubmitterId);
    DataAccessAgreement daa3 = daaDAO.findById(daaId3);
    createRandomDataset(dataSubmitter, dac3);

    // DAR and associated datasets
    DataAccessRequest dar = createDataAccessRequestV3();
    dataAccessRequestDAO.insertDARDatasetRelation(dar.getReferenceId(), d1.getDatasetId());
    dataAccessRequestDAO.insertDARDatasetRelation(dar.getReferenceId(), d2.getDatasetId());

    List<DataAccessAgreement> daas = daaDAO.findByDarReferenceId(dar.getReferenceId());
    assertFalse(daas.isEmpty());
    assertEquals(2, daas.size());
    List<Integer> daaIds = daas.stream().map(DataAccessAgreement::getDaaId).toList();
    assertTrue(daaIds.contains(daa1.getDaaId()));
    assertTrue(daaIds.contains(daa2.getDaaId()));
    assertFalse(daaIds.contains(daa3.getDaaId()));
  }

  @Test
  void testMapDaaIdsToDatasetIds() {
    Integer userId = createUserId();
    Integer dacId =
        dacDAO.createDac(randomAlphabetic(5), randomAlphabetic(5), "", createUser().getUserId());
    Dataset dataset1 = createRandomDataset(userDAO.findUserById(userId), dacDAO.findById(dacId));
    Dataset dataset2 = createRandomDataset(userDAO.findUserById(userId), dacDAO.findById(dacId));

    // Ensure that the DAA is associated to the DAC
    Integer daaId = daaDAO.createDaa(userId, Instant.now(), userId, Instant.now(), dacId);
    daaDAO.createDacDaaRelation(dacId, daaId, userId);

    Set<Integer> daaIds =
        daaDAO.findDaaIdsByDatasetIds(List.of(dataset1.getDatasetId(), dataset2.getDatasetId()));
    assertFalse(daaIds.isEmpty());
    assertEquals(1, daaIds.size());
    assertTrue(daaIds.contains(daaId));
  }

  @Test
  void testFindDaaIdsWithDatasetIds() {
    Integer userId = createUserId();
    Integer dacId1 = dacDAO.createDac("dac1", randomAlphabetic(5), "", userId);
    Integer dacId2 = dacDAO.createDac("dac2", randomAlphabetic(5), "", userId);
    Dataset dataset1 = createRandomDataset(userDAO.findUserById(userId), dacDAO.findById(dacId1));
    Dataset dataset2 = createRandomDataset(userDAO.findUserById(userId), dacDAO.findById(dacId2));
    Dataset dataset3 = createRandomDataset(userDAO.findUserById(userId), dacDAO.findById(dacId2));

    Integer daa1 = daaDAO.createDaa(userId, Instant.now(), userId, Instant.now(), dacId1);
    Integer daa2 = daaDAO.createDaa(userId, Instant.now(), userId, Instant.now(), dacId2);

    daaDAO.createDacDaaRelation(dacId1, daa1, userId);
    daaDAO.createDacDaaRelation(dacId2, daa2, userId);

    Map<Integer, Set<Integer>> daaDatasetMap =
        daaDAO.mapDaaIdsToDatasetIds(
            Set.of(dataset1.getDatasetId(), dataset2.getDatasetId(), dataset3.getDatasetId()));
    assertFalse(daaDatasetMap.isEmpty());
    assertEquals(2, daaDatasetMap.size());

    Set<Integer> daa1Datasets = daaDatasetMap.get(daa1);
    assertEquals(1, daa1Datasets.size());
    assertTrue(daa1Datasets.contains(dataset1.getDatasetId()));

    Set<Integer> daa2Datasets = daaDatasetMap.get(daa2);
    assertEquals(2, daa2Datasets.size());
    assertTrue(daa2Datasets.contains(dataset2.getDatasetId()));
    assertTrue(daa2Datasets.contains(dataset3.getDatasetId()));
  }

  @Test
  void testFindDaaIdsWithDatasetIds_No_IDs() {
    Map<Integer, Set<Integer>> daaDatasetMap = daaDAO.mapDaaIdsToDatasetIds(Set.of());
    assertTrue(daaDatasetMap.isEmpty());
  }

  @Test
  void testFindDaaIdsWithDatasetIds_No_Data() {
    Map<Integer, Set<Integer>> daaDatasetMap =
        daaDAO.mapDaaIdsToDatasetIds(
            Set.of(NON_EXISTENT_ID - 2, NON_EXISTENT_ID - 1, NON_EXISTENT_ID));
    assertTrue(daaDatasetMap.isEmpty());
  }

  @Test
  void testStoreDarDAARelationshipForDar() {
    Integer userId = createUserId();
    Integer dacId1 = dacDAO.createDac("dac1", randomAlphabetic(5), "", userId);
    Integer dacId2 = dacDAO.createDac("dac2", randomAlphabetic(5), "", userId);

    Integer daa1 = daaDAO.createDaa(userId, Instant.now(), userId, Instant.now(), dacId1);
    Integer daa2 = daaDAO.createDaa(userId, Instant.now(), userId, Instant.now(), dacId2);

    daaDAO.createDacDaaRelation(dacId1, daa1, userId);
    daaDAO.createDacDaaRelation(dacId2, daa2, userId);
    Date now = new Date();
    Integer darCollectionId = darCollectionDAO.insertDarCollection("ABC", userId, now);
    Integer darId =
        dataAccessRequestDAO.insertDataAccessRequest(
            darCollectionId,
            UUID.randomUUID().toString(),
            userId,
            now,
            now,
            now,
            new DataAccessRequestData(),
            "`");
    assertDoesNotThrow(() -> daaDAO.insertDarDAARelationship(darId, Set.of(daa1, daa2)));
  }

  @Test
  void testStoreDarDAARelationshipForPR() {
    Integer userId = createUserId();
    Integer dacId1 = dacDAO.createDac("dac1", randomAlphabetic(5), "", userId);
    Integer dacId2 = dacDAO.createDac("dac2", randomAlphabetic(5), "", userId);

    Integer daa1 = daaDAO.createDaa(userId, Instant.now(), userId, Instant.now(), dacId1);
    Integer daa2 = daaDAO.createDaa(userId, Instant.now(), userId, Instant.now(), dacId2);

    daaDAO.createDacDaaRelation(dacId1, daa1, userId);
    daaDAO.createDacDaaRelation(dacId2, daa2, userId);
    Date now = new Date();
    Integer darCollectionId = darCollectionDAO.insertDarCollection("ABC", userId, now);
    String darUUID = UUID.randomUUID().toString();
    Integer darId =
        dataAccessRequestDAO.insertDataAccessRequest(
            darCollectionId, darUUID, userId, now, now, now, new DataAccessRequestData(), "`");
    Integer prId =
        dataAccessRequestDAO.insertProgressReport(
            darId,
            darCollectionId,
            UUID.randomUUID().toString(),
            userId,
            new DataAccessRequestData(),
            "");
    assertDoesNotThrow(() -> daaDAO.insertDarDAARelationship(prId, Set.of(daa1, daa2)));
    assertEquals(2, getDarDaaCount(prId));

    daaDAO.deleteDarDAARelationship(prId);
    assertEquals(0, getDarDaaCount(prId));
  }

  @Test
  void testFindCurrentDaaMappingsByDatasetIds() {
    Integer userId = createUserId();
    Integer dacId1 = dacDAO.createDac("dac1", randomAlphabetic(5), "", userId);
    Integer dacId2 = dacDAO.createDac("dac2", randomAlphabetic(5), "", userId);
    Dataset dataset1 = createRandomDataset(userDAO.findUserById(userId), dacDAO.findById(dacId1));
    Dataset dataset2 = createRandomDataset(userDAO.findUserById(userId), dacDAO.findById(dacId2));

    Integer daa1 = daaDAO.createDaa(userId, Instant.now(), userId, Instant.now(), dacId1);
    Integer daa2 = daaDAO.createDaa(userId, Instant.now(), userId, Instant.now(), dacId2);
    daaDAO.createDacDaaRelation(dacId1, daa1, userId);
    daaDAO.createDacDaaRelation(dacId2, daa2, userId);

    List<DatasetDaaMapping> mappings =
        daaDAO.findCurrentDaaMappingsByDatasetIds(
            List.of(dataset1.getDatasetId(), dataset2.getDatasetId()));

    assertEquals(2, mappings.size());
    assertTrue(
        mappings.stream()
            .anyMatch(
                mapping ->
                    mapping.datasetId().equals(dataset1.getDatasetId())
                        && mapping.daaId().equals(daa1)));
    assertTrue(
        mappings.stream()
            .anyMatch(
                mapping ->
                    mapping.datasetId().equals(dataset2.getDatasetId())
                        && mapping.daaId().equals(daa2)));
  }

  @Test
  void testFindDatasetDaaSnapshotsByReferenceId() {
    Integer userId = createUserId();
    Integer dacId1 = dacDAO.createDac("dac1", randomAlphabetic(5), "", userId);
    Integer dacId2 = dacDAO.createDac("dac2", randomAlphabetic(5), "", userId);
    Dataset dataset1 = createRandomDataset(userDAO.findUserById(userId), dacDAO.findById(dacId1));
    Dataset dataset2 = createRandomDataset(userDAO.findUserById(userId), dacDAO.findById(dacId2));
    Integer daa1 = daaDAO.createDaa(userId, Instant.now(), userId, Instant.now(), dacId1);
    Integer daa2 = daaDAO.createDaa(userId, Instant.now(), userId, Instant.now(), dacId2);
    Date now = new Date();
    Integer darCollectionId = darCollectionDAO.insertDarCollection("ABC", userId, now);
    String referenceId = UUID.randomUUID().toString();
    Integer darId =
        dataAccessRequestDAO.insertDataAccessRequest(
            darCollectionId,
            referenceId,
            userId,
            now,
            now,
            now,
            new DataAccessRequestData(),
            "eraCommonsId");
    Timestamp capturedAt = Timestamp.from(Instant.now());

    daaDAO.insertDarDatasetDaaSnapshots(
        List.of(
            new DarDatasetDaaSnapshot(darId, dataset1.getDatasetId(), daa1, capturedAt),
            new DarDatasetDaaSnapshot(darId, dataset2.getDatasetId(), daa2, capturedAt)));

    List<DatasetDaaSnapshotDetail> snapshots =
        daaDAO.findDatasetDaaSnapshotsByReferenceId(referenceId);

    assertEquals(2, snapshots.size());
    assertTrue(
        snapshots.stream()
            .anyMatch(
                snapshot ->
                    snapshot.datasetId().equals(dataset1.getDatasetId())
                        && snapshot.daaId().equals(daa1)
                        && snapshot.capturedAt().equals(capturedAt)));
    assertTrue(
        snapshots.stream()
            .anyMatch(
                snapshot ->
                    snapshot.datasetId().equals(dataset2.getDatasetId())
                        && snapshot.daaId().equals(daa2)
                        && snapshot.capturedAt().equals(capturedAt)));
  }

  @Test
  void testFindDatasetDaaSnapshotsByProgressReportReferenceId() {
    Integer userId = createUserId();
    Integer dacId = dacDAO.createDac("dac", randomAlphabetic(5), "", userId);
    Dataset dataset = createRandomDataset(userDAO.findUserById(userId), dacDAO.findById(dacId));
    Integer daaId = daaDAO.createDaa(userId, Instant.now(), userId, Instant.now(), dacId);
    Date now = new Date();
    Integer darCollectionId = darCollectionDAO.insertDarCollection("ABC", userId, now);
    Integer parentDarId =
        dataAccessRequestDAO.insertDataAccessRequest(
            darCollectionId,
            UUID.randomUUID().toString(),
            userId,
            now,
            now,
            now,
            new DataAccessRequestData(),
            "eraCommonsId");
    String progressReportReferenceId = UUID.randomUUID().toString();
    Integer progressReportId =
        dataAccessRequestDAO.insertProgressReport(
            parentDarId,
            darCollectionId,
            progressReportReferenceId,
            userId,
            new DataAccessRequestData(),
            "eraCommonsId");
    Timestamp capturedAt = Timestamp.from(Instant.now());

    daaDAO.insertDarDatasetDaaSnapshots(
        List.of(
            new DarDatasetDaaSnapshot(
                progressReportId, dataset.getDatasetId(), daaId, capturedAt)));

    List<DatasetDaaSnapshotDetail> snapshots =
        daaDAO.findDatasetDaaSnapshotsByReferenceId(progressReportReferenceId);

    assertEquals(1, snapshots.size());
    assertEquals(dataset.getDatasetId(), snapshots.getFirst().datasetId());
    assertEquals(daaId, snapshots.getFirst().daaId());
    assertEquals(capturedAt, snapshots.getFirst().capturedAt());
  }

  private static Integer getDarDaaCount(Integer prId) {
    return jdbi.withHandle(
        handle ->
            handle
                .createQuery("SELECT count(*) FROM dar_daa WHERE dar_id = :prId")
                .bind("prId", prId)
                .mapTo(Integer.class)
                .one());
  }

  @Test
  void testMapDaaIdsToDatasetIds_dacNotAssociated() {
    Integer userId = createUserId();
    Integer dacId =
        dacDAO.createDac(randomAlphabetic(5), randomAlphabetic(5), "", createUser().getUserId());
    Dataset dataset1 = createRandomDataset(userDAO.findUserById(userId), dacDAO.findById(dacId));
    Dataset dataset2 = createRandomDataset(userDAO.findUserById(userId), dacDAO.findById(dacId));

    // Create a DAA but do not associate it to the DAC
    daaDAO.createDaa(userId, Instant.now(), userId, Instant.now(), dacId);

    Set<Integer> daaIds =
        daaDAO.findDaaIdsByDatasetIds(List.of(dataset1.getDatasetId(), dataset2.getDatasetId()));
    assertTrue(daaIds.isEmpty());
  }

  @Test
  void testMapDaaIdsToDatasetIds_datasetsNotAssociated() {
    Integer userId = createUserId();
    Integer dacId =
        dacDAO.createDac(randomAlphabetic(5), randomAlphabetic(5), "", createUser().getUserId());
    Dataset dataset1 = createRandomDataset(userDAO.findUserById(userId), dacDAO.findById(dacId));
    Dataset dataset2 = createRandomDataset(userDAO.findUserById(userId), dacDAO.findById(dacId));

    // Create a DAA that is associated to a different DAC than the datasets are
    Integer otherDacId =
        dacDAO.createDac(randomAlphabetic(5), randomAlphabetic(5), "", createUser().getUserId());
    Integer daaId = daaDAO.createDaa(userId, Instant.now(), userId, Instant.now(), otherDacId);
    daaDAO.createDacDaaRelation(otherDacId, daaId, userId);

    Set<Integer> daaIds =
        daaDAO.findDaaIdsByDatasetIds(List.of(dataset1.getDatasetId(), dataset2.getDatasetId()));
    assertTrue(daaIds.isEmpty());
  }

  private LibraryCard createRandomLibraryCard(User user) {
    int lcId =
        libraryCardDAO.insertLibraryCard(
            user.getUserId(),
            randomAlphabetic(5),
            randomAlphabetic(5),
            user.getUserId(),
            new Date());
    return libraryCardDAO.findLibraryCardById(lcId);
  }

  private Dac createRandomDac() {
    int dacId =
        dacDAO.createDac(randomAlphabetic(5), randomAlphabetic(5), createUser().getUserId());
    return dacDAO.findById(dacId);
  }

  private DataAccessAgreement createRandomDataAccessAgreement(User user, Dac dac) {
    int daaId =
        daaDAO.createDaa(
            user.getUserId(), Instant.now(), user.getUserId(), Instant.now(), dac.getDacId());
    return daaDAO.findById(daaId);
  }

  private Dataset createRandomDataset(User user, Dac dac) {
    int datasetId =
        datasetDAO.insertDataset(
            randomAlphabetic(5),
            new Timestamp(Instant.now().getEpochSecond()),
            user.getUserId(),
            null,
            new DataUseBuilder().setGeneralUse(true).build().toString(),
            dac.getDacId());
    return datasetDAO.findDatasetById(datasetId);
  }

  /** Returns true when {@code daa}'s dacs list contains the given dacId. */
  private static boolean dacIdIn(DataAccessAgreement daa, Integer dacId) {
    return daa.getDacs().stream().map(Dac::getDacId).toList().contains(dacId);
  }

  @Test
  void testFindAllDaaAudits() {
    Integer userId = createUserId();
    Integer dacId1 = dacDAO.createDac(randomAlphabetic(5), randomAlphabetic(5), "", userId);
    Integer dacId2 = dacDAO.createDac(randomAlphabetic(5), randomAlphabetic(5), "", userId);
    Integer daaId1 = daaDAO.createDaa(userId, Instant.now(), userId, Instant.now(), dacId1);
    Integer daaId2 = daaDAO.createDaa(userId, Instant.now(), userId, Instant.now(), dacId2);
    daaDAO.createDacDaaRelation(dacId1, daaId1, userId);
    daaDAO.createDacDaaRelation(dacId2, daaId2, userId);

    List<DaaAudit> allAudits = daaDAO.findAllDaaAudits();
    assertNotNull(allAudits);
    assertFalse(allAudits.isEmpty());
    // CREATE audit for each DAA + ADD audit for each relation = at least 4 records
    assertTrue(allAudits.size() >= 4);
    assertTrue(allAudits.stream().anyMatch(a -> a.action().equals(AuditActions.CREATE)));
    assertTrue(allAudits.stream().anyMatch(a -> a.action().equals(AuditActions.ADD)));
    List<Integer> auditDaaIds = allAudits.stream().map(DaaAudit::daaId).toList();
    assertTrue(auditDaaIds.contains(daaId1));
    assertTrue(auditDaaIds.contains(daaId2));
  }

  @Test
  void testCreateDacDaaRelation_ReplaceExistingRelation() {
    Integer userId = createUserId();
    Integer dacId = dacDAO.createDac(randomAlphabetic(5), randomAlphabetic(5), "", userId);
    Integer daaId1 = daaDAO.createDaa(userId, Instant.now(), userId, Instant.now(), dacId);
    Integer daaId2 = daaDAO.createDaa(userId, Instant.now(), userId, Instant.now(), dacId);

    // Create initial relation DAC -> DAA1
    daaDAO.createDacDaaRelation(dacId, daaId1, userId);
    DataAccessAgreement daa1FirstCreate = daaDAO.findById(daaId1);
    assertTrue(dacIdIn(daa1FirstCreate, dacId));

    // Replace with DAC -> DAA2 (DAC to different DAA)
    daaDAO.createDacDaaRelation(dacId, daaId2, userId);

    // Verify DAA1 no longer has DAC association
    DataAccessAgreement daa1AfterReplace = daaDAO.findById(daaId1);
    assertNull(daa1AfterReplace.getDacs());

    // Verify DAA2 now has DAC association
    DataAccessAgreement daa2AfterReplace = daaDAO.findById(daaId2);
    assertTrue(dacIdIn(daa2AfterReplace, dacId));

    // Verify audit records for both REMOVE and ADD are created
    List<DaaAudit> audits1 = daaDAO.findAuditsByDaaId(daaId1);
    assertTrue(audits1.stream().anyMatch(a -> a.action().equals(AuditActions.REMOVE)));

    List<DaaAudit> audits2 = daaDAO.findAuditsByDaaId(daaId2);
    assertTrue(audits2.stream().anyMatch(a -> a.action().equals(AuditActions.ADD)));
  }

  @Test
  void testCreateDacDaaRelation_MultipleReplacements() {
    Integer userId = createUserId();
    Integer dacId = dacDAO.createDac(randomAlphabetic(5), randomAlphabetic(5), "", userId);
    Integer daaId1 = daaDAO.createDaa(userId, Instant.now(), userId, Instant.now(), dacId);
    Integer daaId2 = daaDAO.createDaa(userId, Instant.now(), userId, Instant.now(), dacId);
    Integer daaId3 = daaDAO.createDaa(userId, Instant.now(), userId, Instant.now(), dacId);

    // First assignment: DAC -> DAA1
    daaDAO.createDacDaaRelation(dacId, daaId1, userId);
    DataAccessAgreement daa1After1st = daaDAO.findById(daaId1);
    assertTrue(dacIdIn(daa1After1st, dacId));

    // Second assignment: DAC -> DAA2 (replaces DAA1)
    daaDAO.createDacDaaRelation(dacId, daaId2, userId);
    DataAccessAgreement daa1After2nd = daaDAO.findById(daaId1);
    assertNull(daa1After2nd.getDacs());
    DataAccessAgreement daa2After2nd = daaDAO.findById(daaId2);
    assertTrue(dacIdIn(daa2After2nd, dacId));

    // Third assignment: DAC -> DAA3 (replaces DAA2)
    daaDAO.createDacDaaRelation(dacId, daaId3, userId);
    DataAccessAgreement daa2After3rd = daaDAO.findById(daaId2);
    assertNull(daa2After3rd.getDacs());
    DataAccessAgreement daa3After3rd = daaDAO.findById(daaId3);
    assertTrue(dacIdIn(daa3After3rd, dacId));

    // Verify complete audit trail
    List<DaaAudit> audits1 = daaDAO.findAuditsByDaaId(daaId1);
    assertTrue(audits1.stream().anyMatch(a -> a.action().equals(AuditActions.REMOVE)));

    List<DaaAudit> audits2 = daaDAO.findAuditsByDaaId(daaId2);
    assertTrue(audits2.stream().anyMatch(a -> a.action().equals(AuditActions.ADD)));
    assertTrue(audits2.stream().anyMatch(a -> a.action().equals(AuditActions.REMOVE)));

    List<DaaAudit> audits3 = daaDAO.findAuditsByDaaId(daaId3);
    assertTrue(audits3.stream().anyMatch(a -> a.action().equals(AuditActions.ADD)));
  }
}
