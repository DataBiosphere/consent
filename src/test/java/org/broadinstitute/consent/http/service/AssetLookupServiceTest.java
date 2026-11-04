package org.broadinstitute.consent.http.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.broadinstitute.consent.http.db.AssetCatalogDAO;
import org.broadinstitute.consent.http.db.DAOTestHelper;
import org.broadinstitute.consent.http.enumeration.AssetType;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.AssetIdentifier;
import org.broadinstitute.consent.http.models.AssetLookup;
import org.broadinstitute.consent.http.models.AssetLookup.Status;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.Study;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class AssetLookupServiceTest extends DAOTestHelper {

  private final User user = new User();
  private final User admin = new User();
  private DatasetService datasetService;
  private AssetCatalogDAO catalogDAO;
  private AssetLookupService service;

  @BeforeEach
  void setUpService() {
    admin.addRole(new UserRole(UserRoles.ADMIN.getRoleId(), UserRoles.ADMIN.getRoleName()));
    datasetService = mock(DatasetService.class);
    catalogDAO = jdbi.onDemand(AssetCatalogDAO.class);
    service = new AssetLookupService(jdbi, datasetService);
  }

  @Test
  void testALiveModelIsReturnedWithItsStudy() {
    Integer studyId = insertStudy();
    Integer modelId = insertStudyAssetRow("study_model", studyId, false);
    catalogDAO.insertAssetEntry(AssetType.MODEL, modelId);
    allowStudy(studyId);

    AssetLookup lookup = service.lookup(format(AssetType.MODEL, modelId), user);

    assertEquals(
        new AssetLookup(
            format(AssetType.MODEL, modelId),
            AssetType.MODEL,
            Status.LIVE,
            false,
            modelId,
            studyId),
        lookup);
  }

  @Test
  void testARetiredWorkspaceWithNoEntryIsReturnedFromItsRow() {
    Integer studyId = insertStudy();
    Integer workspaceId = insertStudyAssetRow("study_workspace", studyId, true);
    allowStudy(studyId);

    AssetLookup lookup = service.lookup(format(AssetType.WORKSPACE, workspaceId), user);

    assertEquals(Status.RETIRED, lookup.status());
    assertEquals(workspaceId, lookup.assetId());
  }

  @Test
  void testALiveAssetInAStudyTheCallerCannotReadIsNotFound() {
    Integer studyId = insertStudy();
    Integer modelId = insertStudyAssetRow("study_model", studyId, false);
    when(datasetService.requireReadableStudy(eq(studyId), any()))
        .thenThrow(new NotFoundException("Study not found"));

    assertAnswersAsUnknown(format(AssetType.MODEL, modelId));
  }

  @Test
  void testAModelIsListedOnlyOnceItsStudyHasAnIndexedDataset() {
    Integer studyId = insertStudy();
    Integer modelId = insertStudyAssetRow("study_model", studyId, false);
    Integer datasetId = insertDataset(studyId);
    allowStudy(studyId);
    String identifier = format(AssetType.MODEL, modelId);

    assertEquals(false, service.lookup(identifier, user).listed());
    executeSql(
        "UPDATE dataset SET indexed_date = now() WHERE dataset_id = %d".formatted(datasetId));
    assertEquals(true, service.lookup(identifier, user).listed());
  }

  @Test
  void testARemovedAssetReturnsOnlyItsTypeAndStatusAndOnlyToAnAdmin() {
    Integer studyId = insertStudy();
    Integer modelId = insertStudyAssetRow("study_model", studyId, false);
    catalogDAO.insertAssetEntry(AssetType.MODEL, modelId);
    catalogDAO.markRemovedByStudy(studyId);
    executeSql("DELETE FROM study_model WHERE model_id = %d".formatted(modelId));

    assertEquals(
        AssetLookup.removed(AssetType.MODEL),
        service.lookup(format(AssetType.MODEL, modelId), admin));
    assertAnswersAsUnknown(format(AssetType.MODEL, modelId));
  }

  @Test
  void testAnUnstampedOrphanIsRemoved() {
    int number = insertStudyAssetRow("study_biospecimen", insertStudy(), false) + 100;
    executeSql(
        "INSERT INTO asset_catalog (asset_type, asset_number) VALUES ('BIOSPECIMEN', %d)"
            .formatted(number));

    assertEquals(
        AssetLookup.removed(AssetType.BIOSPECIMEN),
        service.lookup(format(AssetType.BIOSPECIMEN, number), admin));
  }

  @Test
  void testADatasetResolvesUnderItsCurrentAliasAfterAnAdHocChange() {
    Integer studyId = insertStudy();
    Integer datasetId = insertDataset(studyId);
    int oldAlias = datasetAlias(datasetId);
    catalogDAO.insertDatasetEntry(datasetId);
    int newAlias = nextDatasetAlias() + 100;
    executeSql(
        "UPDATE dataset SET alias = %d WHERE dataset_id = %d".formatted(newAlias, datasetId));
    allowDataset(datasetId);

    AssetLookup byOld = service.lookup(format(AssetType.DATASET, oldAlias), user);
    AssetLookup byNew = service.lookup(format(AssetType.DATASET, newAlias), user);

    assertEquals(format(AssetType.DATASET, newAlias), byOld.identifier());
    assertEquals(datasetId, byOld.assetId());
    assertEquals(byOld, byNew);
  }

  @Test
  void testADatasetWithNoStudyResolves() {
    Integer datasetId = insertDataset(null);
    allowDataset(datasetId);

    AssetLookup lookup = service.lookup(format(AssetType.DATASET, datasetAlias(datasetId)), user);

    assertEquals(Status.LIVE, lookup.status());
    assertEquals(datasetId, lookup.assetId());
    assertEquals(null, lookup.studyId());
    assertEquals(false, lookup.listed());
  }

  @Test
  void testADatasetThatReusedARemovedEntrysAliasIsLive() {
    Integer removedId = insertDataset(null);
    int alias = datasetAlias(removedId);
    catalogDAO.insertDatasetEntry(removedId);
    catalogDAO.markRemovedByDataset(removedId);
    datasetDAO.deleteDatasetById(removedId);
    Integer reuserId = insertDataset(null);
    executeSql("UPDATE dataset SET alias = %d WHERE dataset_id = %d".formatted(alias, reuserId));
    allowDataset(reuserId);

    AssetLookup lookup = service.lookup(format(AssetType.DATASET, alias), user);

    assertEquals(Status.LIVE, lookup.status());
    assertEquals(reuserId, lookup.assetId());
  }

  @Test
  void testADatasetTheCallerCannotReadIsNotFound() {
    Integer datasetId = insertDataset(null);
    when(datasetService.findDatasetByIdForRead(any(), eq(datasetId)))
        .thenThrow(new ForbiddenException("User does not have permission"));

    assertAnswersAsUnknown(format(AssetType.DATASET, datasetAlias(datasetId)));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "DUOS-M-45",
        "DUOS-000001",
        "DUOS-M-999999",
        "duos-m-000045",
        "DUOS-D12",
        "DUOS-S12",
        "nonsense",
        ""
      })
  void testAnUnknownOrMalformedIdentifierIsNotFound(String identifier) {
    assertAnswersAsUnknown(identifier);
  }

  @Test
  void testALiveAssetWithNoEntryIsReturnedFromItsRow() {
    Integer studyId = insertStudy();
    Integer biospecimenId = insertStudyAssetRow("study_biospecimen", studyId, false);
    allowStudy(studyId);

    AssetLookup lookup = service.lookup(format(AssetType.BIOSPECIMEN, biospecimenId), user);

    assertEquals(Status.LIVE, lookup.status());
    assertEquals(biospecimenId, lookup.assetId());
  }

  /** The same exception and message as an unused identifier, so a lookup cannot probe. */
  private void assertAnswersAsUnknown(String identifier) {
    NotFoundException thrown =
        assertThrows(NotFoundException.class, () -> service.lookup(identifier, user));
    assertEquals(AssetLookupService.UNKNOWN_IDENTIFIER, thrown.getMessage());
  }

  private void allowStudy(Integer studyId) {
    when(datasetService.requireReadableStudy(eq(studyId), any())).thenReturn(new Study());
  }

  private void allowDataset(Integer datasetId) {
    when(datasetService.findDatasetByIdForRead(any(), eq(datasetId))).thenReturn(new Dataset());
  }

  private static String format(AssetType type, int number) {
    return AssetIdentifier.format(type, number);
  }

  private Integer insertDataset(Integer studyId) {
    Integer datasetId =
        datasetDAO.insertDataset(
            randomAlphabetic(20),
            Timestamp.from(Instant.now()),
            createUser().getUserId(),
            UUID.randomUUID().toString(),
            "{}",
            null);
    if (studyId != null) {
      executeSql(
          "UPDATE dataset SET study_id = %d WHERE dataset_id = %d".formatted(studyId, datasetId));
    }
    return datasetId;
  }

  private Integer insertStudy() {
    return studyDAO.insertStudy(
        randomAlphabetic(20),
        randomAlphabetic(20),
        randomAlphabetic(20),
        null,
        List.of(randomAlphabetic(10)),
        true,
        createUser().getUserId(),
        Instant.now(),
        UUID.randomUUID());
  }
}
