package org.broadinstitute.consent.http.db;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.ws.rs.core.MediaType;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.enumeration.FileCategory;
import org.broadinstitute.consent.http.models.DataAccessAgreement;
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

}
