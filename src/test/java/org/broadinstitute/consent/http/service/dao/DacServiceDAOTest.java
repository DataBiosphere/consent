package org.broadinstitute.consent.http.service.dao;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.db.DAOTestHelper;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DataAccessAgreement;
import org.broadinstitute.consent.http.models.Dataset;
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
    serviceDAO = new DacServiceDAO(jdbi);
  }

  @Test
  void testDeleteDac() {
    User superUser = createUser();
    // Create DACs and all associated objects subject to deletion
    List<Dac> dacs = createMockDACs();
    dacs.forEach(dac -> {
      // DAC
      int dacId = dacDAO.createDac(RandomStringUtils.randomAlphabetic(10), RandomStringUtils.randomAlphabetic(10), RandomStringUtils.randomAlphabetic(10),  new Date());
      // DAA
      int daaId = daaDAO.createDaa(superUser.getUserId(), new Date().toInstant(), superUser.getUserId(), new Date().toInstant(), dacId);
      // DAC-DAA Association
      daaDAO.createDacDaaRelation(dacId, daaId);
      // Library Card User with Institution
      User lcUser = createUser();
      int institutionId = institutionDAO.insertInstitution(
          RandomStringUtils.randomAlphabetic(10),
          RandomStringUtils.randomAlphabetic(10),
          RandomStringUtils.randomAlphabetic(10),
          RandomStringUtils.randomAlphabetic(10),
          RandomUtils.nextInt(10, 100),
          RandomStringUtils.randomAlphabetic(10),
          RandomStringUtils.randomAlphabetic(10),
          RandomStringUtils.randomAlphabetic(10),
          RandomStringUtils.randomAlphabetic(10),
          superUser.getUserId(),
          new Date());
      int lcId = libraryCardDAO.insertLibraryCard(
          lcUser.getUserId(),
          institutionId,
          RandomStringUtils.randomAlphabetic(10),
          RandomStringUtils.randomAlphabetic(10),
          RandomStringUtils.randomAlphabetic(10),
          superUser.getUserId(),
          new Date());
      // Library Card User DAA association
      libraryCardDAO.createLibraryCardDaaRelation(lcId, daaId);
      // DAC Member User
      User member = createUser();
      userRoleDAO.insertSingleUserRole(UserRoles.MEMBER.getRoleId(), member.getUserId());
      // DAC Chair User
      User chair = createUser();
      userRoleDAO.insertSingleUserRole(UserRoles.CHAIRPERSON.getRoleId(), chair.getUserId());
      // DAC associated Dataset
      int datasetId = datasetDAO.insertDataset(
          RandomStringUtils.randomAlphabetic(10),
          Timestamp.from(Instant.now()),
          superUser.getUserId(),
          RandomStringUtils.randomAlphabetic(10),
          RandomStringUtils.randomAlphabetic(10),
          dacId);
      datasetDAO.updateDatasetDacId(datasetId, dacId);
    });
    dacDAO.findAll().forEach(dac -> {
      assertDoesNotThrow(() -> serviceDAO.deleteDacAndDaas(dac), "Delete should not fail");
      List<Dataset> datasets = datasetDAO.findDatasetListByDacIds(List.of(dac.getDacId()));
      assertTrue(datasets.isEmpty());
    });
    datasetDAO.findAllDatasets().forEach(ds -> {
      assertNull(ds.getDacId(), "Dataset should not have a DAC");
      assertNull(ds.getDacApproval(), "Dataset should not have a DAC approval");
    });
  }

  /**
   * @return A list of random, unsaved dac objects
   */
  private List<Dac> createMockDACs() {
    DataAccessAgreement daa = new DataAccessAgreement();
    daa.setDaaId(1);
    return IntStream.range(0, 5).
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
