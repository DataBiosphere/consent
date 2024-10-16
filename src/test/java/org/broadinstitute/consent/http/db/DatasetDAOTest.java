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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
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
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.FileStorageObject;
import org.broadinstitute.consent.http.models.Study;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.dto.DatasetDTO;
import org.jdbi.v3.core.statement.Update;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DatasetDAOTest extends DAOTestHelper {

  @Test
  void testFindAllDatasetStudySummariesDatasetAndStudy() {
    Dataset dataset = insertDataset();
    Study study = insertStudyWithProperties();
    datasetDAO.updateStudyId(dataset.getDataSetId(), study.getStudyId());

    List<DatasetStudySummary> summaries = datasetDAO.findAllDatasetStudySummaries();
    assertThat(summaries, hasSize(1));
    assertEquals(dataset.getDataSetId(), summaries.get(0).dataset_id());
    assertEquals(study.getStudyId(), summaries.get(0).study_id());
  }

  @Test
  void testFindAllDatasetStudySummariesDatasetOnly() {
    Dataset dataset = insertDataset();

    List<DatasetStudySummary> summaries = datasetDAO.findAllDatasetStudySummaries();
    assertThat(summaries, hasSize(1));
    assertEquals(dataset.getDataSetId(), summaries.get(0).dataset_id());
    assertNull(summaries.get(0).study_id());
  }

  @Test
  void testFindDatasetByIdWithDacAndConsent() {
    Dataset dataset = insertDataset();
    Dac dac = insertDac();
    datasetDAO.updateDatasetDacId(dataset.getDataSetId(), dac.getDacId());

    Dataset foundDataset = datasetDAO.findDatasetById(dataset.getDataSetId());
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

    Dataset foundDataset = datasetDAO.findDatasetById(d1.getDataSetId());
    assertNotNull(foundDataset);
    assertEquals(dac.getDacId(), foundDataset.getDacId());
    assertFalse(foundDataset.getProperties().isEmpty());
    assertFalse(foundDataset.getDeletable());

    Dataset foundDataset2 = datasetDAO.findDatasetById(d2.getDataSetId());
    assertNotNull(foundDataset2);
    assertEquals(dac.getDacId(), foundDataset2.getDacId());
    assertFalse(foundDataset2.getProperties().isEmpty());
    assertFalse(foundDataset2.getDeletable());
  }

  @Test
  void testTranslatedDataUse() {
    Dataset d1 = insertDataset();

    String tdu = RandomStringUtils.randomAlphabetic(10);
    datasetDAO.updateDatasetTranslatedDataUse(d1.getDataSetId(), tdu);

    d1 = datasetDAO.findDatasetById(d1.getDataSetId());

    assertEquals(tdu, d1.getTranslatedDataUse());
  }

  @Test
  void testUpdateDatasetName() {
    Dataset dataset = insertDataset();
    String newName = RandomStringUtils.randomAlphabetic(25);
    datasetDAO.updateDatasetName(dataset.getDataSetId(), newName);
    Dataset foundDataset = datasetDAO.findDatasetById(dataset.getDataSetId());
    assertNotNull(foundDataset);
    assertEquals(newName, foundDataset.getName());
  }

  @Test
  void testFindDatasetByAlias() {
    Dataset dataset = insertDataset();

    Dataset foundDataset = datasetDAO.findDatasetByAlias(dataset.getAlias());

    assertNotNull(foundDataset);
    assertEquals(dataset.getDataSetId(), foundDataset.getDataSetId());
  }

  @Test
  void testFindDatasetsByAlias() {
    Dataset dataset1 = insertDataset();
    Dataset dataset2 = insertDataset();

    List<Dataset> foundDatasets = datasetDAO.findDatasetsByAlias(
        List.of(dataset1.getAlias(), dataset2.getAlias()));
    List<Integer> foundDatasetIds = foundDatasets.stream().map(Dataset::getDataSetId).toList();
    assertNotNull(foundDatasets);
    assertTrue(
        foundDatasetIds.containsAll(List.of(dataset1.getDataSetId(), dataset2.getDataSetId())));
  }

  @Test
  void testGetNIHInstitutionalFile() {
    Dataset dataset = insertDataset();

    // create unrelated file with the same id as dataset id but different category, timestamp before
    createFileStorageObject(
        dataset.getDataSetId().toString(),
        FileCategory.ALTERNATIVE_DATA_SHARING_PLAN
    );

    FileStorageObject nihFile = createFileStorageObject(
        dataset.getDataSetId().toString(),
        FileCategory.NIH_INSTITUTIONAL_CERTIFICATION
    );

    // create unrelated files with timestamp later than the NIH file: one attached to dataset, one
    // completely separate from the dataset. ensures that the Mapper is selecting only the NIH file.
    createFileStorageObject();
    createFileStorageObject(
        dataset.getDataSetId().toString(),
        FileCategory.DATA_USE_LETTER
    );

    Dataset found = datasetDAO.findDatasetById(dataset.getDataSetId());

    assertEquals(nihFile, found.getNihInstitutionalCertificationFile());
    assertEquals(nihFile.getBlobId(),
        found.getNihInstitutionalCertificationFile().getBlobId());
  }

  @Test
  void testGetNIHInstitutionalFile_AlwaysLatestUpdated() {
    Dataset dataset = insertDataset();

    String fileName = RandomStringUtils.randomAlphabetic(10);
    String bucketName = RandomStringUtils.randomAlphabetic(10);
    String gcsFileUri = RandomStringUtils.randomAlphabetic(10);
    User createUser = createUser();

    Integer nihFileIdCreatedFirstUpdatedSecond = fileStorageObjectDAO.insertNewFile(
        fileName,
        FileCategory.NIH_INSTITUTIONAL_CERTIFICATION.getValue(),
        bucketName,
        gcsFileUri,
        dataset.getDataSetId().toString(),
        createUser.getUserId(),
        Instant.ofEpochMilli(100)
    );

    Integer nihFileIdCreatedSecondUpdatedFirst = fileStorageObjectDAO.insertNewFile(
        fileName,
        FileCategory.NIH_INSTITUTIONAL_CERTIFICATION.getValue(),
        bucketName,
        gcsFileUri,
        dataset.getDataSetId().toString(),
        createUser.getUserId(),
        Instant.ofEpochMilli(110)
    );

    User updateUser = createUser();

    fileStorageObjectDAO.updateFileById(
        nihFileIdCreatedSecondUpdatedFirst,
        RandomStringUtils.randomAlphabetic(20),
        RandomStringUtils.randomAlphabetic(20),
        updateUser.getUserId(),
        Instant.ofEpochMilli(120));

    fileStorageObjectDAO.updateFileById(
        nihFileIdCreatedFirstUpdatedSecond,
        RandomStringUtils.randomAlphabetic(20),
        RandomStringUtils.randomAlphabetic(20),
        updateUser.getUserId(),
        Instant.ofEpochMilli(130));

    Dataset found = datasetDAO.findDatasetById(dataset.getDataSetId());

    // returns last updated file
    assertEquals(nihFileIdCreatedFirstUpdatedSecond,
        found.getNihInstitutionalCertificationFile().getFileStorageObjectId());
  }

  @Test
  void testGetNIHInstitutionalFile_AlwaysLatestCreated() {
    Dataset dataset = insertDataset();

    String fileName = RandomStringUtils.randomAlphabetic(10);
    String bucketName = RandomStringUtils.randomAlphabetic(10);
    String gcsFileUri = RandomStringUtils.randomAlphabetic(10);
    User createUser = createUser();

    Integer nihFileIdCreatedFirst = fileStorageObjectDAO.insertNewFile(
        fileName,
        FileCategory.NIH_INSTITUTIONAL_CERTIFICATION.getValue(),
        bucketName,
        gcsFileUri,
        dataset.getDataSetId().toString(),
        createUser.getUserId(),
        Instant.ofEpochMilli(100)
    );

    User updateUser = createUser();

    fileStorageObjectDAO.updateFileById(
        nihFileIdCreatedFirst,
        RandomStringUtils.randomAlphabetic(20),
        RandomStringUtils.randomAlphabetic(20),
        updateUser.getUserId(),
        Instant.ofEpochMilli(120));

    Integer nihFileIdCreatedSecond = fileStorageObjectDAO.insertNewFile(
        fileName,
        FileCategory.NIH_INSTITUTIONAL_CERTIFICATION.getValue(),
        bucketName,
        gcsFileUri,
        dataset.getDataSetId().toString(),
        createUser.getUserId(),
        Instant.ofEpochMilli(130)
    );

    Dataset found = datasetDAO.findDatasetById(dataset.getDataSetId());

    // returns last updated file
    assertEquals(nihFileIdCreatedSecond,
        found.getNihInstitutionalCertificationFile().getFileStorageObjectId());
  }

  @Test
  void testGetNIHInstitutionalFile_NotDeleted() {
    Dataset dataset = insertDataset();

    FileStorageObject nihFile = createFileStorageObject(
        dataset.getDataSetId().toString(),
        FileCategory.NIH_INSTITUTIONAL_CERTIFICATION
    );

    User deleteUser = createUser();

    fileStorageObjectDAO.deleteFileById(
        nihFile.getFileStorageObjectId(),
        deleteUser.getUserId(),
        Instant.now()
    );

    Dataset found = datasetDAO.findDatasetById(dataset.getDataSetId());

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
    datasetDAO.updateDatasetDacId(dataset.getDataSetId(), dac.getDacId());

    List<Dataset> datasets = datasetDAO.findDatasetsByIdList(List.of(dataset.getDataSetId()));
    assertFalse(datasets.isEmpty());
    assertEquals(1, datasets.size());
    assertEquals(dac.getDacId(), datasets.get(0).getDacId());
    assertFalse(datasets.get(0).getProperties().isEmpty());
    assertNotNull(datasets.get(0).getCreateUser());
  }

  // User -> UserRoles -> DACs -> Consents -> Consent Associations -> DataSets
  @Test
  void testFindDataSetsByAuthUserEmail() {
    Dataset dataset = insertDataset();
    Dac dac = insertDac();
    datasetDAO.updateDatasetDacId(dataset.getDataSetId(), dac.getDacId());
    User user = createUser();
    createUserRole(UserRoles.CHAIRPERSON.getRoleId(), user.getUserId(), dac.getDacId());

    List<Integer> datasetIds = datasetDAO.findDatasetIdsByDACUserId(user.getUserId());
    assertFalse(datasetIds.isEmpty());
    assertTrue(datasetIds.contains(dataset.getDataSetId()));
  }

  @Test
  void testFindDatasetPropertiesByDatasetId() {
    Dataset d = insertDataset();
    Set<DatasetProperty> properties = datasetDAO.findDatasetPropertiesByDatasetId(d.getDataSetId());
    assertEquals(properties.size(), 1);
  }

  @Test
  void testUpdateDataset() {
    Dataset d = insertDataset();
    Dac dac = insertDac();
    String name = RandomStringUtils.random(20, true, true);
    Timestamp now = new Timestamp(new Date().getTime());
    Integer userId = RandomUtils.nextInt(1, 1000);

    datasetDAO.updateDataset(d.getDataSetId(), name, now, userId, dac.getDacId());
    Dataset updated = datasetDAO.findDatasetById(d.getDataSetId());

    assertEquals(name, updated.getName());
    assertEquals(now, updated.getUpdateDate());
    assertEquals(userId, updated.getUpdateUserId());
    assertEquals(dac.getDacId(), updated.getDacId());
  }

  @Test
  void testUpdateDatasetProperty() {
    Dataset d = insertDataset();
    Set<DatasetProperty> properties = datasetDAO.findDatasetPropertiesByDatasetId(d.getDataSetId());
    DatasetProperty originalProperty = properties.stream().toList().get(0);
    DatasetProperty newProperty = new DatasetProperty(d.getDataSetId(), 1, "Updated Value",
        PropertyType.String, new Date());
    List<DatasetProperty> updatedProperties = new ArrayList<>();
    updatedProperties.add(newProperty);
    datasetDAO.updateDatasetProperty(d.getDataSetId(), updatedProperties.get(0).getPropertyKey(),
        updatedProperties.get(0).getPropertyValue().toString());
    Set<DatasetProperty> returnedProperties = datasetDAO.findDatasetPropertiesByDatasetId(
        d.getDataSetId());
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
        d.getDataSetId());
    DatasetProperty propertyToDelete = new ArrayList<>(oldProperties).get(0);
    datasetDAO.deleteDatasetPropertyByKey(d.getDataSetId(), propertyToDelete.getPropertyKey());

    List<DatasetProperty> newProps = List.of(
        new DatasetProperty(
            d.getDataSetId(),
            1,
            "10",
            PropertyType.Number,
            new Date())
    );
    datasetDAO.insertDatasetProperties(newProps);

    Dataset dWithProps = datasetDAO.findDatasetById(d.getDataSetId());

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
        d.getDataSetId());
    DatasetProperty propertyToDelete = new ArrayList<>(oldProperties).get(0);
    datasetDAO.deleteDatasetPropertyByKey(d.getDataSetId(), propertyToDelete.getPropertyKey());

    DatasetProperty propToAdd = new DatasetProperty(
        d.getDataSetId(),
        1,
        date.toString(),
        PropertyType.Date,
        new Date());

    propToAdd.setSchemaProperty("date");
    List<DatasetProperty> newProps = List.of(
        propToAdd
    );
    datasetDAO.insertDatasetProperties(newProps);

    Set<DatasetProperty> props = datasetDAO.findDatasetPropertiesByDatasetId(d.getDataSetId());
    assertEquals(1, props.size());
    DatasetProperty prop = props.stream().findFirst().get();
    assertEquals(PropertyType.Date, prop.getPropertyType());
    assertEquals(date.toString(), prop.getPropertyValueAsString());
  }

  @Test
  void testCreateBooleanTypedDatasetProperty() {
    Dataset d = insertDataset();
    Boolean bool = Boolean.FALSE;

    Set<DatasetProperty> oldProperties = datasetDAO.findDatasetPropertiesByDatasetId(
        d.getDataSetId());
    DatasetProperty propertyToDelete = new ArrayList<>(oldProperties).get(0);
    datasetDAO.deleteDatasetPropertyByKey(d.getDataSetId(), propertyToDelete.getPropertyKey());

    List<DatasetProperty> newProps = List.of(
        new DatasetProperty(
            d.getDataSetId(),
            1,
            bool.toString(),
            PropertyType.Boolean,
            new Date())
    );
    datasetDAO.insertDatasetProperties(newProps);

    Dataset dWithProps = datasetDAO.findDatasetById(d.getDataSetId());

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
        d.getDataSetId());
    DatasetProperty propertyToDelete = new ArrayList<>(oldProperties).get(0);
    datasetDAO.deleteDatasetPropertyByKey(d.getDataSetId(), propertyToDelete.getPropertyKey());

    List<DatasetProperty> newProps = List.of(
        new DatasetProperty(
            d.getDataSetId(),
            1,
            jsonObject.toString(),
            PropertyType.Json,
            new Date())
    );
    datasetDAO.insertDatasetProperties(newProps);

    Dataset dWithProps = datasetDAO.findDatasetById(d.getDataSetId());

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
        d.getDataSetId());
    DatasetProperty propertyToDelete = new ArrayList<>(oldProperties).get(0);
    datasetDAO.deleteDatasetPropertyByKey(d.getDataSetId(), propertyToDelete.getPropertyKey());

    List<DatasetProperty> newProps = List.of(
        new DatasetProperty(
            d.getDataSetId(),
            1,
            value,
            PropertyType.String,
            new Date())
    );
    datasetDAO.insertDatasetProperties(newProps);

    Dataset dWithProps = datasetDAO.findDatasetById(d.getDataSetId());

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
        d.getDataSetId());
    DatasetProperty propertyToDelete = new ArrayList<>(oldProperties).get(0);
    datasetDAO.deleteDatasetPropertyByKey(d.getDataSetId(), propertyToDelete.getPropertyKey());

    List<DatasetProperty> newProps = List.of(
        new DatasetProperty(
            d.getDataSetId(),
            1,
            schemaValue,
            "asdf",
            PropertyType.String,
            new Date())
    );
    datasetDAO.insertDatasetProperties(newProps);

    Dataset dWithProps = datasetDAO.findDatasetById(d.getDataSetId());

    assertEquals(1, dWithProps.getProperties().size());
    DatasetProperty prop = new ArrayList<>(dWithProps.getProperties()).get(0);
    assertEquals(PropertyType.String, prop.getPropertyType());
    assertEquals(schemaValue, prop.getSchemaProperty());
  }

  @Test
  void testDeleteDatasetPropertyByKey() {
    Dataset d = insertDataset();
    Set<DatasetProperty> properties = datasetDAO.findDatasetPropertiesByDatasetId(d.getDataSetId());
    DatasetProperty propertyToDelete = properties.stream().toList().get(0);
    datasetDAO.deleteDatasetPropertyByKey(d.getDataSetId(), propertyToDelete.getPropertyKey());
    Set<DatasetProperty> returnedProperties = datasetDAO.findDatasetPropertiesByDatasetId(
        d.getDataSetId());
    assertNotEquals(properties.size(), returnedProperties.size());
  }

  @Test
  void testFindAllDatasets() {
    List<Dataset> datasetList = IntStream.range(1, 5).mapToObj(i -> {
      Dataset dataset = insertDataset();
      return dataset;
    }).toList();

    List<Dataset> datasets = datasetDAO.findAllDatasets();
    assertFalse(datasets.isEmpty());
    assertEquals(datasetList.size(), datasets.size());
    List<Integer> insertedDatasetIds = datasetList.stream().map(Dataset::getDataSetId).toList();
    List<Integer> foundDatasetIds = datasets.stream().map(Dataset::getDataSetId).toList();
    assertTrue(foundDatasetIds.containsAll(insertedDatasetIds));
    assertTrue(insertedDatasetIds.containsAll(foundDatasetIds));
  }

  @Test
  void testFindAllDatasetIds() {
    List<Integer> insertedDatasetIds = IntStream.range(1, 5).mapToObj(i -> {
      Dataset dataset = insertDataset();
      return dataset.getDataSetId();
    }).toList();
    List<Integer> datasetIds = datasetDAO.findAllDatasetIds();
    assertThat(datasetIds, contains(insertedDatasetIds.toArray()));
  }

  @Test
  void testZeroAliasValuesValid() {
    Dataset dataset = insertDataset();
    jdbi.useHandle(handle -> {
      Update update = handle.createUpdate(" UPDATE dataset SET alias = 0 WHERE dataset_id = :dataset_id ");
      update.bind("dataset_id", dataset.getDataSetId());
      update.execute();
      handle.commit();
    });
    Dataset updatedDataset = datasetDAO.findDatasetById(dataset.getDataSetId());
    assertEquals(0, updatedDataset.getAlias());
    updatedDataset.setDatasetIdentifier();
    assertNotNull(updatedDataset.getDatasetIdentifier());
  }

  @Test
  void testFindAllStudyNames() {
    Dataset ds1 = insertDataset();
    String ds1Name = RandomStringUtils.randomAlphabetic(20);
    createDatasetProperty(ds1.getDataSetId(), "studyName", ds1Name, PropertyType.String);

    Dataset ds2 = insertDataset();
    String ds2Name = RandomStringUtils.randomAlphabetic(25);
    createDatasetProperty(ds2.getDataSetId(), "studyName", ds2Name, PropertyType.String);

    Dataset ds3 = insertDataset();
    String ds3Name = RandomStringUtils.randomAlphabetic(15);
    createDatasetProperty(ds3.getDataSetId(), "studyName", ds3Name, PropertyType.String);

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
  void testFindDatasetsByDacIds() {
    Dataset dataset = insertDataset();
    Dac dac = insertDac();

    Dataset datasetTwo = insertDataset();
    Dac dacTwo = insertDac();

    datasetDAO.updateDatasetDacId(dataset.getDataSetId(), dac.getDacId());
    datasetDAO.updateDatasetDacId(datasetTwo.getDataSetId(), dacTwo.getDacId());

    List<Integer> datasetIds = List.of(dataset.getDataSetId(), datasetTwo.getDataSetId());
    Set<DatasetDTO> datasets = datasetDAO.findDatasetsByDacIds(
        List.of(dac.getDacId(), dacTwo.getDacId()));
    datasets.forEach(d -> assertTrue(datasetIds.contains(d.getDataSetId())));
  }

  @Test
  void testFindDatasetListByDacIds() {
    Dataset dataset = insertDataset();
    Dac dac = insertDac();

    Dataset datasetTwo = insertDataset();
    Dac dacTwo = insertDac();

    datasetDAO.updateDatasetDacId(dataset.getDataSetId(), dac.getDacId());
    datasetDAO.updateDatasetDacId(datasetTwo.getDataSetId(), dacTwo.getDacId());

    List<Integer> datasetIds = List.of(dataset.getDataSetId(), datasetTwo.getDataSetId());
    List<Dataset> datasets = datasetDAO.findDatasetListByDacIds(
        List.of(dac.getDacId(), dacTwo.getDacId()));
    datasets.forEach(d -> assertTrue(datasetIds.contains(d.getDataSetId())));
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

    datasetDAO.updateDatasetDataUse(dataset.getDataSetId(), newDataUse.toString());
    Dataset updated = datasetDAO.findDatasetById(dataset.getDataSetId());
    assertEquals(newDataUse, updated.getDataUse());
    assertNotEquals(oldDataUse, updated.getDataUse());
  }

  @Test
  void testUpdateDatasetCreateUserId() {
    Dataset dataset = insertDataset();
    User user = createUser();
    datasetDAO.updateDatasetCreateUserId(dataset.getDataSetId(), user.getUserId());
    Dataset updated = datasetDAO.findDatasetById(dataset.getDataSetId());
    assertEquals(user.getUserId(), updated.getCreateUserId());
  }

  @Test
  void testFindDatasetWithDataUseByIdList() {
    Dataset dataset = insertDataset();
    Dac dac = insertDac();
    datasetDAO.updateDatasetDacId(dataset.getDataSetId(), dac.getDacId());

    Set<Dataset> datasets = datasetDAO.findDatasetWithDataUseByIdList(
        Collections.singletonList(dataset.getDataSetId()));
    assertFalse(datasets.isEmpty());
    List<Integer> datasetIds = datasets.stream().map(Dataset::getDataSetId).toList();
    assertTrue(datasetIds.contains(dataset.getDataSetId()));
  }

  @Test
  void testUpdateDatasetApproval() {
    User updateUser = createUser();
    Dataset dataset = insertDataset();
    datasetDAO.updateDatasetApproval(true, Instant.now(), updateUser.getUserId(),
        dataset.getDataSetId());
    Dataset updatedDataset = datasetDAO.findDatasetById(dataset.getDataSetId());
    assertNotNull(updatedDataset);
    assertTrue(updatedDataset.getDacApproval());
    datasetDAO.updateDatasetApproval(false, Instant.now(), updateUser.getUserId(),
        dataset.getDataSetId());
    Dataset updatedDatasetAfterApprovalFalse = datasetDAO.findDatasetById(dataset.getDataSetId());
    assertNotNull(updatedDatasetAfterApprovalFalse);
    assertEquals(dataset.getDataSetId(),
        updatedDatasetAfterApprovalFalse.getDataSetId());
    assertFalse(updatedDatasetAfterApprovalFalse.getDacApproval());

  }

  @Test
  void testInsertDatasetAudit() {
    Dataset d = createDataset();
    DatasetAudit audit = new DatasetAudit(
        d.getDataSetId(),
        "objectid",
        "name",
        new Date(),
        d.getCreateUserId(),
        "action");
    datasetDAO.insertDatasetAudit(audit);
  }

  @Test
  void testUniqueDatasetName() {
    Dataset dataset0 = createStaticDataset();
    try {
      Dataset dataset1 = createStaticDataset();
      Assertions.fail();
    } catch (Exception e) {
      assertTrue(e.getMessage().contains("duplicate key value violates unique constraint"));
    }
  }

  @Test
  void testDatasetWithStudy() {
    Study study = insertStudyWithProperties();

    Dataset ds = createDataset();
    createDataset(); // create unrelated datasets (for testing study's dataset ids)
    Dataset otherDsOnStudy = createDataset();

    datasetDAO.updateStudyId(ds.getDataSetId(), study.getStudyId());
    datasetDAO.updateStudyId(otherDsOnStudy.getDataSetId(), study.getStudyId());

    createDataset(); // create unrelated datasets (for testing study's dataset ids)

    FileStorageObject fso = createFileStorageObject(study.getUuid().toString(),
        FileCategory.ALTERNATIVE_DATA_SHARING_PLAN);

    ds = datasetDAO.findDatasetById(ds.getDataSetId());

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
    assertTrue(ds.getStudy().getDatasetIds().contains(ds.getDataSetId()));
    assertTrue(
        ds.getStudy().getDatasetIds().contains(otherDsOnStudy.getDataSetId()));
  }

  @Test
  void testGetApprovedDatasets() {

    // user with a mix of approved and unapproved datasets
    User user = createUser();

    Dataset dataset1 = createDataset(false);
    Dataset dataset2 = createDataset(true);
    Dataset dataset3 = createDataset(false);
    Dataset dataset4 = createDataset(true);

    Timestamp timestamp = new Timestamp(new Date().getTime());

    Dac dac1 = insertDac();
    datasetDAO.updateDataset(dataset1.getDataSetId(), dataset1.getDatasetName(), timestamp,
        user.getUserId(), dac1.getDacId());
    datasetDAO.updateDataset(dataset2.getDataSetId(), dataset2.getDatasetName(), timestamp,
        user.getUserId(), dac1.getDacId());

    Dac dac2 = insertDac();
    datasetDAO.updateDataset(dataset3.getDataSetId(), dataset3.getDatasetName(), timestamp,
        user.getUserId(), dac2.getDacId());
    datasetDAO.updateDataset(dataset4.getDataSetId(), dataset4.getDatasetName(), timestamp,
        user.getUserId(), dac2.getDacId());

    DarCollection dar1 = createDarCollectionWithDatasets(dac1.getDacId(), user, List.of(dataset1));
    DarCollection dar2 = createDarCollectionWithDatasets(dac2.getDacId(), user,
        List.of(dataset2, dataset3));
    DarCollection dar3 = createDarCollectionWithDatasets(dac2.getDacId(), user, List.of(dataset4));
    List<DarCollection> allDarCollections = List.of(dar1, dar2, dar3);

    Map<Integer, Boolean> expectedFinalVotesForDatasets = Map.of(dataset1.getDataSetId(), false,
        dataset2.getDataSetId(), false, dataset3.getDataSetId(), true, dataset4.getDataSetId(),
        true);

    Map<Integer, Election> elections = new HashMap<>();

    for (DarCollection dar : allDarCollections) {
      for (Map.Entry<String, DataAccessRequest> e : dar.getDars().entrySet()) {
        for (Integer id : e.getValue().getDatasetIds()) {
          elections.put(id, createDataAccessElectionWithVotes(e.getKey(), id, user.getUserId(),
              expectedFinalVotesForDatasets.get(id)));
        }
      }
    }

    List<ApprovedDataset> approvedDatasets = datasetDAO.getApprovedDatasets(user.getUserId());
    assertNotNull(approvedDatasets);

    // checks that all datasets in the result are approved
    approvedDatasets.forEach(approvedDataset -> {
      assertTrue(datasetDAO.findDatasetByAlias(approvedDataset.getAlias()).getDacApproval());
    });

    ApprovedDataset expectedApprovedDataset1 = new ApprovedDataset(dataset3.getAlias(),
        dar2.getDarCode(), dataset3.getDatasetName(), dac2.getName(),
        elections.get(dataset3.getDataSetId()).getLastUpdate());
    ApprovedDataset expectedApprovedDataset2 = new ApprovedDataset(dataset4.getAlias(),
        dar3.getDarCode(), dataset4.getDatasetName(), dac2.getName(),
        elections.get(dataset4.getDataSetId()).getLastUpdate());
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

  @Test
  void testGetApprovedDatasetsWhenNone() {

    // user with only unapproved datasets
    User user = createUser();

    Dataset dataset1 = createDataset(false);
    Dataset dataset2 = createDataset(true);

    Timestamp timestamp = new Timestamp(new Date().getTime());

    Dac dac1 = insertDac();
    datasetDAO.updateDataset(dataset1.getDataSetId(), dataset1.getDatasetName(), timestamp,
        user.getUserId(), dac1.getDacId());
    datasetDAO.updateDataset(dataset2.getDataSetId(), dataset2.getDatasetName(), timestamp,
        user.getUserId(), dac1.getDacId());

    DarCollection dar1 = createDarCollectionWithDatasets(dac1.getDacId(), user,
        List.of(dataset1, dataset2));

    for (Map.Entry<String, DataAccessRequest> e : dar1.getDars().entrySet()) {
      for (Integer id : e.getValue().getDatasetIds()) {
        createDataAccessElectionWithVotes(e.getKey(), id, user.getUserId(), false);
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
  void testFindDatasetSummariesByQuery() {
    Dataset dataset = createDataset();
    Dataset dataset2 = createDataset();
    User user = createUser();
    datasetDAO.updateDatasetApproval(true, Instant.now(), user.getUserId(), dataset.getDataSetId());
    datasetDAO.updateDatasetApproval(true, Instant.now(), user.getUserId(),
        dataset2.getDataSetId());

    List<DatasetSummary> summaries = datasetDAO.findDatasetSummariesByQuery(dataset.getName());
    assertNotNull(summaries);
    assertFalse(summaries.isEmpty());
    assertEquals(dataset.getDataSetId(),
        summaries.stream().map(DatasetSummary::id).toList().get(0));
    assertNotEquals(dataset2.getDataSetId(),
        summaries.stream().map(DatasetSummary::id).toList().get(0));
  }

  @Test
  void testFindDatasetSummariesByQuery_NotApproved() {
    Dataset dataset = createDataset();

    List<DatasetSummary> summaries = datasetDAO.findDatasetSummariesByQuery(dataset.getName());
    assertNotNull(summaries);
    assertTrue(summaries.isEmpty());
  }

  @Test
  void testFindDatasetSummariesByQuery_NullQuery() {
    createDataset();

    List<DatasetSummary> summaries = datasetDAO.findDatasetSummariesByQuery(null);
    assertNotNull(summaries);
    assertTrue(summaries.isEmpty());
  }

  @Test
  void testFindDatasetSummariesByQuery_EmptyQuery() {
    createDataset();

    List<DatasetSummary> summaries = datasetDAO.findDatasetSummariesByQuery("");
    assertNotNull(summaries);
    assertTrue(summaries.isEmpty());
  }

  private DarCollection createDarCollectionWithDatasets(int dacId, User user,
      List<Dataset> datasets) {
    String darCode = "DAR-" + RandomUtils.nextInt(1, 999999);
    Integer collectionId = darCollectionDAO.insertDarCollection(darCode, user.getUserId(),
        new Date());
    IntStream.range(0, datasets.size()).forEach(index -> {
      Dataset dataset = datasets.get(index);
      datasetDAO.updateDatasetDacId(dataset.getDataSetId(), dacId);
      createDataAccessRequestWithDatasetAndCollectionInfo(collectionId, dataset.getDataSetId(),
          user.getUserId());
    });
    return darCollectionDAO.findDARCollectionByCollectionId(collectionId);
  }

  private void createUserRole(Integer roleId, Integer userId, Integer dacId) {
    dacDAO.addDacMember(roleId, userId, dacId);
  }

  private FileStorageObject createFileStorageObject() {
    FileCategory category = List.of(FileCategory.values())
        .get(new Random().nextInt(FileCategory.values().length));
    String entityId = RandomStringUtils.randomAlphabetic(10);

    return createFileStorageObject(entityId, category);
  }

  private FileStorageObject createFileStorageObject(String entityId, FileCategory category) {
    String fileName = RandomStringUtils.randomAlphabetic(10);
    String bucketName = RandomStringUtils.randomAlphabetic(10);
    String gcsFileUri = RandomStringUtils.randomAlphabetic(10);
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

  private Dataset insertDataset() {
    User user = createUser();
    String name = "Name_" + RandomStringUtils.random(20, true, true);
    Timestamp now = new Timestamp(new Date().getTime());
    String objectId = "Object ID_" + RandomStringUtils.random(20, true, true);
    DataUse dataUse = new DataUseBuilder().setGeneralUse(true).build();
    Integer id = datasetDAO.insertDataset(name, now, user.getUserId(), objectId,
        dataUse.toString(), null);
    createDatasetProperties(id);
    return datasetDAO.findDatasetById(id);
  }


  protected void createDatasetProperty(Integer datasetId, String schemaProperty, String value,
      PropertyType type) {
    List<DatasetProperty> list = new ArrayList<>();
    DatasetProperty dsp = new DatasetProperty();
    dsp.setDataSetId(datasetId);
    dsp.setPropertyKey(1);
    dsp.setSchemaProperty(schemaProperty);
    dsp.setPropertyValue(type.coerce(value));
    dsp.setPropertyType(type);
    dsp.setCreateDate(new Date());
    list.add(dsp);
    datasetDAO.insertDatasetProperties(list);
  }

  private Dac insertDac() {
    Integer id = dacDAO.createDac(
        "Test_" + RandomStringUtils.random(20, true, true),
        "Test_" + RandomStringUtils.random(20, true, true),
        new Date());
    return dacDAO.findById(id);
  }

  private Study insertStudyWithProperties() {
    User u = createUser();

    return insertStudyWithProperties(u);
  }

  private Study insertStudyWithProperties(User user) {

    String name = RandomStringUtils.randomAlphabetic(20);
    String description = RandomStringUtils.randomAlphabetic(20);
    List<String> dataTypes = List.of(
        RandomStringUtils.randomAlphabetic(20),
        RandomStringUtils.randomAlphabetic(20)
    );
    String piName = RandomStringUtils.randomAlphabetic(20);
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

  private Study insertPrivateStudyWithProperties() {
    User u = createUser();
    return insertPrivateStudyWithProperties(u);
  }

  private Study insertPrivateStudyWithProperties(User u) {

    String name = RandomStringUtils.randomAlphabetic(20);
    String description = RandomStringUtils.randomAlphabetic(20);
    List<String> dataTypes = List.of(
        RandomStringUtils.randomAlphabetic(20),
        RandomStringUtils.randomAlphabetic(20)
    );
    String piName = RandomStringUtils.randomAlphabetic(20);
    Boolean publicVisibility = false;

    Integer id = studyDAO.insertStudy(
        name,
        description,
        piName,
        dataTypes,
        publicVisibility,
        u.getUserId(),
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

  private DataAccessRequest createDataAccessRequestWithDatasetAndCollectionInfo(int collectionId,
      int datasetId, int userId) {
    DataAccessRequestData data = new DataAccessRequestData();
    data.setProjectTitle(RandomStringUtils.randomAlphabetic(10));
    String referenceId = RandomStringUtils.randomAlphanumeric(20);
    dataAccessRequestDAO.insertDataAccessRequest(collectionId, referenceId, userId, new Date(),
        new Date(), new Date(), new Date(), data);
    dataAccessRequestDAO.insertDARDatasetRelation(referenceId, datasetId);
    return dataAccessRequestDAO.findByReferenceId(referenceId);
  }

  private void createDatasetProperties(Integer datasetId) {
    List<DatasetProperty> list = new ArrayList<>();
    DatasetProperty dsp = new DatasetProperty();
    dsp.setDataSetId(datasetId);
    dsp.setPropertyKey(1);
    dsp.setPropertyValue("Test_PropertyValue");
    dsp.setCreateDate(new Date());
    list.add(dsp);
    datasetDAO.insertDatasetProperties(list);
  }

  private Dataset createDataset() {
    User user = createUser();
    String name = "Name_" + RandomStringUtils.random(20, true, true);
    Timestamp now = new Timestamp(new Date().getTime());
    String objectId = "Object ID_" + RandomStringUtils.random(20, true, true);
    DataUse dataUse = new DataUseBuilder().setGeneralUse(true).build();
    Integer id = datasetDAO.insertDataset(name, now, user.getUserId(), objectId,
        dataUse.toString(), null);
    createDatasetProperties(id);
    return datasetDAO.findDatasetById(id);
  }


  private Dataset createDataset(boolean dacApproval) {
    User user = createUser();
    String name = "Name_" + RandomStringUtils.random(20, true, true);
    Timestamp now = new Timestamp(new Date().getTime());
    Instant instant = Instant.now();
    String objectId = "Object ID_" + RandomStringUtils.random(20, true, true);
    DataUse dataUse = new DataUseBuilder().setGeneralUse(true).build();
    Integer id = datasetDAO.insertDataset(name, now, user.getUserId(), objectId,
        dataUse.toString(), null);
    datasetDAO.updateDatasetApproval(dacApproval, instant, user.getUserId(), id);
    createDatasetProperties(id);
    return datasetDAO.findDatasetById(id);
  }

  private Dataset createStaticDataset() {
    User user = createUser();
    String name = "test_unique_constraint_dataset_name";
    Timestamp now = new Timestamp(new Date().getTime());
    String objectId = "Object ID_" + RandomStringUtils.random(20, true, true);
    DataUse dataUse = new DataUseBuilder().setGeneralUse(true).build();
    Integer id = datasetDAO.insertDataset(name, now, user.getUserId(), objectId,
        dataUse.toString(), null);
    createDatasetProperties(id);
    return datasetDAO.findDatasetById(id);
  }

  private Election createDataAccessElectionWithVotes(String referenceId, Integer datasetId,
      Integer userId, boolean finalVoteApproval) {
    Integer electionId = electionDAO.insertElection(
        ElectionType.DATA_ACCESS.getValue(),
        ElectionStatus.OPEN.getValue(),
        new Date(),
        referenceId,
        datasetId
    );
    Integer voteId = voteDAO.insertVote(userId, electionId, VoteType.FINAL.getValue());
    voteDAO.updateVote(finalVoteApproval, "rationale", new Date(), voteId, false, electionId,
        new Date(), false);
    electionDAO.updateElectionById(electionId, ElectionStatus.CLOSED.getValue(), new Date());
    datasetDAO.updateDatasetApproval(finalVoteApproval, Instant.now(), userId, datasetId);
    return electionDAO.findElectionById(electionId);
  }


}
