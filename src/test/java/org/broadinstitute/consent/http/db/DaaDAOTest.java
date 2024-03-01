package org.broadinstitute.consent.http.db;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DataAccessAgreement;
import org.junit.jupiter.api.Test;

public class DaaDAOTest extends DAOTestHelper {

  @Test
  void testInsert() {
    Integer userId = userDAO.insertUser("blah", "blah", new Date());
    Integer dacId = dacDAO.createDac("blah", "blah", "",  new Date());
    Integer daaId = daaDAO.createDaa(userId, new Date().toInstant(), userId, new Date().toInstant(), dacId);
    assertNotNull(daaId);
  }

  @Test
  void testInsertMultipleDaasOneDacId() {
    Integer userId = userDAO.insertUser("blah", "blah", new Date());
    Integer dacId = dacDAO.createDac("blah", "blah", "",  new Date());
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
    Integer userId = userDAO.insertUser("blah", "blah", new Date());
    Integer dacId = dacDAO.createDac("blah", "blah", "",  new Date());
    Integer daaId = daaDAO.createDaa(userId, new Date().toInstant(), userId, new Date().toInstant(), dacId);
    assertNotNull(daaId);
    List<DataAccessAgreement> daas = daaDAO.findAll();
    assertNotNull(daas);
    assertEquals(1, daas.size());
  }

  @Test
  void testFindAllMultipleDaas() {
    Integer userId = userDAO.insertUser("blah", "blah", new Date());
    Integer dacId = dacDAO.createDac("blah", "blah", "",  new Date());
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
    Integer userId = userDAO.insertUser("blah", "blah", new Date());
    Integer dacId = dacDAO.createDac("blah", "blah", "",  new Date());
    Integer daaId1 = daaDAO.createDaa(userId, new Date().toInstant(), userId, new Date().toInstant(), dacId);
    Integer daaId2 = daaDAO.createDaa(userId, new Date().toInstant(), userId, new Date().toInstant(), dacId);
    assertNotNull(daaId1);
    assertNotNull(daaId2);
    DataAccessAgreement daa1 = daaDAO.findById(1);
    assertNotNull(daa1);
    assertEquals(daa1.getId(), 1);
    DataAccessAgreement daa2 = daaDAO.findById(2);
    assertNotNull(daa2);
    assertEquals(daa2.getId(), 2);
  }
  @Test
  void testFindByIdInvalid() {
    Integer userId = userDAO.insertUser("blah", "blah", new Date());
    Integer dacId = dacDAO.createDac("blah", "blah", "",  new Date());
    Integer daaId1 = daaDAO.createDaa(userId, new Date().toInstant(), userId, new Date().toInstant(), dacId);
    Integer daaId2 = daaDAO.createDaa(userId, new Date().toInstant(), userId, new Date().toInstant(), dacId);
    DataAccessAgreement daa1 = daaDAO.findById(1);
    DataAccessAgreement daa2 = daaDAO.findById(2);
    DataAccessAgreement daa3 = daaDAO.findById(3);
    assertNull(daa3);
  }

  @Test
  void testFindByDacId() {
    Integer userId = userDAO.insertUser("blah", "blah", new Date());
    Integer dacId = dacDAO.createDac("blah", "blah", "",  new Date());
    Integer dacId2 = dacDAO.createDac("blah", "blah", "",  new Date());
    Integer daaId1 = daaDAO.createDaa(userId, new Date().toInstant(), userId, new Date().toInstant(), dacId);
    Integer daaId2 = daaDAO.createDaa(userId, new Date().toInstant(), userId, new Date().toInstant(), dacId2);
    assertNotNull(daaId1);
    DataAccessAgreement daa1 = daaDAO.findByDacId(1);
    assertNotNull(daa1);
    assertEquals(daa1.getInitialDacId(), 1);
    DataAccessAgreement daa2 = daaDAO.findByDacId(2);
    assertNotNull(daa2);
    assertEquals(daa2.getInitialDacId(), 2);
    DataAccessAgreement daa3 = daaDAO.findByDacId(3);
    assertNull(daa3);
  }

