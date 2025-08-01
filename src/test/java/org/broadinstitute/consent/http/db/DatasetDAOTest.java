package org.broadinstitute.consent.http.db;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;
import org.broadinstitute.consent.http.enumeration.ElectionStatus;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.enumeration.FileCategory;
import org.broadinstitute.consent.http.enumeration.PropertyType;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.enumeration.VoteType;
import org.broadinstitute.consent.http.models.ApprovedDataset;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DarCollection;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.models.DataUseBuilder;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.DatasetAudit;
import org.broadinstitute.consent.http.models.DatasetProperty;
import org.broadinstitute.consent.http.models.DatasetStudySummary;
import org.broadinstitute.consent.http.models.DatasetSummary;
import org.broadinstitute.consent.http.models.Dictionary;
import org.broadinstitute.consent.http.models.FileStorageObject;
import org.broadinstitute.consent.http.models.Study;
import org.broadinstitute.consent.http.models.User;
import org.jdbi.v3.core.statement.Update;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DatasetDAOTest extends DAOTestHelper {

  @Test
  void testFindAllDatasetStudySummariesDatasetAndStudy() {
    Dataset dataset = insertDataset();
    Study study = insertStudyWithProperties();
    datasetDAO.updateStudyId(dataset.getDatasetId(), study.getStudyId());

    List<DatasetStudySummary> summaries = datasetDAO.findAllDatasetStudySummaries();
    assertThat(summaries, hasSize(1));
    assertEquals(dataset.getDatasetId(), summaries.get(0).dataset_id());
    assertEquals(study.getStudyId(), summaries.get(0).study_id());
  }

  @Test
  void testFindAllDatasetStudySummariesDatasetOnly() {
    Dataset dataset = insertDataset();

    List<DatasetStudySummary> summaries = datasetDAO.findAllDatasetStudySummaries();
    assertThat(summaries, hasSize(1));
    assertEquals(dataset.getDatasetId(), summaries.get(0).dataset_id());
    assertNull(summaries.get(0).study_id());
  }

  @Test
  void testFindDatasetByIdWithDacAndConsent() {
    Dataset dataset = insertDataset();
    Dac dac = insertDac();
    datasetDAO.updateDatasetDacId(dataset.getDatasetId(), dac.getDacId());

    Dataset foundDataset = datasetDAO.findDatasetById(dataset.getDatasetId());
    assertNotNull(foundDataset);
    assertEquals(dac.getDacId(), foundDataset.getDacId());
    assertFalse(foundDataset.getProperties().isEmpty());
    assertTrue(foundDataset.getDeletable());
    assertNotNull(foundDataset.getCreateUser());
  }

  @Test
  void testFindDatasetByIdWithDacAndConsentNotDeletable() {
    User user = createUser();
    Dataset d1 = insertDataset();
    Dataset d2 = insertDataset();
    Dac dac = insertDac();
    // Create a collection that references the created datasets
    createDarCollectionWithDatasets(dac.getDacId(), user, List.of(d1, d2));

    Dataset foundDataset = datasetDAO.findDatasetById(d1.getDatasetId());
    assertNotNull(foundDataset);
    assertEquals(dac.getDacId(), foundDataset.getDacId());
    assertFalse(foundDataset.getProperties().isEmpty());
    assertFalse(foundDataset.getDeletable());

    Dataset foundDataset2 = datasetDAO.findDatasetById(d2.getDatasetId());
    assertNotNull(foundDataset2);
    assertEquals(dac.getDacId(), foundDataset2.getDacId());
    assertFalse(foundDataset2.getProperties().isEmpty());
    assertFalse(foundDataset2.getDeletable());
  }

  @Test
  void testTranslatedDataUse() {
    Dataset d1 = insertDataset();

    String tdu = randomAlphabetic(10);
    datasetDAO.updateDatasetTranslatedDataUse(d1.getDatasetId(), tdu);

    d1 = datasetDAO.findDatasetById(d1.getDatasetId());

    assertEquals(tdu, d1.getTranslatedDataUse());
  }

  @Test
  void testUpdateDatasetName() {
    Dataset dataset = insertDataset();
    String newName = randomAlphabetic(25);
    datasetDAO.updateDatasetName(dataset.getDatasetId(), newName);
    Dataset foundDataset = datasetDAO.findDatasetById(dataset.getDatasetId());
    assertNotNull(foundDataset);
    assertEquals(newName, foundDataset.getName());
  }

  @Test
  void testFindDatasetByAlias() {
    Dataset dataset = insertDataset();

    Dataset foundDataset = datasetDAO.findDatasetByAlias(dataset.getAlias());

    assertNotNull(foundDataset);
    assertEquals(dataset.getDatasetId(), foundDataset.getDatasetId());
  }

  @Test
  void testFindDatasetsByAlias() {
    Dataset dataset1 = insertDataset();
    Dataset dataset2 = insertDataset();

    List<Dataset> foundDatasets = datasetDAO.findDatasetsByAlias(
        List.of(dataset1.getAlias(), dataset2.getAlias()));
    List<Integer> foundDatasetIds = foundDatasets.stream().map(Dataset::getDatasetId).toList();
    assertNotNull(foundDatasets);
    assertTrue(
        foundDatasetIds.containsAll(List.of(dataset1.getDatasetId(), dataset2.getDatasetId())));
  }

  @Test
  void testGetNIHInstitutionalFile() {
    Dataset dataset = insertDataset();

    // create unrelated file with the same id as dataset id but different category, timestamp before
    createFileStorageObject(
        dataset.getDatasetId().toString(),
        FileCategory.ALTERNATIVE_DATA_SHARING_PLAN
    );

    FileStorageObject nihFile = createFileStorageObject(
        dataset.getDatasetId().toString(),
        FileCategory.NIH_INSTITUTIONAL_CERTIFICATION
    );

    // create unrelated files with timestamp later than the NIH file: one attached to dataset, one
    // completely separate from the dataset. ensures that the Mapper is selecting only the NIH file.
    createFileStorageObject();
    createFileStorageObject(
        dataset.getDatasetId().toString(),
        FileCategory.DATA_USE_LETTER
    );

    Dataset found = datasetDAO.findDatasetById(dataset.getDatasetId());

    assertEquals(nihFile, found.getNihInstitutionalCertificationFile());
    assertEquals(nihFile.getBlobId(),
        found.getNihInstitutionalCertificationFile().getBlobId());
  }

  @Test
  void testGetNIHInstitutionalFile_AlwaysLatestUpdated() {
    Dataset dataset = insertDataset();

    String fileName = randomAlphabetic(10);
    String bucketName = randomAlphabetic(10);
    String gcsFileUri = randomAlphabetic(10);
    User createUser = createUser();

    Integer nihFileIdCreatedFirstUpdatedSecond = fileStorageObjectDAO.insertNewFile(
        fileName,
        FileCategory.NIH_INSTITUTIONAL_CERTIFICATION.getValue(),
        bucketName,
        gcsFileUri,
        dataset.getDatasetId().toString(),
        createUser.getUserId(),
        Instant.ofEpochMilli(100)
    );

    Integer nihFileIdCreatedSecondUpdatedFirst = fileStorageObjectDAO.insertNewFile(
        fileName,
        FileCategory.NIH_INSTITUTIONAL_CERTIFICATION.getValue(),
        bucketName,
        gcsFileUri,
        dataset.getDatasetId().toString(),
        createUser.getUserId(),
        Instant.ofEpochMilli(110)
    );

    User updateUser = createUser();

    fileStorageObjectDAO.updateFileById(
        nihFileIdCreatedSecondUpdatedFirst,
        randomAlphabetic(20),
        randomAlphabetic(20),
        updateUser.getUserId(),
        Instant.ofEpochMilli(120));

    fileStorageObjectDAO.updateFileById(
        nihFileIdCreatedFirstUpdatedSecond,
        randomAlphabetic(20),
        randomAlphabetic(20),
        updateUser.getUserId(),
        Instant.ofEpochMilli(130));

    Dataset found = datasetDAO.findDatasetById(dataset.getDatasetId());

    // returns last updated file
    assertEquals(nihFileIdCreatedFirstUpdatedSecond,
        found.getNihInstitutionalCertificationFile().getFileStorageObjectId());
  }

  @Test
  void testGetNIHInstitutionalFile_AlwaysLatestCreated() {
    Dataset dataset = insertDataset();

    String fileName = randomAlphabetic(10);
    String bucketName = randomAlphabetic(10);
    String gcsFileUri = randomAlphabetic(10);
    User createUser = createUser();

    Integer nihFileIdCreatedFirst = fileStorageObjectDAO.insertNewFile(
        fileName,
        FileCategory.NIH_INSTITUTIONAL_CERTIFICATION.getValue(),
        bucketName,
        gcsFileUri,
        dataset.getDatasetId().toString(),
        createUser.getUserId(),
        Instant.ofEpochMilli(100)
    );

    User updateUser = createUser();

    fileStorageObjectDAO.updateFileById(
        nihFileIdCreatedFirst,
        randomAlphabetic(20),
        randomAlphabetic(20),
        updateUser.getUserId(),
        Instant.ofEpochMilli(120));

    Integer nihFileIdCreatedSecond = fileStorageObjectDAO.insertNewFile(
        fileName,
        FileCategory.NIH_INSTITUTIONAL_CERTIFICATION.getValue(),
        bucketName,
        gcsFileUri,
        dataset.getDatasetId().toString(),
        createUser.getUserId(),
        Instant.ofEpochMilli(130)
    );

    Dataset found = datasetDAO.findDatasetById(dataset.getDatasetId());

    // returns last updated file
    assertEquals(nihFileIdCreatedSecond,
        found.getNihInstitutionalCertificationFile().getFileStorageObjectId());
  }

  @Test
  void testGetNIHInstitutionalFile_NotDeleted() {
    Dataset dataset = insertDataset();

    FileStorageObject nihFile = createFileStorageObject(
        dataset.getDatasetId().toString(),
        FileCategory.NIH_INSTITUTIONAL_CERTIFICATION
    );

    User deleteUser = createUser();

    fileStorageObjectDAO.deleteFileById(
        nihFile.getFileStorageObjectId(),
        deleteUser.getUserId(),
        Instant.now()
    );

    Dataset found = datasetDAO.findDatasetById(dataset.getDatasetId());

    assertNull(found.getNihInstitutionalCertificationFile());
  }

  @Test
  void testGetDictionaryTerms() {
    List<Dictionary> terms = datasetDAO.getDictionaryTerms();
    assertFalse(terms.isEmpty());
    terms.forEach(t -> {
      assertNotNull(t.getKeyId());
      assertNotNull(t.getKey());
    });
  }

  @Test
  void testFindDatasetsByIdList() {
    Dataset dataset = insertDataset();
    Dac dac = insertDac();
    datasetDAO.updateDatasetDacId(dataset.getDatasetId(), dac.getDacId());

    List<Dataset> datasets = datasetDAO.findDatasetsByIdList(List.of(dataset.getDatasetId()));
    assertFalse(datasets.isEmpty());
    assertEquals(1, datasets.size());
    assertEquals(dac.getDacId(), datasets.get(0).getDacId());
    assertFalse(datasets.get(0).getProperties().isEmpty());
    assertNotNull(datasets.get(0).getCreateUser());
  }

  @Test
  void testFindDatasetsByEmptyIdList() {
    insertDataset();
    List<Dataset> datasets = datasetDAO.findDatasetsByIdList(List.of());
    assertTrue(datasets.isEmpty());
  }

  @Test
  void testFindDatasetsByNullIdList() {
    insertDataset();
    List<Dataset> datasets = datasetDAO.findDatasetsByIdList(null);
    assertTrue(datasets.isEmpty());
  }

  // User -> UserRoles -> DACs -> Consents -> Consent Associations -> DataSets
  @Test
  void testFindDataSetsByAuthUserEmail() {
    Dataset dataset = insertDataset();
    Dac dac = insertDac();
    datasetDAO.updateDatasetDacId(dataset.getDatasetId(), dac.getDacId());
    User user = createUser();
    createUserRole(UserRoles.CHAIRPERSON.getRoleId(), user.getUserId(), dac.getDacId());

    List<Integer> datasetIds = datasetDAO.findDatasetIdsByDACUserId(user.getUserId());
    assertFalse(datasetIds.isEmpty());
    assertTrue(datasetIds.contains(dataset.getDatasetId()));
  }

  @Test
  void testFindDatasetPropertiesByDatasetId() {
    Dataset d = insertDataset();
    Set<DatasetProperty> properties = datasetDAO.findDatasetPropertiesByDatasetId(d.getDatasetId());
    assertEquals(1, properties.size());
  }

  @Test
  void testUpdateDataset() {
    Dataset d = insertDataset();
    Dac dac = insertDac();
    String name = randomAlphanumeric(20);
    Timestamp now = new Timestamp(new Date().getTime());
    Integer userId = randomInt(1, 1000);

    datasetDAO.updateDataset(d.getDatasetId(), name, now, userId, dac.getDacId());
    Dataset updated = datasetDAO.findDatasetById(d.getDatasetId());

    assertEquals(name, updated.getName());
    assertEquals(now, updated.getUpdateDate());
    assertEquals(userId, updated.getUpdateUserId());
    assertEquals(dac.getDacId(), updated.getDacId());
  }

  @Test
  void testUpdateDatasetProperty() {
    Dataset d = insertDataset();
    Set<DatasetProperty> properties = datasetDAO.findDatasetPropertiesByDatasetId(d.getDatasetId());
    DatasetProperty originalProperty = properties.stream().toList().get(0);
    DatasetProperty newProperty = new DatasetProperty(
        d.getDatasetId(),
        1,
        "dataAccessCommitteeId",
        "Updated Value",
        PropertyType.String, new Date()
    );
    List<DatasetProperty> updatedProperties = new ArrayList<>();
    updatedProperties.add(newProperty);
    datasetDAO.updateDatasetProperty(d.getDatasetId(), updatedProperties.get(0).getPropertyKey(),
        updatedProperties.get(0).getPropertyValue().toString());
    Set<DatasetProperty> returnedProperties = datasetDAO.findDatasetPropertiesByDatasetId(
        d.getDatasetId());
    DatasetProperty returnedProperty = returnedProperties.stream().toList().get(0);
    assertEquals(originalProperty.getPropertyKey(),
        returnedProperty.getPropertyKey());
    assertEquals(originalProperty.getPropertyId(), returnedProperty.getPropertyId());
    assertNotEquals(originalProperty.getPropertyValue(),
        returnedProperty.getPropertyValue());
  }

  @Test
  void testCreateNumberTypedDatasetProperty() {
    Dataset d = insertDataset();

    Set<DatasetProperty> oldProperties = datasetDAO.findDatasetPropertiesByDatasetId(
        d.getDatasetId());
    DatasetProperty propertyToDelete = new ArrayList<>(oldProperties).get(0);
    datasetDAO.deleteDatasetPropertyByKey(d.getDatasetId(), propertyToDelete.getPropertyKey());

    List<DatasetProperty> newProps = List.of(
        new DatasetProperty(
            d.getDatasetId(),
            1,
            "dataAccessCommitteeId",
            "10",
            PropertyType.Number,
            new Date())
    );
    datasetDAO.insertDatasetProperties(newProps);

    Dataset dWithProps = datasetDAO.findDatasetById(d.getDatasetId());

    assertEquals(1, dWithProps.getProperties().size());
    DatasetProperty prop = new ArrayList<>(dWithProps.getProperties()).get(0);
    assertEquals(PropertyType.Number, prop.getPropertyType());
    assertEquals("10", prop.getPropertyValueAsString());
    assertEquals(10, prop.getPropertyValue());
  }

  @Test
  void testCreateDateTypedDatasetProperty() {
    Dataset d = insertDataset();
    Instant date = Instant.now();

    Set<DatasetProperty> oldProperties = datasetDAO.findDatasetPropertiesByDatasetId(
        d.getDatasetId());
    DatasetProperty propertyToDelete = new ArrayList<>(oldProperties).get(0);
    datasetDAO.deleteDatasetPropertyByKey(d.getDatasetId(), propertyToDelete.getPropertyKey());

    DatasetProperty propToAdd = new DatasetProperty(
        d.getDatasetId(),
        1,
        "dataAccessCommitteeId",
        date.toString(),
        PropertyType.Date,
        new Date());

    propToAdd.setSchemaProperty("date");
    List<DatasetProperty> newProps = List.of(
        propToAdd
    );
    datasetDAO.insertDatasetProperties(newProps);

    Set<DatasetProperty> props = datasetDAO.findDatasetPropertiesByDatasetId(d.getDatasetId());
    assertEquals(1, props.size());
    Optional<DatasetProperty> prop = props.stream().findFirst();
    assertTrue(prop.isPresent());
    assertEquals(PropertyType.Date, prop.get().getPropertyType());
    assertEquals(date.toString(), prop.get().getPropertyValueAsString());
  }

  @Test
  void testCreateBooleanTypedDatasetProperty() {
    Dataset d = insertDataset();
    Boolean bool = Boolean.FALSE;

    Set<DatasetProperty> oldProperties = datasetDAO.findDatasetPropertiesByDatasetId(
        d.getDatasetId());
    DatasetProperty propertyToDelete = new ArrayList<>(oldProperties).get(0);
    datasetDAO.deleteDatasetPropertyByKey(d.getDatasetId(), propertyToDelete.getPropertyKey());

    List<DatasetProperty> newProps = List.of(
        new DatasetProperty(
            d.getDatasetId(),
            1,
            "dataAccessCommitteeId",
            bool.toString(),
            PropertyType.Boolean,
            new Date())
    );
    datasetDAO.insertDatasetProperties(newProps);

    Dataset dWithProps = datasetDAO.findDatasetById(d.getDatasetId());

    assertEquals(1, dWithProps.getProperties().size());
    DatasetProperty prop = new ArrayList<>(dWithProps.getProperties()).get(0);
    assertEquals(PropertyType.Boolean, prop.getPropertyType());
    assertEquals(bool.toString(), prop.getPropertyValueAsString());
    assertEquals(Boolean.FALSE, prop.getPropertyValue());
  }

  @Test
  void testCreateJsonTypedDatasetProperty() {
    Dataset d = insertDataset();
    JsonObject jsonObject = new JsonObject();
    jsonObject.add("test", new JsonObject());

    Set<DatasetProperty> oldProperties = datasetDAO.findDatasetPropertiesByDatasetId(
        d.getDatasetId());
    DatasetProperty propertyToDelete = new ArrayList<>(oldProperties).get(0);
    datasetDAO.deleteDatasetPropertyByKey(d.getDatasetId(), propertyToDelete.getPropertyKey());

    List<DatasetProperty> newProps = List.of(
        new DatasetProperty(
            d.getDatasetId(),
            1,
            "dataAccessCommitteeId",
            jsonObject.toString(),
            PropertyType.Json,
            new Date())
    );
    datasetDAO.insertDatasetProperties(newProps);

    Dataset dWithProps = datasetDAO.findDatasetById(d.getDatasetId());

    assertEquals(1, dWithProps.getProperties().size());
    DatasetProperty prop = new ArrayList<>(dWithProps.getProperties()).get(0);
    assertEquals(PropertyType.Json, prop.getPropertyType());
    assertEquals(jsonObject.toString(), prop.getPropertyValueAsString());
    assertEquals(jsonObject, prop.getPropertyValue());
  }

  @Test
  void testCreateStringTypedDatasetProperty() {
    Dataset d = insertDataset();
    String value = "hi";

    Set<DatasetProperty> oldProperties = datasetDAO.findDatasetPropertiesByDatasetId(
        d.getDatasetId());
    DatasetProperty propertyToDelete = new ArrayList<>(oldProperties).get(0);
    datasetDAO.deleteDatasetPropertyByKey(d.getDatasetId(), propertyToDelete.getPropertyKey());

    List<DatasetProperty> newProps = List.of(
        new DatasetProperty(
            d.getDatasetId(),
            1,
            "dataAccessCommitteeId",
            value,
            PropertyType.String,
            new Date())
    );
    datasetDAO.insertDatasetProperties(newProps);

    Dataset dWithProps = datasetDAO.findDatasetById(d.getDatasetId());

    assertEquals(1, dWithProps.getProperties().size());
    DatasetProperty prop = new ArrayList<>(dWithProps.getProperties()).get(0);
    assertEquals(PropertyType.String, prop.getPropertyType());
    assertEquals(value, prop.getPropertyValueAsString());
    assertEquals(value, prop.getPropertyValue());
  }

  @Test
  void testCreateTypedDatasetPropertyWithSchema() {
    Dataset d = insertDataset();
    String schemaValue = "test test test test";

    Set<DatasetProperty> oldProperties = datasetDAO.findDatasetPropertiesByDatasetId(
        d.getDatasetId());
    DatasetProperty propertyToDelete = new ArrayList<>(oldProperties).get(0);
    datasetDAO.deleteDatasetPropertyByKey(d.getDatasetId(), propertyToDelete.getPropertyKey());

    List<DatasetProperty> newProps = List.of(
        new DatasetProperty(
            d.getDatasetId(),
            1,
            schemaValue,
            "asdf",
            PropertyType.String,
            new Date())
    );
    datasetDAO.insertDatasetProperties(newProps);

    Dataset dWithProps = datasetDAO.findDatasetById(d.getDatasetId());

    assertEquals(1, dWithProps.getProperties().size());
    DatasetProperty prop = new ArrayList<>(dWithProps.getProperties()).get(0);
    assertEquals(PropertyType.String, prop.getPropertyType());
    assertEquals(schemaValue, prop.getSchemaProperty());
  }

  @Test
  void testDeleteDatasetPropertyByKey() {
    Dataset d = insertDataset();
    Set<DatasetProperty> properties = datasetDAO.findDatasetPropertiesByDatasetId(d.getDatasetId());
    DatasetProperty propertyToDelete = properties.stream().toList().get(0);
    datasetDAO.deleteDatasetPropertyByKey(d.getDatasetId(), propertyToDelete.getPropertyKey());
    Set<DatasetProperty> returnedProperties = datasetDAO.findDatasetPropertiesByDatasetId(
        d.getDatasetId());
    assertNotEquals(properties.size(), returnedProperties.size());
  }

  @Test
  void testFindAllDatasetIds() {
    List<Integer> insertedDatasetIds = IntStream.range(1, 5).mapToObj(i -> {
      Dataset dataset = insertDataset();
      return dataset.getDatasetId();
    }).toList();
    List<Integer> datasetIds = datasetDAO.findAllDatasetIds();
    assertThat(datasetIds, contains(insertedDatasetIds.toArray()));
  }

  @Test
  void testZeroAliasValuesValid() {
    Dataset dataset = insertDataset();
    jdbi.useHandle(handle -> {
      Update update = handle.createUpdate(" UPDATE dataset SET alias = 0 WHERE dataset_id = :dataset_id ");
      update.bind("dataset_id", dataset.getDatasetId());
      update.execute();
      handle.commit();
    });
    Dataset updatedDataset = datasetDAO.findDatasetById(dataset.getDatasetId());
    assertEquals(0, updatedDataset.getAlias());
    updatedDataset.setDatasetIdentifier();
    assertNotNull(updatedDataset.getDatasetIdentifier());
  }

  @Test
  void testFindAllStudyNames() {
    Dataset ds1 = insertDataset();
    String ds1Name = randomAlphabetic(20);
    createStringDatasetProperty(ds1.getDatasetId(),  ds1Name);

    Dataset ds2 = insertDataset();
    String ds2Name = randomAlphabetic(25);
    createStringDatasetProperty(ds2.getDatasetId(), ds2Name);

    Dataset ds3 = insertDataset();
    String ds3Name = randomAlphabetic(15);
    createStringDatasetProperty(ds3.getDatasetId(), ds3Name);

    Study study = insertStudyWithProperties();

    Set<String> returned = datasetDAO.findAllStudyNames();

    Set<String> names = Set.of(ds1Name, ds2Name, ds3Name, study.getName());
    assertEquals(names.size(), returned.size());
    assertTrue(returned.containsAll(names));
  }

  @Test
  void testFindAllDatasetNames() {
    Dataset ds1 = insertDataset();
    Dataset ds2 = insertDataset();

    List<String> dsNames = datasetDAO.findAllDatasetNames();
    assertTrue(dsNames.contains(ds1.getDatasetName()));
    assertTrue(dsNames.contains(ds2.getDatasetName()));
  }

  @Test
  void testFindDatasetListByDacIds() {
    Dataset dataset = insertDataset();
    Dac dac = insertDac();

    Dataset datasetTwo = insertDataset();
    Dac dacTwo = insertDac();

    datasetDAO.updateDatasetDacId(dataset.getDatasetId(), dac.getDacId());
    datasetDAO.updateDatasetDacId(datasetTwo.getDatasetId(), dacTwo.getDacId());

    List<Integer> datasetIds = List.of(dataset.getDatasetId(), datasetTwo.getDatasetId());
    List<Dataset> datasets = datasetDAO.findDatasetListByDacIds(
        List.of(dac.getDacId(), dacTwo.getDacId()));
    datasets.forEach(d -> assertTrue(datasetIds.contains(d.getDatasetId())));
  }

  @Test
  void testFindDatasetIdsByDacIds() {
    Dataset dataset = insertDataset();
    Dac dac = insertDac();

    Dataset datasetTwo = insertDataset();
    Dac dacTwo = insertDac();

    Dataset datasetThree = insertDataset();

    datasetDAO.updateDatasetDacId(dataset.getDatasetId(), dac.getDacId());
    datasetDAO.updateDatasetDacId(datasetTwo.getDatasetId(), dacTwo.getDacId());

    List<Integer> datasetIds = List.of(dataset.getDatasetId(), datasetTwo.getDatasetId());
    List<Integer> foundDatasetIds = datasetDAO.findDatasetIdsByDacIds(
        List.of(dac.getDacId(), dacTwo.getDacId()));
    assertTrue(datasetIds.containsAll(foundDatasetIds));
    assertFalse(foundDatasetIds.contains(datasetThree.getDatasetId()));
  }

  @Test
  void testUpdateDatasetDataUse() {
    Dataset dataset = insertDataset();
    DataUse oldDataUse = dataset.getDataUse();
    DataUse newDataUse = new DataUseBuilder()
        .setGeneralUse(false)
        .setNonProfitUse(true)
        .setHmbResearch(true)
        .setDiseaseRestrictions(List.of("DOID_1"))
        .build();

    datasetDAO.updateDatasetDataUse(dataset.getDatasetId(), newDataUse.toString());
    Dataset updated = datasetDAO.findDatasetById(dataset.getDatasetId());
    assertEquals(newDataUse, updated.getDataUse());
    assertNotEquals(oldDataUse, updated.getDataUse());
  }

  @Test
  void testUpdateDatasetCreateUserId() {
    Dataset dataset = insertDataset();
    User user = createUser();
    datasetDAO.updateDatasetCreateUserId(dataset.getDatasetId(), user.getUserId());
    Dataset updated = datasetDAO.findDatasetById(dataset.getDatasetId());
    assertEquals(user.getUserId(), updated.getCreateUserId());
  }

  @Test
  void testUpdateDatasetNameWithUpdateUser() {
    Dataset dataset = insertDataset();
    String newName = randomAlphabetic(dataset.getName().length() + 5);
    datasetDAO.updateDatasetNameWithUpdateUser(
        dataset.getDatasetId(),
        newName,
        new Timestamp(new Date().getTime()),
        dataset.getCreateUserId());
    Dataset foundDataset = datasetDAO.findDatasetById(dataset.getDatasetId());
    assertNotNull(foundDataset);
    assertEquals(newName, foundDataset.getName());
  }

  @Test
  void testUpdateDatasetUpdateUser() {
    Dataset dataset = insertDataset();
    User user = createUser();
    datasetDAO.updateDatasetUpdateUser(
        dataset.getDatasetId(),
        new Timestamp(new Date().getTime()),
        user.getUserId());
    Dataset foundDataset = datasetDAO.findDatasetById(dataset.getDatasetId());
    assertNotNull(foundDataset);
    assertEquals(user.getUserId(), foundDataset.getUpdateUserId());
    assertTrue(foundDataset.getUpdateDate().after(dataset.getUpdateDate()));
  }

  @Test
  void testUpdateDatasetApproval() {
    User updateUser = createUser();
    Dataset dataset = insertDataset();
    datasetDAO.updateDatasetApproval(true, Instant.now(), updateUser.getUserId(),
        dataset.getDatasetId());
    Dataset updatedDataset = datasetDAO.findDatasetById(dataset.getDatasetId());
    assertNotNull(updatedDataset);
    assertTrue(updatedDataset.getDacApproval());
    datasetDAO.updateDatasetApproval(false, Instant.now(), updateUser.getUserId(),
        dataset.getDatasetId());
    Dataset updatedDatasetAfterApprovalFalse = datasetDAO.findDatasetById(dataset.getDatasetId());
    assertNotNull(updatedDatasetAfterApprovalFalse);
    assertEquals(dataset.getDatasetId(),
        updatedDatasetAfterApprovalFalse.getDatasetId());
    assertFalse(updatedDatasetAfterApprovalFalse.getDacApproval());

  }

  @Test
  void testInsertDatasetAudit() {
    Dataset d = insertDataset();
    DatasetAudit audit = new DatasetAudit(
        d.getDatasetId(),
        "objectid",
        "name",
        new Date(),
        d.getCreateUserId(),
        "action");
    Integer auditId = datasetDAO.insertDatasetAudit(audit);
    Optional<DatasetAudit> auditResponse = Optional.ofNullable(
        datasetDAO.findAuditsByDatasetId(d.getDatasetId()).get(0));
    assertTrue(auditResponse.isPresent());
    assertEquals(auditId, auditResponse.get().getDataSetAuditId());
  }

  @Test
  void testUniqueDatasetName() {
    createStaticDataset();
    try {
      createStaticDataset();
      Assertions.fail();
    } catch (Exception e) {
      assertTrue(e.getMessage().contains("duplicate key value violates unique constraint"));
    }
  }

  @Test
  void testDatasetWithStudy() {
    Study study = insertStudyWithProperties();

    Dataset ds = insertDataset();
    insertDataset(); // create unrelated datasets (for testing study's dataset ids)
    Dataset otherDsOnStudy = insertDataset();

    datasetDAO.updateStudyId(ds.getDatasetId(), study.getStudyId());
    datasetDAO.updateStudyId(otherDsOnStudy.getDatasetId(), study.getStudyId());

    insertDataset(); // create unrelated datasets (for testing study's dataset ids)

    FileStorageObject fso = createFileStorageObject(study.getUuid().toString(),
        FileCategory.ALTERNATIVE_DATA_SHARING_PLAN);

    ds = datasetDAO.findDatasetById(ds.getDatasetId());

    assertNotNull(ds.getStudy());

    // mapper ran properly
    assertEquals(study.getName(), ds.getStudy().getName());
    // reducer caught properties
    assertEquals(study.getProperties().size(), ds.getStudy().getProperties().size());
    // reducer caught FSO
    assertNotNull(fso);
    assertEquals(fso.getFileStorageObjectId(),
        ds.getStudy().getAlternativeDataSharingPlan().getFileStorageObjectId());
    assertEquals(2, ds.getStudy().getDatasetIds().size());
    assertTrue(ds.getStudy().getDatasetIds().contains(ds.getDatasetId()));
    assertTrue(
        ds.getStudy().getDatasetIds().contains(otherDsOnStudy.getDatasetId()));
  }

  @ParameterizedTest
  @ValueSource(strings = {"RADAR_APPROVE", "FINAL"})
  void testGetApprovedDatasets_user_without_library_card(String voteType) {
    User user = createUser();

    Dataset dataset1 = createDataset(false);
    Dataset dataset2 = createDataset(true);
    Dataset dataset3 = createDataset(false);
    Dataset dataset4 = createDataset(true);

    Timestamp timestamp = new Timestamp(new Date().getTime());

    Dac dac1 = insertDac();
    datasetDAO.updateDataset(dataset1.getDatasetId(), dataset1.getDatasetName(), timestamp,
        user.getUserId(), dac1.getDacId());
    datasetDAO.updateDataset(dataset2.getDatasetId(), dataset2.getDatasetName(), timestamp,
        user.getUserId(), dac1.getDacId());

    Dac dac2 = insertDac();
    datasetDAO.updateDataset(dataset3.getDatasetId(), dataset3.getDatasetName(), timestamp,
        user.getUserId(), dac2.getDacId());
    datasetDAO.updateDataset(dataset4.getDatasetId(), dataset4.getDatasetName(), timestamp,
        user.getUserId(), dac2.getDacId());

    DarCollection dar1 = createDarCollectionWithDatasets(dac1.getDacId(), user, List.of(dataset1));
    DarCollection dar2 = createDarCollectionWithDatasets(dac2.getDacId(), user,
        List.of(dataset2, dataset3));
    DarCollection dar3 = createDarCollectionWithDatasets(dac2.getDacId(), user, List.of(dataset4));
    List<DarCollection> allDarCollections = List.of(dar1, dar2, dar3);

    Map<Integer, Boolean> expectedFinalVotesForDatasets = Map.of(dataset1.getDatasetId(), false,
        dataset2.getDatasetId(), false, dataset3.getDatasetId(), true, dataset4.getDatasetId(),
        true);

    for (DarCollection dar : allDarCollections) {
      for (Map.Entry<String, DataAccessRequest> e : dar.getDars().entrySet()) {
        for (Integer id : e.getValue().getDatasetIds()) {
          createDataAccessElectionWithVotes(e.getKey(), id, user.getUserId(),
              expectedFinalVotesForDatasets.get(id), VoteType.valueOf(voteType));
        }
      }
    }

    List<ApprovedDataset> approvedDatasets = datasetDAO.getApprovedDatasets(user.getUserId());
    assertNotNull(approvedDatasets);
    assertEquals(0, approvedDatasets.size());
  }

  @ParameterizedTest
  @ValueSource(strings = {"RADAR_APPROVE", "FINAL"})
  void testGetApprovedDatasets(String voteType) {

    // user with a mix of approved and unapproved datasets
    User user = createUser();
    libraryCardDAO.insertLibraryCard(user.getUserId(), user.getDisplayName(), user.getEmail(), user.getUserId(), new Date());

    Dataset dataset1 = createDataset(false);
    Dataset dataset2 = createDataset(true);
    Dataset dataset3 = createDataset(false);
    Dataset dataset4 = createDataset(true);

    Timestamp timestamp = new Timestamp(new Date().getTime());

    Dac dac1 = insertDac();
    datasetDAO.updateDataset(dataset1.getDatasetId(), dataset1.getDatasetName(), timestamp,
        user.getUserId(), dac1.getDacId());
    datasetDAO.updateDataset(dataset2.getDatasetId(), dataset2.getDatasetName(), timestamp,
        user.getUserId(), dac1.getDacId());

    Dac dac2 = insertDac();
    datasetDAO.updateDataset(dataset3.getDatasetId(), dataset3.getDatasetName(), timestamp,
        user.getUserId(), dac2.getDacId());
    datasetDAO.updateDataset(dataset4.getDatasetId(), dataset4.getDatasetName(), timestamp,
        user.getUserId(), dac2.getDacId());

    DarCollection dar1 = createDarCollectionWithDatasets(dac1.getDacId(), user, List.of(dataset1));
    DarCollection dar2 = createDarCollectionWithDatasets(dac2.getDacId(), user,
        List.of(dataset2, dataset3));
    DarCollection dar3 = createDarCollectionWithDatasets(dac2.getDacId(), user, List.of(dataset4));
    List<DarCollection> allDarCollections = List.of(dar1, dar2, dar3);

    Map<Integer, Boolean> expectedFinalVotesForDatasets = Map.of(dataset1.getDatasetId(), false,
        dataset2.getDatasetId(), false, dataset3.getDatasetId(), true, dataset4.getDatasetId(),
        true);


    for (DarCollection dar : allDarCollections) {
      for (Map.Entry<String, DataAccessRequest> e : dar.getDars().entrySet()) {
        for (Integer id : e.getValue().getDatasetIds()) {
          createDataAccessElectionWithVotes(e.getKey(), id, user.getUserId(),
              expectedFinalVotesForDatasets.get(id), VoteType.valueOf(voteType));
        }
      }
    }

    List<ApprovedDataset> approvedDatasets = datasetDAO.getApprovedDatasets(user.getUserId());
    assertNotNull(approvedDatasets);

    // checks that all datasets in the result are approved
    approvedDatasets.forEach(approvedDataset -> assertTrue(
        datasetDAO.findDatasetByAlias(approvedDataset.getAlias()).getDacApproval()));

    ApprovedDataset expectedApprovedDataset1 = new ApprovedDataset(dataset3.getAlias(),
        dar2.getDarCode(), dataset3.getDatasetName(), dac2.getName(), dar2.getMostRecentDar().getExpiresAt());
    ApprovedDataset expectedApprovedDataset2 = new ApprovedDataset(dataset4.getAlias(),
        dar3.getDarCode(), dataset4.getDatasetName(), dac2.getName(), dar3.getMostRecentDar().getExpiresAt());
    Map<Integer, ApprovedDataset> expectedDatasets = Map.of(dataset3.getAlias(),
        expectedApprovedDataset1, dataset4.getAlias(), expectedApprovedDataset2);

    // checks that the expected result list size and contents match the observed result
    assertEquals(expectedDatasets.size(), approvedDatasets.size());
    IntStream.range(0, approvedDatasets.size()).forEach(index -> {
      ApprovedDataset dataset = approvedDatasets.get(index);
      ApprovedDataset expectedDataset = expectedDatasets.get(dataset.getAlias());
      assertTrue(dataset.isApprovedDatasetEqual(expectedDataset));
    });


  }

  @ParameterizedTest
  @ValueSource(strings = {"RADAR_APPROVE", "FINAL"})
  void testGetApprovedDatasetsWhenNone(String voteType) {

    // user with only unapproved datasets
    User user = createUser();

    Dataset dataset1 = createDataset(false);
    Dataset dataset2 = createDataset(true);

    Timestamp timestamp = new Timestamp(new Date().getTime());

    Dac dac1 = insertDac();
    datasetDAO.updateDataset(dataset1.getDatasetId(), dataset1.getDatasetName(), timestamp,
        user.getUserId(), dac1.getDacId());
    datasetDAO.updateDataset(dataset2.getDatasetId(), dataset2.getDatasetName(), timestamp,
        user.getUserId(), dac1.getDacId());

    DarCollection dar1 = createDarCollectionWithDatasets(dac1.getDacId(), user,
        List.of(dataset1, dataset2));

    for (Map.Entry<String, DataAccessRequest> e : dar1.getDars().entrySet()) {
      for (Integer id : e.getValue().getDatasetIds()) {
        createDataAccessElectionWithVotes(
            e.getKey(), id, user.getUserId(), false, VoteType.valueOf(voteType));
      }
    }

    List<ApprovedDataset> approvedDatasets = datasetDAO.getApprovedDatasets(user.getUserId());
    assertEquals(0, approvedDatasets.size());
  }

  @Test
  void testGetApprovedDatasetsWhenEmpty() {

    // user with no datasets
    User user = createUser();
    List<ApprovedDataset> approvedDatasets = datasetDAO.getApprovedDatasets(user.getUserId());
    assertEquals(0, approvedDatasets.size());

  }

  @Test
  void testGetApprovedDatasetsForMultiDACElections() {
    Date now = new Date();
    // user with a mix of approved and unapproved datasets
    User user = createUser();
    User chairperson1 = createUser();
    User chairperson2 = createUser();
    libraryCardDAO.insertLibraryCard(user.getUserId(), user.getDisplayName(), user.getEmail(), user.getUserId(), now);

    Dataset dataset1 = createDataset(true);
    Dataset dataset2 = createDataset(true);
    Dataset dataset3 = createDataset(true);
    Dataset dataset4 = createDataset(true);

    Timestamp timestamp = new Timestamp(now.getTime());

    Dac dac1 = insertDac();
    datasetDAO.updateDataset(dataset1.getDatasetId(), dataset1.getDatasetName(), timestamp, chairperson1.getUserId(), dac1.getDacId());
    datasetDAO.updateDataset(dataset2.getDatasetId(), dataset2.getDatasetName(), timestamp, chairperson1.getUserId(), dac1.getDacId());
    datasetDAO.updateDatasetApproval(true, Instant.now(), chairperson1.getUserId(), dataset1.getDatasetId());
    datasetDAO.updateDatasetApproval(true, Instant.now(), chairperson1.getUserId(), dataset2.getDatasetId());


    Dac dac2 = insertDac();
    datasetDAO.updateDataset(dataset3.getDatasetId(), dataset3.getDatasetName(), timestamp, chairperson2.getUserId(), dac2.getDacId());
    datasetDAO.updateDataset(dataset4.getDatasetId(), dataset4.getDatasetName(), timestamp, chairperson2.getUserId(), dac2.getDacId());
    datasetDAO.updateDatasetApproval(true, Instant.now(), chairperson2.getUserId(), dataset3.getDatasetId());
    datasetDAO.updateDatasetApproval(true, Instant.now(), chairperson2.getUserId(), dataset4.getDatasetId());

    DarCollection darCollection = createDarCollectionWithDatasetsNewModel(user, List.of(dataset1, dataset2, dataset3, dataset4));
    assertEquals(0, datasetDAO.getApprovedDatasets(user.getUserId()).size());

    // Simulate 2 DAC 1 elections for yesterday
    Date yesterday = Date.from(Instant.now().minus(1, ChronoUnit.DAYS));
    Integer electionId1 = electionDAO.insertElection(
        ElectionType.DATA_ACCESS.getValue(),
        ElectionStatus.OPEN.getValue(),
        yesterday,
        darCollection.getMostRecentDar().getReferenceId(),
        dataset1.getDatasetId()
    );
    Integer voteId1 = voteDAO.insertVote(chairperson1.getUserId(), electionId1, VoteType.FINAL.getValue());
    updateVote(true, "rationale", yesterday, voteId1, false, electionId1, yesterday, false);
    electionDAO.updateElectionById(electionId1, ElectionStatus.CLOSED.getValue(), yesterday);
    List<ApprovedDataset> approvedDatasets = datasetDAO.getApprovedDatasets(user.getUserId());
    assertEquals(1, approvedDatasets.size());
    assertEquals(darCollection.getMostRecentDar().getExpiresAt(), approvedDatasets.get(0).getExpirationDate());


    Integer electionId2 = electionDAO.insertElection(
        ElectionType.DATA_ACCESS.getValue(),
        ElectionStatus.OPEN.getValue(),
        yesterday,
        darCollection.getMostRecentDar().getReferenceId(),
        dataset2.getDatasetId()
    );
    Integer voteId2 = voteDAO.insertVote(chairperson1.getUserId(), electionId2, VoteType.FINAL.getValue());
    updateVote(true, "rationale", yesterday, voteId2, false, electionId1, yesterday, false);
    electionDAO.updateElectionById(electionId2, ElectionStatus.CLOSED.getValue(), yesterday);
    assertEquals(2, datasetDAO.getApprovedDatasets(user.getUserId()).size());

    // Simulate 2 DAC 2 elections for today
    Date today = new Date();
    Integer electionId3 = electionDAO.insertElection(
        ElectionType.DATA_ACCESS.getValue(),
        ElectionStatus.OPEN.getValue(),
        today,
        darCollection.getMostRecentDar().getReferenceId(),
        dataset3.getDatasetId()
    );
    Integer voteId3 = voteDAO.insertVote(chairperson2.getUserId(), electionId3, VoteType.FINAL.getValue());
    updateVote(true, "rationale", today, voteId3, false, electionId3, today, false);
    electionDAO.updateElectionById(electionId3, ElectionStatus.CLOSED.getValue(), today);
    assertEquals(3, datasetDAO.getApprovedDatasets(user.getUserId()).size());

    Integer electionId4 = electionDAO.insertElection(
        ElectionType.DATA_ACCESS.getValue(),
        ElectionStatus.OPEN.getValue(),
        today,
        darCollection.getMostRecentDar().getReferenceId(),
        dataset4.getDatasetId()
    );
    Integer voteId4 = voteDAO.insertVote(chairperson2.getUserId(), electionId4, VoteType.FINAL.getValue());
    updateVote(true, "rationale", today, voteId4, false, electionId4, today, false);
    electionDAO.updateElectionById(electionId4, ElectionStatus.CLOSED.getValue(), today);

    List<ApprovedDataset> approvedDatasets2 = datasetDAO.getApprovedDatasets(user.getUserId());
    assertNotNull(approvedDatasets2);
    assertEquals(4, approvedDatasets2.size());

    // manually age the dar off
    jdbi.useHandle(
        handle ->
            handle
                .createUpdate(
                    "UPDATE data_access_request SET submission_date=CURRENT_DATE - INTERVAL '365 days' WHERE data_access_request.reference_id = ?")
                .bind(0, darCollection.getMostRecentDar().getReferenceId())
                .execute());

    // confirm the expiration part of the query works.
    List<ApprovedDataset> approvedDatasets3 = datasetDAO.getApprovedDatasets(user.getUserId());
    assertEquals(0, approvedDatasets3.size());

    DataAccessRequest recentDar = darCollection.getMostRecentDar();

    // submit a progress report.
    DataAccessRequest progressReport = createProgressReport(recentDar.getData(), recentDar.getEraCommonsId(), recentDar.getUserId(), recentDar.getCollectionId(), recentDar.getId(), recentDar.getDatasetIds());

    // ensure we still have no approved datasets.
    List<ApprovedDataset> approvedDatasets4 = datasetDAO.getApprovedDatasets(user.getUserId());
    assertEquals(0, approvedDatasets4.size());

    // Simulate 2 DAC 2 elections for today
    // vote yes on dataset 3
    Integer electionId5 = electionDAO.insertElection(
        ElectionType.DATA_ACCESS.getValue(),
        ElectionStatus.OPEN.getValue(),
        today,
        progressReport.getReferenceId(),
        dataset3.getDatasetId()
    );
    Integer voteId5 = voteDAO.insertVote(chairperson2.getUserId(), electionId5, VoteType.FINAL.getValue());
    updateVote(true, "rationale", today, voteId5, false, electionId5, today, false);
    electionDAO.updateElectionById(electionId5, ElectionStatus.CLOSED.getValue(), today);
    List<ApprovedDataset> approvedDatasets5 = datasetDAO.getApprovedDatasets(user.getUserId());
    assertEquals(1, approvedDatasets5.size());
    assertEquals(progressReport.getExpiresAt(), approvedDatasets5.get(0).getExpirationDate());

    Integer electionId6 = electionDAO.insertElection(
        ElectionType.DATA_ACCESS.getValue(),
        ElectionStatus.OPEN.getValue(),
        today,
        progressReport.getReferenceId(),
        dataset4.getDatasetId()
    );
    Integer voteId6 = voteDAO.insertVote(chairperson2.getUserId(), electionId6, VoteType.FINAL.getValue());
    // vote no on dataset 4
    updateVote(false, "rationale", today, voteId6, false, electionId6, today, false);
    electionDAO.updateElectionById(electionId6, ElectionStatus.CLOSED.getValue(), today);

    List<ApprovedDataset> approvedDatasets6 = datasetDAO.getApprovedDatasets(user.getUserId());
    assertNotNull(approvedDatasets6);
    assertEquals(1, approvedDatasets6.size());

    List<DataAccessRequest> dataset3Dars = dataAccessRequestDAO.findApprovedDARsByDatasetId(dataset3.getDatasetId());
    assertEquals(1, dataset3Dars.size());

    List<DataAccessRequest> dataset4Dars = dataAccessRequestDAO.findApprovedDARsByDatasetId(dataset4.getDatasetId());
    assertEquals(0, dataset4Dars.size());

    today = new Date();  //adjust the date into the future or the LAST_VALUE won't work correctly.
    // make a new election for dataset 3 and now vote no
    Integer electionId7 = electionDAO.insertElection(
        ElectionType.DATA_ACCESS.getValue(),
        ElectionStatus.OPEN.getValue(),
        today,
        progressReport.getReferenceId(),
        dataset3.getDatasetId()
    );
    Integer voteId7 = voteDAO.insertVote(chairperson2.getUserId(), electionId7, VoteType.FINAL.getValue());
    updateVote(false, "rationale", today, voteId7, false, electionId7, today, false);
    electionDAO.updateElectionById(electionId7, ElectionStatus.CLOSED.getValue(), today);
    assertEquals(0, datasetDAO.getApprovedDatasets(user.getUserId()).size());

    dataset3Dars = dataAccessRequestDAO.findApprovedDARsByDatasetId(dataset3.getDatasetId());
    assertEquals(0, dataset3Dars.size());

  }

  @Test
  void testFindDatasetSummariesByQuery() {
    Dataset dataset = insertDataset();
    Dataset dataset2 = insertDataset();
    User user = createUser();
    datasetDAO.updateDatasetApproval(true, Instant.now(), user.getUserId(), dataset.getDatasetId());
    datasetDAO.updateDatasetApproval(true, Instant.now(), user.getUserId(),
        dataset2.getDatasetId());

    List<DatasetSummary> summaries = datasetDAO.findDatasetSummariesByQuery(dataset.getName());
    assertNotNull(summaries);
    assertFalse(summaries.isEmpty());
    assertEquals(dataset.getDatasetId(),
        summaries.stream().map(DatasetSummary::id).toList().get(0));
    assertNotEquals(dataset2.getDatasetId(),
        summaries.stream().map(DatasetSummary::id).toList().get(0));
  }

  @Test
  void testUpdateDatasetIndexedDate() {
    Dataset dataset = insertDataset();
    datasetDAO.updateDatasetIndexedDate(dataset.getDatasetId(), Instant.now());
    Dataset updatedDataset = datasetDAO.findDatasetById(dataset.getDatasetId());
    assertNotNull(updatedDataset.getIndexedDate());
    datasetDAO.updateDatasetIndexedDate(dataset.getDatasetId(), null);
    Dataset updatedDataset2 = datasetDAO.findDatasetById(dataset.getDatasetId());
    assertNull(updatedDataset2.getIndexedDate());
  }

  @Test
  void testFindDatasetSummariesByQuery_NotApproved() {
    Dataset dataset = insertDataset();

    List<DatasetSummary> summaries = datasetDAO.findDatasetSummariesByQuery(dataset.getName());
    assertNotNull(summaries);
    assertTrue(summaries.isEmpty());
  }

  @Test
  void testFindDatasetSummariesByQuery_NullQuery() {
    insertDataset();

    List<DatasetSummary> summaries = datasetDAO.findDatasetSummariesByQuery(null);
    assertNotNull(summaries);
    assertTrue(summaries.isEmpty());
  }

  @Test
  void testFindDatasetSummariesByQuery_EmptyQuery() {
    insertDataset();

    List<DatasetSummary> summaries = datasetDAO.findDatasetSummariesByQuery("");
    assertNotNull(summaries);
    assertTrue(summaries.isEmpty());
  }

  private DarCollection createDarCollectionWithDatasets(int dacId, User user,
      List<Dataset> datasets) {
    String darCode = "DAR-" + randomInt(1, 999999);
    Integer collectionId = darCollectionDAO.insertDarCollection(darCode, user.getUserId(),
        new Date());
    IntStream.range(0, datasets.size()).forEach(index -> {
      Dataset dataset = datasets.get(index);
      datasetDAO.updateDatasetDacId(dataset.getDatasetId(), dacId);
      createDataAccessRequestWithDatasetAndCollectionInfo(collectionId, dataset.getDatasetId(),
          user.getUserId());
    });
    return darCollectionDAO.findDARCollectionByCollectionId(collectionId);
  }

  private DarCollection createDarCollectionWithDatasetsNewModel(User user, List<Dataset> datasets) {
    String darCode = "DAR-" + randomInt(1, 999999);
    Date now = new Date();
    Integer collectionId = darCollectionDAO.insertDarCollection(darCode, user.getUserId(),
        now);
    List<Integer> datasetIds = datasets.stream().map(Dataset::getDatasetId).toList();
    createDataAccessRequestNewModel(collectionId, datasetIds, user.getUserId());
    return darCollectionDAO.findDARCollectionByCollectionId(collectionId);
  }

  private void createUserRole(Integer roleId, Integer userId, Integer dacId) {
    dacDAO.addDacMember(roleId, userId, dacId);
  }

  private void createFileStorageObject() {
    FileCategory category = List.of(FileCategory.values())
        .get(new Random().nextInt(FileCategory.values().length));
    String entityId = randomAlphabetic(10);
    createFileStorageObject(entityId, category);
  }

  private FileStorageObject createFileStorageObject(String entityId, FileCategory category) {
    String fileName = randomAlphabetic(10);
    String bucketName = randomAlphabetic(10);
    String gcsFileUri = randomAlphabetic(10);
    User createUser = createUser();
    Instant createDate = Instant.now();

    Integer newFileStorageObjectId = fileStorageObjectDAO.insertNewFile(
        fileName,
        category.getValue(),
        bucketName,
        gcsFileUri,
        entityId,
        createUser.getUserId(),
        createDate
    );
    return fileStorageObjectDAO.findFileById(newFileStorageObjectId);
  }

  protected void createStringDatasetProperty(Integer datasetId, String value) {
    List<DatasetProperty> list = new ArrayList<>();
    PropertyType type = PropertyType.String;
    DatasetProperty dsp = new DatasetProperty();
    dsp.setDatasetId(datasetId);
    dsp.setPropertyKey(1);
    dsp.setSchemaProperty("studyName");
    dsp.setPropertyValue(type.coerce(value));
    dsp.setPropertyType(type);
    dsp.setCreateDate(new Date());
    list.add(dsp);
    datasetDAO.insertDatasetProperties(list);
  }

  private Dac insertDac() {
    Integer id = dacDAO.createDac(
        "Test_" + randomAlphanumeric(20),
        "Test_" + randomAlphanumeric(20),
        new Date());
    return dacDAO.findById(id);
  }

  private Study insertStudyWithProperties() {
    User u = createUser();

    return insertStudyWithProperties(u);
  }

  private Study insertStudyWithProperties(User user) {

    String name = randomAlphabetic(20);
    String description = randomAlphabetic(20);
    List<String> dataTypes = List.of(
        randomAlphabetic(20),
        randomAlphabetic(20)
    );
    String piName = randomAlphabetic(20);
    Boolean publicVisibility = true;

    Integer id = studyDAO.insertStudy(
        name,
        description,
        piName,
        dataTypes,
        publicVisibility,
        user.getUserId(),
        Instant.now(),
        UUID.randomUUID()
    );

    studyDAO.insertStudyProperty(
        id,
        "prop1",
        PropertyType.String.toString(),
        "asdf"
    );

    studyDAO.insertStudyProperty(
        id,
        "prop2",
        PropertyType.Number.toString(),
        "1"
    );

    return studyDAO.findStudyById(id);
  }


  private void createDataAccessRequestWithDatasetAndCollectionInfo(int collectionId,
      int datasetId, int userId) {
    DataAccessRequestData data = new DataAccessRequestData();
    data.setProjectTitle(randomAlphabetic(10));
    String referenceId = randomAlphanumeric(20);
    dataAccessRequestDAO.insertDataAccessRequest(collectionId, referenceId, userId, new Date(),
        new Date(), new Date(), new Date(), data, randomAlphabetic(10));
    dataAccessRequestDAO.insertDARDatasetRelation(referenceId, datasetId);
  }

  private void createDataAccessRequestNewModel(int collectionId, List<Integer> datasetIds, int userId) {
    DataAccessRequestData data = new DataAccessRequestData();
    data.setProjectTitle(randomAlphabetic(10));
    String referenceId = randomAlphabetic(20);
    Date now = new Date();
    dataAccessRequestDAO.insertDataAccessRequest(collectionId, referenceId, userId, now, now, now, now, data, randomAlphabetic(10));
    datasetIds.forEach(
        datasetId -> dataAccessRequestDAO.insertDARDatasetRelation(referenceId, datasetId));
  }

  private DataAccessRequest createProgressReport(DataAccessRequestData data, String eraCommonsId, Integer userId, Integer collectionId,
      Integer parentId, List<Integer> datasetIds) {
    String referenceId = UUID.randomUUID().toString();
    dataAccessRequestDAO.insertProgressReport(
        parentId,
        collectionId,
        referenceId,
        userId,
        data,
        eraCommonsId);
    datasetIds.forEach(
        datasetId -> dataAccessRequestDAO.insertDARDatasetRelation(referenceId, datasetId));
    return dataAccessRequestDAO.findByReferenceId(referenceId);
  }

  private void createDatasetProperties(Integer datasetId) {
    List<DatasetProperty> list = new ArrayList<>();
    DatasetProperty dsp = new DatasetProperty();
    dsp.setDatasetId(datasetId);
    dsp.setPropertyKey(1);
    dsp.setPropertyValue("Test_PropertyValue");
    dsp.setCreateDate(new Date());
    list.add(dsp);
    datasetDAO.insertDatasetProperties(list);
  }

  private Dataset insertDataset() {
    User user = createUser();
    String name = "Name_" + randomAlphanumeric(20);
    Timestamp now = new Timestamp(new Date().getTime());
    String objectId = "Object ID_" + randomAlphanumeric(20);
    DataUse dataUse = new DataUseBuilder().setGeneralUse(true).build();
    Integer id = datasetDAO.insertDataset(name, now, user.getUserId(), objectId,
        dataUse.toString(), null);
    createDatasetProperties(id);
    return datasetDAO.findDatasetById(id);
  }


  private Dataset createDataset(boolean dacApproval) {
    User user = createUser();
    String name = "Name_" + randomAlphanumeric(20);
    Timestamp now = new Timestamp(new Date().getTime());
    Instant instant = Instant.now();
    String objectId = "Object ID_" + randomAlphanumeric(20);
    DataUse dataUse = new DataUseBuilder().setGeneralUse(true).build();
    Integer id = datasetDAO.insertDataset(name, now, user.getUserId(), objectId,
        dataUse.toString(), null);
    datasetDAO.updateDatasetApproval(dacApproval, instant, user.getUserId(), id);
    createDatasetProperties(id);
    return datasetDAO.findDatasetById(id);
  }

  private void createStaticDataset() {
    User user = createUser();
    String name = "test_unique_constraint_dataset_name";
    Timestamp now = new Timestamp(new Date().getTime());
    String objectId = "Object ID_" + randomAlphanumeric(20);
    DataUse dataUse = new DataUseBuilder().setGeneralUse(true).build();
    Integer id = datasetDAO.insertDataset(name, now, user.getUserId(), objectId,
        dataUse.toString(), null);
    createDatasetProperties(id);
  }

  private void createDataAccessElectionWithVotes(String referenceId, Integer datasetId,
      Integer userId, boolean finalVoteApproval, VoteType voteType) {
    Integer electionId = electionDAO.insertElection(
        ElectionType.DATA_ACCESS.getValue(),
        ElectionStatus.OPEN.getValue(),
        new Date(),
        referenceId,
        datasetId
    );
    Integer voteId = voteDAO.insertVote(userId, electionId, voteType.getValue());
    updateVote(finalVoteApproval, "rationale", new Date(), voteId, false, electionId,
        new Date(), false);
    electionDAO.updateElectionById(electionId, ElectionStatus.CLOSED.getValue(), new Date());
    datasetDAO.updateDatasetApproval(finalVoteApproval, Instant.now(), userId, datasetId);
  }

}
