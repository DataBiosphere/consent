package org.broadinstitute.consent.http.service.dao;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import org.broadinstitute.consent.http.db.DAOTestHelper;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DataAccessAgreement;
import org.broadinstitute.consent.http.models.DataUseBuilder;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.rules.DACAutomationRule;
import org.broadinstitute.consent.http.rules.RuleState;
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
    //  * DatasetAutomationRules associated to the DAC
    List<Dac> dacs = createMockDACs();
    List<Integer> createdDatasetIds = new ArrayList<>();
    dacs.forEach(
        _ -> {
          // DAC
          int dacId =
              dacDAO.createDac(
                  "dac name: " + randomAlphabetic(10),
                  "dac description: " + randomAlphabetic(10),
                  "dac email: " + randomAlphabetic(10),
                  new Date());
          // Data Access Agreement
          int daaId =
              daaDAO.createDaa(
                  superUser.getUserId(),
                  new Date().toInstant(),
                  superUser.getUserId(),
                  new Date().toInstant(),
                  dacId);
          // DAC->DAA Association.
          daaDAO.createDacDaaRelation(dacId, daaId, superUser.getUserId());
          // Library Card User
          User lcUser = createUser();
          // A user's library card needs an institution
          int dunsNumber = randomInt(10, 100);
          institutionDAO.insertInstitution(
              "institution name: " + randomAlphabetic(10),
              "it director name: " + randomAlphabetic(10),
              "it director email: " + randomAlphabetic(10),
              "institution url: " + randomAlphabetic(10),
              dunsNumber,
              "org chart url: " + randomAlphabetic(10),
              "verification url: " + randomAlphabetic(10),
              "verification file name: " + randomAlphabetic(10),
              "org type: " + randomAlphabetic(10),
              superUser.getUserId(),
              new Date());
          int userLcId =
              libraryCardDAO.insertLibraryCard(
                  lcUser.getUserId(),
                  "library card user name: " + randomAlphabetic(10),
                  "library card user email: " + randomAlphabetic(10),
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
          // Dataset associated to the DAC. The Dataset will become dissociated from the deleted
          // DAC.
          int datasetId =
              datasetDAO.insertDataset(
                  "dataset name: " + randomAlphabetic(10),
                  Timestamp.from(Instant.now()),
                  superUser.getUserId(),
                  "object id: " + randomAlphabetic(10),
                  new DataUseBuilder().setGeneralUse(true).build().toString(),
                  dacId);
          createdDatasetIds.add(datasetId);
          datasetDAO.updateDatasetDacId(datasetId, dacId);
          Optional<DACAutomationRule> activeAutomation =
              dacAutomationRuleDAO.findAllDACAutomationRulesByDACId(dacId).stream()
                  .filter(r -> r.ruleState() == RuleState.AVAILABLE)
                  .findFirst();
          assertTrue(activeAutomation.isPresent());
          dacAutomationRuleDAO.auditedInsertDACRuleSetting(
              dacId, activeAutomation.get().id(), chair.getUserId(), Instant.now());
        });
    dacDAO
        .findAll()
        .forEach(
            dac -> {
              assertDoesNotThrow(
                  () -> serviceDAO.deleteDacAndDaas(superUser, dac), "Delete should not fail");
              List<DACAutomationRule> rules =
                  dacAutomationRuleDAO.findAllDACAutomationRulesByDACId(dac.getDacId()).stream()
                      .filter(r -> r.enabledByUserId() != null)
                      .toList();
              assertTrue(
                  rules.isEmpty(), "There should be no dac automation rules enabled by users.");
              List<Dataset> datasets = datasetDAO.findDatasetListByDacIds(List.of(dac.getDacId()));
              assertTrue(datasets.isEmpty());
              List<User> members = dacDAO.findMembersByDacId(dac.getDacId());
              assertTrue(members.isEmpty());
              DataAccessAgreement daa = daaDAO.findByDacId(dac.getDacId());
              assertNull(daa);
              // Assert that there are no DAAs that reference this DAC
              daaDAO
                  .findAll()
                  .forEach(
                      d -> {
                        List<Integer> daaDacIds = d.getDacs().stream().map(Dac::getDacId).toList();
                        assertFalse(
                            daaDacIds.contains(dac.getDacId()),
                            "There should be no DAAs that have DACs matching this deleted Dac ID");
                      });
              // Assert that there are no Library Cards with DAAs that reference this DAC
              libraryCardDAO
                  .findAllLibraryCards()
                  .forEach(
                      lc -> {
                        List<Integer> daaIds = lc.getDaaIds();
                        if (!daaIds.isEmpty()) {
                          daaIds.forEach(
                              daaId -> {
                                DataAccessAgreement innerDaa = daaDAO.findById(daaId);
                                List<Integer> innerDacIds =
                                    innerDaa.getDacs().stream().map(Dac::getDacId).toList();
                                assertFalse(
                                    innerDacIds.contains(dac.getDacId()),
                                    "There should be no Library Cards with DAAs that have DACs matching this deleted Dac ID");
                              });
                        }
                      });
            });
    createdDatasetIds.forEach(
        id -> {
          Dataset ds = datasetDAO.findDatasetById(id);
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
    return IntStream.range(0, 5)
        .mapToObj(
            i -> {
              Dac dac = new Dac();
              dac.setDacId(i);
              dac.setDescription("Dac " + i);
              dac.setName("Dac " + i);
              dac.setAssociatedDaa(daa);
              return dac;
            })
        .toList();
  }
}
