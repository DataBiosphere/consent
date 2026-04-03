package org.broadinstitute.consent.http.db;

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
import java.util.Set;
import org.broadinstitute.consent.http.enumeration.AuditActions;
import org.broadinstitute.consent.http.enumeration.FileCategory;
import org.broadinstitute.consent.http.models.DaaAudit;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DataAccessAgreement;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataUseBuilder;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.LibraryCard;
import org.broadinstitute.consent.http.models.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DaaDAOTest extends DAOTestHelper {

  private Integer createUserId() {
    return createUser().getUserId();
  }

  @Test
  void testInsert() {
    Integer userId = createUserId();
    Integer dacId = dacDAO.createDac(randomAlphabetic(5), randomAlphabetic(5), "", new Date());
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
    Integer dacId = dacDAO.createDac(randomAlphabetic(5), randomAlphabetic(5), "", new Date());
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
    Integer dacId = dacDAO.createDac(randomAlphabetic(5), randomAlphabetic(5), "", new Date());
    Integer daaId = daaDAO.createDaa(userId, Instant.now(), userId, Instant.now(), dacId);
    assertNotNull(daaId);
    List<DataAccessAgreement> daas = daaDAO.findAll();
    assertNotNull(daas);
    assertEquals(1, daas.size());
  }

  @Test
  void testFindAllMultipleDaas() {
    Integer userId = createUserId();
    Integer dacId = dacDAO.createDac(randomAlphabetic(5), randomAlphabetic(5), "", new Date());
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
    Integer dacId = dacDAO.createDac(randomAlphabetic(5), randomAlphabetic(5), "", new Date());
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
    DataAccessAgreement daa3 = daaDAO.findById(randomInt(10000, 100000));
    assertNull(daa3);
  }

  @Test
  void testFindByDacId() {
    Integer userId = createUserId();
    Integer dacId = dacDAO.createDac(randomAlphabetic(5), randomAlphabetic(5), "", new Date());
    Integer dacId2 = dacDAO.createDac(randomAlphabetic(5), randomAlphabetic(5), "", new Date());
    Integer daaId1 = daaDAO.createDaa(userId, Instant.now(), userId, Instant.now(), dacId);
    daaDAO.createDaa(userId, Instant.now(), userId, Instant.now(), dacId2);
    assertNotNull(daaId1);
    DataAccessAgreement daa1 = daaDAO.findByDacId(dacId);
    assertNotNull(daa1);
    assertEquals(daa1.getInitialDacId(), dacId);
    DataAccessAgreement daa2 = daaDAO.findByDacId(dacId2);
    assertNotNull(daa2);
    assertEquals(daa2.getInitialDacId(), dacId2);
    DataAccessAgreement daa3 = daaDAO.findByDacId(3);
    assertNull(daa3);
  }

  @Test
  void testFindByDacIdInvalid() {
    Integer userId = createUserId();
    Integer dacId = dacDAO.createDac(randomAlphabetic(5), randomAlphabetic(5), "", new Date());
    Integer dacId2 = dacDAO.createDac(randomAlphabetic(5), randomAlphabetic(5), "", new Date());
    daaDAO.createDaa(userId, Instant.now(), userId, Instant.now(), dacId);
    daaDAO.createDaa(userId, Instant.now(), userId, Instant.now(), dacId2);
    DataAccessAgreement daa1 = daaDAO.findByDacId(dacId);
    DataAccessAgreement daa2 = daaDAO.findByDacId(dacId2);
    DataAccessAgreement daa3 = daaDAO.findByDacId(randomInt(10000, 100000));
    assertNotNull(daa1);
    assertNotNull(daa2);
    assertNull(daa3);
  }

  @Test
  void testCreateDaaDacRelation() {
    Integer userId = createUserId();
    Integer dacId = dacDAO.createDac(randomAlphabetic(5), randomAlphabetic(5), "", new Date());
    Integer dacId2 = dacDAO.createDac(randomAlphabetic(5), randomAlphabetic(5), "", new Date());
    Integer dacId3 = dacDAO.createDac(randomAlphabetic(5), randomAlphabetic(5), "", new Date());
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
    assertTrue(testDAA1.getDacs().stream().map(Dac::getDacId).toList().contains(dacId));
    assertFalse(testDAA1.getDacs().stream().map(Dac::getDacId).toList().contains(dacId2));
    assertFalse(testDAA1.getDacs().stream().map(Dac::getDacId).toList().contains(dacId3));
    DataAccessAgreement testDAA2 = daaDAO.findById(daa2.getDaaId());
    assertFalse(testDAA2.getDacs().stream().map(Dac::getDacId).toList().contains(dacId));
    assertTrue(testDAA2.getDacs().stream().map(Dac::getDacId).toList().contains(dacId2));
    assertTrue(testDAA2.getDacs().stream().map(Dac::getDacId).toList().contains(dacId3));

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
    Integer dacId1 = dacDAO.createDac(randomAlphabetic(5), randomAlphabetic(5), "", new Date());
    Integer dacId2 = dacDAO.createDac(randomAlphabetic(5), randomAlphabetic(5), "", new Date());
    Integer dacId3 = dacDAO.createDac(randomAlphabetic(5), randomAlphabetic(5), "", new Date());

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
    Integer dacId = dacDAO.createDac(randomAlphabetic(5), randomAlphabetic(5), "", new Date());
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
        dacDAO.createDac(randomAlphabetic(5), "Dac 1", randomAlphabetic(15), new Date());
    Integer dacId2 =
        dacDAO.createDac(randomAlphabetic(5), "Dac 2", randomAlphabetic(15), new Date());
    Integer dacId3 =
        dacDAO.createDac(randomAlphabetic(5), "Dac 3", randomAlphabetic(15), new Date());
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
  void testFindDaaDatasetIdsByUserId() {
    // Testing the case of a user requesting DAR access to a dataset.
    // That user must have an LC with a DAA associated to the same DAC that the dataset is
    // associated to.
    User user = createUserWithInstitution();
    LibraryCard lc = createRandomLibraryCard(user);
    Dac dac1 = createRandomDac();
    Dac dac2 = createRandomDac();
    DataAccessAgreement daa = createRandomDataAccessAgreement(user, dac1);
    // Associate the DAC to the Data Access Agreeement:
    daaDAO.createDacDaaRelation(dac1.getDacId(), daa.getDaaId(), user.getUserId());
    // Associate the user's Library Card to the Data Access Agreement:
    libraryCardDAO.createLibraryCardDaaRelation(lc.getId(), daa.getDaaId());
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
    Integer dac1Id = dacDAO.createDac(randomAlphabetic(5), randomAlphabetic(5), "", new Date());
    Dac dac1 = dacDAO.findById(dac1Id);
    Integer daaId1 =
        daaDAO.createDaa(dataSubmitterId, Instant.now(), dataSubmitterId, Instant.now(), dac1Id);
    daaDAO.createDacDaaRelation(dac1Id, daaId1, dataSubmitterId);
    DataAccessAgreement daa1 = daaDAO.findById(daaId1);
    Dataset d1 = createRandomDataset(dataSubmitter, dac1);

    // Dac/Dataset/DAA 2
    Integer dacId2 = dacDAO.createDac(randomAlphabetic(5), randomAlphabetic(5), "", new Date());
    Dac dac2 = dacDAO.findById(dacId2);
    Integer daaId2 =
        daaDAO.createDaa(dataSubmitterId, Instant.now(), dataSubmitterId, Instant.now(), dacId2);
    daaDAO.createDacDaaRelation(dacId2, daaId2, dataSubmitterId);
    DataAccessAgreement daa2 = daaDAO.findById(daaId2);
    Dataset d2 = createRandomDataset(dataSubmitter, dac2);

    // Dac/Dataset/DAA 3 which should not be in the returned results
    Integer dacId3 = dacDAO.createDac(randomAlphabetic(5), randomAlphabetic(5), "", new Date());
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
  void testFindDaaIdsByDatasetIds() {
    Integer userId = createUserId();
    Integer dacId = dacDAO.createDac(randomAlphabetic(5), randomAlphabetic(5), "", new Date());
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
  void testFindDaaIdsByDatasetIds_dacNotAssociated() {
    Integer userId = createUserId();
    Integer dacId = dacDAO.createDac(randomAlphabetic(5), randomAlphabetic(5), "", new Date());
    Dataset dataset1 = createRandomDataset(userDAO.findUserById(userId), dacDAO.findById(dacId));
    Dataset dataset2 = createRandomDataset(userDAO.findUserById(userId), dacDAO.findById(dacId));

    // Create a DAA but do not associate it to the DAC
    daaDAO.createDaa(userId, Instant.now(), userId, Instant.now(), dacId);

    Set<Integer> daaIds =
        daaDAO.findDaaIdsByDatasetIds(List.of(dataset1.getDatasetId(), dataset2.getDatasetId()));
    assertTrue(daaIds.isEmpty());
  }

  @Test
  void testFindDaaIdsByDatasetIds_datasetsNotAssociated() {
    Integer userId = createUserId();
    Integer dacId = dacDAO.createDac(randomAlphabetic(5), randomAlphabetic(5), "", new Date());
    Dataset dataset1 = createRandomDataset(userDAO.findUserById(userId), dacDAO.findById(dacId));
    Dataset dataset2 = createRandomDataset(userDAO.findUserById(userId), dacDAO.findById(dacId));

    // Create a DAA that is associated to a different DAC than the datasets are
    Integer otherDacId = dacDAO.createDac(randomAlphabetic(5), randomAlphabetic(5), "", new Date());
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
    int dacId = dacDAO.createDac(randomAlphabetic(5), randomAlphabetic(5), new Date());
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
}
