package org.broadinstitute.consent.http.service.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.commons.lang3.RandomStringUtils;
import org.broadinstitute.consent.http.db.DAOTestHelper;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DataAccessAgreement;
import org.broadinstitute.consent.http.models.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DacServiceDAOTest extends DAOTestHelper {

  private DacServiceDAO serviceDAO;

  @BeforeEach
  void setUp() {
    serviceDAO = new DacServiceDAO(jdbi, daaDAO);
  }

  @Test
  void testDeleteDac() {

    List<Dac> dacs = getDacs();
    dacs.forEach(dac -> dacDAO.createDac(RandomStringUtils.randomAlphabetic(10), RandomStringUtils.randomAlphabetic(10), RandomStringUtils.randomAlphabetic(10),  new Date()));
    List<Dac> dacsInDatabase = dacDAO.findAll();

    Dac fullDac = dacDAO.findById(dacsInDatabase.get(0).getDacId());
    try {
      serviceDAO.deleteDacAndDaas(fullDac);
    } catch (Exception e) {
      fail("Delete should not fail");
    }
    List<Dac> dacListRemoved = dacDAO.findAll();
    assertEquals(3, dacListRemoved.size());
  }

  @Test
  void testDeleteDacWithDaas() {

    List<Dac> dacs = getDacs();
    dacs.forEach(dac -> dacDAO.createDac(RandomStringUtils.randomAlphabetic(10), RandomStringUtils.randomAlphabetic(10), RandomStringUtils.randomAlphabetic(10),  new Date()));

    List<Dac> dacsInDatabase = dacDAO.findAll();
    User user = createUser();
    daaDAO.createDaa(user.getUserId(), new Date().toInstant(), user.getUserId(), new Date().toInstant(), dacsInDatabase.get(0).getDacId());
    daaDAO.createDaa(user.getUserId(), new Date().toInstant(), user.getUserId(), new Date().toInstant(), dacsInDatabase.get(0).getDacId());
    Dac fullDac = dacDAO.findById(dacsInDatabase.get(0).getDacId());

    try {
      serviceDAO.deleteDacAndDaas(fullDac);
    } catch (Exception e) {
      fail("Delete should not fail");
    }
    List<Dac> dacListRemoved = dacDAO.findAll();
    assertEquals(3, dacListRemoved.size());
    List<DataAccessAgreement> daaListRemoved = daaDAO.findAll();
    assertEquals(0, daaListRemoved.size());
  }

  @Test
  void testDeleteDacWithDaasAndBroadDaa() {
    getDacs();
    Integer dacId = dacDAO.createDac(RandomStringUtils.randomAlphabetic(10), RandomStringUtils.randomAlphabetic(10), RandomStringUtils.randomAlphabetic(10),  new Date());
    Integer broadDacId = dacDAO.createDac("Broad", RandomStringUtils.randomAlphabetic(10), RandomStringUtils.randomAlphabetic(10),  new Date());
    User user = createUser();
    Integer daaId = daaDAO.createDaa(user.getUserId(), new Date().toInstant(), user.getUserId(), new Date().toInstant(), dacId);
    DataAccessAgreement daa = daaDAO.findById(daaId);
    Integer daaId2 = daaDAO.createDaa(user.getUserId(), new Date().toInstant(), user.getUserId(), new Date().toInstant(), broadDacId);
    daaDAO.createDacDaaRelation(dacId, daaId2);

    Dac fullDac = dacDAO.findById(dacId);

    try {
      serviceDAO.deleteDacAndDaas(fullDac);
    } catch (Exception e) {
      fail("Delete should not fail");
    }
    List<Dac> dacListRemoved = dacDAO.findAll();
    assertEquals(1, dacListRemoved.size());
    List<DataAccessAgreement> daaListRemoved = daaDAO.findAll();
    assertEquals(1, daaListRemoved.size());
  }

  @Test
  void testDeleteDacWithBroadDaa() {
    getDacs();
    Integer dacId = dacDAO.createDac(RandomStringUtils.randomAlphabetic(10), RandomStringUtils.randomAlphabetic(10), RandomStringUtils.randomAlphabetic(10),  new Date());
    Integer broadDacId = dacDAO.createDac("Broad", RandomStringUtils.randomAlphabetic(10), RandomStringUtils.randomAlphabetic(10),  new Date());
    User user = createUser();
    Integer daaId = daaDAO.createDaa(user.getUserId(), new Date().toInstant(), user.getUserId(), new Date().toInstant(), broadDacId);
    daaDAO.createDacDaaRelation(dacId, daaId);
    Dac fullDac = dacDAO.findById(dacId);

    try {
      serviceDAO.deleteDacAndDaas(fullDac);
    } catch (Exception e) {
      fail("Delete should not fail");
    }
    List<Dac> dacListRemoved = dacDAO.findAll();
    assertEquals(1, dacListRemoved.size());
    List<DataAccessAgreement> daaListRemoved = daaDAO.findAll();
    assertEquals(1, daaListRemoved.size());
  }

  /**
   * @return A list of 5 dacs
   */
  private List<Dac> getDacs() {
    DataAccessAgreement daa = new DataAccessAgreement();
    daa.setDaaId(1);
    return IntStream.range(1, 5).
        mapToObj(i -> {
          Dac dac = new Dac();
          dac.setDacId(i);
          dac.setDescription("Dac " + i);
          dac.setName("Dac " + i);
          dac.setAssociatedDaa(daa);
          return dac;
        }).collect(Collectors.toList());
  }

}
