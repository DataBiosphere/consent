package org.broadinstitute.consent.http.service.dao;

import static org.broadinstitute.consent.http.models.StudyPatch.ALTERNATIVE_DATA_SHARING_PLAN_TARGET_DELIVERY_DATE;
import static org.broadinstitute.consent.http.models.StudyPatch.ALTERNATIVE_DATA_SHARING_PLAN_TARGET_PUBLIC_RELEASE_DATE;
import static org.broadinstitute.consent.http.models.StudyPatch.DATA_CUSTODIAN_EMAIL;
import static org.broadinstitute.consent.http.models.StudyPatch.EXTERNAL_IDENTIFIER;
import static org.broadinstitute.consent.http.models.StudyPatch.EXTERNAL_IDENTIFIER_TYPE;
import static org.broadinstitute.consent.http.models.StudyPatch.PHENOTYPE_INDICATION;
import static org.broadinstitute.consent.http.models.StudyPatch.SPECIES_KEY;
import static org.broadinstitute.consent.http.models.StudyPatch.STUDY_TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.cloud.storage.BlobId;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import org.broadinstitute.consent.http.db.DAOTestHelper;
import org.broadinstitute.consent.http.enumeration.AuditActions;
import org.broadinstitute.consent.http.enumeration.DataUseTranslationType;
import org.broadinstitute.consent.http.enumeration.FileCategory;
import org.broadinstitute.consent.http.enumeration.PropertyType;
import org.broadinstitute.consent.http.matching.TranslationUtil;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.models.DataUseBuilder;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.DatasetAudit;
import org.broadinstitute.consent.http.models.DatasetPatch;
import org.broadinstitute.consent.http.models.DatasetProperty;
import org.broadinstitute.consent.http.models.Dictionary;
import org.broadinstitute.consent.http.models.FileStorageObject;
import org.broadinstitute.consent.http.models.Study;
import org.broadinstitute.consent.http.models.StudyPatch;
import org.broadinstitute.consent.http.models.StudyProperty;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.dataset_registration_v1.DatasetRegistrationSchemaV1.StudyType;
import org.broadinstitute.consent.http.service.dao.DatasetServiceDAO.DatasetInsert;
import org.broadinstitute.consent.http.service.dao.DatasetServiceDAO.DatasetUpdate;
import org.broadinstitute.consent.http.service.dao.DatasetServiceDAO.StudyInsert;
import org.broadinstitute.consent.http.service.dao.DatasetServiceDAO.StudyUpdate;
import org.broadinstitute.consent.http.util.gson.GsonUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DatasetServiceDAOTest extends DAOTestHelper {

  private DatasetServiceDAO serviceDAO;

  @BeforeEach
  void setUp() {
    serviceDAO = new DatasetServiceDAO(jdbi, datasetDAO, studyDAO, datasetAuthorizationReaderDAO);
  }

  @Test
  void testDeleteDataset() throws Exception {
    Dataset dataset = createDataset();

    serviceDAO.deleteDataset(dataset, dataset.getCreateUserId());
    // Assert that the dataset is deleted:
    Dataset deleted = datasetDAO.findDatasetById(dataset.getDatasetId());
    assertNull(deleted);

    // Validate that an audit record was added:
    List<DatasetAudit> audits = datasetDAO.findAuditsByDatasetId(dataset.getDatasetId());
    assertEquals(1, audits.size());
    assertEquals(AuditActions.DELETE.name(), audits.getFirst().getAction());
  }

  @Test
  void testInsertDatasets() throws Exception {

    Dac dac = createDac();
    User user = createUser();

    DatasetProperty prop1 = new DatasetProperty();
    prop1.setSchemaProperty(randomAlphabetic(10));
    prop1.setPropertyName(randomAlphabetic(10));
    prop1.setPropertyType(PropertyType.Number);
    prop1.setPropertyValue(new Random().nextInt());

    DatasetProperty prop2 = new DatasetProperty();
    prop2.setSchemaProperty(randomAlphabetic(10));
    prop2.setPropertyName(randomAlphabetic(10));
    prop2.setPropertyType(PropertyType.Date);
    prop2.setPropertyValueAsString("2000-10-20");

    FileStorageObject file1 = new FileStorageObject();
    file1.setMediaType(randomAlphabetic(20));
    file1.setCategory(FileCategory.NIH_INSTITUTIONAL_CERTIFICATION);
    file1.setBlobId(BlobId.of(randomAlphabetic(10), randomAlphabetic(10)));
    file1.setFileName(randomAlphabetic(10));

    DatasetInsert insert =
        new DatasetInsert(
            randomAlphabetic(20),
            dac.getDacId(),
            new DataUseBuilder().setStigmatizeDiseases(true).setGeneralUse(true).build(),
            user.getUserId(),
            List.of(prop1, prop2),
            List.of(file1));

    List<Integer> createdIds = serviceDAO.insertDatasetRegistration(null, List.of(insert));

    assertEquals(1, createdIds.size());

    Dataset created = datasetDAO.findDatasetById(createdIds.getFirst());

    assertEquals(insert.name(), created.getName());
    assertEquals(insert.dacId(), created.getDacId());

    assertEquals(2, created.getProperties().size());

    DatasetProperty createdProp1 =
        created.getProperties().stream()
            .filter(p -> p.getPropertyName().equals(prop1.getPropertyName()))
            .findFirst()
            .orElse(null);
    DatasetProperty createdProp2 =
        created.getProperties().stream()
            .filter(p -> p.getPropertyName().equals(prop2.getPropertyName()))
            .findFirst()
            .orElse(null);

    assertNotNull(createdProp1);
    assertEquals(created.getDatasetId(), createdProp1.getDatasetId());
    assertEquals(prop1.getPropertyValue(), createdProp1.getPropertyValue());
    assertEquals(prop1.getPropertyType(), createdProp1.getPropertyType());

    assertNotNull(createdProp2);
    assertEquals(created.getDatasetId(), createdProp2.getDatasetId());
    assertEquals(prop2.getPropertyValue(), createdProp2.getPropertyValue());
    assertEquals(prop2.getPropertyType(), createdProp2.getPropertyType());

    assertNotNull(created.getNihInstitutionalCertificationFile());

    assertEquals(file1.getFileName(), created.getNihInstitutionalCertificationFile().getFileName());
    assertEquals(file1.getBlobId(), created.getNihInstitutionalCertificationFile().getBlobId());
  }

  @Test
  void testInsertMultipleDatasets() throws Exception {

    Dac dac = createDac();
    User user = createUser();

    DatasetProperty prop1 = new DatasetProperty();
    prop1.setSchemaProperty(randomAlphabetic(10));
    prop1.setPropertyName(randomAlphabetic(10));
    prop1.setPropertyValue(new Random().nextInt());
    prop1.setPropertyType(PropertyType.Number);

    DatasetInsert insert1 =
        new DatasetInsert(
            randomAlphabetic(20),
            dac.getDacId(),
            new DataUseBuilder().setGeneralUse(true).build(),
            user.getUserId(),
            List.of(),
            List.of());

    DatasetInsert insert2 =
        new DatasetInsert(
            randomAlphabetic(20),
            dac.getDacId(),
            new DataUseBuilder().setIllegalBehavior(true).build(),
            user.getUserId(),
            List.of(prop1),
            List.of());

    List<Integer> createdIds =
        serviceDAO.insertDatasetRegistration(null, List.of(insert1, insert2));

    List<Dataset> datasets = datasetDAO.findDatasetsByIdList(createdIds);

    assertEquals(2, datasets.size());

    Optional<Dataset> ds1Optional =
        datasets.stream().filter(d -> d.getName().equals(insert1.name())).findFirst();
    assertTrue(ds1Optional.isPresent());
    Dataset dataset1 = ds1Optional.get();

    assertEquals(insert1.name(), dataset1.getName());
    assertEquals(insert1.dacId(), dataset1.getDacId());
    assertEquals(true, dataset1.getDataUse().getGeneralUse());
    assertNull(dataset1.getProperties());
    assertNull(dataset1.getNihInstitutionalCertificationFile());

    Optional<Dataset> ds2Optional =
        datasets.stream().filter(d -> d.getName().equals(insert2.name())).findFirst();
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

    StudyInsert studyInsert =
        new StudyInsert(
            randomAlphabetic(10),
            randomAlphabetic(10),
            List.of(randomAlphabetic(10)),
            randomAlphabetic(10),
            randomAlphabetic(10),
            true,
            user.getUserId(),
            List.of(),
            List.of());

    DatasetInsert datasetInsert =
        new DatasetInsert(
            randomAlphabetic(20),
            dac.getDacId(),
            new DataUseBuilder().setGeneralUse(true).build(),
            user.getUserId(),
            List.of(),
            List.of());

    List<Integer> createdIds =
        serviceDAO.insertDatasetRegistration(studyInsert, List.of(datasetInsert));

    List<Dataset> datasets = datasetDAO.findDatasetsByIdList(createdIds);

    assertEquals(1, datasets.size());

    Dataset dataset1 = datasetDAO.findDatasetById(createdIds.getFirst());

    assertNotNull(dataset1.getStudy());
    Study s = dataset1.getStudy();
    assertEquals(studyInsert.name(), s.getName());
    assertEquals(studyInsert.description(), s.getDescription());
    assertEquals(studyInsert.dataTypes(), s.getDataTypes());
    assertEquals(studyInsert.piName(), s.getPiName());
    assertEquals(studyInsert.piEmail(), s.getPiEmail());
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
    prop1.setKey(randomAlphabetic(10));
    prop1.setType(PropertyType.String);
    prop1.setValue(randomAlphabetic(10));

    StudyProperty prop2 = new StudyProperty();
    prop2.setKey(randomAlphabetic(10));
    prop2.setType(PropertyType.Number);
    prop2.setValue(new Random().nextInt());

    StudyInsert studyInsert =
        new StudyInsert(
            randomAlphabetic(10),
            randomAlphabetic(10),
            List.of(randomAlphabetic(10)),
            randomAlphabetic(10),
            randomAlphabetic(10),
            true,
            user.getUserId(),
            List.of(prop1, prop2),
            List.of());

    DatasetInsert datasetInsert =
        new DatasetInsert(
            randomAlphabetic(20),
            dac.getDacId(),
            new DataUseBuilder().setGeneralUse(true).build(),
            user.getUserId(),
            List.of(),
            List.of());

    List<Integer> createdIds =
        serviceDAO.insertDatasetRegistration(studyInsert, List.of(datasetInsert));

    List<Dataset> datasets = datasetDAO.findDatasetsByIdList(createdIds);

    assertEquals(1, datasets.size());

    Dataset dataset1 = datasetDAO.findDatasetById(createdIds.getFirst());

    assertNotNull(dataset1.getStudy());
    Study s = dataset1.getStudy();
    assertEquals(studyInsert.name(), s.getName());
    assertEquals(studyInsert.description(), s.getDescription());
    assertEquals(studyInsert.dataTypes(), s.getDataTypes());
    assertEquals(studyInsert.piName(), s.getPiName());
    assertEquals(studyInsert.piEmail(), s.getPiEmail());
    assertEquals(studyInsert.publicVisibility(), s.getPublicVisibility());
    assertEquals(studyInsert.userId(), s.getCreateUserId());
    assertNotNull(s.getCreateDate());

    StudyProperty createdProp1 =
        dataset1.getStudy().getProperties().stream()
            .filter(p -> p.getKey().equals(prop1.getKey()))
            .findFirst()
            .orElse(null);
    StudyProperty createdProp2 =
        dataset1.getStudy().getProperties().stream()
            .filter(p -> p.getKey().equals(prop2.getKey()))
            .findFirst()
            .orElse(null);

    assertNotNull(createdProp1);
    assertEquals(prop1.getType(), createdProp1.getType());
    assertEquals(prop1.getValue(), createdProp1.getValue());
    assertNotNull(createdProp2);
    assertEquals(prop2.getType(), createdProp2.getType());
    assertEquals(prop2.getValue(), createdProp2.getValue());

    assertNull(s.getAlternativeDataSharingPlan());
  }

  @Test
  void testInsertStudyWithAlternativeDataSharingFile() throws Exception {
    Dac dac = createDac();
    User user = createUser();

    StudyProperty prop1 = new StudyProperty();
    prop1.setKey(randomAlphabetic(10));
    prop1.setType(PropertyType.String);
    prop1.setValue(randomAlphabetic(10));

    StudyProperty prop2 = new StudyProperty();
    prop2.setKey(randomAlphabetic(10));
    prop2.setType(PropertyType.Number);
    prop2.setValue(new Random().nextInt());

    FileStorageObject file = new FileStorageObject();
    file.setMediaType(randomAlphabetic(20));
    file.setCategory(FileCategory.ALTERNATIVE_DATA_SHARING_PLAN);
    file.setBlobId(BlobId.of(randomAlphabetic(10), randomAlphabetic(10)));
    file.setFileName(randomAlphabetic(10));

    StudyInsert studyInsert =
        new StudyInsert(
            randomAlphabetic(10),
            randomAlphabetic(10),
            List.of(randomAlphabetic(10)),
            randomAlphabetic(10),
            randomAlphabetic(10),
            true,
            user.getUserId(),
            List.of(prop1, prop2),
            List.of(file));

    DatasetInsert datasetInsert =
        new DatasetInsert(
            randomAlphabetic(20),
            dac.getDacId(),
            new DataUseBuilder().setGeneralUse(true).build(),
            user.getUserId(),
            List.of(),
            List.of());

    List<Integer> createdIds =
        serviceDAO.insertDatasetRegistration(studyInsert, List.of(datasetInsert));

    List<Dataset> datasets = datasetDAO.findDatasetsByIdList(createdIds);

    assertEquals(1, datasets.size());

    Dataset dataset1 = datasetDAO.findDatasetById(createdIds.getFirst());

    assertNotNull(dataset1.getStudy());
    Study s = dataset1.getStudy();
    assertEquals(studyInsert.name(), s.getName());
    assertEquals(studyInsert.description(), s.getDescription());
    assertEquals(studyInsert.dataTypes(), s.getDataTypes());
    assertEquals(studyInsert.piName(), s.getPiName());
    assertEquals(studyInsert.piEmail(), s.getPiEmail());
    assertEquals(studyInsert.publicVisibility(), s.getPublicVisibility());
    assertEquals(studyInsert.userId(), s.getCreateUserId());
    assertNotNull(s.getCreateDate());

    StudyProperty createdProp1 =
        dataset1.getStudy().getProperties().stream()
            .filter(p -> p.getKey().equals(prop1.getKey()))
            .findFirst()
            .orElse(null);
    StudyProperty createdProp2 =
        dataset1.getStudy().getProperties().stream()
            .filter(p -> p.getKey().equals(prop2.getKey()))
            .findFirst()
            .orElse(null);

    assertNotNull(createdProp1);
    assertEquals(prop1.getType(), createdProp1.getType());
    assertEquals(prop1.getValue(), createdProp1.getValue());
    assertNotNull(createdProp2);
    assertEquals(prop2.getType(), createdProp2.getType());
    assertEquals(prop2.getValue(), createdProp2.getValue());

    assertNotNull(s.getAlternativeDataSharingPlan());

    assertEquals(file.getBlobId(), s.getAlternativeDataSharingPlan().getBlobId());
    assertEquals(file.getFileName(), s.getAlternativeDataSharingPlan().getFileName());
    assertEquals(file.getCategory(), s.getAlternativeDataSharingPlan().getCategory());

    // Validate that an audit record was added for each dataset
    datasets.forEach(
        d -> {
          List<DatasetAudit> audits = datasetDAO.findAuditsByDatasetId(d.getDatasetId());
          assertEquals(1, audits.size());
          assertEquals(AuditActions.CREATE.name(), audits.getFirst().getAction());
        });
  }

  @Test
  void testUpdateDatasetWithProps() throws Exception {
    Dataset dataset = createDataset();

    // Set up two existing props for updating
    DatasetProperty prop1 = new DatasetProperty();
    prop1.setSchemaProperty(randomAlphabetic(10));
    prop1.setPropertyName(randomAlphabetic(10));
    prop1.setPropertyType(PropertyType.Number);
    prop1.setPropertyKey(1);
    prop1.setPropertyValue(new Random().nextInt());
    prop1.setDatasetId(dataset.getDatasetId());
    prop1.setCreateDate(new Date());

    DatasetProperty prop2 = new DatasetProperty();
    prop2.setSchemaProperty(randomAlphabetic(10));
    prop2.setPropertyName(randomAlphabetic(10));
    prop2.setPropertyType(PropertyType.Date);
    prop2.setPropertyKey(2);
    prop2.setPropertyValue("2000-10-20");
    prop2.setDatasetId(dataset.getDatasetId());
    prop2.setCreateDate(new Date());

    // Prop for deletion
    DatasetProperty prop3 = new DatasetProperty();
    prop3.setSchemaProperty(randomAlphabetic(10));
    prop3.setPropertyName(randomAlphabetic(10));
    prop3.setPropertyType(PropertyType.String);
    prop3.setPropertyKey(3);
    prop3.setPropertyValue(randomAlphabetic(10));
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
    prop4.setSchemaProperty(randomAlphabetic(10));
    prop4.setPropertyName(randomAlphabetic(10));
    prop4.setPropertyType(PropertyType.String);
    prop4.setPropertyKey(4);
    prop4.setPropertyValue("new prop4 value");
    prop4.setDatasetId(dataset.getDatasetId());
    prop4.setCreateDate(new Date());

    String newName = "New Name";
    DatasetUpdate updates =
        new DatasetUpdate(
            dataset.getDatasetId(),
            newName,
            dataset.getCreateUserId(),
            dataset.getDacId(),
            List.of(updateProp1, updateProp2, prop4),
            List.of());
    serviceDAO.updateDataset(updates);

    // Validate that the dataset props have been updated, deleted, or added:
    Set<DatasetProperty> updatedProps =
        datasetDAO.findDatasetPropertiesByDatasetId(dataset.getDatasetId());
    Optional<DatasetProperty> updated1 =
        updatedProps.stream()
            .filter(p -> p.getPropertyName().equals(prop1.getPropertyName()))
            .findFirst();
    Optional<DatasetProperty> updated2 =
        updatedProps.stream()
            .filter(p -> p.getPropertyName().equals(prop2.getPropertyName()))
            .findFirst();
    Optional<DatasetProperty> deleted3 =
        updatedProps.stream()
            .filter(p -> p.getPropertyName().equals(prop3.getPropertyName()))
            .findFirst();
    Optional<DatasetProperty> added4 =
        updatedProps.stream()
            .filter(p -> p.getPropertyName().equals(prop4.getPropertyName()))
            .findFirst();
    assertTrue(updated1.isPresent());
    assertEquals(updateProp1.getPropertyValueAsString(), updated1.get().getPropertyValueAsString());
    assertTrue(updated2.isPresent());
    assertEquals(updateProp2.getPropertyValueAsString(), updated2.get().getPropertyValueAsString());
    assertFalse(deleted3.isPresent());
    assertTrue(added4.isPresent());
    assertEquals(prop4.getPropertyValueAsString(), added4.get().getPropertyValueAsString());

    Dataset updatedDataset = datasetDAO.findDatasetById(dataset.getDatasetId());
    assertEquals(newName, updatedDataset.getDatasetName());

    // Validate that an audit record was added:
    List<DatasetAudit> audits = datasetDAO.findAuditsByDatasetId(dataset.getDatasetId());
    assertEquals(1, audits.size());
    assertEquals(AuditActions.UPDATE.name(), audits.getFirst().getAction());
  }

  @Test
  void testUpdateStudyDetails() throws Exception {
    Study study = createStudy(null, null, null);

    String newStudyName = "New Study Name";
    String newStudyDescription = "New Study Description";
    String newPIName = "New PI Name";
    List<String> newDataTypes = List.of("DT 1", "DT 2", "DT 3");
    StudyUpdate studyUpdate =
        new StudyUpdate(
            newStudyName,
            study.getStudyId(),
            newStudyDescription,
            newDataTypes,
            newPIName,
            null,
            !study.getPublicVisibility(),
            study.getCreateUserId(),
            List.copyOf(study.getProperties()),
            List.of());

    Study updatedStudy = serviceDAO.updateStudy(studyUpdate, List.of(), List.of());
    assertEquals(newStudyName, updatedStudy.getName());
    assertEquals(newStudyDescription, updatedStudy.getDescription());
    assertEquals(newPIName, updatedStudy.getPiName());
    assertEquals(newDataTypes, updatedStudy.getDataTypes());

    // Validate that NO update records were added for each dataset
    updatedStudy
        .getDatasetIds()
        .forEach(
            id -> {
              List<DatasetAudit> audits = datasetDAO.findAuditsByDatasetId(id);
              assertFalse(audits.isEmpty());
              assertFalse(
                  audits.stream()
                      .anyMatch(a -> a.getAction().equalsIgnoreCase(AuditActions.UPDATE.name())));
            });
  }

  @Test
  void testUpdateStudyWithPropUpdates() throws Exception {
    Study study = createStudy(null, null, null);
    List<StudyProperty> props = List.copyOf(study.getProperties());

    StudyProperty newProp = new StudyProperty();
    newProp.setKey(randomAlphabetic(10));
    newProp.setType(PropertyType.String);
    newProp.setValue(randomAlphabetic(10));

    String newPropValue = "New Study Prop Value";
    StudyProperty prop1 = props.getFirst();
    prop1.setValue(newPropValue);

    // Create a study update with a changed prop, a new prop, and a to-be-deleted prop
    StudyUpdate studyUpdate =
        new StudyUpdate(
            study.getName(),
            study.getStudyId(),
            study.getDescription(),
            study.getDataTypes(),
            study.getPiName(),
            study.getPiEmail(),
            !study.getPublicVisibility(),
            study.getCreateUserId(),
            List.of(newProp, prop1),
            List.of());

    Study updatedStudy = serviceDAO.updateStudy(studyUpdate, List.of(), List.of());
    // Updated prop
    Optional<StudyProperty> updatedProp1 =
        updatedStudy.getProperties().stream()
            .filter(p -> p.getKey().equals(prop1.getKey()))
            .findFirst();
    assertTrue(updatedProp1.isPresent());
    assertEquals(newPropValue, updatedProp1.get().getValue());
    // Added prop
    Optional<StudyProperty> addedNewProp =
        updatedStudy.getProperties().stream()
            .filter(p -> newProp.getValue().equals(p.getValue()))
            .findFirst();
    assertTrue(addedNewProp.isPresent());

    // Validate that NO update records were added for each dataset
    updatedStudy
        .getDatasetIds()
        .forEach(
            id -> {
              List<DatasetAudit> audits = datasetDAO.findAuditsByDatasetId(id);
              assertFalse(audits.isEmpty());
              assertFalse(
                  audits.stream()
                      .anyMatch(a -> a.getAction().equalsIgnoreCase(AuditActions.UPDATE.name())));
            });
  }

  @Test
  void testUpdateStudyWithDatasetUpdates() throws Exception {
    Study study = createStudy(null, null, null);
    Dataset dataset =
        datasetDAO.findDatasetsByIdList(List.copyOf(study.getDatasetIds())).getFirst();

    StudyUpdate studyUpdate =
        new StudyUpdate(
            study.getName(),
            study.getStudyId(),
            study.getDescription(),
            study.getDataTypes(),
            study.getPiName(),
            study.getPiEmail(),
            !study.getPublicVisibility(),
            study.getCreateUserId(),
            List.copyOf(study.getProperties()),
            List.of());

    String newDatasetName = "New Dataset Name";
    DatasetUpdate datasetUpdate =
        new DatasetUpdate(
            dataset.getDatasetId(),
            newDatasetName,
            study.getCreateUserId(),
            dataset.getDacId(),
            List.copyOf(dataset.getProperties()),
            List.of());

    String newInsertName = "New Dataset Insert Name";
    DatasetInsert datasetInsert =
        new DatasetInsert(
            newInsertName,
            dataset.getDacId(),
            new DataUseBuilder().setGeneralUse(true).build(),
            study.getCreateUserId(),
            List.of(),
            List.of());

    Study updatedStudy =
        serviceDAO.updateStudy(studyUpdate, List.of(datasetUpdate), List.of(datasetInsert));
    List<Dataset> updatedDatasets =
        datasetDAO.findDatasetsByIdList(new ArrayList<>(updatedStudy.getDatasetIds()));
    assertTrue(updatedDatasets.contains(dataset));
    assertEquals(updatedStudy.getDatasetIds().size(), updatedDatasets.size());
    assertTrue(updatedDatasets.stream().anyMatch(d -> d.getDatasetName().equals(newDatasetName)));
    assertTrue(updatedDatasets.stream().anyMatch(d -> d.getDatasetName().equals(newInsertName)));

    // Validate that update records were added for each dataset
    updatedStudy
        .getDatasetIds()
        .forEach(
            id -> {
              List<DatasetAudit> audits = datasetDAO.findAuditsByDatasetId(id);
              assertFalse(audits.isEmpty());
              assertTrue(
                  audits.stream()
                      .anyMatch(a -> a.getAction().equalsIgnoreCase(AuditActions.CREATE.name())));
            });
  }

  @Test
  void testUpdateStudyWithFileUpdates() throws Exception {
    FileStorageObject fso1 = new FileStorageObject();
    fso1.setMediaType(randomAlphabetic(20));
    fso1.setCategory(FileCategory.ALTERNATIVE_DATA_SHARING_PLAN);
    fso1.setBlobId(BlobId.of(randomAlphabetic(10), randomAlphabetic(10)));
    fso1.setFileName(randomAlphabetic(10));

    FileStorageObject fso2 = new FileStorageObject();
    fso2.setMediaType(randomAlphabetic(20));
    fso2.setCategory(FileCategory.NIH_INSTITUTIONAL_CERTIFICATION);
    fso2.setBlobId(BlobId.of(randomAlphabetic(10), randomAlphabetic(10)));
    fso2.setFileName(randomAlphabetic(10));

    FileStorageObject fso3 = new FileStorageObject();
    fso3.setMediaType(randomAlphabetic(20));
    fso3.setCategory(FileCategory.NIH_INSTITUTIONAL_CERTIFICATION);
    fso3.setBlobId(BlobId.of(randomAlphabetic(10), randomAlphabetic(10)));
    fso3.setFileName(randomAlphabetic(10));

    Study study = createStudy(fso1, fso2, fso3);
    Dataset datasetForUpdate =
        datasetDAO.findDatasetById((Integer) study.getDatasetIds().toArray()[0]);

    FileStorageObject updatedFso1 = new FileStorageObject();
    updatedFso1.setMediaType(randomAlphabetic(20));
    updatedFso1.setCategory(FileCategory.ALTERNATIVE_DATA_SHARING_PLAN);
    updatedFso1.setBlobId(BlobId.of(randomAlphabetic(10), randomAlphabetic(10)));
    updatedFso1.setFileName(randomAlphabetic(10));

    FileStorageObject updatedFso2 = new FileStorageObject();
    updatedFso2.setMediaType(randomAlphabetic(20));
    updatedFso2.setCategory(FileCategory.NIH_INSTITUTIONAL_CERTIFICATION);
    updatedFso2.setBlobId(BlobId.of(randomAlphabetic(10), randomAlphabetic(10)));
    updatedFso2.setFileName(randomAlphabetic(10));

    StudyUpdate studyUpdate =
        new StudyUpdate(
            study.getName(),
            study.getStudyId(),
            study.getDescription(),
            study.getDataTypes(),
            study.getPiName(),
            study.getPiEmail(),
            !study.getPublicVisibility(),
            study.getCreateUserId(),
            List.copyOf(study.getProperties()),
            List.of(updatedFso1)); // A StudyUpdate should only include study level files.

    DatasetUpdate datasetUpdate =
        new DatasetUpdate(
            datasetForUpdate.getDatasetId(),
            datasetForUpdate.getDatasetName(),
            study.getCreateUserId(),
            datasetForUpdate.getDacId(),
            List.copyOf(datasetForUpdate.getProperties()),
            List.of(updatedFso2));

    Study updatedStudy = serviceDAO.updateStudy(studyUpdate, List.of(datasetUpdate), List.of());
    assertNotNull(updatedStudy.getAlternativeDataSharingPlan());
    assertEquals(
        updatedFso1.getFileName(), updatedStudy.getAlternativeDataSharingPlan().getFileName());
    assertTrue(updatedStudy.getDatasetIds().stream().findFirst().isPresent());
    Dataset dataset =
        datasetDAO.findDatasetById(updatedStudy.getDatasetIds().stream().findFirst().get());
    assertNotNull(dataset.getNihInstitutionalCertificationFile());
    assertEquals(
        updatedFso2.getFileName(), dataset.getNihInstitutionalCertificationFile().getFileName());

    // If the update works correctly, we should have an update audit record for dataset 1 because
    // the dataset has to be modified
    // to point to the new file storage object reference.  If it's not changing, something else is
    // incorrect.

    // Expect an update audit on the first dataset
    Object[] updatedStudyDatasetIds = updatedStudy.getDatasetIds().toArray();
    assertEquals(2, updatedStudyDatasetIds.length);
    List<DatasetAudit> firstDatasetAudits =
        datasetDAO.findAuditsByDatasetId((Integer) updatedStudyDatasetIds[0]);
    assertFalse(firstDatasetAudits.isEmpty());
    assertTrue(
        firstDatasetAudits.stream()
            .anyMatch(a -> a.getAction().equalsIgnoreCase(AuditActions.UPDATE.name())));

    // we should have no audit record because we're not modifying the second dataset's file
    List<DatasetAudit> secondDatasetAudits =
        datasetDAO.findAuditsByDatasetId((Integer) updatedStudyDatasetIds[1]);
    assertFalse(secondDatasetAudits.isEmpty());
    assertFalse(
        secondDatasetAudits.stream()
            .anyMatch(a -> a.getAction().equalsIgnoreCase(AuditActions.UPDATE.name())));
  }

  @Test
  void testUpdateStudyWithFileUpdatesOnSecondDataset() throws Exception {
    FileStorageObject fso1 = new FileStorageObject();
    fso1.setMediaType(randomAlphabetic(20));
    fso1.setCategory(FileCategory.ALTERNATIVE_DATA_SHARING_PLAN);
    fso1.setBlobId(BlobId.of(randomAlphabetic(10), randomAlphabetic(10)));
    fso1.setFileName(randomAlphabetic(10));

    FileStorageObject fso2 = new FileStorageObject();
    fso2.setMediaType(randomAlphabetic(20));
    fso2.setCategory(FileCategory.NIH_INSTITUTIONAL_CERTIFICATION);
    fso2.setBlobId(BlobId.of(randomAlphabetic(10), randomAlphabetic(10)));
    fso2.setFileName(randomAlphabetic(10));

    Study study = createStudy(fso1, fso2, null);

    FileStorageObject updatedFso1 = new FileStorageObject();
    updatedFso1.setMediaType(randomAlphabetic(20));
    updatedFso1.setCategory(FileCategory.ALTERNATIVE_DATA_SHARING_PLAN);
    updatedFso1.setBlobId(BlobId.of(randomAlphabetic(10), randomAlphabetic(10)));
    updatedFso1.setFileName(randomAlphabetic(10));

    FileStorageObject updatedFso2 = new FileStorageObject();
    updatedFso2.setMediaType(randomAlphabetic(20));
    updatedFso2.setCategory(FileCategory.NIH_INSTITUTIONAL_CERTIFICATION);
    updatedFso2.setBlobId(BlobId.of(randomAlphabetic(10), randomAlphabetic(10)));
    updatedFso2.setFileName(randomAlphabetic(10));

    assertTrue(study.getDatasetIds().size() > 1);
    Iterator<Integer> dsIterator = study.getDatasetIds().iterator();
    dsIterator.next(); // skip the first entry because the code was set to update the first entry.
    Dataset datasetForUpdate = datasetDAO.findDatasetById(dsIterator.next());
    DatasetUpdate datasetUpdate =
        new DatasetUpdate(
            datasetForUpdate.getDatasetId(),
            datasetForUpdate.getDatasetName(),
            study.getCreateUserId(),
            datasetForUpdate.getDacId(),
            List.copyOf(datasetForUpdate.getProperties()),
            List.of(updatedFso2));

    StudyUpdate studyUpdate =
        new StudyUpdate(
            study.getName(),
            study.getStudyId(),
            study.getDescription(),
            study.getDataTypes(),
            study.getPiName(),
            study.getPiEmail(),
            !study.getPublicVisibility(),
            study.getCreateUserId(),
            List.copyOf(study.getProperties()),
            List.of(updatedFso1, updatedFso2));

    Study updatedStudy = serviceDAO.updateStudy(studyUpdate, List.of(datasetUpdate), List.of());
    assertNotNull(updatedStudy.getAlternativeDataSharingPlan());
    assertEquals(
        updatedFso1.getFileName(), updatedStudy.getAlternativeDataSharingPlan().getFileName());
    assertTrue(updatedStudy.getDatasetIds().stream().findFirst().isPresent());
    Dataset dataset = datasetDAO.findDatasetById(datasetForUpdate.getDatasetId());
    assertNotNull(dataset.getNihInstitutionalCertificationFile());
    assertEquals(
        updatedFso2.getFileName(), dataset.getNihInstitutionalCertificationFile().getFileName());

    // Expect no update audit on the first dataset
    Object[] updatedStudyDatasetIds = updatedStudy.getDatasetIds().toArray();
    assertEquals(2, updatedStudyDatasetIds.length);
    List<DatasetAudit> firstDatasetAudits =
        datasetDAO.findAuditsByDatasetId((Integer) updatedStudyDatasetIds[0]);
    assertFalse(firstDatasetAudits.isEmpty());
    assertFalse(
        firstDatasetAudits.stream()
            .anyMatch(a -> a.getAction().equalsIgnoreCase(AuditActions.UPDATE.name())));

    // we should have an audit record because we're modifying the dataset through the study API.
    List<DatasetAudit> secondDatasetAudits =
        datasetDAO.findAuditsByDatasetId((Integer) updatedStudyDatasetIds[1]);
    assertFalse(secondDatasetAudits.isEmpty());
    assertTrue(
        secondDatasetAudits.stream()
            .anyMatch(a -> a.getAction().equalsIgnoreCase(AuditActions.UPDATE.name())));
  }

  @Test
  void testDeleteStudy() throws Exception {
    FileStorageObject fso1 = new FileStorageObject();
    fso1.setMediaType(randomAlphabetic(20));
    fso1.setCategory(FileCategory.ALTERNATIVE_DATA_SHARING_PLAN);
    fso1.setBlobId(BlobId.of(randomAlphabetic(10), randomAlphabetic(10)));
    fso1.setFileName(randomAlphabetic(10));

    FileStorageObject fso2 = new FileStorageObject();
    fso2.setMediaType(randomAlphabetic(20));
    fso2.setCategory(FileCategory.NIH_INSTITUTIONAL_CERTIFICATION);
    fso2.setBlobId(BlobId.of(randomAlphabetic(10), randomAlphabetic(10)));
    fso2.setFileName(randomAlphabetic(10));
    Study study = createStudy(fso1, fso2, null);

    List<Dataset> datasets =
        datasetDAO.findDatasetsByIdList(new ArrayList<>(study.getDatasetIds()));
    study.addDatasets(datasets);

    serviceDAO.deleteStudy(study, createUser());
    Study deletedStudy = studyDAO.findStudyById(study.getStudyId());
    assertNull(deletedStudy);

    // Validate that audit records are added for each dataset:
    study
        .getDatasetIds()
        .forEach(
            id -> {
              List<DatasetAudit> audits = datasetDAO.findAuditsByDatasetId(id);
              assertFalse(audits.isEmpty());
              assertTrue(
                  audits.stream()
                      .anyMatch(a -> a.getAction().equalsIgnoreCase(AuditActions.CREATE.name())));
              assertTrue(
                  audits.stream()
                      .anyMatch(a -> a.getAction().equalsIgnoreCase(AuditActions.DELETE.name())));
            });
  }

  @Test
  void testDeleteStudyWithNoDatasets() throws Exception {
    // Registration process creates a study and dataset with properties
    Study study = createStudy(null, null, null);
    // Delete any created datasets
    study
        .getDatasetIds()
        .forEach(
            id -> {
              datasetDAO.deleteDatasetPropertiesByDatasetId(id);
              datasetDAO.deleteDatasetById(id);
            });
    // Ensure that study deletion succeeds
    serviceDAO.deleteStudy(study, createUser());
    Study deletedStudy = studyDAO.findStudyById(study.getStudyId());
    assertNull(deletedStudy);
  }

  @Test
  void testExecuteUpdateDatasetWithNullName() throws Exception {
    // This creates a study with a single dataset:
    Study study = createStudy(null, null, null);
    List<Dataset> datasets = datasetDAO.findDatasetsByIdList(study.getDatasetIds());
    Dataset dataset = datasets.getFirst();
    jdbi.useHandle(
        handle ->
            serviceDAO.executeUpdateDatasetWithFiles(
                handle,
                dataset.getDatasetId(),
                null,
                study.getCreateUserId(),
                dataset.getDacId(),
                List.of(),
                List.of(),
                false));
    Dataset updatedDataset = datasetDAO.findDatasetById(dataset.getDatasetId());
    assertNotNull(updatedDataset.getName());
    List<DatasetAudit> audits = datasetDAO.findAuditsByDatasetId(dataset.getDatasetId());
    assertFalse(audits.isEmpty());
  }

  @Test
  void testExecuteUpdateDatasetWithEmptyName() throws Exception {
    // This creates a study with a single dataset:
    Study study = createStudy(null, null, null);
    List<Dataset> datasets =
        datasetDAO.findDatasetsByIdList(study.getDatasetIds().stream().toList());
    Dataset dataset = datasets.getFirst();
    jdbi.useHandle(
        handle ->
            serviceDAO.executeUpdateDatasetWithFiles(
                handle,
                dataset.getDatasetId(),
                "",
                study.getCreateUserId(),
                dataset.getDacId(),
                List.of(),
                List.of(),
                false));
    Dataset updatedDataset = datasetDAO.findDatasetById(dataset.getDatasetId());
    assertNotNull(updatedDataset.getName());
    List<DatasetAudit> audits = datasetDAO.findAuditsByDatasetId(dataset.getDatasetId());
    assertFalse(audits.isEmpty());
  }

  @Test
  void testExecuteUpdateDatasetWithNewName() throws Exception {
    // This creates a study with a single dataset:
    Study study = createStudy(null, null, null);
    List<Dataset> datasets =
        datasetDAO.findDatasetsByIdList(study.getDatasetIds().stream().toList());
    Dataset dataset = datasets.getFirst();
    String newName = randomAlphabetic(dataset.getName().length() + 10);
    jdbi.useHandle(
        handle ->
            serviceDAO.executeUpdateDatasetWithFiles(
                handle,
                dataset.getDatasetId(),
                newName,
                study.getCreateUserId(),
                dataset.getDacId(),
                List.of(),
                List.of(),
                false));
    Dataset updatedDataset = datasetDAO.findDatasetById(dataset.getDatasetId());
    assertEquals(newName, updatedDataset.getName());
    List<DatasetAudit> audits = datasetDAO.findAuditsByDatasetId(dataset.getDatasetId());
    assertFalse(audits.isEmpty());
  }

  @Test
  void testPatchDataset() throws Exception {
    List<Dictionary> dictionaries = datasetDAO.getDictionaryTerms();
    Dictionary one =
        dictionaries.stream().filter(d -> d.getKeyId().equals(1)).findFirst().orElse(null);
    Dictionary two =
        dictionaries.stream().filter(d -> d.getKeyId().equals(2)).findFirst().orElse(null);
    Dictionary three =
        dictionaries.stream().filter(d -> d.getKeyId().equals(3)).findFirst().orElse(null);
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
    prop1.setPropertyValue(randomAlphabetic(10));
    prop1.setDatasetId(dataset.getDatasetId());
    prop1.setCreateDate(new Date());

    // This prop WILL change
    DatasetProperty prop2 = new DatasetProperty();
    prop2.setSchemaProperty(two.getKey());
    prop2.setPropertyName(two.getKey());
    prop2.setPropertyType(PropertyType.String);
    prop2.setPropertyKey(two.getKeyId());
    prop2.setPropertyValue(randomAlphabetic(10));
    prop2.setDatasetId(dataset.getDatasetId());
    prop2.setCreateDate(new Date());

    datasetDAO.insertDatasetProperties(List.of(prop1, prop2));

    // Patch to prop2
    DatasetProperty patchProp = new DatasetProperty();
    patchProp.setSchemaProperty(prop2.getSchemaProperty());
    patchProp.setPropertyName(prop2.getPropertyName());
    patchProp.setPropertyType(prop2.getPropertyType());
    patchProp.setPropertyKey(prop2.getPropertyKey());
    patchProp.setPropertyValue(randomAlphabetic(10));

    // New, added prop
    DatasetProperty prop3 = new DatasetProperty();
    prop3.setSchemaProperty(three.getKey());
    prop3.setPropertyName(three.getKey());
    prop3.setPropertyType(PropertyType.String);
    prop3.setPropertyKey(three.getKeyId());
    prop3.setPropertyValue(randomAlphabetic(10));
    prop3.setCreateDate(new Date());

    String newName = randomAlphabetic(10);
    DatasetPatch patch = new DatasetPatch(newName, List.of(patchProp, prop3));

    serviceDAO.patchDataset(dataset.getDatasetId(), user, patch);
    Dataset patched = datasetDAO.findDatasetById(dataset.getDatasetId());

    // Validate that the name is updated
    assertEquals(newName, patched.getDatasetName());

    Set<DatasetProperty> updatedProps =
        datasetDAO.findDatasetPropertiesByDatasetId(dataset.getDatasetId());

    // Validate that the first prop was not changed
    Optional<DatasetProperty> original =
        updatedProps.stream()
            .filter(p -> p.getPropertyName().equals(prop1.getPropertyName()))
            .findFirst();
    assertTrue(original.isPresent());
    assertEquals(prop1.getPropertyValue(), original.get().getPropertyValue());

    // Validate that the new value was updated
    Optional<DatasetProperty> updated =
        updatedProps.stream()
            .filter(p -> p.getPropertyName().equals(prop2.getPropertyName()))
            .findFirst();
    assertTrue(updated.isPresent());
    assertEquals(patchProp.getPropertyValue(), updated.get().getPropertyValue());

    // Validate that the new prop was added
    Optional<DatasetProperty> added =
        updatedProps.stream()
            .filter(p -> p.getPropertyName().equals(prop3.getPropertyName()))
            .findFirst();
    assertTrue(added.isPresent());
    assertEquals(prop3.getPropertyValue(), added.get().getPropertyValue());

    // Validate that no props were deleted
    assertEquals(3, patched.getProperties().size());

    // Validate that an audit record was added:
    List<DatasetAudit> audits = datasetDAO.findAuditsByDatasetId(dataset.getDatasetId());
    assertFalse(audits.isEmpty());
    assertTrue(
        audits.stream().anyMatch(a -> a.getAction().equalsIgnoreCase(AuditActions.UPDATE.name())));
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"   "})
  void testPatchDatasetWithNullAndEmptyNames(String input) throws Exception {
    Dataset dataset = createDataset();
    User user = userDAO.findUserById(dataset.getCreateUserId());
    DatasetPatch patch = new DatasetPatch(input, List.of());
    serviceDAO.patchDataset(dataset.getDatasetId(), user, patch);
    Dataset patched = datasetDAO.findDatasetById(dataset.getDatasetId());

    // Validate that the name is NOT updated to a null value
    assertEquals(dataset.getName(), patched.getDatasetName());

    // Validate that an update audit record was added:
    List<DatasetAudit> audits = datasetDAO.findAuditsByDatasetId(dataset.getDatasetId());
    assertFalse(audits.isEmpty());
    assertTrue(
        audits.stream().anyMatch(a -> a.getAction().equalsIgnoreCase(AuditActions.UPDATE.name())));
  }

  @Test
  void testUpdateDatasetIndexWithDate() throws Exception {
    Dataset dataset = createDataset();
    serviceDAO.updateDatasetIndex(dataset.getDatasetId(), dataset.getCreateUserId(), Instant.now());
    Dataset updatedDataset = datasetDAO.findDatasetById(dataset.getDatasetId());

    // Validate that the indexed date is updated
    assertNotNull(updatedDataset.getIndexedDate());

    // Validate that an INDEXED audit record was added:
    List<DatasetAudit> audits = datasetDAO.findAuditsByDatasetId(updatedDataset.getDatasetId());
    assertFalse(audits.isEmpty());
    assertTrue(
        audits.stream().anyMatch(a -> a.getAction().equalsIgnoreCase(AuditActions.INDEXED.name())));
  }

  @Test
  void testUpdateDatasetIndexWithNull() throws Exception {
    Dataset dataset = createDataset();
    serviceDAO.updateDatasetIndex(dataset.getDatasetId(), dataset.getCreateUserId(), null);
    Dataset updatedDataset = datasetDAO.findDatasetById(dataset.getDatasetId());

    // Validate that the indexed date is null
    assertNull(updatedDataset.getIndexedDate());

    // Validate that a DEINDEXED audit record was added:
    List<DatasetAudit> audits = datasetDAO.findAuditsByDatasetId(updatedDataset.getDatasetId());
    assertFalse(audits.isEmpty());
    assertTrue(
        audits.stream()
            .anyMatch(a -> a.getAction().equalsIgnoreCase(AuditActions.DEINDEXED.name())));
  }

  @Test
  void testPatchStudyAllProperties() throws Exception {
    Study study = createStudy(null, null, null);
    User user = userDAO.findUserById(study.getCreateUserId());
    StudyPatch patch =
        new StudyPatch(
            randomAlphabetic(10),
            StudyType.OBSERVATIONAL,
            randomAlphabetic(10),
            List.of("tag1", "tag2"),
            randomAlphabetic(10),
            randomAlphabetic(10),
            randomAlphabetic(10),
            null,
            List.of("email1", "email2"),
            randomAlphabetic(10),
            randomAlphabetic(10),
            true,
            randomAlphabetic(10),
            randomAlphabetic(10));
    Study patched = serviceDAO.patchStudy(study, user, patch);
    assertEquals(patch.name(), patched.getName());
    assertEquals(patch.description(), patched.getDescription());
    assertEquals(patch.dataTypes(), patched.getDataTypes());
    assertEquals(patch.piName(), patched.getPiName());
    assertEquals(patch.publicVisibility(), patched.getPublicVisibility());
    assertEquals(
        patch.studyType().value(),
        patched.getProperties().stream()
            .filter(p -> p.getKey().equals(STUDY_TYPE))
            .findFirst()
            .orElseThrow()
            .getValue());
    assertEquals(
        patch.phenotypeIndication(),
        patched.getProperties().stream()
            .filter(p -> p.getKey().equals(PHENOTYPE_INDICATION))
            .findFirst()
            .orElseThrow()
            .getValue());
    assertEquals(
        patch.species(),
        patched.getProperties().stream()
            .filter(p -> p.getKey().equals(SPECIES_KEY))
            .findFirst()
            .orElseThrow()
            .getValue());
    assertEquals(
        GsonUtil.getInstance().toJson(patch.dataCustodianEmail()),
        patched.getProperties().stream()
            .filter(p -> p.getKey().equals(DATA_CUSTODIAN_EMAIL))
            .findFirst()
            .orElseThrow()
            .getValue()
            .toString());
    assertEquals(
        patch.alternativeDataSharingPlanTargetDeliveryDate(),
        patched.getProperties().stream()
            .filter(p -> p.getKey().equals(ALTERNATIVE_DATA_SHARING_PLAN_TARGET_DELIVERY_DATE))
            .findFirst()
            .orElseThrow()
            .getValue());
    assertEquals(
        patch.alternativeDataSharingPlanTargetPublicReleaseDate(),
        patched.getProperties().stream()
            .filter(
                p -> p.getKey().equals(ALTERNATIVE_DATA_SHARING_PLAN_TARGET_PUBLIC_RELEASE_DATE))
            .findFirst()
            .orElseThrow()
            .getValue());
  }

  @Test
  void testPatchStudyNoChanges() throws Exception {
    Study study = createStudy(null, null, null);
    User user = userDAO.findUserById(study.getCreateUserId());
    StudyPatch patch =
        new StudyPatch(
            null, null, null, null, null, null, null, null, null, null, null, null, null, null);
    Study patched = serviceDAO.patchStudy(study, user, patch);
    assertEquals(study.getName(), patched.getName());
    assertEquals(study.getDescription(), patched.getDescription());
    assertEquals(study.getDataTypes(), patched.getDataTypes());
    assertEquals(study.getPiName(), patched.getPiName());
    assertEquals(study.getPublicVisibility(), patched.getPublicVisibility());
    assertTrue(study.getProperties().stream().noneMatch(p -> p.getKey().equals(STUDY_TYPE)));
    assertTrue(
        study.getProperties().stream().noneMatch(p -> p.getKey().equals(PHENOTYPE_INDICATION)));
    assertTrue(study.getProperties().stream().noneMatch(p -> p.getKey().equals(SPECIES_KEY)));
    assertTrue(
        study.getProperties().stream().noneMatch(p -> p.getKey().equals(DATA_CUSTODIAN_EMAIL)));
    assertTrue(
        study.getProperties().stream()
            .noneMatch(p -> p.getKey().equals(ALTERNATIVE_DATA_SHARING_PLAN_TARGET_DELIVERY_DATE)));
    assertTrue(
        study.getProperties().stream()
            .noneMatch(
                p -> p.getKey().equals(ALTERNATIVE_DATA_SHARING_PLAN_TARGET_PUBLIC_RELEASE_DATE)));
  }

  @Test
  void testPatchStudyName() throws Exception {
    Study study = createStudy(null, null, null);
    User user = userDAO.findUserById(study.getCreateUserId());
    StudyPatch patch =
        new StudyPatch(
            randomAlphabetic(10),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null);
    Study patched = serviceDAO.patchStudy(study, user, patch);
    assertEquals(patch.name(), patched.getName());
    assertEquals(study.getDescription(), patched.getDescription());
    assertEquals(study.getDataTypes(), patched.getDataTypes());
    assertEquals(study.getPiName(), patched.getPiName());
    assertEquals(study.getPublicVisibility(), patched.getPublicVisibility());
    assertTrue(study.getProperties().stream().noneMatch(p -> p.getKey().equals(STUDY_TYPE)));
    assertTrue(
        study.getProperties().stream().noneMatch(p -> p.getKey().equals(PHENOTYPE_INDICATION)));
    assertTrue(study.getProperties().stream().noneMatch(p -> p.getKey().equals(SPECIES_KEY)));
    assertTrue(
        study.getProperties().stream().noneMatch(p -> p.getKey().equals(DATA_CUSTODIAN_EMAIL)));
    assertTrue(
        study.getProperties().stream()
            .noneMatch(p -> p.getKey().equals(ALTERNATIVE_DATA_SHARING_PLAN_TARGET_DELIVERY_DATE)));
    assertTrue(
        study.getProperties().stream()
            .noneMatch(
                p -> p.getKey().equals(ALTERNATIVE_DATA_SHARING_PLAN_TARGET_PUBLIC_RELEASE_DATE)));
  }

  @Test
  void testPatchStudyDescription() throws Exception {
    Study study = createStudy(null, null, null);
    User user = userDAO.findUserById(study.getCreateUserId());
    StudyPatch patch =
        new StudyPatch(
            null,
            null,
            randomAlphabetic(10),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null);
    Study patched = serviceDAO.patchStudy(study, user, patch);
    assertEquals(study.getName(), patched.getName());
    assertEquals(patch.description(), patched.getDescription());
    assertEquals(study.getDataTypes(), patched.getDataTypes());
    assertEquals(study.getPiName(), patched.getPiName());
    assertEquals(study.getPublicVisibility(), patched.getPublicVisibility());
    assertTrue(study.getProperties().stream().noneMatch(p -> p.getKey().equals(STUDY_TYPE)));
    assertTrue(
        study.getProperties().stream().noneMatch(p -> p.getKey().equals(PHENOTYPE_INDICATION)));
    assertTrue(study.getProperties().stream().noneMatch(p -> p.getKey().equals(SPECIES_KEY)));
    assertTrue(
        study.getProperties().stream().noneMatch(p -> p.getKey().equals(DATA_CUSTODIAN_EMAIL)));
    assertTrue(
        study.getProperties().stream()
            .noneMatch(p -> p.getKey().equals(ALTERNATIVE_DATA_SHARING_PLAN_TARGET_DELIVERY_DATE)));
    assertTrue(
        study.getProperties().stream()
            .noneMatch(
                p -> p.getKey().equals(ALTERNATIVE_DATA_SHARING_PLAN_TARGET_PUBLIC_RELEASE_DATE)));
  }

  @Test
  void testPatchStudyDataTypes() throws Exception {
    Study study = createStudy(null, null, null);
    User user = userDAO.findUserById(study.getCreateUserId());
    StudyPatch patch =
        new StudyPatch(
            null,
            null,
            null,
            List.of("tag1", "tag2"),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null);
    Study patched = serviceDAO.patchStudy(study, user, patch);
    assertEquals(study.getName(), patched.getName());
    assertEquals(study.getDescription(), patched.getDescription());
    assertEquals(patch.dataTypes(), patched.getDataTypes());
    assertEquals(study.getPiName(), patched.getPiName());
    assertEquals(study.getPublicVisibility(), patched.getPublicVisibility());
    assertTrue(study.getProperties().stream().noneMatch(p -> p.getKey().equals(STUDY_TYPE)));
    assertTrue(
        study.getProperties().stream().noneMatch(p -> p.getKey().equals(PHENOTYPE_INDICATION)));
    assertTrue(study.getProperties().stream().noneMatch(p -> p.getKey().equals(SPECIES_KEY)));
    assertTrue(
        study.getProperties().stream().noneMatch(p -> p.getKey().equals(DATA_CUSTODIAN_EMAIL)));
    assertTrue(
        study.getProperties().stream()
            .noneMatch(p -> p.getKey().equals(ALTERNATIVE_DATA_SHARING_PLAN_TARGET_DELIVERY_DATE)));
    assertTrue(
        study.getProperties().stream()
            .noneMatch(
                p -> p.getKey().equals(ALTERNATIVE_DATA_SHARING_PLAN_TARGET_PUBLIC_RELEASE_DATE)));
  }

  @Test
  void testPatchStudyPIName() throws Exception {
    Study study = createStudy(null, null, null);
    User user = userDAO.findUserById(study.getCreateUserId());
    StudyPatch patch =
        new StudyPatch(
            null,
            null,
            null,
            null,
            null,
            null,
            randomAlphabetic(10),
            null,
            null,
            null,
            null,
            null,
            null,
            null);
    Study patched = serviceDAO.patchStudy(study, user, patch);
    assertEquals(study.getName(), patched.getName());
    assertEquals(study.getDescription(), patched.getDescription());
    assertEquals(study.getDataTypes(), patched.getDataTypes());
    assertEquals(patch.piName(), patched.getPiName());
    assertEquals(study.getPublicVisibility(), patched.getPublicVisibility());
    assertTrue(study.getProperties().stream().noneMatch(p -> p.getKey().equals(STUDY_TYPE)));
    assertTrue(
        study.getProperties().stream().noneMatch(p -> p.getKey().equals(PHENOTYPE_INDICATION)));
    assertTrue(study.getProperties().stream().noneMatch(p -> p.getKey().equals(SPECIES_KEY)));
    assertTrue(
        study.getProperties().stream().noneMatch(p -> p.getKey().equals(DATA_CUSTODIAN_EMAIL)));
    assertTrue(
        study.getProperties().stream()
            .noneMatch(p -> p.getKey().equals(ALTERNATIVE_DATA_SHARING_PLAN_TARGET_DELIVERY_DATE)));
    assertTrue(
        study.getProperties().stream()
            .noneMatch(
                p -> p.getKey().equals(ALTERNATIVE_DATA_SHARING_PLAN_TARGET_PUBLIC_RELEASE_DATE)));
  }

  @Test
  void testPatchStudyPublicVisibility() throws Exception {
    Study study = createStudy(null, null, null);
    User user = userDAO.findUserById(study.getCreateUserId());
    StudyPatch patch =
        new StudyPatch(
            null, null, null, null, null, null, null, null, null, null, null, false, null, null);
    Study patched = serviceDAO.patchStudy(study, user, patch);
    assertEquals(study.getName(), patched.getName());
    assertEquals(study.getDescription(), patched.getDescription());
    assertEquals(study.getDataTypes(), patched.getDataTypes());
    assertEquals(study.getPiName(), patched.getPiName());
    assertEquals(patch.publicVisibility(), patched.getPublicVisibility());
    assertTrue(study.getProperties().stream().noneMatch(p -> p.getKey().equals(STUDY_TYPE)));
    assertTrue(
        study.getProperties().stream().noneMatch(p -> p.getKey().equals(PHENOTYPE_INDICATION)));
    assertTrue(study.getProperties().stream().noneMatch(p -> p.getKey().equals(SPECIES_KEY)));
    assertTrue(
        study.getProperties().stream().noneMatch(p -> p.getKey().equals(DATA_CUSTODIAN_EMAIL)));
    assertTrue(
        study.getProperties().stream()
            .noneMatch(p -> p.getKey().equals(ALTERNATIVE_DATA_SHARING_PLAN_TARGET_DELIVERY_DATE)));
    assertTrue(
        study.getProperties().stream()
            .noneMatch(
                p -> p.getKey().equals(ALTERNATIVE_DATA_SHARING_PLAN_TARGET_PUBLIC_RELEASE_DATE)));
  }

  @Test
  void testPatchStudyStudyType() throws Exception {
    Study study = createStudy(null, null, null);
    User user = userDAO.findUserById(study.getCreateUserId());
    StudyPatch patch =
        new StudyPatch(
            null,
            StudyType.COHORT_STUDY,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null);
    Study patched = serviceDAO.patchStudy(study, user, patch);
    assertEquals(study.getName(), patched.getName());
    assertEquals(study.getDescription(), patched.getDescription());
    assertEquals(study.getDataTypes(), patched.getDataTypes());
    assertEquals(study.getPiName(), patched.getPiName());
    assertEquals(study.getPublicVisibility(), patched.getPublicVisibility());
    assertEquals(
        patch.studyType().value(),
        patched.getProperties().stream()
            .filter(p -> p.getKey().equals(STUDY_TYPE))
            .findFirst()
            .orElseThrow()
            .getValue());
    assertTrue(
        study.getProperties().stream().noneMatch(p -> p.getKey().equals(PHENOTYPE_INDICATION)));
    assertTrue(study.getProperties().stream().noneMatch(p -> p.getKey().equals(SPECIES_KEY)));
    assertTrue(
        study.getProperties().stream().noneMatch(p -> p.getKey().equals(DATA_CUSTODIAN_EMAIL)));
    assertTrue(
        study.getProperties().stream()
            .noneMatch(p -> p.getKey().equals(ALTERNATIVE_DATA_SHARING_PLAN_TARGET_DELIVERY_DATE)));
    assertTrue(
        study.getProperties().stream()
            .noneMatch(
                p -> p.getKey().equals(ALTERNATIVE_DATA_SHARING_PLAN_TARGET_PUBLIC_RELEASE_DATE)));
  }

  @Test
  void testPatchStudyPhenotypeIndication() throws Exception {
    Study study = createStudy(null, null, null);
    User user = userDAO.findUserById(study.getCreateUserId());
    StudyPatch patch =
        new StudyPatch(
            null,
            null,
            null,
            null,
            randomAlphabetic(10),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null);
    Study patched = serviceDAO.patchStudy(study, user, patch);
    assertEquals(study.getName(), patched.getName());
    assertEquals(study.getDescription(), patched.getDescription());
    assertEquals(study.getDataTypes(), patched.getDataTypes());
    assertEquals(study.getPiName(), patched.getPiName());
    assertEquals(study.getPublicVisibility(), patched.getPublicVisibility());
    assertTrue(patched.getProperties().stream().noneMatch(p -> p.getKey().equals(STUDY_TYPE)));
    assertEquals(
        patch.phenotypeIndication(),
        patched.getProperties().stream()
            .filter(p -> p.getKey().equals(PHENOTYPE_INDICATION))
            .findFirst()
            .orElseThrow()
            .getValue());
    assertTrue(study.getProperties().stream().noneMatch(p -> p.getKey().equals(SPECIES_KEY)));
    assertTrue(
        study.getProperties().stream().noneMatch(p -> p.getKey().equals(DATA_CUSTODIAN_EMAIL)));
    assertTrue(
        study.getProperties().stream()
            .noneMatch(p -> p.getKey().equals(ALTERNATIVE_DATA_SHARING_PLAN_TARGET_DELIVERY_DATE)));
    assertTrue(
        study.getProperties().stream()
            .noneMatch(
                p -> p.getKey().equals(ALTERNATIVE_DATA_SHARING_PLAN_TARGET_PUBLIC_RELEASE_DATE)));
  }

  @Test
  void testPatchStudySpecies() throws Exception {
    Study study = createStudy(null, null, null);
    User user = userDAO.findUserById(study.getCreateUserId());
    StudyPatch patch =
        new StudyPatch(
            null,
            null,
            null,
            null,
            null,
            randomAlphabetic(10),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null);
    Study patched = serviceDAO.patchStudy(study, user, patch);
    assertEquals(study.getName(), patched.getName());
    assertEquals(study.getDescription(), patched.getDescription());
    assertEquals(study.getDataTypes(), patched.getDataTypes());
    assertEquals(study.getPiName(), patched.getPiName());
    assertEquals(study.getPublicVisibility(), patched.getPublicVisibility());
    assertTrue(patched.getProperties().stream().noneMatch(p -> p.getKey().equals(STUDY_TYPE)));
    assertTrue(
        study.getProperties().stream().noneMatch(p -> p.getKey().equals(PHENOTYPE_INDICATION)));
    assertEquals(
        patch.species(),
        patched.getProperties().stream()
            .filter(p -> p.getKey().equals(SPECIES_KEY))
            .findFirst()
            .orElseThrow()
            .getValue());
    assertTrue(
        study.getProperties().stream().noneMatch(p -> p.getKey().equals(DATA_CUSTODIAN_EMAIL)));
    assertTrue(
        study.getProperties().stream()
            .noneMatch(p -> p.getKey().equals(ALTERNATIVE_DATA_SHARING_PLAN_TARGET_DELIVERY_DATE)));
    assertTrue(
        study.getProperties().stream()
            .noneMatch(
                p -> p.getKey().equals(ALTERNATIVE_DATA_SHARING_PLAN_TARGET_PUBLIC_RELEASE_DATE)));
  }

  @Test
  void testPatchStudyDataCustodianEmail() throws Exception {
    Study study = createStudy(null, null, null);
    User user = userDAO.findUserById(study.getCreateUserId());
    StudyPatch patch =
        new StudyPatch(
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            List.of("email1", "email2"),
            null,
            null,
            null,
            null,
            null);
    Study patched = serviceDAO.patchStudy(study, user, patch);
    assertEquals(study.getName(), patched.getName());
    assertEquals(study.getDescription(), patched.getDescription());
    assertEquals(study.getDataTypes(), patched.getDataTypes());
    assertEquals(study.getPiName(), patched.getPiName());
    assertEquals(study.getPublicVisibility(), patched.getPublicVisibility());
    assertTrue(patched.getProperties().stream().noneMatch(p -> p.getKey().equals(STUDY_TYPE)));
    assertTrue(
        study.getProperties().stream().noneMatch(p -> p.getKey().equals(PHENOTYPE_INDICATION)));
    assertTrue(study.getProperties().stream().noneMatch(p -> p.getKey().equals(SPECIES_KEY)));
    assertEquals(
        GsonUtil.getInstance().toJson(patch.dataCustodianEmail()),
        patched.getProperties().stream()
            .filter(p -> p.getKey().equals(DATA_CUSTODIAN_EMAIL))
            .findFirst()
            .orElseThrow()
            .getValue()
            .toString());
    assertTrue(
        study.getProperties().stream()
            .noneMatch(p -> p.getKey().equals(ALTERNATIVE_DATA_SHARING_PLAN_TARGET_DELIVERY_DATE)));
    assertTrue(
        study.getProperties().stream()
            .noneMatch(
                p -> p.getKey().equals(ALTERNATIVE_DATA_SHARING_PLAN_TARGET_PUBLIC_RELEASE_DATE)));
  }

  @Test
  void testPatchStudyAlternativeDataSharingPlanTargetDeliveryDate() throws Exception {
    Study study = createStudy(null, null, null);
    User user = userDAO.findUserById(study.getCreateUserId());
    StudyPatch patch =
        new StudyPatch(
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            randomAlphabetic(10),
            null,
            null,
            null,
            null);
    Study patched = serviceDAO.patchStudy(study, user, patch);
    assertEquals(study.getName(), patched.getName());
    assertEquals(study.getDescription(), patched.getDescription());
    assertEquals(study.getDataTypes(), patched.getDataTypes());
    assertEquals(study.getPiName(), patched.getPiName());
    assertEquals(study.getPublicVisibility(), patched.getPublicVisibility());
    assertTrue(patched.getProperties().stream().noneMatch(p -> p.getKey().equals(STUDY_TYPE)));
    assertTrue(
        study.getProperties().stream().noneMatch(p -> p.getKey().equals(PHENOTYPE_INDICATION)));
    assertTrue(study.getProperties().stream().noneMatch(p -> p.getKey().equals(SPECIES_KEY)));
    assertTrue(
        study.getProperties().stream().noneMatch(p -> p.getKey().equals(DATA_CUSTODIAN_EMAIL)));
    assertEquals(
        patch.alternativeDataSharingPlanTargetDeliveryDate(),
        patched.getProperties().stream()
            .filter(p -> p.getKey().equals(ALTERNATIVE_DATA_SHARING_PLAN_TARGET_DELIVERY_DATE))
            .findFirst()
            .orElseThrow()
            .getValue());
    assertTrue(
        study.getProperties().stream()
            .noneMatch(
                p -> p.getKey().equals(ALTERNATIVE_DATA_SHARING_PLAN_TARGET_PUBLIC_RELEASE_DATE)));
  }

  @Test
  void testPatchStudyAlternativeDataSharingPlanTargetPublicReleaseDate() throws Exception {
    Study study = createStudy(null, null, null);
    User user = userDAO.findUserById(study.getCreateUserId());
    StudyPatch patch =
        new StudyPatch(
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            randomAlphabetic(10),
            null,
            null,
            null);
    Study patched = serviceDAO.patchStudy(study, user, patch);
    assertEquals(study.getName(), patched.getName());
    assertEquals(study.getDescription(), patched.getDescription());
    assertEquals(study.getDataTypes(), patched.getDataTypes());
    assertEquals(study.getPiName(), patched.getPiName());
    assertEquals(study.getPublicVisibility(), patched.getPublicVisibility());
    assertTrue(patched.getProperties().stream().noneMatch(p -> p.getKey().equals(STUDY_TYPE)));
    assertTrue(
        study.getProperties().stream().noneMatch(p -> p.getKey().equals(PHENOTYPE_INDICATION)));
    assertTrue(study.getProperties().stream().noneMatch(p -> p.getKey().equals(SPECIES_KEY)));
    assertTrue(
        study.getProperties().stream().noneMatch(p -> p.getKey().equals(DATA_CUSTODIAN_EMAIL)));
    assertTrue(
        study.getProperties().stream()
            .noneMatch(p -> p.getKey().equals(ALTERNATIVE_DATA_SHARING_PLAN_TARGET_DELIVERY_DATE)));
    assertEquals(
        patch.alternativeDataSharingPlanTargetPublicReleaseDate(),
        patched.getProperties().stream()
            .filter(
                p -> p.getKey().equals(ALTERNATIVE_DATA_SHARING_PLAN_TARGET_PUBLIC_RELEASE_DATE))
            .findFirst()
            .orElseThrow()
            .getValue());
  }

  @Test
  void testPatchStudyExternalIdentifier() throws Exception {
    Study study = createStudy(null, null, null);
    User user = userDAO.findUserById(study.getCreateUserId());
    StudyPatch patch =
        new StudyPatch(
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            randomAlphabetic(10),
            null);
    Study patched = serviceDAO.patchStudy(study, user, patch);
    assertEquals(study.getName(), patched.getName());
    assertEquals(
        patch.externalIdentifier(),
        patched.getProperties().stream()
            .filter(p -> p.getKey().equals(EXTERNAL_IDENTIFIER))
            .findFirst()
            .orElseThrow()
            .getValue());
  }

  @Test
  void testPatchStudyExternalIdentifierType() throws Exception {
    Study study = createStudy(null, null, null);
    User user = userDAO.findUserById(study.getCreateUserId());
    StudyPatch patch =
        new StudyPatch(
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            randomAlphabetic(10));
    Study patched = serviceDAO.patchStudy(study, user, patch);
    assertEquals(study.getName(), patched.getName());
    assertEquals(
        patch.externalIdentifierType(),
        patched.getProperties().stream()
            .filter(p -> p.getKey().equals(EXTERNAL_IDENTIFIER_TYPE))
            .findFirst()
            .orElseThrow()
            .getValue());
  }

  @Test
  void testUpdateDatasetDataUse() {
    Dataset dataset = createDataset();
    User user = userDAO.findUserById(dataset.getCreateUserId());
    DataUse newDataUse = new DataUseBuilder().setGeneralUse(true).setHmbResearch(true).build();
    String translatedDataUse =
        new TranslationUtil(ontologyDAO).translate(newDataUse, DataUseTranslationType.DATASET);
    serviceDAO.updateDatasetDataUse(user, dataset, newDataUse, translatedDataUse);
    Dataset updatedDataset = datasetDAO.findDatasetById(dataset.getDatasetId());
    assertEquals(translatedDataUse, updatedDataset.getTranslatedDataUse());
    assertEquals(newDataUse, updatedDataset.getDataUse());
    assertNotNull(updatedDataset.getUpdateDate());
    assertEquals(user.getUserId(), updatedDataset.getUpdateUserId());
    List<DatasetAudit> audits = datasetDAO.findAuditsByDatasetId(dataset.getDatasetId());
    assertFalse(audits.isEmpty());
    Optional<DatasetAudit> updateAudit =
        audits.stream()
            .filter(a -> a.getAction().equalsIgnoreCase(AuditActions.UPDATE.getValue()))
            .findFirst();
    assertTrue(updateAudit.isPresent());
    assertEquals(user.getUserId(), updateAudit.get().getUser());
  }

  /**
   * Helper method to create a study with two props and one dataset
   *
   * @param alternateSharingPlanFSO Optional FSO to use as part of the study insert
   * @param dataset1InstitutionalCertification Optional FSO to use as part of the first dataset
   *     insert
   * @param dataset2InstitutionalCertification Optional FSO to use as part of the second dataset
   *     insert
   * @return Study
   * @throws Exception The exception
   */
  private Study createStudy(
      FileStorageObject alternateSharingPlanFSO,
      FileStorageObject dataset1InstitutionalCertification,
      FileStorageObject dataset2InstitutionalCertification)
      throws Exception {
    Dac dac = createDac();
    User user = createUser();

    StudyProperty prop1 = new StudyProperty();
    prop1.setKey(randomAlphabetic(10));
    prop1.setType(PropertyType.String);
    prop1.setValue(randomAlphabetic(10));

    StudyProperty prop2 = new StudyProperty();
    prop2.setKey(randomAlphabetic(10));
    prop2.setType(PropertyType.String);
    prop2.setValue(randomAlphabetic(10));

    StudyInsert studyInsert =
        new StudyInsert(
            randomAlphabetic(10),
            randomAlphabetic(10),
            List.of(randomAlphabetic(10)),
            randomAlphabetic(10),
            null,
            true,
            user.getUserId(),
            List.of(prop1, prop2),
            Objects.isNull(alternateSharingPlanFSO) ? List.of() : List.of(alternateSharingPlanFSO));

    DatasetProperty datasetProperty = new DatasetProperty();
    datasetProperty.setSchemaProperty(randomAlphabetic(10));
    datasetProperty.setPropertyName(randomAlphabetic(10));
    datasetProperty.setPropertyType(PropertyType.Number);
    datasetProperty.setPropertyKey(1);
    datasetProperty.setPropertyValue(new Random().nextInt());
    datasetProperty.setCreateDate(new Date());

    DatasetInsert datasetInsert1 =
        new DatasetInsert(
            randomAlphabetic(20),
            dac.getDacId(),
            new DataUseBuilder().setGeneralUse(true).build(),
            user.getUserId(),
            List.of(datasetProperty),
            Objects.isNull(dataset1InstitutionalCertification)
                ? List.of()
                : List.of(dataset1InstitutionalCertification));

    DatasetInsert datasetInsert2 =
        new DatasetInsert(
            randomAlphabetic(20),
            dac.getDacId(),
            new DataUseBuilder().setGeneralUse(true).build(),
            user.getUserId(),
            List.of(datasetProperty),
            Objects.isNull(dataset2InstitutionalCertification)
                ? List.of()
                : List.of(dataset2InstitutionalCertification));

    List<Integer> createdIds =
        serviceDAO.insertDatasetRegistration(studyInsert, List.of(datasetInsert1, datasetInsert2));

    Dataset createdDataset = datasetDAO.findDatasetById(createdIds.getFirst());
    Study studyFromFirstDatasetCreated = createdDataset.getStudy();
    return studyDAO.findStudyById(studyFromFirstDatasetCreated.getStudyId());
  }

  private Dataset createDataset() {
    User user = createUser();
    String name = "Name_" + randomAlphanumeric(20);
    Timestamp now = new Timestamp(new Date().getTime());
    String objectId = "Object ID_" + randomAlphanumeric(20);
    DataUse dataUse = new DataUseBuilder().setGeneralUse(true).build();
    Integer id =
        datasetDAO.insertDataset(name, now, user.getUserId(), objectId, dataUse.toString(), null);
    return datasetDAO.findDatasetById(id);
  }

  private Dac createDac() {
    Integer id =
        dacDAO.createDac(
            "Test_" + randomAlphanumeric(20),
            "Test_" + randomAlphanumeric(20),
            createUser().getUserId());
    return dacDAO.findById(id);
  }
}
