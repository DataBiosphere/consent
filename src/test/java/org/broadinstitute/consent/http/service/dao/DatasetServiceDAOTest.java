package org.broadinstitute.consent.http.service.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.cloud.storage.BlobId;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import org.apache.commons.lang3.RandomStringUtils;
import org.broadinstitute.consent.http.db.DAOTestHelper;
import org.broadinstitute.consent.http.enumeration.FileCategory;
import org.broadinstitute.consent.http.enumeration.PropertyType;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.models.DataUseBuilder;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.DatasetPatch;
import org.broadinstitute.consent.http.models.DatasetProperty;
import org.broadinstitute.consent.http.models.Dictionary;
import org.broadinstitute.consent.http.models.FileStorageObject;
import org.broadinstitute.consent.http.models.Study;
import org.broadinstitute.consent.http.models.StudyProperty;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.service.dao.DatasetServiceDAO.DatasetInsert;
import org.broadinstitute.consent.http.service.dao.DatasetServiceDAO.DatasetUpdate;
import org.broadinstitute.consent.http.service.dao.DatasetServiceDAO.StudyUpdate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DatasetServiceDAOTest extends DAOTestHelper {

  private DatasetServiceDAO serviceDAO;

  @BeforeEach
  public void setUp() {
    serviceDAO = new DatasetServiceDAO(jdbi, datasetDAO, studyDAO);
  }

  @Test
  void testDeleteDataset() throws Exception {
    Dataset dataset = createDataset();

    serviceDAO.deleteDataset(dataset, dataset.getCreateUserId());
    // Assert that the dataset is deleted:
    Dataset deleted = datasetDAO.findDatasetById(dataset.getDatasetId());
    assertNull(deleted);

  }

  @Test
  void testInsertDatasets() throws Exception {

    Dac dac = createDac();
    User user = createUser();

    DatasetProperty prop1 = new DatasetProperty();
    prop1.setSchemaProperty(RandomStringUtils.randomAlphabetic(10));
    prop1.setPropertyName(RandomStringUtils.randomAlphabetic(10));
    prop1.setPropertyType(PropertyType.Number);
    prop1.setPropertyValue(new Random().nextInt());

    DatasetProperty prop2 = new DatasetProperty();
    prop2.setSchemaProperty(RandomStringUtils.randomAlphabetic(10));
    prop2.setPropertyName(RandomStringUtils.randomAlphabetic(10));
    prop2.setPropertyType(PropertyType.Date);
    prop2.setPropertyValueAsString("2000-10-20");

    FileStorageObject file1 = new FileStorageObject();
    file1.setMediaType(RandomStringUtils.randomAlphabetic(20));
    file1.setCategory(FileCategory.NIH_INSTITUTIONAL_CERTIFICATION);
    file1.setBlobId(
        BlobId.of(RandomStringUtils.randomAlphabetic(10), RandomStringUtils.randomAlphabetic(10)));
    file1.setFileName(RandomStringUtils.randomAlphabetic(10));

    DatasetServiceDAO.DatasetInsert insert = new DatasetServiceDAO.DatasetInsert(
        RandomStringUtils.randomAlphabetic(20),
        dac.getDacId(),
        new DataUseBuilder().setStigmatizeDiseases(true).setGeneralUse(true).build(),
        user.getUserId(),
        List.of(prop1, prop2),
        List.of(file1));

    List<Integer> createdIds = serviceDAO.insertDatasetRegistration(null, List.of(insert));

    assertEquals(1, createdIds.size());

    Dataset created = datasetDAO.findDatasetById(createdIds.get(0));

    assertEquals(insert.name(), created.getName());
    assertEquals(insert.dacId(), created.getDacId());

    assertEquals(2, created.getProperties().size());

    DatasetProperty createdProp1 = created.getProperties().stream()
        .filter((p) -> p.getPropertyName().equals(prop1.getPropertyName())).findFirst().get();
    DatasetProperty createdProp2 = created.getProperties().stream()
        .filter((p) -> p.getPropertyName().equals(prop2.getPropertyName())).findFirst().get();

    assertEquals(created.getDatasetId(), createdProp1.getDatasetId());
    assertEquals(prop1.getPropertyValue(), createdProp1.getPropertyValue());
    assertEquals(prop1.getPropertyType(), createdProp1.getPropertyType());

    assertEquals(created.getDatasetId(), createdProp2.getDatasetId());
    assertEquals(prop2.getPropertyValue(), createdProp2.getPropertyValue());
    assertEquals(prop2.getPropertyType(), createdProp2.getPropertyType());

    assertNotNull(created.getNihInstitutionalCertificationFile());

    assertEquals(file1.getFileName(),
        created.getNihInstitutionalCertificationFile().getFileName());
    assertEquals(file1.getBlobId(),
        created.getNihInstitutionalCertificationFile().getBlobId());
  }


  @Test
  void testInsertMultipleDatasets() throws Exception {

    Dac dac = createDac();
    User user = createUser();

    DatasetProperty prop1 = new DatasetProperty();
    prop1.setSchemaProperty(RandomStringUtils.randomAlphabetic(10));
    prop1.setPropertyName(RandomStringUtils.randomAlphabetic(10));
    prop1.setPropertyValue(new Random().nextInt());
    prop1.setPropertyType(PropertyType.Number);

    DatasetServiceDAO.DatasetInsert insert1 = new DatasetServiceDAO.DatasetInsert(
        RandomStringUtils.randomAlphabetic(20),
        dac.getDacId(),
        new DataUseBuilder().setGeneralUse(true).build(),
        user.getUserId(),
        List.of(),
        List.of());

    DatasetServiceDAO.DatasetInsert insert2 = new DatasetServiceDAO.DatasetInsert(
        RandomStringUtils.randomAlphabetic(20),
        dac.getDacId(),
        new DataUseBuilder().setIllegalBehavior(true).build(),
        user.getUserId(),
        List.of(prop1),
        List.of());

    List<Integer> createdIds = serviceDAO.insertDatasetRegistration(null,
        List.of(insert1, insert2));

    List<Dataset> datasets = datasetDAO.findDatasetsByIdList(createdIds);

    assertEquals(2, datasets.size());

    Optional<Dataset> ds1Optional = datasets.stream()
        .filter(d -> d.getName().equals(insert1.name())).findFirst();
    assertTrue(ds1Optional.isPresent());
    Dataset dataset1 = ds1Optional.get();

    assertEquals(insert1.name(), dataset1.getName());
    assertEquals(insert1.dacId(), dataset1.getDacId());
    assertEquals(true, dataset1.getDataUse().getGeneralUse());
    assertNull(dataset1.getProperties());
    assertNull(dataset1.getNihInstitutionalCertificationFile());

    Optional<Dataset> ds2Optional = datasets.stream()
        .filter(d -> d.getName().equals(insert2.name())).findFirst();
    assertTrue(ds2Optional.isPresent());
    Dataset dataset2 = ds2Optional.get();

    assertEquals(insert2.name(), dataset2.getName());
    assertEquals(insert2.dacId(), dataset2.getDacId());
    assertEquals(true, dataset2.getDataUse().getIllegalBehavior());
    assertEquals(1, dataset2.getProperties().size());
    assertNull(dataset2.getNihInstitutionalCertificationFile());
  }

  @Test
  void testInsertStudyWithDatasets() throws Exception {
    Dac dac = createDac();
    User user = createUser();

    DatasetServiceDAO.StudyInsert studyInsert = new DatasetServiceDAO.StudyInsert(
        RandomStringUtils.randomAlphabetic(10),
        RandomStringUtils.randomAlphabetic(10),
        List.of(RandomStringUtils.randomAlphabetic(10)),
        RandomStringUtils.randomAlphabetic(10),
        true,
        user.getUserId(),
        List.of(),
        List.of());

    DatasetServiceDAO.DatasetInsert datasetInsert = new DatasetServiceDAO.DatasetInsert(
        RandomStringUtils.randomAlphabetic(20),
        dac.getDacId(),
        new DataUseBuilder().setGeneralUse(true).build(),
        user.getUserId(),
        List.of(),
        List.of());

    List<Integer> createdIds = serviceDAO.insertDatasetRegistration(studyInsert,
        List.of(datasetInsert));

    List<Dataset> datasets = datasetDAO.findDatasetsByIdList(createdIds);

    assertEquals(1, datasets.size());

    Dataset dataset1 = datasets.get(0);

    assertNotNull(dataset1.getStudy());
    Study s = dataset1.getStudy();
    assertEquals(studyInsert.name(), s.getName());
    assertEquals(studyInsert.description(), s.getDescription());
    assertEquals(studyInsert.dataTypes(), s.getDataTypes());
    assertEquals(studyInsert.piName(), s.getPiName());
    assertEquals(studyInsert.publicVisibility(), s.getPublicVisibility());
    assertEquals(studyInsert.userId(), s.getCreateUserId());
    assertNotNull(s.getCreateDate());

    assertTrue(Objects.isNull(s.getProperties()) || s.getProperties().isEmpty());
    assertNull(s.getAlternativeDataSharingPlan());
  }

  @Test
  void testInsertStudyWithProps() throws Exception {
    Dac dac = createDac();
    User user = createUser();

    StudyProperty prop1 = new StudyProperty();
    prop1.setKey(RandomStringUtils.randomAlphabetic(10));
    prop1.setType(PropertyType.String);
    prop1.setValue(RandomStringUtils.randomAlphabetic(10));

    StudyProperty prop2 = new StudyProperty();
    prop2.setKey(RandomStringUtils.randomAlphabetic(10));
    prop2.setType(PropertyType.Number);
    prop2.setValue(new Random().nextInt());

    DatasetServiceDAO.StudyInsert studyInsert = new DatasetServiceDAO.StudyInsert(
        RandomStringUtils.randomAlphabetic(10),
        RandomStringUtils.randomAlphabetic(10),
        List.of(RandomStringUtils.randomAlphabetic(10)),
        RandomStringUtils.randomAlphabetic(10),
        true,
        user.getUserId(),
        List.of(prop1, prop2),
        List.of());

    DatasetServiceDAO.DatasetInsert datasetInsert = new DatasetServiceDAO.DatasetInsert(
        RandomStringUtils.randomAlphabetic(20),
        dac.getDacId(),
        new DataUseBuilder().setGeneralUse(true).build(),
        user.getUserId(),
        List.of(),
        List.of());

    List<Integer> createdIds = serviceDAO.insertDatasetRegistration(studyInsert,
        List.of(datasetInsert));

    List<Dataset> datasets = datasetDAO.findDatasetsByIdList(createdIds);

    assertEquals(1, datasets.size());

    Dataset dataset1 = datasets.get(0);

    assertNotNull(dataset1.getStudy());
    Study s = dataset1.getStudy();
    assertEquals(studyInsert.name(), s.getName());
    assertEquals(studyInsert.description(), s.getDescription());
    assertEquals(studyInsert.dataTypes(), s.getDataTypes());
    assertEquals(studyInsert.piName(), s.getPiName());
    assertEquals(studyInsert.publicVisibility(), s.getPublicVisibility());
    assertEquals(studyInsert.userId(), s.getCreateUserId());
    assertNotNull(s.getCreateDate());

    StudyProperty createdProp1 = dataset1.getStudy().getProperties().stream()
        .filter((p) -> p.getKey().equals(prop1.getKey())).findFirst().get();
    StudyProperty createdProp2 = dataset1.getStudy().getProperties().stream()
        .filter((p) -> p.getKey().equals(prop2.getKey())).findFirst().get();

    assertEquals(prop1.getType(), createdProp1.getType());
    assertEquals(prop1.getValue(), createdProp1.getValue());
    assertEquals(prop2.getType(), createdProp2.getType());
    assertEquals(prop2.getValue(), createdProp2.getValue());

    assertNull(s.getAlternativeDataSharingPlan());
  }

  @Test
  void testInsertStudyWithAlternativeDataSharingFile() throws Exception {
    Dac dac = createDac();
    User user = createUser();

    StudyProperty prop1 = new StudyProperty();
    prop1.setKey(RandomStringUtils.randomAlphabetic(10));
    prop1.setType(PropertyType.String);
    prop1.setValue(RandomStringUtils.randomAlphabetic(10));

    StudyProperty prop2 = new StudyProperty();
    prop2.setKey(RandomStringUtils.randomAlphabetic(10));
    prop2.setType(PropertyType.Number);
    prop2.setValue(new Random().nextInt());

    FileStorageObject file = new FileStorageObject();
    file.setMediaType(RandomStringUtils.randomAlphabetic(20));
    file.setCategory(FileCategory.ALTERNATIVE_DATA_SHARING_PLAN);
    file.setBlobId(
        BlobId.of(RandomStringUtils.randomAlphabetic(10), RandomStringUtils.randomAlphabetic(10)));
    file.setFileName(RandomStringUtils.randomAlphabetic(10));

    DatasetServiceDAO.StudyInsert studyInsert = new DatasetServiceDAO.StudyInsert(
        RandomStringUtils.randomAlphabetic(10),
        RandomStringUtils.randomAlphabetic(10),
        List.of(RandomStringUtils.randomAlphabetic(10)),
        RandomStringUtils.randomAlphabetic(10),
        true,
        user.getUserId(),
        List.of(prop1, prop2),
        List.of(file));

    DatasetServiceDAO.DatasetInsert datasetInsert = new DatasetServiceDAO.DatasetInsert(
        RandomStringUtils.randomAlphabetic(20),
        dac.getDacId(),
        new DataUseBuilder().setGeneralUse(true).build(),
        user.getUserId(),
        List.of(),
        List.of());

    List<Integer> createdIds = serviceDAO.insertDatasetRegistration(studyInsert,
        List.of(datasetInsert));

    List<Dataset> datasets = datasetDAO.findDatasetsByIdList(createdIds);

    assertEquals(1, datasets.size());

    Dataset dataset1 = datasets.get(0);

    assertNotNull(dataset1.getStudy());
    Study s = dataset1.getStudy();
    assertEquals(studyInsert.name(), s.getName());
    assertEquals(studyInsert.description(), s.getDescription());
    assertEquals(studyInsert.dataTypes(), s.getDataTypes());
    assertEquals(studyInsert.piName(), s.getPiName());
    assertEquals(studyInsert.publicVisibility(), s.getPublicVisibility());
    assertEquals(studyInsert.userId(), s.getCreateUserId());
    assertNotNull(s.getCreateDate());

    StudyProperty createdProp1 = dataset1.getStudy().getProperties().stream()
        .filter((p) -> p.getKey().equals(prop1.getKey())).findFirst().get();
    StudyProperty createdProp2 = dataset1.getStudy().getProperties().stream()
        .filter((p) -> p.getKey().equals(prop2.getKey())).findFirst().get();

    assertEquals(prop1.getType(), createdProp1.getType());
    assertEquals(prop1.getValue(), createdProp1.getValue());
    assertEquals(prop2.getType(), createdProp2.getType());
    assertEquals(prop2.getValue(), createdProp2.getValue());

    assertNotNull(s.getAlternativeDataSharingPlan());

    assertEquals(file.getBlobId(), s.getAlternativeDataSharingPlan().getBlobId());
    assertEquals(file.getFileName(),
        s.getAlternativeDataSharingPlan().getFileName());
    assertEquals(file.getCategory(),
        s.getAlternativeDataSharingPlan().getCategory());
  }

  @Test
  void testUpdateDatasetWithProps() throws Exception {
    Dataset dataset = createDataset();

    // Set up two existing props for updating
    DatasetProperty prop1 = new DatasetProperty();
    prop1.setSchemaProperty(RandomStringUtils.randomAlphabetic(10));
    prop1.setPropertyName(RandomStringUtils.randomAlphabetic(10));
    prop1.setPropertyType(PropertyType.Number);
    prop1.setPropertyKey(1);
    prop1.setPropertyValue(new Random().nextInt());
    prop1.setDatasetId(dataset.getDatasetId());
    prop1.setCreateDate(new Date());

    DatasetProperty prop2 = new DatasetProperty();
    prop2.setSchemaProperty(RandomStringUtils.randomAlphabetic(10));
    prop2.setPropertyName(RandomStringUtils.randomAlphabetic(10));
    prop2.setPropertyType(PropertyType.Date);
    prop2.setPropertyKey(2);
    prop2.setPropertyValue("2000-10-20");
    prop2.setDatasetId(dataset.getDatasetId());
    prop2.setCreateDate(new Date());

    // Prop for deletion
    DatasetProperty prop3 = new DatasetProperty();
    prop3.setSchemaProperty(RandomStringUtils.randomAlphabetic(10));
    prop3.setPropertyName(RandomStringUtils.randomAlphabetic(10));
    prop3.setPropertyType(PropertyType.String);
    prop3.setPropertyKey(3);
    prop3.setPropertyValue(RandomStringUtils.randomAlphabetic(10));
    prop3.setDatasetId(dataset.getDatasetId());
    prop3.setCreateDate(new Date());

    datasetDAO.insertDatasetProperties(List.of(prop1, prop2, prop3));

    // Updates to existing props
    DatasetProperty updateProp1 = new DatasetProperty();
    updateProp1.setPropertyValue("new prop1 value");
    updateProp1.setPropertyName(prop1.getPropertyName());

    DatasetProperty updateProp2 = new DatasetProperty();
    updateProp2.setPropertyValue("new prop2 value");
    updateProp2.setPropertyName(prop2.getPropertyName());

    // New prop to add as part of the update
    DatasetProperty prop4 = new DatasetProperty();
    prop4.setSchemaProperty(RandomStringUtils.randomAlphabetic(10));
    prop4.setPropertyName(RandomStringUtils.randomAlphabetic(10));
    prop4.setPropertyType(PropertyType.String);
    prop4.setPropertyKey(4);
    prop4.setPropertyValue("new prop4 value");
    prop4.setDatasetId(dataset.getDatasetId());
    prop4.setCreateDate(new Date());

    String newName = "New Name";
    DatasetUpdate updates = new DatasetUpdate(
        dataset.getDatasetId(),
        newName,
        dataset.getCreateUserId(),
        dataset.getDacId(),
        List.of(updateProp1, updateProp2, prop4),
        List.of()
    );
    serviceDAO.updateDataset(updates);

    // Validate that the dataset props have been updated, deleted, or added:
    Set<DatasetProperty> updatedProps = datasetDAO.findDatasetPropertiesByDatasetId(dataset.getDatasetId());
    Optional<DatasetProperty> updated1 = updatedProps.stream().filter(p -> p.getPropertyName().equals(prop1.getPropertyName())).findFirst();
    Optional<DatasetProperty> updated2 = updatedProps.stream().filter(p -> p.getPropertyName().equals(prop2.getPropertyName())).findFirst();
    Optional<DatasetProperty> deleted3 = updatedProps.stream().filter(p -> p.getPropertyName().equals(prop3.getPropertyName())).findFirst();
    Optional<DatasetProperty> added4 = updatedProps.stream().filter(p -> p.getPropertyName().equals(prop4.getPropertyName())).findFirst();
    assertTrue(updated1.isPresent());
    assertEquals(updateProp1.getPropertyValueAsString(), updated1.get().getPropertyValueAsString());
    assertTrue(updated2.isPresent());
    assertEquals(updateProp2.getPropertyValueAsString(), updated2.get().getPropertyValueAsString());
    assertFalse(deleted3.isPresent());
    assertTrue(added4.isPresent());
    assertEquals(prop4.getPropertyValueAsString(), added4.get().getPropertyValueAsString());

    Dataset updatedDataset = datasetDAO.findDatasetById(dataset.getDatasetId());
    assertEquals(newName, updatedDataset.getDatasetName());
  }

  @Test
  void testUpdateStudyDetails() throws Exception {
    Study study = createStudy(null);

    String newStudyName = "New Study Name";
    String newStudyDescription = "New Study Description";
    String newPIName = "New PI Name";
    List<String> newDataTypes = List.of("DT 1", "DT 2", "DT 3");
    StudyUpdate studyUpdate = new StudyUpdate(
        newStudyName,
        study.getStudyId(),
        newStudyDescription,
        newDataTypes,
        newPIName,
        !study.getPublicVisibility(),
        study.getCreateUserId(),
        List.copyOf(study.getProperties()),
        List.of()
    );

    Study updatedStudy = serviceDAO.updateStudy(studyUpdate, List.of(), List.of());
    assertEquals(newStudyName, updatedStudy.getName());
    assertEquals(newStudyDescription, updatedStudy.getDescription());
    assertEquals(newPIName, updatedStudy.getPiName());
    assertEquals(newDataTypes, updatedStudy.getDataTypes());
  }

  @Test
  void testUpdateStudyWithPropUpdates() throws Exception {
    Study study = createStudy(null);
    List<StudyProperty> props = List.copyOf(study.getProperties());

    StudyProperty newProp = new StudyProperty();
    newProp.setKey(RandomStringUtils.randomAlphabetic(10));
    newProp.setType(PropertyType.String);
    newProp.setValue(RandomStringUtils.randomAlphabetic(10));

    String newPropValue = "New Study Prop Value";
    StudyProperty prop1 = props.get(0);
    prop1.setValue(newPropValue);

    // Create a study update with a changed prop, a new prop, and a to-be-deleted prop
    StudyUpdate studyUpdate = new StudyUpdate(
        study.getName(),
        study.getStudyId(),
        study.getDescription(),
        study.getDataTypes(),
        study.getPiName(),
        !study.getPublicVisibility(),
        study.getCreateUserId(),
        List.of(newProp, prop1),
        List.of()
    );

    Study updatedStudy = serviceDAO.updateStudy(studyUpdate, List.of(), List.of());
    // Updated prop
    Optional<StudyProperty> updatedProp1 = updatedStudy.getProperties().stream().filter(p -> p.getStudyPropertyId().equals(prop1.getStudyPropertyId())).findFirst();
    assertTrue(updatedProp1.isPresent());
    assertEquals(newPropValue, updatedProp1.get().getValue());
    // Added prop
    Optional<StudyProperty> addedNewProp = updatedStudy.getProperties().stream().filter(p -> newProp.getValue().equals(p.getValue())).findFirst();
    assertTrue(addedNewProp.isPresent());
  }

  @Test
  void testUpdateStudyWithDatasetUpdates() throws Exception {
    Study study = createStudy(null);
    Dataset dataset = datasetDAO.findDatasetsByIdList(List.copyOf(study.getDatasetIds())).get(0);

    StudyUpdate studyUpdate = new StudyUpdate(
        study.getName(),
        study.getStudyId(),
        study.getDescription(),
        study.getDataTypes(),
        study.getPiName(),
        !study.getPublicVisibility(),
        study.getCreateUserId(),
        List.copyOf(study.getProperties()),
        List.of()
    );

    String newDatasetName = "New Dataset Name";
    DatasetUpdate datasetUpdate = new DatasetUpdate(
        dataset.getDatasetId(),
        newDatasetName,
        study.getCreateUserId(),
        dataset.getDacId(),
        List.copyOf(dataset.getProperties()),
        List.of()
    );

    String newInsertName = "New Dataset Insert Name";
    DatasetInsert datasetInsert = new DatasetInsert(
        newInsertName,
        dataset.getDacId(),
        new DataUseBuilder().setGeneralUse(true).build(),
        study.getCreateUserId(),
        List.of(),
        List.of()
    );

    Study updatedStudy = serviceDAO.updateStudy(studyUpdate, List.of(datasetUpdate), List.of(datasetInsert));
    List<Dataset> updatedDatasets = datasetDAO.findDatasetsByIdList(new ArrayList<>(updatedStudy.getDatasetIds()));
    assertTrue(updatedDatasets.contains(dataset));
    assertEquals(updatedStudy.getDatasetIds().size(), updatedDatasets.size());
    assertTrue(updatedDatasets.stream().anyMatch(d -> d.getDatasetName().equals(newDatasetName)));
    assertTrue(updatedDatasets.stream().anyMatch(d -> d.getDatasetName().equals(newInsertName)));
  }

  @Test
  void testUpdateStudyWithFileUpdates() throws Exception {
    FileStorageObject fso1 = new FileStorageObject();
    fso1.setMediaType(RandomStringUtils.randomAlphabetic(20));
    fso1.setCategory(FileCategory.ALTERNATIVE_DATA_SHARING_PLAN);
    fso1.setBlobId(
        BlobId.of(RandomStringUtils.randomAlphabetic(10), RandomStringUtils.randomAlphabetic(10)));
    fso1.setFileName(RandomStringUtils.randomAlphabetic(10));

    FileStorageObject fso2 = new FileStorageObject();
    fso2.setMediaType(RandomStringUtils.randomAlphabetic(20));
    fso2.setCategory(FileCategory.NIH_INSTITUTIONAL_CERTIFICATION);
    fso2.setBlobId(
        BlobId.of(RandomStringUtils.randomAlphabetic(10), RandomStringUtils.randomAlphabetic(10)));
    fso2.setFileName(RandomStringUtils.randomAlphabetic(10));
    Study study = createStudy(List.of(fso1, fso2));

    FileStorageObject updatedFso1 = new FileStorageObject();
    updatedFso1.setMediaType(RandomStringUtils.randomAlphabetic(20));
    updatedFso1.setCategory(FileCategory.ALTERNATIVE_DATA_SHARING_PLAN);
    updatedFso1.setBlobId(
        BlobId.of(RandomStringUtils.randomAlphabetic(10), RandomStringUtils.randomAlphabetic(10)));
    updatedFso1.setFileName(RandomStringUtils.randomAlphabetic(10));

    FileStorageObject updatedFso2 = new FileStorageObject();
    updatedFso2.setMediaType(RandomStringUtils.randomAlphabetic(20));
    updatedFso2.setCategory(FileCategory.NIH_INSTITUTIONAL_CERTIFICATION);
    updatedFso2.setBlobId(
        BlobId.of(RandomStringUtils.randomAlphabetic(10), RandomStringUtils.randomAlphabetic(10)));
    updatedFso2.setFileName(RandomStringUtils.randomAlphabetic(10));

    StudyUpdate studyUpdate = new StudyUpdate(
        study.getName(),
        study.getStudyId(),
        study.getDescription(),
        study.getDataTypes(),
        study.getPiName(),
        !study.getPublicVisibility(),
        study.getCreateUserId(),
        List.copyOf(study.getProperties()),
        List.of(updatedFso1, updatedFso2)
    );

    Study updatedStudy = serviceDAO.updateStudy(studyUpdate, List.of(), List.of());
    assertNotNull(updatedStudy.getAlternativeDataSharingPlan());
    assertEquals(updatedFso1.getFileName(), updatedStudy.getAlternativeDataSharingPlan().getFileName());
    assertTrue(updatedStudy.getDatasetIds().stream().findFirst().isPresent());
    Dataset dataset = datasetDAO.findDatasetById(updatedStudy.getDatasetIds().stream().findFirst().get());
    assertNotNull(dataset.getNihInstitutionalCertificationFile());
    assertEquals(updatedFso2.getFileName(), dataset.getNihInstitutionalCertificationFile().getFileName());
  }


  @Test
  void testDeleteStudy() throws Exception {
    FileStorageObject fso1 = new FileStorageObject();
    fso1.setMediaType(RandomStringUtils.randomAlphabetic(20));
    fso1.setCategory(FileCategory.ALTERNATIVE_DATA_SHARING_PLAN);
    fso1.setBlobId(
        BlobId.of(RandomStringUtils.randomAlphabetic(10), RandomStringUtils.randomAlphabetic(10)));
    fso1.setFileName(RandomStringUtils.randomAlphabetic(10));

    FileStorageObject fso2 = new FileStorageObject();
    fso2.setMediaType(RandomStringUtils.randomAlphabetic(20));
    fso2.setCategory(FileCategory.NIH_INSTITUTIONAL_CERTIFICATION);
    fso2.setBlobId(
        BlobId.of(RandomStringUtils.randomAlphabetic(10), RandomStringUtils.randomAlphabetic(10)));
    fso2.setFileName(RandomStringUtils.randomAlphabetic(10));
    Study study = createStudy(List.of(fso1, fso2));

    List<Dataset> datasets = datasetDAO.findDatasetsByIdList(new ArrayList<>(study.getDatasetIds()));
    study.addDatasets(datasets);

    serviceDAO.deleteStudy(study, createUser());
    Study deletedStudy = studyDAO.findStudyById(study.getStudyId());
    assertNull(deletedStudy);
  }

  @Test
  void testPatchDataset() throws Exception {
    List<Dictionary> dictionaries = datasetDAO.getDictionaryTerms();
    Dictionary one = dictionaries.stream().filter(d -> d.getKeyId().equals(1)).findFirst().orElse(null);
    Dictionary two = dictionaries.stream().filter(d -> d.getKeyId().equals(2)).findFirst().orElse(null);
    Dictionary three = dictionaries.stream().filter(d -> d.getKeyId().equals(3)).findFirst().orElse(null);
    assertNotNull(one);
    assertNotNull(two);
    assertNotNull(three);

    // Dataset with user and an existing props
    Dataset dataset = createDataset();
    User user = userDAO.findUserById(dataset.getCreateUserId());

    // This prop will NOT change
    DatasetProperty prop1 = new DatasetProperty();
    prop1.setSchemaProperty(one.getKey());
    prop1.setPropertyName(one.getKey());
    prop1.setPropertyType(PropertyType.String);
    prop1.setPropertyKey(one.getKeyId());
    prop1.setPropertyValue(RandomStringUtils.randomAlphabetic(10));
    prop1.setDatasetId(dataset.getDatasetId());
    prop1.setCreateDate(new Date());

    // This prop WILL change
    DatasetProperty prop2 = new DatasetProperty();
    prop2.setSchemaProperty(two.getKey());
    prop2.setPropertyName(two.getKey());
    prop2.setPropertyType(PropertyType.String);
    prop2.setPropertyKey(two.getKeyId());
    prop2.setPropertyValue(RandomStringUtils.randomAlphabetic(10));
    prop2.setDatasetId(dataset.getDatasetId());
    prop2.setCreateDate(new Date());

    datasetDAO.insertDatasetProperties(List.of(prop1, prop2));

    // Patch to prop2
    DatasetProperty patchProp = new DatasetProperty();
    patchProp.setSchemaProperty(prop2.getSchemaProperty());
    patchProp.setPropertyName(prop2.getPropertyName());
    patchProp.setPropertyType(prop2.getPropertyType());
    patchProp.setPropertyKey(prop2.getPropertyKey());
    patchProp.setPropertyValue(RandomStringUtils.randomAlphabetic(10));

    // New, added prop
    DatasetProperty prop3 = new DatasetProperty();
    prop3.setSchemaProperty(three.getKey());
    prop3.setPropertyName(three.getKey());
    prop3.setPropertyType(PropertyType.String);
    prop3.setPropertyKey(three.getKeyId());
    prop3.setPropertyValue(RandomStringUtils.randomAlphabetic(10));
    prop3.setCreateDate(new Date());

    String newName = RandomStringUtils.randomAlphabetic(10);
    DatasetPatch patch = new DatasetPatch(newName, List.of(patchProp, prop3));

    serviceDAO.patchDataset(dataset.getDatasetId(), user, patch);
    Dataset patched = datasetDAO.findDatasetById(dataset.getDatasetId());

    // Validate that the name is updated
    assertEquals(newName, patched.getDatasetName());

    Set<DatasetProperty> updatedProps = datasetDAO.findDatasetPropertiesByDatasetId(dataset.getDatasetId());

    // Validate that the first prop was not changed
    Optional<DatasetProperty> original = updatedProps.stream().filter(p -> p.getPropertyName().equals(prop1.getPropertyName())).findFirst();
    assertTrue(original.isPresent());
    assertEquals(prop1.getPropertyValue(), original.get().getPropertyValue());

    // Validate that the new value was updated
    Optional<DatasetProperty> updated = updatedProps.stream().filter(p -> p.getPropertyName().equals(prop2.getPropertyName())).findFirst();
    assertTrue(updated.isPresent());
    assertEquals(patchProp.getPropertyValue(), updated.get().getPropertyValue());

    // Validate that the new prop was added
    Optional<DatasetProperty> added = updatedProps.stream().filter(p -> p.getPropertyName().equals(prop3.getPropertyName())).findFirst();
    assertTrue(added.isPresent());
    assertEquals(prop3.getPropertyValue(), added.get().getPropertyValue());

    // Validate that no props were deleted
    assertEquals(3, patched.getProperties().size());
  }

  /**
   * Helper method to create a study with two props and one dataset
   * @param fso Optional FSO to use as part of the study insert
   * @return Study
   * @throws Exception The exception
   */
  private Study createStudy(List<FileStorageObject> fso) throws Exception {
    Dac dac = createDac();
    User user = createUser();

    StudyProperty prop1 = new StudyProperty();
    prop1.setKey(RandomStringUtils.randomAlphabetic(10));
    prop1.setType(PropertyType.String);
    prop1.setValue(RandomStringUtils.randomAlphabetic(10));

    StudyProperty prop2 = new StudyProperty();
    prop2.setKey(RandomStringUtils.randomAlphabetic(10));
    prop2.setType(PropertyType.String);
    prop2.setValue(RandomStringUtils.randomAlphabetic(10));

    DatasetServiceDAO.StudyInsert studyInsert = new DatasetServiceDAO.StudyInsert(
        RandomStringUtils.randomAlphabetic(10),
        RandomStringUtils.randomAlphabetic(10),
        List.of(RandomStringUtils.randomAlphabetic(10)),
        RandomStringUtils.randomAlphabetic(10),
        true,
        user.getUserId(),
        List.of(prop1, prop2),
        Objects.isNull(fso) ? List.of() : fso);

    DatasetProperty datasetProperty = new DatasetProperty();
    datasetProperty.setSchemaProperty(RandomStringUtils.randomAlphabetic(10));
    datasetProperty.setPropertyName(RandomStringUtils.randomAlphabetic(10));
    datasetProperty.setPropertyType(PropertyType.Number);
    datasetProperty.setPropertyKey(1);
    datasetProperty.setPropertyValue(new Random().nextInt());
    datasetProperty.setCreateDate(new Date());

    DatasetServiceDAO.DatasetInsert datasetInsert = new DatasetServiceDAO.DatasetInsert(
        RandomStringUtils.randomAlphabetic(20),
        dac.getDacId(),
        new DataUseBuilder().setGeneralUse(true).build(),
        user.getUserId(),
        List.of(datasetProperty),
        List.of());

    List<Integer> createdIds = serviceDAO.insertDatasetRegistration(studyInsert,
        List.of(datasetInsert));
    List<Dataset> createdDatasets = datasetDAO.findDatasetsByIdList(createdIds);
    return createdDatasets.get(0).getStudy();
  }

  private Dataset createDataset() {
    User user = createUser();
    String name = "Name_" + RandomStringUtils.random(20, true, true);
    Timestamp now = new Timestamp(new Date().getTime());
    String objectId = "Object ID_" + RandomStringUtils.random(20, true, true);
    DataUse dataUse = new DataUseBuilder().setGeneralUse(true).build();
    Integer id = datasetDAO.insertDataset(name, now, user.getUserId(), objectId,
        dataUse.toString(), null);
    return datasetDAO.findDatasetById(id);
  }

  private Dac createDac() {
    Integer id = dacDAO.createDac(
        "Test_" + RandomStringUtils.random(20, true, true),
        "Test_" + RandomStringUtils.random(20, true, true),
        new Date());
    return dacDAO.findById(id);
  }

}