  @Test
  void testFindByDacIdInvalid() {
    Integer userId = userDAO.insertUser("blah", "blah", new Date());
    Integer dacId = dacDAO.createDac("blah", "blah", "",  new Date());
    Integer dacId2 = dacDAO.createDac("blah", "blah", "",  new Date());
    Integer daaId1 = daaDAO.createDaa(userId, new Date().toInstant(), userId, new Date().toInstant(), dacId);
    Integer daaId2 = daaDAO.createDaa(userId, new Date().toInstant(), userId, new Date().toInstant(), dacId2);
    DataAccessAgreement daa1 = daaDAO.findByDacId(1);
    DataAccessAgreement daa2 = daaDAO.findByDacId(2);
    DataAccessAgreement daa3 = daaDAO.findByDacId(3);
    assertNull(daa3);
  }

  @Test
  void testCreateDaaDacRelation() {
    Integer userId = userDAO.insertUser("blah", "blah", new Date());
    Integer dacId = dacDAO.createDac("blah", "blah", "",  new Date());
    Integer dacId2 = dacDAO.createDac("blah", "blah", "",  new Date());
    Integer dacId3 = dacDAO.createDac("blah", "blah", "",  new Date());
    Integer daaId1 = daaDAO.createDaa(userId, new Date().toInstant(), userId, new Date().toInstant(), dacId);
    Integer daaId2 = daaDAO.createDaa(userId, new Date().toInstant(), userId, new Date().toInstant(), dacId2);
    DataAccessAgreement daa1 = daaDAO.findByDacId(1);

    DataAccessAgreement daa2 = daaDAO.findByDacId(2);


    daaDAO.createDaaDacRelation(daa1.getId(),dacId);
    daaDAO.createDaaDacRelation(daa1.getId(),dacId2);
    daaDAO.createDaaDacRelation(daa2.getId(),dacId3);

    DataAccessAgreement daa1Copy = daaDAO.findByDacId(daaId1);
    List<Dac> associatedDacs = daa1Copy.getAssociatedDacs();
    assertNotNull(associatedDacs);
    List<Dac> expectedAssociatedDacs = new ArrayList<>();
    expectedAssociatedDacs.add(dacDAO.findById(dacId));
    assertEquals(expectedAssociatedDacs.get(0).getDacId(), associatedDacs.get(0).getDacId());
    assertEquals(1, associatedDacs.size());

//    DataAccessAgreement daa2Copy = daaDAO.findByDacId(2);
//    List<Dac> associatedDacs2 = daa2Copy.getAssociatedDacs();
//    assertNotNull(associatedDacs2);
//    List<Dac> expectedAssociatedDacs2 = new ArrayList<>();
//    expectedAssociatedDacs2.add(dacDAO.findById(2));
//    assertEquals(expectedAssociatedDacs2.get(0).getDacId(), associatedDacs2.get(0).getDacId());
//    assertEquals(2, associatedDacs2.size());
  }

  @Test
  void testDeleteDaaDacRelation() {
    Integer userId = userDAO.insertUser("blah", "blah", new Date());
    Integer dacId = dacDAO.createDac("blah", "blah", "",  new Date());
    Integer dacId2 = dacDAO.createDac("blah", "blah", "",  new Date());
    Integer dacId3 = dacDAO.createDac("blah", "blah", "",  new Date());
    Integer daaId1 = daaDAO.createDaa(userId, new Date().toInstant(), userId, new Date().toInstant(), dacId);
    Integer daaId2 = daaDAO.createDaa(userId, new Date().toInstant(), userId, new Date().toInstant(), dacId2);
    assertNotNull(daaId1);
    DataAccessAgreement daa1 = daaDAO.findByDacId(1);
    assertNotNull(daa1);
    assertEquals(daa1.getInitialDacId(), 1);
    DataAccessAgreement daa2 = daaDAO.findByDacId(2);
    assertNotNull(daa2);
    assertEquals(daa2.getInitialDacId(), 2);

    daaDAO.createDaaDacRelation(daa1.getId(),dacId);
    daaDAO.createDaaDacRelation(daa2.getId(),dacId2);
    daaDAO.createDaaDacRelation(daa2.getId(),dacId3);

    daaDAO.deleteDaaDacRelation(dacId);
    daaDAO.deleteDaaDacRelation(dacId2);
    daaDAO.deleteDaaDacRelation(dacId3);

    DataAccessAgreement daa1Copy = daaDAO.findByDacId(1);
    List<Dac> associatedDacs = daa1Copy.getAssociatedDacs();
    assertNull(associatedDacs);
  }
}
