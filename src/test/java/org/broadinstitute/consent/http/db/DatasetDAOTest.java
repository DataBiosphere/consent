package org.broadinstitute.consent.http.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.google.gson.JsonObject;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
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
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DarCollection;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.models.DataUseBuilder;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.DatasetAudit;
import org.broadinstitute.consent.http.models.DatasetProperty;
import org.broadinstitute.consent.http.models.Dictionary;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.FileStorageObject;
import org.broadinstitute.consent.http.models.Study;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.models.Vote;
import org.broadinstitute.consent.http.models.dto.DatasetDTO;
import org.broadinstitute.consent.http.util.gson.GsonUtil;
import org.junit.jupiter.api.Test;

public class DatasetDAOTest extends DAOTestHelper {

  @Test
  public void testFindDatasetByIdWithDacAndConsent() {
    Dataset dataset = insertDataset();
    Dac dac = insertDac();
    Consent consent = insertConsent();
    datasetDAO.updateDatasetDacId(dataset.getDataSetId(), dac.getDacId());
    consentDAO.insertConsentAssociation(consent.getConsentId(), ASSOCIATION_TYPE_TEST,
        dataset.getDataSetId());

    Dataset foundDataset = datasetDAO.findDatasetById(dataset.getDataSetId());
    assertNotNull(foundDataset);
    assertEquals(dac.getDacId(), foundDataset.getDacId());
    assertEquals(consent.getConsentId(), foundDataset.getConsentId());
    assertEquals(consent.getTranslatedUseRestriction(),
        foundDataset.getTranslatedUseRestriction());
    assertFalse(foundDataset.getProperties().isEmpty());
    assertTrue(foundDataset.getDeletable());
    assertNotNull(foundDataset.getCreateUser());
  }

  @Test
  public void testGetActiveDatasets_positive_case() {
    // This inserts a dataset with the active property set to true
    Dataset ds = insertDataset();
    Dac dac = insertDac();
    Consent consent = insertConsent();
    datasetDAO.updateDatasetDacId(ds.getDataSetId(), dac.getDacId());
    consentDAO.insertConsentAssociation(consent.getConsentId(), ASSOCIATION_TYPE_TEST,
        ds.getDataSetId());

    List<Dataset> activeDatasets = datasetDAO.getActiveDatasets();
    assertFalse(activeDatasets.isEmpty());
    assertEquals(1, activeDatasets.size());
    assertTrue(activeDatasets.get(0).getActive());
  }

  @Test
  public void testGetActiveDatasets_negative_case() {
    // This inserts a dataset with the active property set to true
    Dataset ds = insertDataset();
    // Update so it is inactive
    datasetDAO.updateDatasetActive(ds.getDataSetId(), false);
    Dac dac = insertDac();
    Consent consent = insertConsent();
    datasetDAO.updateDatasetDacId(ds.getDataSetId(), dac.getDacId());
    consentDAO.insertConsentAssociation(consent.getConsentId(), ASSOCIATION_TYPE_TEST,
        ds.getDataSetId());

    List<Dataset> activeDatasets = datasetDAO.getActiveDatasets();
    assertTrue(activeDatasets.isEmpty());
  }

  @Test
  public void testFindPublicDatasets_positive() {
    User user = createUser();

    Dataset ds1 = insertDataset();
    Study s1 = insertStudyWithProperties();

    datasetDAO.updateStudyId(ds1.getDataSetId(), s1.getStudyId());
    datasetDAO.updateDatasetApproval(true, Instant.now(), user.getUserId(),
        ds1.getDataSetId());

    Dataset ds2 = insertDataset();
    Study s2 = insertStudyWithProperties();

    datasetDAO.updateStudyId(ds2.getDataSetId(), s2.getStudyId());
    datasetDAO.updateDatasetApproval(true, Instant.now(), user.getUserId(),
        ds2.getDataSetId());

    List<Dataset> datasets = datasetDAO.findPublicDatasets();

    assertEquals(2, datasets.size());

    assertTrue(
        datasets
            .stream()
            .map(Dataset::getDataSetId)
            .toList()
            .containsAll(
                List.of(
                    ds1.getDataSetId(),
                    ds2.getDataSetId()
                )));


  }

