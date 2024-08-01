package org.broadinstitute.consent.http.service.dao;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import org.broadinstitute.consent.http.models.DataUseBuilder;
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
    // Create DACs and all associated objects subject to update based on DAC deletion:
    //  * DAC
    //  * Data Access Agreement
    //  * User with:
    //    * Library Card
    //    * Institution
    //  * DAC Member and Chairperson
    //  * Dataset associated to the DAC
    List<Dac> dacs = createMockDACs();
    dacs.forEach(dac -> {
      // DAC
      int dacId = dacDAO.createDac(
          "dac name: " + RandomStringUtils.randomAlphabetic(10),
          "dac description: " + RandomStringUtils.randomAlphabetic(10),
          "dac email: " + RandomStringUtils.randomAlphabetic(10),
          new Date());
      // Data Access Agreement
      int daaId = daaDAO.createDaa(superUser.getUserId(), new Date().toInstant(), superUser.getUserId(), new Date().toInstant(), dacId);
      // DAC->DAA Association.
      daaDAO.createDacDaaRelation(dacId, daaId);
      // Library Card User
      User lcUser = createUser();
      // A user's library card needs an institution
      int dunsNumber = RandomUtils.nextInt(10, 100);
      int userInstitutionId = institutionDAO.insertInstitution(
          "institution name: " + RandomStringUtils.randomAlphabetic(10),
          "it director name: " + RandomStringUtils.randomAlphabetic(10),
          "it director email: " + RandomStringUtils.randomAlphabetic(10),
          "institution url: " + RandomStringUtils.randomAlphabetic(10),
          dunsNumber,
          "org chart url: " + RandomStringUtils.randomAlphabetic(10),
          "verification url: " + RandomStringUtils.randomAlphabetic(10),
          "verification file name: " + RandomStringUtils.randomAlphabetic(10),
          "org type: " + RandomStringUtils.randomAlphabetic(10),
          superUser.getUserId(),
          new Date());
      int userLcId = libraryCardDAO.insertLibraryCard(
          lcUser.getUserId(),
          userInstitutionId,
          "era commons id: " + RandomStringUtils.randomAlphabetic(10),
          "library card user name: " + RandomStringUtils.randomAlphabetic(10),
          "library card user email: " + RandomStringUtils.randomAlphabetic(10),
          superUser.getUserId(),
          new Date());
      // Library Card User to Data Access Agreement association
      libraryCardDAO.createLibraryCardDaaRelation(userLcId, daaId);
      // DAC Member User. When deleting the dac, this role will be deleted
      User member = createUser();
      userRoleDAO.insertSingleUserRole(UserRoles.MEMBER.getRoleId(), member.getUserId());
      // DAC Chair User. When deleting the dac, this role will be deleted
      User chair = createUser();
      userRoleDAO.insertSingleUserRole(UserRoles.CHAIRPERSON.getRoleId(), chair.getUserId());
      // Dataset associated to the DAC. The Dataset will become dissociated from the deleted DAC.
      int datasetId = datasetDAO.insertDataset(
          "dataset name: " + RandomStringUtils.randomAlphabetic(10),
          Timestamp.from(Instant.now()),
          superUser.getUserId(),
          "object id: " + RandomStringUtils.randomAlphabetic(10),
          new DataUseBuilder().setGeneralUse(true).build().toString(),
          dacId);
      datasetDAO.updateDatasetDacId(datasetId, dacId);
    });
    dacDAO.findAll().forEach(dac -> {
      assertDoesNotThrow(() -> serviceDAO.deleteDacAndDaas(dac), "Delete should not fail");
      List<Dataset> datasets = datasetDAO.findDatasetListByDacIds(List.of(dac.getDacId()));
      assertTrue(datasets.isEmpty());
      List<User> members = dacDAO.findMembersByDacId(dac.getDacId());
      assertTrue(members.isEmpty());
      DataAccessAgreement daa = daaDAO.findByDacId(dac.getDacId());
      assertNull(daa);
      // Assert that there are no DAAs that reference this DAC
      daaDAO.findAll().forEach(d -> {
        List<Integer> daaDacIds = d.getDacs().stream().map(Dac::getDacId).toList();
        assertFalse(daaDacIds.contains(dac.getDacId()), "There should be no DAAs that have DACs matching this deleted Dac ID");
      });
      // Assert that there are no Library Cards with DAAs that reference this DAC
      libraryCardDAO.findAllLibraryCards().forEach(lc -> {
        List<Integer> daaIds = lc.getDaaIds();
        if (!daaIds.isEmpty()) {
          daaIds.forEach(daaId -> {
            DataAccessAgreement innerDaa = daaDAO.findById(daaId);
            List<Integer> innerDacIds = innerDaa.getDacs().stream().map(Dac::getDacId).toList();
            assertFalse(innerDacIds.contains(dac.getDacId()), "There should be no Library Cards with DAAs that have DACs matching this deleted Dac ID");
          });
        }
      });
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
