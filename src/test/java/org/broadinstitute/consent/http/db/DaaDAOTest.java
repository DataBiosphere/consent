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
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.enumeration.FileCategory;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DataAccessAgreement;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataUseBuilder;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.Institution;
import org.broadinstitute.consent.http.models.LibraryCard;
import org.broadinstitute.consent.http.models.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DaaDAOTest extends DAOTestHelper {

  @Test
  void testInsert() {
    Integer userId = userDAO.insertUser(RandomStringUtils.randomAlphabetic(5), RandomStringUtils.randomAlphabetic(5), new Date());
    Integer dacId = dacDAO.createDac(RandomStringUtils.randomAlphabetic(5), RandomStringUtils.randomAlphabetic(5), "",  new Date());
    Integer daaId = daaDAO.createDaa(userId, new Date().toInstant(), userId, new Date().toInstant(), dacId);
    assertNotNull(daaId);
  }

  @Test
  void testInsertMultipleDaasOneDacId() {
    Integer userId = userDAO.insertUser(RandomStringUtils.randomAlphabetic(5), RandomStringUtils.randomAlphabetic(5), new Date());
    Integer dacId = dacDAO.createDac(RandomStringUtils.randomAlphabetic(5), RandomStringUtils.randomAlphabetic(5), "",  new Date());
    Integer daaId1 = daaDAO.createDaa(userId, new Date().toInstant(), userId, new Date().toInstant(), dacId);
    Integer daaId2 = daaDAO.createDaa(userId, new Date().toInstant(), userId, new Date().toInstant(), dacId);
    Integer daaId3 = daaDAO.createDaa(userId, new Date().toInstant(), userId, new Date().toInstant(), dacId);
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
    Integer userId = userDAO.insertUser(RandomStringUtils.randomAlphabetic(5), RandomStringUtils.randomAlphabetic(5), new Date());
    Integer dacId = dacDAO.createDac(RandomStringUtils.randomAlphabetic(5), RandomStringUtils.randomAlphabetic(5), "",  new Date());
    Integer daaId = daaDAO.createDaa(userId, new Date().toInstant(), userId, new Date().toInstant(), dacId);
    assertNotNull(daaId);
    List<DataAccessAgreement> daas = daaDAO.findAll();
    assertNotNull(daas);
    assertEquals(1, daas.size());
  }

  @Test
  void testFindAllMultipleDaas() {
    Integer userId = userDAO.insertUser(RandomStringUtils.randomAlphabetic(5), RandomStringUtils.randomAlphabetic(5), new Date());
    Integer dacId = dacDAO.createDac(RandomStringUtils.randomAlphabetic(5), RandomStringUtils.randomAlphabetic(5), "",  new Date());
    Integer daaId1 = daaDAO.createDaa(userId, new Date().toInstant(), userId, new Date().toInstant(), dacId);
    Integer daaId2 = daaDAO.createDaa(userId, new Date().toInstant(), userId, new Date().toInstant(), dacId);
    Integer daaId3 = daaDAO.createDaa(userId, new Date().toInstant(), userId, new Date().toInstant(), dacId);
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
    Integer userId = userDAO.insertUser(RandomStringUtils.randomAlphabetic(5), RandomStringUtils.randomAlphabetic(5), new Date());
    Integer dacId = dacDAO.createDac(RandomStringUtils.randomAlphabetic(5), RandomStringUtils.randomAlphabetic(5), "",  new Date());
    Integer daaId1 = daaDAO.createDaa(userId, new Date().toInstant(), userId, new Date().toInstant(), dacId);
    Integer daaId2 = daaDAO.createDaa(userId, new Date().toInstant(), userId, new Date().toInstant(), dacId);
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
    DataAccessAgreement daa3 = daaDAO.findById(RandomUtils.nextInt(10000, 100000));
    assertNull(daa3);
  }

  @Test
  void testFindByDacId() {
    Integer userId = userDAO.insertUser(RandomStringUtils.randomAlphabetic(5), RandomStringUtils.randomAlphabetic(5), new Date());
    Integer dacId = dacDAO.createDac(RandomStringUtils.randomAlphabetic(5), RandomStringUtils.randomAlphabetic(5), "",  new Date());
    Integer dacId2 = dacDAO.createDac(RandomStringUtils.randomAlphabetic(5), RandomStringUtils.randomAlphabetic(5), "",  new Date());
    Integer daaId1 = daaDAO.createDaa(userId, new Date().toInstant(), userId, new Date().toInstant(), dacId);
    Integer daaId2 = daaDAO.createDaa(userId, new Date().toInstant(), userId, new Date().toInstant(), dacId2);
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
    Integer userId = userDAO.insertUser(RandomStringUtils.randomAlphabetic(5), RandomStringUtils.randomAlphabetic(5), new Date());
    Integer dacId = dacDAO.createDac(RandomStringUtils.randomAlphabetic(5), RandomStringUtils.randomAlphabetic(5), "",  new Date());
    Integer dacId2 = dacDAO.createDac(RandomStringUtils.randomAlphabetic(5), RandomStringUtils.randomAlphabetic(5), "",  new Date());
    daaDAO.createDaa(userId, new Date().toInstant(), userId, new Date().toInstant(), dacId);
    daaDAO.createDaa(userId, new Date().toInstant(), userId, new Date().toInstant(), dacId2);
    DataAccessAgreement daa1 = daaDAO.findByDacId(dacId);
    DataAccessAgreement daa2 = daaDAO.findByDacId(dacId2);
    DataAccessAgreement daa3 = daaDAO.findByDacId(RandomUtils.nextInt(10000, 100000));
    assertNotNull(daa1);
    assertNotNull(daa2);
    assertNull(daa3);
  }

  @Test
  void testCreateDaaDacRelation() {
    Integer userId = userDAO.insertUser(RandomStringUtils.randomAlphabetic(5), RandomStringUtils.randomAlphabetic(5), new Date());
    Integer dacId = dacDAO.createDac(RandomStringUtils.randomAlphabetic(5), RandomStringUtils.randomAlphabetic(5), "",  new Date());
    Integer dacId2 = dacDAO.createDac(RandomStringUtils.randomAlphabetic(5), RandomStringUtils.randomAlphabetic(5), "",  new Date());
    Integer dacId3 = dacDAO.createDac(RandomStringUtils.randomAlphabetic(5), RandomStringUtils.randomAlphabetic(5), "",  new Date());
    Integer daaId1 = daaDAO.createDaa(userId, new Date().toInstant(), userId, new Date().toInstant(), dacId);
    Integer daaId2 = daaDAO.createDaa(userId, new Date().toInstant(), userId, new Date().toInstant(), dacId2);
    assertNotNull(daaId1);
    DataAccessAgreement daa1 = daaDAO.findByDacId(dacId);
    assertNotNull(daa1);
    assertEquals(daa1.getInitialDacId(), dacId);
    DataAccessAgreement daa2 = daaDAO.findByDacId(dacId2);
    assertNotNull(daa2);
    assertEquals(daa2.getInitialDacId(), dacId2);

    daaDAO.createDacDaaRelation(dacId, daa1.getDaaId());
    daaDAO.createDacDaaRelation(dacId2, daa2.getDaaId());
    daaDAO.createDacDaaRelation(dacId3, daa2.getDaaId());
  }

  @Test
  void testDeleteDaaDacRelation() {
    Integer userId = userDAO.insertUser(RandomStringUtils.randomAlphabetic(5), RandomStringUtils.randomAlphabetic(5), new Date());
    Integer dacId = dacDAO.createDac(RandomStringUtils.randomAlphabetic(5), RandomStringUtils.randomAlphabetic(5), "",  new Date());
    Integer dacId2 = dacDAO.createDac(RandomStringUtils.randomAlphabetic(5), RandomStringUtils.randomAlphabetic(5), "",  new Date());
    Integer dacId3 = dacDAO.createDac(RandomStringUtils.randomAlphabetic(5), RandomStringUtils.randomAlphabetic(5), "",  new Date());
    Integer daaId1 = daaDAO.createDaa(userId, new Date().toInstant(), userId, new Date().toInstant(), dacId);
    Integer daaId2 = daaDAO.createDaa(userId, new Date().toInstant(), userId, new Date().toInstant(), dacId2);
    assertNotNull(daaId1);
    DataAccessAgreement daa1 = daaDAO.findByDacId(dacId);
    assertNotNull(daa1);
    assertEquals(daa1.getInitialDacId(), dacId);
    DataAccessAgreement daa2 = daaDAO.findByDacId(dacId2);
    assertNotNull(daa2);
    assertEquals(daa2.getInitialDacId(), dacId2);

    daaDAO.createDacDaaRelation(dacId, daa1.getDaaId());
    daaDAO.createDacDaaRelation(dacId2, daa2.getDaaId());
    daaDAO.createDacDaaRelation(dacId3, daa2.getDaaId());

    daaDAO.deleteDacDaaRelation(dacId, daaId1);
    daaDAO.deleteDacDaaRelation(dacId2, daaId2);
  }

  @Test
  void testFindWithFileStorageObject() {
    Integer userId = userDAO.insertUser(RandomStringUtils.randomAlphabetic(5), RandomStringUtils.randomAlphabetic(5), new Date());
    Integer dacId = dacDAO.createDac(RandomStringUtils.randomAlphabetic(5), RandomStringUtils.randomAlphabetic(5), "",  new Date());
    Integer daaId = daaDAO.createDaa(userId, new Date().toInstant(), userId, new Date().toInstant(), dacId);
    Integer fsoId = fileStorageObjectDAO.insertNewFile(
        RandomStringUtils.randomAlphabetic(10),
        FileCategory.DATA_ACCESS_AGREEMENT.getValue(),
        RandomStringUtils.randomAlphabetic(10),
        MediaType.TEXT_PLAIN_TYPE.getType(),
        daaId.toString(),
        userId,
        Instant.now()
    );
    DataAccessAgreement daa = daaDAO.findById(daaId);
    assertNotNull(daa);
    assertNotNull(daa.getFile());
    assertEquals(fsoId, daa.getFile().getFileStorageObjectId());
  }

  @Test
  void testFindWithDacs() {
    Integer userId = userDAO.insertUser(RandomStringUtils.randomAlphabetic(15), RandomStringUtils.randomAlphabetic(5), new Date());
    Integer dacId = dacDAO.createDac(RandomStringUtils.randomAlphabetic(5), "Dac 1", RandomStringUtils.randomAlphabetic(15),  new Date());
    Integer dacId2 = dacDAO.createDac(RandomStringUtils.randomAlphabetic(5), "Dac 2", RandomStringUtils.randomAlphabetic(15),  new Date());
    Integer dacId3 = dacDAO.createDac(RandomStringUtils.randomAlphabetic(5), "Dac 3", RandomStringUtils.randomAlphabetic(15),  new Date());
    Integer daaId = daaDAO.createDaa(userId, new Date().toInstant(), userId, new Date().toInstant(), dacId);
    daaDAO.createDacDaaRelation(dacId, daaId);
    daaDAO.createDacDaaRelation(dacId2, daaId);
    daaDAO.createDacDaaRelation(dacId3, daaId);
    DataAccessAgreement daa = daaDAO.findById(daaId);

    assertNotNull(daa);
    assertNotNull(daa.getDacs());
    assertEquals(3, daa.getDacs().size());
  }

    @Test
  void testFindDaaDatasetIdsByUserId() {
    // Testing the case of a user requesting DAR access to a dataset.
    // That user must have an LC with a DAA associated to the same DAC that the dataset is associated to.
    User user = createRandomUser();
    Institution institution = createRandomInstitution(user.getUserId());
    LibraryCard lc = createRandomLibraryCard(user, institution);
    Dac dac1 = createRandomDac();
    Dac dac2 = createRandomDac();
    DataAccessAgreement daa = createRandomDataAccessAgreement(user, dac1);
    // Associate the DAC to the Data Access Agreeement:
    daaDAO.createDacDaaRelation(dac1.getDacId(), daa.getDaaId());
    // Associate the user's Library Card to the Data Access Agreeement:
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
  void testDeleteDaa() {
    Integer userId = userDAO.insertUser(RandomStringUtils.randomAlphabetic(5), RandomStringUtils.randomAlphabetic(5), new Date());
    User user = userDAO.findUserById(userId);
    Institution institution = createRandomInstitution(user.getUserId());
    Integer dacId = dacDAO.createDac(RandomStringUtils.randomAlphabetic(5), RandomStringUtils.randomAlphabetic(5), "",  new Date());;
    Integer daaId1 = daaDAO.createDaa(userId, new Date().toInstant(), userId, new Date().toInstant(), dacId);
    LibraryCard lc = createRandomLibraryCard(user, institution);
    DataAccessAgreement daa1 = daaDAO.findById(daaId1);

    daaDAO.createDacDaaRelation(dacId, daa1.getDaaId());
    libraryCardDAO.createLibraryCardDaaRelation(lc.getId(), daaId1);

    daaDAO.deleteDaa(daa1.getDaaId());

    List<DataAccessAgreement> daas = daaDAO.findAll();
    assertTrue(daas.isEmpty());
    assertTrue(lc.getDaaIds().isEmpty());
  }

  @Test
  void testFindByDarReferenceId() {
    // This test requires a good deal of model setup: DAR, Dataset, DAC, and DataAccessAgreements
    // We'll create a single DAR with 2 datasets, each one in a separate DAC with separate DAAs
    // and a third dac/dataset/daa that should not be found.
    Integer dataSubmitterId = userDAO.insertUser(RandomStringUtils.randomAlphabetic(5), RandomStringUtils.randomAlphabetic(5), new Date());
    User dataSubmitter = userDAO.findUserById(dataSubmitterId);

    // DAC/Dataset/DAA 1
    Integer dac1Id = dacDAO.createDac(RandomStringUtils.randomAlphabetic(5), RandomStringUtils.randomAlphabetic(5), "",  new Date());
    Dac dac1 = dacDAO.findById(dac1Id);
    Integer daaId1 = daaDAO.createDaa(dataSubmitterId, new Date().toInstant(), dataSubmitterId, new Date().toInstant(), dac1Id);
    daaDAO.createDacDaaRelation(dac1Id, daaId1);
    DataAccessAgreement daa1 = daaDAO.findById(daaId1);
    Dataset d1 = createRandomDataset(dataSubmitter, dac1);

    // Dac/Dataset/DAA 2
    Integer dacId2 = dacDAO.createDac(RandomStringUtils.randomAlphabetic(5), RandomStringUtils.randomAlphabetic(5), "",  new Date());
    Dac dac2 = dacDAO.findById(dacId2);
    Integer daaId2 = daaDAO.createDaa(dataSubmitterId, new Date().toInstant(), dataSubmitterId, new Date().toInstant(), dacId2);
    daaDAO.createDacDaaRelation(dacId2, daaId2);
    DataAccessAgreement daa2 = daaDAO.findById(daaId2);
    Dataset d2 = createRandomDataset(dataSubmitter, dac2);

    // Dac/Dataset/DAA 3 which should not be in the returned results
    Integer dacId3 = dacDAO.createDac(RandomStringUtils.randomAlphabetic(5), RandomStringUtils.randomAlphabetic(5), "",  new Date());
    Dac dac3 = dacDAO.findById(dacId3);
    Integer daaId3 = daaDAO.createDaa(dataSubmitterId, new Date().toInstant(), dataSubmitterId, new Date().toInstant(), dacId3);
    daaDAO.createDacDaaRelation(dacId3, daaId3);
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

  private User createRandomUser() {
    int userId = userDAO.insertUser(RandomStringUtils.randomAlphabetic(15),
        RandomStringUtils.randomAlphabetic(5), new Date());
    return userDAO.findUserById(userId);
  }

  private Institution createRandomInstitution(int userId) {
    int institutionId = institutionDAO.insertInstitution(
        RandomStringUtils.randomAlphabetic(5),
        RandomStringUtils.randomAlphabetic(5),
        RandomStringUtils.randomAlphabetic(5),
        RandomStringUtils.randomAlphabetic(5),
        null,
        null,
        null,
        null,
        null,
        userId,
        new Date());
    return institutionDAO.findInstitutionById(institutionId);
  }

  private LibraryCard createRandomLibraryCard(User user, Institution institution) {
    int lcId = libraryCardDAO.insertLibraryCard(
        user.getUserId(),
        institution.getId(),
        RandomStringUtils.randomAlphabetic(5),
        RandomStringUtils.randomAlphabetic(5),
        RandomStringUtils.randomAlphabetic(5),
        user.getUserId(),
        new Date());
    return libraryCardDAO.findLibraryCardById(lcId);
  }

  private Dac createRandomDac() {
    int dacId = dacDAO.createDac(
        RandomStringUtils.randomAlphabetic(5),
        RandomStringUtils.randomAlphabetic(5),
        new Date());
    return dacDAO.findById(dacId);
  }

  private DataAccessAgreement createRandomDataAccessAgreement(User user, Dac dac) {
    int daaId = daaDAO.createDaa(
        user.getUserId(),
        Instant.now(),
        user.getUserId(),
        Instant.now(),
        dac.getDacId());
    return daaDAO.findById(daaId);
  }

  private Dataset createRandomDataset(User user, Dac dac) {
    int datasetId = datasetDAO.insertDataset(
        RandomStringUtils.randomAlphabetic(5),
        new Timestamp(Instant.now().getEpochSecond()),
        user.getUserId(),
        null,
        new DataUseBuilder().setGeneralUse(true).build().toString(),
        dac.getDacId());
    return datasetDAO.findDatasetById(datasetId);
  }

}