  @Test
  public void testFindPublicDatasets_negative() {
    User user = createUser();

    Dataset ds1 = insertDataset();
    Study s1 = insertPrivateStudyWithProperties();

    datasetDAO.updateStudyId(ds1.getDataSetId(), s1.getStudyId());
    datasetDAO.updateDatasetApproval(true, Instant.now(), user.getUserId(),
        ds1.getDataSetId());

    Dataset ds2 = insertDataset();
    Study s2 = insertPrivateStudyWithProperties();

    datasetDAO.updateStudyId(ds2.getDataSetId(), s2.getStudyId());
    datasetDAO.updateDatasetApproval(true, Instant.now(), user.getUserId(),
        ds2.getDataSetId());

    List<Dataset> datasets = datasetDAO.findPublicDatasets();

    assertEquals(0, datasets.size());

  }

  @Test
  public void testFindPublicDatasets_no_study() {
    User user = createUser();

    Dataset ds1 = insertDataset();
    datasetDAO.updateDatasetApproval(true, Instant.now(), user.getUserId(),
        ds1.getDataSetId());

    Dataset ds2 = insertDataset();
    datasetDAO.updateDatasetApproval(true, Instant.now(), user.getUserId(),
        ds2.getDataSetId());

    List<Dataset> datasets = datasetDAO.findPublicDatasets();

    assertEquals(2, datasets.size());

    assertTrue(
        datasets
            .stream()
            .map(Dataset::getDataSetId)
            .toList()
            .containsAll(
                List.of(
                    ds1.getDataSetId(),
                    ds2.getDataSetId()
                )));
  }

  @Test
  public void testFindPublicDatasets_not_approved() {
    insertDataset();
    insertDataset();

    List<Dataset> datasets = datasetDAO.findPublicDatasets();

    assertEquals(0, datasets.size());
  }

  @Test
  public void testFindDatasetsForDataSubmitter_email() {
    User user = createUser();

    Dataset ds1 = insertDataset();
    Study s1 = insertPrivateStudyWithProperties();

    studyDAO.insertStudyProperty(s1.getStudyId(),
        "dataCustodianEmail",
        PropertyType.Json.toString(),
        GsonUtil.getInstance().toJson(List.of("user@example.com", user.getEmail()))
    );

    datasetDAO.updateStudyId(ds1.getDataSetId(), s1.getStudyId());

    List<Dataset> datasets = datasetDAO.findDatasetsForDataSubmitter(user.getUserId(),
        user.getEmail());

    assertEquals(1, datasets.size());

    assertEquals(ds1.getDataSetId(), datasets.get(0).getDataSetId());
  }

  @Test
  public void testFindDatasetsForDataSubmitter_create_user() {
    User user = createUser();

    Dataset ds1 = insertDataset();
    Study s1 = insertPrivateStudyWithProperties(user);

    datasetDAO.updateStudyId(ds1.getDataSetId(), s1.getStudyId());

    List<Dataset> datasets = datasetDAO.findDatasetsForDataSubmitter(user.getUserId(),
        user.getEmail());

    assertEquals(1, datasets.size());

    assertEquals(ds1.getDataSetId(), datasets.get(0).getDataSetId());
  }

  @Test
  public void testFindDatasetsForDataSubmitter_public() {
    User user = createUser();

    Dataset ds1 = insertDataset();
    Study s1 = insertStudyWithProperties();

    datasetDAO.updateStudyId(ds1.getDataSetId(), s1.getStudyId());
    datasetDAO.updateDatasetApproval(true, Instant.now(), user.getUserId(),
        ds1.getDataSetId());

    Dataset ds2 = insertDataset();
    Study s2 = insertStudyWithProperties();

    datasetDAO.updateStudyId(ds2.getDataSetId(), s2.getStudyId());
    datasetDAO.updateDatasetApproval(true, Instant.now(), user.getUserId(),
        ds2.getDataSetId());

    List<Dataset> datasets = datasetDAO.findDatasetsForDataSubmitter(user.getUserId(),
        user.getEmail());

    assertEquals(2, datasets.size());

    assertTrue(
        datasets
            .stream()
            .map(Dataset::getDataSetId)
            .toList()
            .containsAll(
                List.of(
                    ds1.getDataSetId(),
                    ds2.getDataSetId()
                )));
  }

