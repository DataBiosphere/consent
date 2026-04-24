package org.broadinstitute.consent.http.service.dao;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import org.broadinstitute.consent.http.db.DAOTestHelper;
import org.broadinstitute.consent.http.enumeration.AuditActions;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.DaaAudit;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DacDatasetExternalizationRequest;
import org.broadinstitute.consent.http.models.DacDatasetExternalizationResponse;
import org.broadinstitute.consent.http.models.DataAccessAgreement;
import org.broadinstitute.consent.http.models.DataUseBuilder;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.LibraryCard;
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
    // DAC Member User. When deleting the dac, users DAC role will be deleted but not their
    // Researcher role
    User member = createUser();
    userRoleDAO.insertSingleUserRole(UserRoles.RESEARCHER.getRoleId(), member.getUserId());
    dacs.forEach(
        ignored -> {
          // DAC
          int dacId =
              dacDAO.createDac(
                  "dac name: " + randomAlphabetic(10),
                  "dac description: " + randomAlphabetic(10),
                  "dac email: " + randomAlphabetic(10),
                  createUser().getUserId());
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
          libraryCardDAO.createLibraryCardDaaRelation(
              lcUser.getUserId(), superUser.getUserId(), userLcId, daaId);
          // User as a DAC Member. When deleting the dac, users DAC role will be deleted
          dacDAO.addDacMember(
              UserRoles.MEMBER.getRoleId(), member.getUserId(), dacId, superUser.getUserId());
          // DAC Chair User. When deleting the dac, this role will be deleted
          User chair = createUser();
          dacDAO.addDacMember(
              UserRoles.CHAIRPERSON.getRoleId(), chair.getUserId(), dacId, superUser.getUserId());
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
                  () -> serviceDAO.deleteDacAndRemoveDaaAssociation(superUser, dac),
                  "Delete should not fail");
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
              // DAA deletion is not allowed even when deleting a DAC
              DataAccessAgreement daa = daaDAO.findByDacId(dac.getDacId());
              assertNotNull(daa);
              // Assert that there are DAA audit records for this dac deletion
              List<DaaAudit> daaAudits = daaDAO.findAuditsByDaaId(daa.getDaaId());
              assertFalse(daaAudits.isEmpty());
              assertTrue(daaAudits.stream().anyMatch(a -> a.action().equals(AuditActions.REMOVE)));
              // Assert that created library cards are not deleted and that their association to the
              // DAA is not removed
              List<LibraryCard> libraryCards =
                  libraryCardDAO.findAllLibraryCards().stream()
                      .filter(lc -> lc.getDaaIds().contains(daa.getDaaId()))
                      .toList();
              assertFalse(libraryCards.isEmpty());
            });
    createdDatasetIds.forEach(
        id -> {
          Dataset ds = datasetDAO.findDatasetById(id);
          assertNull(ds.getDacId(), "Dataset should not have a DAC");
          assertNull(ds.getDacApproval(), "Dataset should not have a DAC approval");
        });
    // Assert that Member's Researcher role still exists, but their Member role does not
    User updatedMember = userDAO.findUserById(member.getUserId());
    assertNotNull(updatedMember);
    assertTrue(
        updatedMember.getRoles().stream()
            .anyMatch(r -> r.getRoleId().equals(UserRoles.RESEARCHER.getRoleId())));
    assertFalse(
        updatedMember.getRoles().stream()
            .anyMatch(r -> r.getRoleId().equals(UserRoles.MEMBER.getRoleId())));
  }

  @Test
  void testDeleteDac_nullDAC() {
    User superUser = createUser();
    assertThrows(
        IllegalArgumentException.class,
        () -> serviceDAO.deleteDacAndRemoveDaaAssociation(superUser, null),
        "Should throw IllegalArgumentException when DAC is null");
  }

  @Test
  void testDeleteDac_nullDAA() {
    User superUser = createUser();
    int dacId =
        dacDAO.createDac(
            "dac name: " + randomAlphabetic(10),
            "dac description: " + randomAlphabetic(10),
            "dac email: " + randomAlphabetic(10),
            superUser.getUserId());
    Dac dac = dacDAO.findById(dacId);
    try {
      serviceDAO.deleteDacAndRemoveDaaAssociation(superUser, dac);
      List<DaaAudit> daaAudits = daaDAO.findAllDaaAudits();
      assertTrue(daaAudits.isEmpty(), "There should be no DaaAudits in a DAC delete operation");
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }

  // Fixture record and builder shared by the three focused externalization tests below.
  private record ExternalizationTestFixture(
      Integer dacId,
      Integer controlledDatasetId,
      Integer openDatasetId,
      String controlledDatasetObjectId,
      String openDatasetObjectId,
      String referenceId,
      String openReferenceId,
      Date controlledDarUpdatedAt,
      Date openDarUpdatedAt,
      String existingAdminNote,
      Integer controlledElectionId,
      Integer openElectionId,
      User admin,
      User darOwner,
      User openDarOwner,
      DacDatasetExternalizationResponse response) {}

  private ExternalizationTestFixture buildExternalizationFixture() {
    User admin = createUser();
    User darOwner = createUser();
    User openDarOwner = createUser();
    Integer dacId =
        dacDAO.createDac(
            "dac name: " + randomAlphabetic(10),
            "dac description: " + randomAlphabetic(10),
            "dac email: " + randomAlphabetic(10),
            admin.getUserId());
    String controlledDatasetObjectId = "controlled-" + randomAlphabetic(10);
    String openDatasetObjectId = "open-" + randomAlphabetic(10);
    Integer accessManagementKeyId =
        datasetDAO.getDictionaryTerms().stream().map(d -> d.getKeyId()).findFirst().orElseThrow();
    Integer datasetId =
        datasetDAO.insertDataset(
            "dataset name: " + randomAlphabetic(10),
            Timestamp.from(Instant.now()),
            admin.getUserId(),
            controlledDatasetObjectId,
            new DataUseBuilder().setGeneralUse(true).build().toString(),
            dacId);
    Integer openDatasetId =
        datasetDAO.insertDataset(
            "dataset name: " + randomAlphabetic(10),
            Timestamp.from(Instant.now()),
            admin.getUserId(),
            openDatasetObjectId,
            new DataUseBuilder().setGeneralUse(true).build().toString(),
            dacId);
    jdbi.useHandle(
        handle ->
            handle
                .createUpdate(
                    """
                        INSERT INTO dataset_property(dataset_id, property_key, schema_property, property_value, property_type, create_date)
                        VALUES (:datasetId, :propertyKey, 'accessManagement', 'controlled', 'string', now())
                        """)
                .bind("datasetId", datasetId)
                .bind("propertyKey", accessManagementKeyId)
                .execute());
    jdbi.useHandle(
        handle ->
            handle
                .createUpdate(
                    """
                        INSERT INTO dataset_property(dataset_id, property_key, schema_property, property_value, property_type, create_date)
                        VALUES (:datasetId, :propertyKey, 'accessManagement', 'open', 'string', now())
                        """)
                .bind("datasetId", openDatasetId)
                .bind("propertyKey", accessManagementKeyId)
                .execute());
    datasetDAO.updateDatasetApproval(true, Instant.now(), admin.getUserId(), datasetId);
    datasetDAO.updateDatasetApproval(true, Instant.now(), admin.getUserId(), openDatasetId);
    String referenceId = randomAlphabetic(8);
    String openReferenceId = randomAlphabetic(8);
    Date controlledDarCreatedAt = Date.from(Instant.now().minusSeconds(7200));
    Date controlledDarUpdatedAt = Date.from(Instant.now().minusSeconds(3600));
    Date openDarCreatedAt = Date.from(Instant.now().minusSeconds(5400));
    Date openDarUpdatedAt = Date.from(Instant.now().minusSeconds(1800));
    dataAccessRequestDAO.insertDraftDataAccessRequest(
        referenceId,
        darOwner.getUserId(),
        controlledDarCreatedAt,
        controlledDarUpdatedAt,
        new org.broadinstitute.consent.http.models.DataAccessRequestData());
    dataAccessRequestDAO.insertDraftDataAccessRequest(
        openReferenceId,
        openDarOwner.getUserId(),
        openDarCreatedAt,
        openDarUpdatedAt,
        new org.broadinstitute.consent.http.models.DataAccessRequestData());
    dataAccessRequestDAO.insertDARDatasetRelation(referenceId, datasetId);
    dataAccessRequestDAO.insertDARDatasetRelation(openReferenceId, openDatasetId);
    String existingAdminNote = "Existing administrative note";
    jdbi.useHandle(
        handle ->
            handle
                .createUpdate(
                    "UPDATE data_access_request SET admin_dar_notes = :note WHERE reference_id = :referenceId")
                .bind("note", existingAdminNote)
                .bind("referenceId", referenceId)
                .execute());
    Integer controlledElectionId =
        electionDAO.insertElection("DataAccess", "Open", new Date(), referenceId, datasetId);
    Integer openElectionId =
        electionDAO.insertElection(
            "DataAccess", "Open", new Date(), openReferenceId, openDatasetId);
    DacDatasetExternalizationResponse response =
        serviceDAO.convertDacDatasetsToExternal(
            dacId,
            admin.getUserId(),
            new DacDatasetExternalizationRequest("policy update", false, true, true, null));
    return new ExternalizationTestFixture(
        dacId,
        datasetId,
        openDatasetId,
        controlledDatasetObjectId,
        openDatasetObjectId,
        referenceId,
        openReferenceId,
        controlledDarUpdatedAt,
        openDarUpdatedAt,
        existingAdminNote,
        controlledElectionId,
        openElectionId,
        admin,
        darOwner,
        openDarOwner,
        response);
  }

  @Test
  void testConvertDacDatasetsToExternal() {
    ExternalizationTestFixture f = buildExternalizationFixture();

    assertEquals(2, f.response().datasetsTotalInDac());
    assertEquals(1, f.response().datasetsConvertedToExternal());
    assertEquals(1, f.response().darDatasetApprovalsRevoked());
    assertEquals(1, f.response().openElectionsCanceled());

    String controlledAccessManagement =
        jdbi.withHandle(
            handle ->
                handle
                    .createQuery(
                        "SELECT property_value FROM dataset_property WHERE dataset_id = :datasetId AND schema_property = 'accessManagement'")
                    .bind("datasetId", f.controlledDatasetId())
                    .mapTo(String.class)
                    .one());
    assertEquals("external", controlledAccessManagement);

    String openAccessManagement =
        jdbi.withHandle(
            handle ->
                handle
                    .createQuery(
                        "SELECT property_value FROM dataset_property WHERE dataset_id = :datasetId AND schema_property = 'accessManagement'")
                    .bind("datasetId", f.openDatasetId())
                    .mapTo(String.class)
                    .one());
    assertEquals("open", openAccessManagement);

    int remainingControlledRelations =
        jdbi.withHandle(
            handle ->
                handle
                    .createQuery(
                        "SELECT COUNT(*) FROM dar_dataset WHERE reference_id = :ref AND dataset_id = :ds")
                    .bind("ref", f.referenceId())
                    .bind("ds", f.controlledDatasetId())
                    .mapTo(Integer.class)
                    .one());
    assertEquals(0, remainingControlledRelations);

    int remainingOpenRelations =
        jdbi.withHandle(
            handle ->
                handle
                    .createQuery(
                        "SELECT COUNT(*) FROM dar_dataset WHERE reference_id = :ref AND dataset_id = :ds")
                    .bind("ref", f.openReferenceId())
                    .bind("ds", f.openDatasetId())
                    .mapTo(Integer.class)
                    .one());
    assertEquals(1, remainingOpenRelations);

    assertEquals("Canceled", electionDAO.findElectionById(f.controlledElectionId()).getStatus());
    assertEquals("Open", electionDAO.findElectionById(f.openElectionId()).getStatus());

    assertNull(datasetDAO.findDatasetById(f.controlledDatasetId()).getDacId());
    assertNull(datasetDAO.findDatasetById(f.controlledDatasetId()).getDacApproval());
    Timestamp dacApprovalDate =
        jdbi.withHandle(
            handle ->
                handle
                    .createQuery(
                        "SELECT dac_approval_date FROM dataset WHERE dataset_id = :datasetId")
                    .bind("datasetId", f.controlledDatasetId())
                    .mapTo(Timestamp.class)
                    .one());
    assertNull(dacApprovalDate);
    Integer updateUserId =
        jdbi.withHandle(
            handle ->
                handle
                    .createQuery("SELECT update_user_id FROM dataset WHERE dataset_id = :datasetId")
                    .bind("datasetId", f.controlledDatasetId())
                    .mapTo(Integer.class)
                    .one());
    assertEquals(f.admin().getUserId(), updateUserId);
    assertNotNull(
        jdbi.withHandle(
            handle ->
                handle
                    .createQuery("SELECT update_date FROM dataset WHERE dataset_id = :datasetId")
                    .bind("datasetId", f.controlledDatasetId())
                    .mapTo(Timestamp.class)
                    .one()));
  }

  @Test
  void testConvertDacDatasetsToExternal_controlledDarUpdated() {
    ExternalizationTestFixture f = buildExternalizationFixture();

    Integer controlledDarUserId =
        jdbi.withHandle(
            handle ->
                handle
                    .createQuery(
                        "SELECT user_id FROM data_access_request WHERE reference_id = :referenceId")
                    .bind("referenceId", f.referenceId())
                    .mapTo(Integer.class)
                    .one());
    Timestamp controlledDarUpdateDate =
        jdbi.withHandle(
            handle ->
                handle
                    .createQuery(
                        "SELECT update_date FROM data_access_request WHERE reference_id = :referenceId")
                    .bind("referenceId", f.referenceId())
                    .mapTo(Timestamp.class)
                    .one());
    String controlledDarAdminNotes =
        jdbi.withHandle(
            handle ->
                handle
                    .createQuery(
                        "SELECT admin_dar_notes FROM data_access_request WHERE reference_id = :referenceId")
                    .bind("referenceId", f.referenceId())
                    .mapTo(String.class)
                    .one());

    assertEquals(f.darOwner().getUserId(), controlledDarUserId);
    assertNotNull(controlledDarUpdateDate);
    assertTrue(controlledDarUpdateDate.toInstant().isAfter(f.controlledDarUpdatedAt().toInstant()));
    assertTrue(controlledDarAdminNotes.startsWith(f.existingAdminNote() + " On "));
    // Dataset identifier should be in DUOS-XXXXXX form when alias is available, or object ID
    // otherwise
    assertTrue(
        controlledDarAdminNotes.contains("DUOS-")
            || controlledDarAdminNotes.contains(f.controlledDatasetObjectId()),
        "Admin notes should contain dataset identifier in DUOS-XXXXXX or object ID format");
    assertFalse(controlledDarAdminNotes.contains(f.openDatasetObjectId()));
    assertTrue(
        controlledDarAdminNotes.contains(
            "the following datasets were removed administratively from this request because the responsible Data Access Committee no longer manages access using DUOS."));
  }

  @Test
  void testConvertDacDatasetsToExternal_openDatasetUnchanged() {
    ExternalizationTestFixture f = buildExternalizationFixture();

    assertNotNull(datasetDAO.findDatasetById(f.openDatasetId()).getDacId());
    assertNotNull(datasetDAO.findDatasetById(f.openDatasetId()).getDacApproval());
    Timestamp openDacApprovalDate =
        jdbi.withHandle(
            handle ->
                handle
                    .createQuery(
                        "SELECT dac_approval_date FROM dataset WHERE dataset_id = :datasetId")
                    .bind("datasetId", f.openDatasetId())
                    .mapTo(Timestamp.class)
                    .one());
    assertNotNull(openDacApprovalDate);
    Integer openDarUserId =
        jdbi.withHandle(
            handle ->
                handle
                    .createQuery(
                        "SELECT user_id FROM data_access_request WHERE reference_id = :referenceId")
                    .bind("referenceId", f.openReferenceId())
                    .mapTo(Integer.class)
                    .one());
    Timestamp openDarUpdateDate =
        jdbi.withHandle(
            handle ->
                handle
                    .createQuery(
                        "SELECT update_date FROM data_access_request WHERE reference_id = :referenceId")
                    .bind("referenceId", f.openReferenceId())
                    .mapTo(Timestamp.class)
                    .one());
    String openDarAdminNotes =
        jdbi.withHandle(
            handle ->
                handle
                    .createQuery(
                        "SELECT admin_dar_notes FROM data_access_request WHERE reference_id = :referenceId")
                    .bind("referenceId", f.openReferenceId())
                    .mapTo(String.class)
                    .one());
    assertEquals(f.openDarOwner().getUserId(), openDarUserId);
    assertEquals(f.openDarUpdatedAt().toInstant(), openDarUpdateDate.toInstant());
    assertNull(openDarAdminNotes);
  }

  @Test
  void testConvertDacDatasetsToExternalIncludesOpenWhenRequested() {
    User admin = createUser();
    User controlledDarOwner = createUser();
    User openDarOwner = createUser();
    Integer dacId =
        dacDAO.createDac(
            "dac name: " + randomAlphabetic(10),
            "dac description: " + randomAlphabetic(10),
            "dac email: " + randomAlphabetic(10),
            admin.getUserId());
    String controlledDatasetObjectId = "controlled-" + randomAlphabetic(10);
    String openDatasetObjectId = "open-" + randomAlphabetic(10);
    Integer controlledDatasetId =
        datasetDAO.insertDataset(
            "dataset name: " + randomAlphabetic(10),
            Timestamp.from(Instant.now()),
            admin.getUserId(),
            controlledDatasetObjectId,
            new DataUseBuilder().setGeneralUse(true).build().toString(),
            dacId);
    Integer openDatasetId =
        datasetDAO.insertDataset(
            "dataset name: " + randomAlphabetic(10),
            Timestamp.from(Instant.now()),
            admin.getUserId(),
            openDatasetObjectId,
            new DataUseBuilder().setGeneralUse(true).build().toString(),
            dacId);

    Integer accessManagementKeyId =
        datasetDAO.getDictionaryTerms().stream().map(d -> d.getKeyId()).findFirst().orElseThrow();
    jdbi.useHandle(
        handle ->
            handle
                .createUpdate(
                    """
                        INSERT INTO dataset_property(dataset_id, property_key, schema_property, property_value, property_type, create_date)
                        VALUES (:datasetId, :propertyKey, :schemaProperty, :propertyValue, 'string', now())
                        """)
                .bind("datasetId", controlledDatasetId)
                .bind("propertyKey", accessManagementKeyId)
                .bind("schemaProperty", "accessManagement")
                .bind("propertyValue", "controlled")
                .execute());
    jdbi.useHandle(
        handle ->
            handle
                .createUpdate(
                    """
                        INSERT INTO dataset_property(dataset_id, property_key, schema_property, property_value, property_type, create_date)
                        VALUES (:datasetId, :propertyKey, :schemaProperty, :propertyValue, 'string', now())
                        """)
                .bind("datasetId", openDatasetId)
                .bind("propertyKey", accessManagementKeyId)
                .bind("schemaProperty", "accessManagement")
                .bind("propertyValue", "open")
                .execute());

    datasetDAO.updateDatasetApproval(true, Instant.now(), admin.getUserId(), controlledDatasetId);
    datasetDAO.updateDatasetApproval(true, Instant.now(), admin.getUserId(), openDatasetId);

    String controlledReferenceId = randomAlphabetic(8);
    String openReferenceId = randomAlphabetic(8);
    Date controlledDarCreatedAt = Date.from(Instant.now().minusSeconds(7200));
    Date controlledDarUpdatedAt = Date.from(Instant.now().minusSeconds(3600));
    Date openDarCreatedAt = Date.from(Instant.now().minusSeconds(5400));
    Date openDarUpdatedAt = Date.from(Instant.now().minusSeconds(1800));
    dataAccessRequestDAO.insertDraftDataAccessRequest(
        controlledReferenceId,
        controlledDarOwner.getUserId(),
        controlledDarCreatedAt,
        controlledDarUpdatedAt,
        new org.broadinstitute.consent.http.models.DataAccessRequestData());
    dataAccessRequestDAO.insertDraftDataAccessRequest(
        openReferenceId,
        openDarOwner.getUserId(),
        openDarCreatedAt,
        openDarUpdatedAt,
        new org.broadinstitute.consent.http.models.DataAccessRequestData());
    dataAccessRequestDAO.insertDARDatasetRelation(controlledReferenceId, controlledDatasetId);
    dataAccessRequestDAO.insertDARDatasetRelation(openReferenceId, openDatasetId);
    Integer controlledElectionId =
        electionDAO.insertElection(
            "DataAccess", "Open", new Date(), controlledReferenceId, controlledDatasetId);
    Integer openElectionId =
        electionDAO.insertElection(
            "DataAccess", "Open", new Date(), openReferenceId, openDatasetId);

    DacDatasetExternalizationResponse response =
        serviceDAO.convertDacDatasetsToExternal(
            dacId,
            admin.getUserId(),
            new DacDatasetExternalizationRequest("policy update", false, true, true, true));

    assertEquals(2, response.datasetsTotalInDac());
    assertEquals(2, response.datasetsConvertedToExternal());
    assertEquals(2, response.darDatasetApprovalsRevoked());
    assertEquals(2, response.openElectionsCanceled());

    Integer convertedCount =
        jdbi.withHandle(
            handle ->
                handle
                    .createQuery(
                        """
                            SELECT COUNT(*)
                            FROM dataset_property
                            WHERE dataset_id IN (<datasetIds>)
                              AND schema_property = 'accessManagement'
                              AND property_value = 'external'
                            """)
                    .bindList("datasetIds", List.of(controlledDatasetId, openDatasetId))
                    .mapTo(Integer.class)
                    .one());
    assertEquals(2, convertedCount);

    Integer remainingDarDatasetRows =
        jdbi.withHandle(
            handle ->
                handle
                    .createQuery(
                        "SELECT COUNT(*) FROM dar_dataset WHERE dataset_id IN (<datasetIds>)")
                    .bindList("datasetIds", List.of(controlledDatasetId, openDatasetId))
                    .mapTo(Integer.class)
                    .one());
    assertEquals(0, remainingDarDatasetRows);

    String controlledElectionStatus =
        electionDAO.findElectionById(controlledElectionId).getStatus();
    String openElectionStatus = electionDAO.findElectionById(openElectionId).getStatus();
    assertEquals("Canceled", controlledElectionStatus);
    assertEquals("Canceled", openElectionStatus);

    Integer controlledDatasetDacId = datasetDAO.findDatasetById(controlledDatasetId).getDacId();
    Boolean controlledDatasetDacApproval =
        datasetDAO.findDatasetById(controlledDatasetId).getDacApproval();
    Timestamp controlledDatasetDacApprovalDate =
        jdbi.withHandle(
            handle ->
                handle
                    .createQuery(
                        "SELECT dac_approval_date FROM dataset WHERE dataset_id = :datasetId")
                    .bind("datasetId", controlledDatasetId)
                    .mapTo(Timestamp.class)
                    .one());
    Integer controlledDatasetUpdateUserId =
        jdbi.withHandle(
            handle ->
                handle
                    .createQuery("SELECT update_user_id FROM dataset WHERE dataset_id = :datasetId")
                    .bind("datasetId", controlledDatasetId)
                    .mapTo(Integer.class)
                    .one());
    Timestamp controlledDatasetUpdateDate =
        jdbi.withHandle(
            handle ->
                handle
                    .createQuery("SELECT update_date FROM dataset WHERE dataset_id = :datasetId")
                    .bind("datasetId", controlledDatasetId)
                    .mapTo(Timestamp.class)
                    .one());
    assertNull(controlledDatasetDacId);
    assertNull(controlledDatasetDacApproval);
    assertNull(controlledDatasetDacApprovalDate);
    assertEquals(admin.getUserId(), controlledDatasetUpdateUserId);
    assertNotNull(controlledDatasetUpdateDate);

    Integer openDatasetDacId = datasetDAO.findDatasetById(openDatasetId).getDacId();
    Boolean openDatasetDacApproval = datasetDAO.findDatasetById(openDatasetId).getDacApproval();
    Timestamp openDatasetDacApprovalDate =
        jdbi.withHandle(
            handle ->
                handle
                    .createQuery(
                        "SELECT dac_approval_date FROM dataset WHERE dataset_id = :datasetId")
                    .bind("datasetId", openDatasetId)
                    .mapTo(Timestamp.class)
                    .one());
    Integer openDatasetUpdateUserId =
        jdbi.withHandle(
            handle ->
                handle
                    .createQuery("SELECT update_user_id FROM dataset WHERE dataset_id = :datasetId")
                    .bind("datasetId", openDatasetId)
                    .mapTo(Integer.class)
                    .one());
    Timestamp openDatasetUpdateDate =
        jdbi.withHandle(
            handle ->
                handle
                    .createQuery("SELECT update_date FROM dataset WHERE dataset_id = :datasetId")
                    .bind("datasetId", openDatasetId)
                    .mapTo(Timestamp.class)
                    .one());
    assertNull(openDatasetDacId);
    assertNull(openDatasetDacApproval);
    assertNull(openDatasetDacApprovalDate);
    assertEquals(admin.getUserId(), openDatasetUpdateUserId);
    assertNotNull(openDatasetUpdateDate);

    Integer controlledDarUserId =
        jdbi.withHandle(
            handle ->
                handle
                    .createQuery(
                        "SELECT user_id FROM data_access_request WHERE reference_id = :referenceId")
                    .bind("referenceId", controlledReferenceId)
                    .mapTo(Integer.class)
                    .one());
    Integer openDarUserId =
        jdbi.withHandle(
            handle ->
                handle
                    .createQuery(
                        "SELECT user_id FROM data_access_request WHERE reference_id = :referenceId")
                    .bind("referenceId", openReferenceId)
                    .mapTo(Integer.class)
                    .one());
    Timestamp controlledDarUpdateDate =
        jdbi.withHandle(
            handle ->
                handle
                    .createQuery(
                        "SELECT update_date FROM data_access_request WHERE reference_id = :referenceId")
                    .bind("referenceId", controlledReferenceId)
                    .mapTo(Timestamp.class)
                    .one());
    Timestamp openDarUpdateDate =
        jdbi.withHandle(
            handle ->
                handle
                    .createQuery(
                        "SELECT update_date FROM data_access_request WHERE reference_id = :referenceId")
                    .bind("referenceId", openReferenceId)
                    .mapTo(Timestamp.class)
                    .one());
    String controlledDarAdminNotes =
        jdbi.withHandle(
            handle ->
                handle
                    .createQuery(
                        "SELECT admin_dar_notes FROM data_access_request WHERE reference_id = :referenceId")
                    .bind("referenceId", controlledReferenceId)
                    .mapTo(String.class)
                    .one());
    String openDarAdminNotes =
        jdbi.withHandle(
            handle ->
                handle
                    .createQuery(
                        "SELECT admin_dar_notes FROM data_access_request WHERE reference_id = :referenceId")
                    .bind("referenceId", openReferenceId)
                    .mapTo(String.class)
                    .one());
    assertEquals(controlledDarOwner.getUserId(), controlledDarUserId);
    assertEquals(openDarOwner.getUserId(), openDarUserId);
    assertNotNull(controlledDarUpdateDate);
    assertNotNull(openDarUpdateDate);
    assertTrue(controlledDarUpdateDate.toInstant().isAfter(controlledDarUpdatedAt.toInstant()));
    assertTrue(openDarUpdateDate.toInstant().isAfter(openDarUpdatedAt.toInstant()));
    assertTrue(controlledDarAdminNotes.startsWith("On "));
    // Dataset identifier should be in formatted DUOS-XXXXXX form when alias is available, or object
    // ID otherwise
    assertTrue(
        controlledDarAdminNotes.contains("DUOS-")
            || controlledDarAdminNotes.contains(controlledDatasetObjectId),
        "Controlled dataset admin notes should contain dataset identifier");
    assertTrue(openDarAdminNotes.startsWith("On "));
    assertTrue(
        openDarAdminNotes.contains("DUOS-") || openDarAdminNotes.contains(openDatasetObjectId),
        "Open dataset admin notes should contain dataset identifier");
  }

  @Test
  void testConvertDacDatasetsToExternalAddsCommaSeparatedDatasetIdentifiersInDarNote() {
    User admin = createUser();
    User darOwner = createUser();
    Integer dacId =
        dacDAO.createDac(
            "dac name: " + randomAlphabetic(10),
            "dac description: " + randomAlphabetic(10),
            "dac email: " + randomAlphabetic(10),
            admin.getUserId());
    String datasetObjectIdA = "a-dataset-id";
    String datasetObjectIdB = "b-dataset-id";
    Integer datasetIdA =
        datasetDAO.insertDataset(
            "dataset name: " + randomAlphabetic(10),
            Timestamp.from(Instant.now()),
            admin.getUserId(),
            datasetObjectIdA,
            new DataUseBuilder().setGeneralUse(true).build().toString(),
            dacId);
    Integer datasetIdB =
        datasetDAO.insertDataset(
            "dataset name: " + randomAlphabetic(10),
            Timestamp.from(Instant.now()),
            admin.getUserId(),
            datasetObjectIdB,
            new DataUseBuilder().setGeneralUse(true).build().toString(),
            dacId);

    Integer accessManagementKeyId =
        datasetDAO.getDictionaryTerms().stream().map(d -> d.getKeyId()).findFirst().orElseThrow();
    jdbi.useHandle(
        handle ->
            handle
                .createUpdate(
                    """
                        INSERT INTO dataset_property(dataset_id, property_key, schema_property, property_value, property_type, create_date)
                        VALUES (:datasetId, :propertyKey, 'accessManagement', 'controlled', 'string', now())
                        """)
                .bind("datasetId", datasetIdA)
                .bind("propertyKey", accessManagementKeyId)
                .execute());
    jdbi.useHandle(
        handle ->
            handle
                .createUpdate(
                    """
                        INSERT INTO dataset_property(dataset_id, property_key, schema_property, property_value, property_type, create_date)
                        VALUES (:datasetId, :propertyKey, 'accessManagement', 'open', 'string', now())
                        """)
                .bind("datasetId", datasetIdB)
                .bind("propertyKey", accessManagementKeyId)
                .execute());

    String referenceId = randomAlphabetic(8);
    dataAccessRequestDAO.insertDraftDataAccessRequest(
        referenceId,
        darOwner.getUserId(),
        new Date(),
        new Date(),
        new org.broadinstitute.consent.http.models.DataAccessRequestData());
    dataAccessRequestDAO.insertDARDatasetRelation(referenceId, datasetIdA);
    dataAccessRequestDAO.insertDARDatasetRelation(referenceId, datasetIdB);

    serviceDAO.convertDacDatasetsToExternal(
        dacId,
        admin.getUserId(),
        new DacDatasetExternalizationRequest("policy update", false, true, false, true));

    String darAdminNotes =
        jdbi.withHandle(
            handle ->
                handle
                    .createQuery(
                        "SELECT admin_dar_notes FROM data_access_request WHERE reference_id = :referenceId")
                    .bind("referenceId", referenceId)
                    .mapTo(String.class)
                    .one());
    assertNotNull(darAdminNotes);
    // Datasets should be identified by their formatted identifiers (DUOS-XXXXXX) from alias when
    // available
    assertTrue(
        darAdminNotes.contains("DUOS-") || darAdminNotes.contains(datasetObjectIdA),
        "Admin notes should contain dataset identifiers");
  }

  @Test
  void testConvertDacDatasetsToExternalDryRunDoesNotMutate() {
    User admin = createUser();
    Integer dacId =
        dacDAO.createDac(
            "dac name: " + randomAlphabetic(10),
            "dac description: " + randomAlphabetic(10),
            "dac email: " + randomAlphabetic(10),
            admin.getUserId());

    Integer accessManagementKeyId =
        datasetDAO.getDictionaryTerms().stream().map(d -> d.getKeyId()).findFirst().orElseThrow();
    Integer datasetId =
        datasetDAO.insertDataset(
            "dataset name: " + randomAlphabetic(10),
            Timestamp.from(Instant.now()),
            admin.getUserId(),
            "obj-" + randomAlphabetic(10),
            new DataUseBuilder().setGeneralUse(true).build().toString(),
            dacId);
    jdbi.useHandle(
        handle ->
            handle
                .createUpdate(
                    """
                        INSERT INTO dataset_property(dataset_id, property_key, schema_property, property_value, property_type, create_date)
                        VALUES (:datasetId, :propertyKey, 'accessManagement', 'controlled', 'string', now())
                        """)
                .bind("datasetId", datasetId)
                .bind("propertyKey", accessManagementKeyId)
                .execute());

    String referenceId = randomAlphabetic(8);
    dataAccessRequestDAO.insertDraftDataAccessRequest(
        referenceId,
        admin.getUserId(),
        new Date(),
        new Date(),
        new org.broadinstitute.consent.http.models.DataAccessRequestData());
    dataAccessRequestDAO.insertDARDatasetRelation(referenceId, datasetId);

    DacDatasetExternalizationResponse response =
        serviceDAO.convertDacDatasetsToExternal(
            dacId,
            admin.getUserId(),
            new DacDatasetExternalizationRequest("policy update", true, true, true, null));

    assertTrue(response.dryRun());
    assertEquals(1, response.datasetsConvertedToExternal());
    assertEquals(1, response.darDatasetApprovalsRevoked());

    // Dry run must not mutate any data
    String accessManagementValue =
        jdbi.withHandle(
            handle ->
                handle
                    .createQuery(
                        "SELECT property_value FROM dataset_property WHERE dataset_id = :datasetId AND schema_property = 'accessManagement'")
                    .bind("datasetId", datasetId)
                    .mapTo(String.class)
                    .one());
    assertEquals("controlled", accessManagementValue, "Dry run must not update access management");

    Integer darDatasetRowCount =
        jdbi.withHandle(
            handle ->
                handle
                    .createQuery(
                        "SELECT COUNT(*) FROM dar_dataset WHERE reference_id = :referenceId AND dataset_id = :datasetId")
                    .bind("referenceId", referenceId)
                    .bind("datasetId", datasetId)
                    .mapTo(Integer.class)
                    .one());
    assertEquals(1, darDatasetRowCount, "Dry run must not delete dar_dataset rows");
  }

  @Test
  void testConvertDacDatasetsToExternalEmptyDac() {
    User admin = createUser();
    Integer dacId =
        dacDAO.createDac(
            "dac name: " + randomAlphabetic(10),
            "dac description: " + randomAlphabetic(10),
            "dac email: " + randomAlphabetic(10),
            admin.getUserId());

    DacDatasetExternalizationResponse response =
        serviceDAO.convertDacDatasetsToExternal(
            dacId,
            admin.getUserId(),
            new DacDatasetExternalizationRequest("policy update", false, true, true, null));

    assertEquals(0, response.datasetsTotalInDac());
    assertEquals(0, response.datasetsConvertedToExternal());
    assertEquals(0, response.darDatasetApprovalsRevoked());
    assertEquals(0, response.openElectionsCanceled());
  }

  @Test
  void testFindConvertibleDatasetIds_EmptyDac() {
    User admin = createUser();
    Integer dacId =
        dacDAO.createDac(
            "dac name: " + randomAlphabetic(10),
            "dac description: " + randomAlphabetic(10),
            "dac email: " + randomAlphabetic(10),
            admin.getUserId());

    List<Integer> result =
        serviceDAO.findConvertibleDatasetIds(
            dacId, new DacDatasetExternalizationRequest("policy update", false, true, true, null));

    assertTrue(result.isEmpty());
  }

  @Test
  void testFindConvertibleDatasetIds_WithControlledDataset() {
    User admin = createUser();
    Integer dacId =
        dacDAO.createDac(
            "dac name: " + randomAlphabetic(10),
            "dac description: " + randomAlphabetic(10),
            "dac email: " + randomAlphabetic(10),
            admin.getUserId());
    Integer accessManagementKeyId =
        datasetDAO.getDictionaryTerms().stream().map(d -> d.getKeyId()).findFirst().orElseThrow();
    Integer datasetId =
        datasetDAO.insertDataset(
            "dataset name: " + randomAlphabetic(10),
            Timestamp.from(Instant.now()),
            admin.getUserId(),
            "obj-" + randomAlphabetic(10),
            new DataUseBuilder().setGeneralUse(true).build().toString(),
            dacId);
    jdbi.useHandle(
        handle ->
            handle
                .createUpdate(
                    """
                        INSERT INTO dataset_property(dataset_id, property_key, schema_property, property_value, property_type, create_date)
                        VALUES (:datasetId, :propertyKey, 'accessManagement', 'controlled', 'string', now())
                        """)
                .bind("datasetId", datasetId)
                .bind("propertyKey", accessManagementKeyId)
                .execute());

    List<Integer> result =
        serviceDAO.findConvertibleDatasetIds(
            dacId, new DacDatasetExternalizationRequest("policy update", false, true, true, null));

    assertEquals(1, result.size());
    assertEquals(datasetId, result.get(0));
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