  @Test
  public void testFindDatasetsForDataSubmitter_negative() {
    User user = createUser();

    Dataset ds1 = insertDataset();
    Study s1 = insertPrivateStudyWithProperties();

    datasetDAO.updateStudyId(ds1.getDataSetId(), s1.getStudyId());
    datasetDAO.updateDatasetApproval(true, Instant.now(), user.getUserId(),
        ds1.getDataSetId());

    Dataset ds2 = insertDataset();
    Study s2 = insertPrivateStudyWithProperties();

    datasetDAO.updateStudyId(ds2.getDataSetId(), s2.getStudyId());
    datasetDAO.updateDatasetApproval(true, Instant.now(), user.getUserId(),
        ds2.getDataSetId());

    List<Dataset> datasets = datasetDAO.findDatasetsForDataSubmitter(user.getUserId(),
        user.getEmail());

    assertEquals(0, datasets.size());

  }

  @Test
  public void testFindDatasetsForDataSubmitter_no_study() {
    User user = createUser();

    Dataset ds1 = insertDataset();
    datasetDAO.updateDatasetApproval(true, Instant.now(), user.getUserId(),
        ds1.getDataSetId());

    Dataset ds2 = insertDataset();
    datasetDAO.updateDatasetApproval(true, Instant.now(), user.getUserId(),
        ds2.getDataSetId());

    List<Dataset> datasets = datasetDAO.findDatasetsForDataSubmitter(user.getUserId(),
        user.getEmail());

    assertEquals(2, datasets.size());

    assertTrue(
        datasets
            .stream()
            .map(Dataset::getDataSetId)
            .toList()
            .containsAll(
                List.of(
                    ds1.getDataSetId(),
                    ds2.getDataSetId()
                )));
  }

  @Test
  public void testFindDatasetsForDataSubmitter_not_approved() {
    User user = createUser();

    insertDataset();
    insertDataset();

    List<Dataset> datasets = datasetDAO.findDatasetsForDataSubmitter(user.getUserId(),
        user.getEmail());

    assertEquals(0, datasets.size());
  }

  @Test
  public void testFindDatasetsForChairperson_positive() {
    Dac dac = insertDac();

    User chairperson = createUser();
    createUserRole(UserRoles.CHAIRPERSON.getRoleId(), chairperson.getUserId(), dac.getDacId());

    // public dataset with study (visible)
    Dataset ds1 = insertDataset();
    Study s1 = insertStudyWithProperties();

    datasetDAO.updateStudyId(ds1.getDataSetId(), s1.getStudyId());
    datasetDAO.updateDatasetApproval(true, Instant.now(), chairperson.getUserId(),
        ds1.getDataSetId());

    // public dataset no study (visible)
    Dataset ds2 = insertDataset();
    datasetDAO.updateDatasetApproval(true, Instant.now(), chairperson.getUserId(),
        ds2.getDataSetId());

    // private dataset with study (should not be visible)
    Dataset privateDataset = insertDataset();
    Study privateStudy = insertPrivateStudyWithProperties();

    datasetDAO.updateStudyId(privateDataset.getDataSetId(), privateStudy.getStudyId());
    datasetDAO.updateDatasetApproval(true, Instant.now(), chairperson.getUserId(),
        privateDataset.getDataSetId());

    // not approved (not visible)
    insertDataset();

    // private dataset with the same dac as chairperson (visible)
    Dataset ds3 = insertDataset();
    Study s3 = insertPrivateStudyWithProperties();

    datasetDAO.updateStudyId(ds3.getDataSetId(), s3.getStudyId());
    datasetDAO.updateDatasetDacId(ds3.getDataSetId(), dac.getDacId());

    List<Dataset> datasets = datasetDAO.findDatasetsForChairperson(List.of(dac.getDacId()));

    assertEquals(3, datasets.size());
    assertTrue(
        datasets
            .stream()
            .map(Dataset::getDataSetId).toList()
            .containsAll(
                List.of(
                    ds1.getDataSetId(),
                    ds2.getDataSetId(),
                    ds3.getDataSetId())));
  }

  @Test
  public void testFindDatasetByIdWithDacAndConsentNotDeletable() {
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
  public void testTranslatedDataUse() {
    Dataset d1 = insertDataset();

    String tdu = RandomStringUtils.randomAlphabetic(10);
    datasetDAO.updateDatasetTranslatedDataUse(d1.getDataSetId(), tdu);

    d1 = datasetDAO.findDatasetById(d1.getDataSetId());

    assertEquals(tdu, d1.getTranslatedDataUse());
  }

  @Test
  public void testFindDatasetByAlias() {
    Dataset dataset = insertDataset();

    Dataset foundDataset = datasetDAO.findDatasetByAlias(dataset.getAlias());

    assertNotNull(foundDataset);
    assertEquals(dataset.getDataSetId(), foundDataset.getDataSetId());
  }

  @Test
  public void testFindDatasetsByAlias() {
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
  public void testGetNIHInstitutionalFile() {
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
  public void testGetNIHInstitutionalFile_AlwaysLatestUpdated() throws InterruptedException {
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
  public void testGetNIHInstitutionalFile_AlwaysLatestCreated() throws InterruptedException {
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
  public void testGetNIHInstitutionalFile_NotDeleted() {
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
  public void testGetDictionaryTerms() {
    List<Dictionary> terms = datasetDAO.getDictionaryTerms();
    assertFalse(terms.isEmpty());
    terms.forEach(t -> {
      assertNotNull(t.getKeyId());
      assertNotNull(t.getKey());
    });
  }

  @Test
  public void testFindDatasetsByIdList() {
    Dataset dataset = insertDataset();
    Dac dac = insertDac();
    Consent consent = insertConsent();
    datasetDAO.updateDatasetDacId(dataset.getDataSetId(), dac.getDacId());
    consentDAO.insertConsentAssociation(consent.getConsentId(), ASSOCIATION_TYPE_TEST,
        dataset.getDataSetId());

    List<Dataset> datasets = datasetDAO.findDatasetsByIdList(List.of(dataset.getDataSetId()));
    assertFalse(datasets.isEmpty());
    assertEquals(1, datasets.size());
    assertEquals(dac.getDacId(), datasets.get(0).getDacId());
    assertEquals(consent.getConsentId(), datasets.get(0).getConsentId());
    assertEquals(consent.getTranslatedUseRestriction(),
        datasets.get(0).getTranslatedUseRestriction());
    assertFalse(datasets.get(0).getProperties().isEmpty());
    assertNotNull(datasets.get(0).getCreateUser());
  }

  // User -> UserRoles -> DACs -> Consents -> Consent Associations -> DataSets
  @Test
  public void testFindDataSetsByAuthUserEmail() {
    Dataset dataset = insertDataset();
    Dac dac = insertDac();
    Consent consent = insertConsent();
    datasetDAO.updateDatasetDacId(dataset.getDataSetId(), dac.getDacId());
    consentDAO.insertConsentAssociation(consent.getConsentId(), ASSOCIATION_TYPE_TEST,
        dataset.getDataSetId());
    User user = createUser();
    createUserRole(UserRoles.CHAIRPERSON.getRoleId(), user.getUserId(), dac.getDacId());

    List<Dataset> datasets = datasetDAO.findDatasetsByAuthUserEmail(user.getEmail());
    assertFalse(datasets.isEmpty());
    List<Integer> datasetIds = datasets.stream().map(Dataset::getDataSetId).toList();
    assertTrue(datasetIds.contains(dataset.getDataSetId()));
  }

  @Test
  public void testFindDatasetPropertiesByDatasetId() {
    Dataset d = insertDataset();
    Set<DatasetProperty> properties = datasetDAO.findDatasetPropertiesByDatasetId(d.getDataSetId());
    assertEquals(properties.size(), 1);
  }

  @Test
  public void testUpdateDataset() {
    Dataset d = insertDataset();
    Dac dac = insertDac();
    String name = RandomStringUtils.random(20, true, true);
    Timestamp now = new Timestamp(new Date().getTime());
    Integer userId = RandomUtils.nextInt(1, 1000);

    datasetDAO.updateDataset(d.getDataSetId(), name, now, userId, true, dac.getDacId());
    Dataset updated = datasetDAO.findDatasetById(d.getDataSetId());

    assertEquals(name, updated.getName());
    assertEquals(now, updated.getUpdateDate());
    assertEquals(userId, updated.getUpdateUserId());
    assertEquals(dac.getDacId(), updated.getDacId());
  }

  @Test
  public void testUpdateDatasetProperty() {
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
  public void testCreateNumberTypedDatasetProperty() {
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
  public void testCreateDateTypedDatasetProperty() {
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
  public void testCreateBooleanTypedDatasetProperty() {
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
  public void testCreateJsonTypedDatasetProperty() {
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
  public void testCreateStringTypedDatasetProperty() {
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
  public void testCreateTypedDatasetPropertyWithSchema() {
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
  public void testDeleteDatasetPropertyByKey() {
    Dataset d = insertDataset();
    Set<DatasetProperty> properties = datasetDAO.findDatasetPropertiesByDatasetId(d.getDataSetId());
    DatasetProperty propertyToDelete = properties.stream().toList().get(0);
    datasetDAO.deleteDatasetPropertyByKey(d.getDataSetId(), propertyToDelete.getPropertyKey());
    Set<DatasetProperty> returnedProperties = datasetDAO.findDatasetPropertiesByDatasetId(
        d.getDataSetId());
    assertNotEquals(properties.size(), returnedProperties.size());
  }

  @Test
  public void testFindAllDatasets() {
    List<Dataset> datasetList = IntStream.range(1, 5).mapToObj(i -> {
      Dataset dataset = insertDataset();
      Consent consent = insertConsent();
      consentDAO.insertConsentAssociation(consent.getConsentId(), ASSOCIATION_TYPE_TEST,
          dataset.getDataSetId());
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
  public void testFindAllDatasetDTOs() {
    Dataset dataset = insertDataset();
    Consent consent = insertConsent();
    consentDAO.insertConsentAssociation(consent.getConsentId(), ASSOCIATION_TYPE_TEST,
        dataset.getDataSetId());

    Set<DatasetDTO> datasets = datasetDAO.findAllDatasetDTOs();
    assertFalse(datasets.isEmpty());
    List<Integer> datasetIds = datasets.stream().map(DatasetDTO::getDataSetId).toList();
    assertTrue(datasetIds.contains(dataset.getDataSetId()));
  }

  @Test
  public void testFindActiveDatasets() {
    Dataset dataset = insertDataset();
    Consent consent = insertConsent();

    consentDAO.insertConsentAssociation(consent.getConsentId(), ASSOCIATION_TYPE_TEST,
        dataset.getDataSetId());

    Set<DatasetDTO> datasets = datasetDAO.findActiveDatasetDTOs();
    assertFalse(datasets.isEmpty());
    List<Integer> datasetIds = datasets.stream().map(DatasetDTO::getDataSetId).toList();
    assertTrue(datasetIds.contains(dataset.getDataSetId()));
  }

  @Test
  public void testFindAllStudyNames() {
    Dataset ds1 = insertDataset();
    String ds1Name = RandomStringUtils.randomAlphabetic(20);
    createDatasetProperty(ds1.getDataSetId(), "studyName", ds1Name, PropertyType.String);

    Dataset ds2 = insertDataset();
    String ds2Name = RandomStringUtils.randomAlphabetic(25);
    createDatasetProperty(ds2.getDataSetId(), "studyName", ds2Name, PropertyType.String);

    Dataset ds3 = insertDataset();
    String ds3Name = RandomStringUtils.randomAlphabetic(15);
    createDatasetProperty(ds3.getDataSetId(), "studyName", ds3Name, PropertyType.String);

    Set<String> returned = datasetDAO.findAllStudyNames();

    assertEquals(3, returned.size());
    assertTrue(returned.containsAll(Set.of(ds1Name, ds2Name, ds3Name)));
  }

  @Test
  public void testFindAllStudyNamesWithInactiveDataset() {
    Dataset ds1 = insertDataset();
    String ds1Name = RandomStringUtils.randomAlphabetic(20);
    createDatasetProperty(ds1.getDataSetId(), "studyName", ds1Name, PropertyType.String);

    Dataset ds2 = insertDataset();
    String ds2Name = RandomStringUtils.randomAlphabetic(20);
    createDatasetProperty(ds2.getDataSetId(), "studyName", ds2Name, PropertyType.String);

    datasetDAO.updateDatasetActive(ds1.getDataSetId(), false);

    Set<String> returned = datasetDAO.findAllStudyNames();
    assertEquals(2, returned.size());
    assertTrue(returned.contains(ds2Name));
  }

  @Test
  public void testFindDatasetsByUser() {
    Dataset dataset = insertDataset();
    Dac dac = insertDac();
    Consent consent = insertConsent();
    datasetDAO.updateDatasetDacId(dataset.getDataSetId(), dac.getDacId());
    consentDAO.insertConsentAssociation(consent.getConsentId(), ASSOCIATION_TYPE_TEST,
        dataset.getDataSetId());
    User user = createUser();
    createUserRole(UserRoles.CHAIRPERSON.getRoleId(), user.getUserId(), dac.getDacId());

    Set<DatasetDTO> datasets = datasetDAO.findDatasetDTOsByUserId(user.getUserId());
    assertFalse(datasets.isEmpty());
    List<Integer> datasetIds = datasets.stream().map(DatasetDTO::getDataSetId).toList();
    assertTrue(datasetIds.contains(dataset.getDataSetId()));
  }

  @Test
  public void testFindDatasetsByDacIds() {
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
  public void testFindDatasetListByDacIds() {
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
  public void testUpdateDatasetDataUse() {
    Dataset dataset = insertDataset();
    DataUse oldDataUse = dataset.getDataUse();
    DataUse newDataUse = new DataUseBuilder()
        .setGeneralUse(false)
        .setCommercialUse(true)
        .setHmbResearch(true)
        .setDiseaseRestrictions(List.of("DOID_1"))
        .build();

    datasetDAO.updateDatasetDataUse(dataset.getDataSetId(), newDataUse.toString());
    Dataset updated = datasetDAO.findDatasetById(dataset.getDataSetId());
    assertEquals(newDataUse, updated.getDataUse());
    assertNotEquals(oldDataUse, updated.getDataUse());
  }

  @Test
  public void testFindDatasetWithDataUseByIdList() {
    Dataset dataset = insertDataset();
    Dac dac = insertDac();
    Consent consent = insertConsent();
    datasetDAO.updateDatasetDacId(dataset.getDataSetId(), dac.getDacId());
    consentDAO.insertConsentAssociation(consent.getConsentId(), ASSOCIATION_TYPE_TEST,
        dataset.getDataSetId());

    Set<Dataset> datasets = datasetDAO.findDatasetWithDataUseByIdList(
        Collections.singletonList(dataset.getDataSetId()));
    assertFalse(datasets.isEmpty());
    List<Integer> datasetIds = datasets.stream().map(Dataset::getDataSetId).toList();
    assertTrue(datasetIds.contains(dataset.getDataSetId()));
  }

  @Test
  public void testGetDatasetsForConsent() {
    Integer datasetId = datasetDAO.insertDataset(RandomStringUtils.randomAlphabetic(10), null,
        null, RandomStringUtils.randomAlphabetic(10), true, null, null);
    //negative record, make sure this isn't pulled in
    datasetDAO.insertDataset(RandomStringUtils.randomAlphabetic(10), null, null,
        RandomStringUtils.randomAlphabetic(10), true, null, null);
    String consentId = RandomStringUtils.randomAlphabetic(10);
    consentDAO.insertConsent(consentId, false, "", null,
        RandomStringUtils.randomAlphabetic(10), RandomStringUtils.randomAlphabetic(10), new Date(),
        new Date(),
        null, RandomStringUtils.randomAlphabetic(10));
    consentDAO.insertConsentAssociation(consentId, RandomStringUtils.randomAlphabetic(10),
        datasetId);

    List<Dataset> datasets = datasetDAO.getDatasetsForConsent(consentId);
    assertEquals(1, datasets.size());
    Dataset targetDataset = datasets.get(0);
    assertEquals(datasetId, targetDataset.getDataSetId());
    assertNull(targetDataset.getDacApproval());
  }

  @Test
  public void testUpdateDatasetApproval() {
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
  public void testInsertDatasetAudit() {
    Dataset d = createDataset();
    DatasetAudit audit = new DatasetAudit(
        d.getDataSetId(),
        "objectid",
        "name",
        new Date(),
        false, d.getCreateUserId(),
        "action");
    datasetDAO.insertDatasetAudit(audit);
  }

  @Test
  public void testDatasetWithStudy() {
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
  public void testGetApprovedDatasets() throws Exception {

    // user with approved and unapproved datasets
    User user = createUser();

    Dataset dataset1 = createDataset(false);
    Dataset dataset2 = createDataset(true);
    Dataset dataset3 = createDataset(false);
    Dataset dataset4 = createDataset(true);

    Timestamp timestamp = new Timestamp(new Date().getTime());

    Dac dac1 = insertDac();
    datasetDAO.updateDataset(dataset1.getDataSetId(), dataset1.getDatasetName(), timestamp,
        user.getUserId(), false, dac1.getDacId());
    datasetDAO.updateDataset(dataset2.getDataSetId(), dataset2.getDatasetName(), timestamp,
        user.getUserId(), false, dac1.getDacId());

    Dac dac2 = insertDac();
    datasetDAO.updateDataset(dataset3.getDataSetId(), dataset3.getDatasetName(), timestamp,
        user.getUserId(), false, dac2.getDacId());
    datasetDAO.updateDataset(dataset4.getDataSetId(), dataset4.getDatasetName(), timestamp,
        user.getUserId(), false, dac2.getDacId());

    DarCollection dar1 = createDarCollectionWithDatasets(dac1.getDacId(), user, List.of(dataset1));
    DarCollection dar2 = createDarCollectionWithDatasets(dac2.getDacId(), user, List.of(dataset2, dataset3));
    DarCollection dar3 = createDarCollectionWithDatasets(dac2.getDacId(), user, List.of(dataset4));

    String firstKey1 = dar1.getDars().keySet().stream().findFirst().get();
    String firstKey2 = dar2.getDars().keySet().stream().findFirst().get();
    String firstKey3 = dar3.getDars().keySet().stream().findFirst().get();

    createDataAccessElectionWithVotes(firstKey1, dataset1.getDataSetId(), user.getUserId(), false);
    createDataAccessElectionWithVotes(firstKey2, dataset2.getDataSetId(), user.getUserId(), false);
    Election election3 = createDataAccessElectionWithVotes(firstKey2, dataset3.getDataSetId(), user.getUserId(), true);
    Election election4 = createDataAccessElectionWithVotes(firstKey3, dataset4.getDataSetId(), user.getUserId(), true);

    List<ApprovedDataset> approvedDatasets = datasetDAO.getApprovedDatasets(user.getUserId());
    assertNotNull(approvedDatasets);

    // checks that all datasets in the result are approved
    approvedDatasets.forEach(approvedDataset -> {
      assertTrue(datasetDAO.findDatasetById(Integer.parseInt(approvedDataset.getDatasetIdentifier())).getDacApproval());
    });

    ApprovedDataset expectedApprovedDataset1 = new ApprovedDataset(dataset3.getAlias(), dar2.getDarCode(), dataset3.getDatasetName(), dac2.getName(), election3.getFinalVoteDate());
    ApprovedDataset expectedApprovedDataset2 = new ApprovedDataset(dataset4.getAlias(), dar3.getDarCode(), dataset4.getDatasetName(), dac2.getName(), election4.getFinalVoteDate());
    List<ApprovedDataset> expected = List.of(expectedApprovedDataset1, expectedApprovedDataset2);

    // checks that the expected result list size and contents match the observed result
    assertEquals(approvedDatasets.size(), expected.size());
    IntStream.range(0, approvedDatasets.size()).forEach(index -> {
      ApprovedDataset dataset = approvedDatasets.get(index);
      ApprovedDataset expectedDataset = expected.get(index);
      assertTrue(isApprovedDatasetEqual(dataset, expectedDataset));
    });


    // reducer works properly
    // mapper works properly
    // check that the expected is equal to the actual
    // check that all datasets in result are approved

    // election needs to be closed automatically when the final vote is cast: election status = true
    // dacApproval should automatically change when the final election vote is true
    // is it good practice to leave it to the user?

  }

  @Test
  public void testGetApprovedDatasetsWhenNone() throws Exception {

    // user with unapproved datasets but no approved datasets
    User user = createUser();

    Dataset dataset1 = createDataset(false);
    Dataset dataset2 = createDataset(true);

    Timestamp timestamp = new Timestamp(new Date().getTime());

    Dac dac1 = insertDac();
    datasetDAO.updateDataset(dataset1.getDataSetId(), dataset1.getDatasetName(), timestamp,
        user.getUserId(), false, dac1.getDacId());
    datasetDAO.updateDataset(dataset2.getDataSetId(), dataset2.getDatasetName(), timestamp,
        user.getUserId(), false, dac1.getDacId());

    DarCollection dar1 = createDarCollectionWithDatasets(dac1.getDacId(), user, List.of(dataset1, dataset2));

    String firstKey1 = dar1.getDars().keySet().stream().findFirst().get();

    createDataAccessElectionWithVotes(firstKey1, dataset1.getDataSetId(), user.getUserId(), false);
    createDataAccessElectionWithVotes(firstKey1, dataset2.getDataSetId(), user.getUserId(), false);

    List<ApprovedDataset> approvedDatasets = datasetDAO.getApprovedDatasets(user.getUserId());
    assertTrue(approvedDatasets.size() == 0);
  }

  @Test
  public void testGetApprovedDatasetsWhenEmpty() throws Exception {

    // user with no datasets
    User user = createUser();
    List<ApprovedDataset> approvedDatasets = datasetDAO.getApprovedDatasets(user.getUserId());
    assertTrue(approvedDatasets.size() == 0);

  }

  private Boolean isApprovedDatasetEqual(ApprovedDataset a, ApprovedDataset b) {
    return a.getAlias() == b.getAlias()
        && a.getDatasetName().equals(b.getDatasetName())
        && a.getDatasetIdentifier() == b.getDatasetIdentifier()
        && a.getDarCode().equals(b.getDarCode())
        && a.getDacName().equals(b.getDacName())
        && (a.getApprovalDate().compareTo(b.getApprovalDate()) == 0);
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
    Integer id = datasetDAO.insertDataset(name, now, user.getUserId(), objectId, true,
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

  protected Consent insertConsent() {
    String consentId = UUID.randomUUID().toString();
    consentDAO.insertConsent(consentId,
        false,
        """
            {"generalUse":true}""",
        "dul",
        consentId,
        "dulName",
        new Date(),
        new Date(),
        "Everything",
        "Group");
    return consentDAO.findConsentById(consentId);
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
    Integer id = datasetDAO.insertDataset(name, now, user.getUserId(), objectId, false,
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
    Integer id = datasetDAO.insertDataset(name, now, user.getUserId(), objectId, false,
        dataUse.toString(), null);
    datasetDAO.updateDatasetApproval(dacApproval, instant, user.getUserId(), id);
    createDatasetProperties(id);
    return datasetDAO.findDatasetById(id);
  }

  private Election createDataAccessElectionWithVotes(String referenceId, Integer datasetId, Integer userId, boolean approval) {
    Integer electionId = electionDAO.insertElection(
        ElectionType.DATA_ACCESS.getValue(),
        ElectionStatus.OPEN.getValue(),
        new Date(),
        referenceId,
        datasetId
    );
    Integer voteId = voteDAO.insertVote(userId, electionId, VoteType.FINAL.getValue());
    voteDAO.updateVote(approval, "rationale", new Date(), voteId, false, electionId, new Date(), false);
    electionDAO.updateElectionById(electionId, ElectionStatus.CLOSED.getValue(), new Date());
    if (approval) {
      datasetDAO.findDatasetById(datasetId).setDacApproval(true);
    }
    return electionDAO.findElectionById(electionId);
  }


}
