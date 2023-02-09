package org.broadinstitute.consent.http.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

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
import org.apache.commons.lang3.tuple.Pair;
import org.broadinstitute.consent.http.enumeration.DatasetPropertyType;
import org.broadinstitute.consent.http.enumeration.FileCategory;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DarCollection;
import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.models.DataUseBuilder;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.DatasetProperty;
import org.broadinstitute.consent.http.models.Dictionary;
import org.broadinstitute.consent.http.models.FileStorageObject;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.dto.DatasetDTO;
import org.junit.Test;

public class DatasetDAOTest extends DAOTestHelper {

    @Test
    public void testFindDatasetByIdWithDacAndConsent() {
        Dataset dataset = insertDataset();
        Dac dac = insertDac();
        Consent consent = insertConsent();
        datasetDAO.updateDatasetDacId(dataset.getDataSetId(), dac.getDacId());
        consentDAO.insertConsentAssociation(consent.getConsentId(), ASSOCIATION_TYPE_TEST, dataset.getDataSetId());

        Dataset foundDataset = datasetDAO.findDatasetById(dataset.getDataSetId());
        assertNotNull(foundDataset);
        assertEquals(dac.getDacId(), foundDataset.getDacId());
        assertEquals(consent.getConsentId(), foundDataset.getConsentId());
        assertEquals(consent.getTranslatedUseRestriction(), foundDataset.getTranslatedUseRestriction());
        assertFalse(foundDataset.getProperties().isEmpty());
        assertTrue(foundDataset.getDeletable());
        assertNotNull(foundDataset.getCreateUser());
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

        List<Dataset> foundDatasets = datasetDAO.findDatasetsByAlias(List.of(dataset1.getAlias(), dataset2.getAlias()));
        List<Integer> foundDatasetIds = foundDatasets.stream().map(Dataset::getDataSetId).toList();
        assertNotNull(foundDatasets);
        assertTrue(foundDatasetIds.containsAll(List.of(dataset1.getDataSetId(), dataset2.getDataSetId())));
    }

    @Test
    public void testFindNeedsApprovalDataSetByDataSetId() {
        Dataset dataset = insertDataset();
        datasetDAO.updateDatasetNeedsApproval(dataset.getDataSetId(), true);
        Dac dac = insertDac();
        Consent consent = insertConsent();
        datasetDAO.updateDatasetDacId(dataset.getDataSetId(), dac.getDacId());
        consentDAO.insertConsentAssociation(consent.getConsentId(), ASSOCIATION_TYPE_TEST, dataset.getDataSetId());

        List<Dataset> datasets = datasetDAO.findNeedsApprovalDatasetByDatasetId(List.of(dataset.getDataSetId()));
        assertFalse(datasets.isEmpty());
        assertEquals(1, datasets.size());
        assertEquals(dac.getDacId(), datasets.get(0).getDacId());
        assertEquals(consent.getConsentId(), datasets.get(0).getConsentId());
        assertEquals(consent.getTranslatedUseRestriction(), datasets.get(0).getTranslatedUseRestriction());
        assertFalse(datasets.get(0).getProperties().isEmpty());
        assertTrue(datasets.get(0).getNeedsApproval());
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
        assertEquals(nihFile.getBlobId(), found.getNihInstitutionalCertificationFile().getBlobId());
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
        assertEquals(nihFileIdCreatedFirstUpdatedSecond, found.getNihInstitutionalCertificationFile().getFileStorageObjectId());
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
        assertEquals(nihFileIdCreatedSecond, found.getNihInstitutionalCertificationFile().getFileStorageObjectId());
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
    public void testGetAlternativeDataSharingFile() {
        Dataset dataset = insertDataset();

        // create unrelated file with the same id as dataset id but different category, timestamp before
        createFileStorageObject(
                dataset.getDataSetId().toString(),
                FileCategory.NIH_INSTITUTIONAL_CERTIFICATION
        );

        FileStorageObject altFile = createFileStorageObject(
                dataset.getDataSetId().toString(),
                FileCategory.ALTERNATIVE_DATA_SHARING_PLAN
        );

        // create unrelated files with timestamp later than the ADSP file: one attached to dataset, one
        // completely separate from the dataset. ensures that the Mapper is selecting only the right file.
        createFileStorageObject();
        createFileStorageObject(
                dataset.getDataSetId().toString(),
                FileCategory.DATA_USE_LETTER
        );

        Dataset found = datasetDAO.findDatasetById(dataset.getDataSetId());

        assertEquals(altFile, found.getAlternativeDataSharingPlanFile());
        assertEquals(altFile.getBlobId(), found.getAlternativeDataSharingPlanFile().getBlobId());
    }

    @Test
    public void testGetAlternativeDataSharingPlanFile_AlwaysLatestCreated() throws InterruptedException {
        Dataset dataset = insertDataset();

        String fileName = RandomStringUtils.randomAlphabetic(10);
        String bucketName = RandomStringUtils.randomAlphabetic(10);
        String gcsFileUri = RandomStringUtils.randomAlphabetic(10);
        User createUser = createUser();

        Integer altFileIdCreatedFirst = fileStorageObjectDAO.insertNewFile(
                fileName,
                FileCategory.ALTERNATIVE_DATA_SHARING_PLAN.getValue(),
                bucketName,
                gcsFileUri,
                dataset.getDataSetId().toString(),
                createUser.getUserId(),
                Instant.ofEpochMilli(100)
        );

        User updateUser = createUser();

        fileStorageObjectDAO.updateFileById(
                altFileIdCreatedFirst,
                RandomStringUtils.randomAlphabetic(20),
                RandomStringUtils.randomAlphabetic(20),
                updateUser.getUserId(),
                Instant.ofEpochMilli(120));

        Integer altFileIdCreatedSecond = fileStorageObjectDAO.insertNewFile(
                fileName,
                FileCategory.ALTERNATIVE_DATA_SHARING_PLAN.getValue(),
                bucketName,
                gcsFileUri,
                dataset.getDataSetId().toString(),
                createUser.getUserId(),
                Instant.ofEpochMilli(130)
        );


        Dataset found = datasetDAO.findDatasetById(dataset.getDataSetId());

        // returns last updated file
        assertEquals(altFileIdCreatedSecond, found.getAlternativeDataSharingPlanFile().getFileStorageObjectId());
    }

    @Test
    public void testGetAlternativeDataSharingPlanFile_NotDeleted() {
        Dataset dataset = insertDataset();

        FileStorageObject altFile = createFileStorageObject(
                dataset.getDataSetId().toString(),
                FileCategory.ALTERNATIVE_DATA_SHARING_PLAN
        );

        User deleteUser = createUser();

        fileStorageObjectDAO.deleteFileById(
                altFile.getFileStorageObjectId(),
                deleteUser.getUserId(),
                Instant.now()
        );

        Dataset found = datasetDAO.findDatasetById(dataset.getDataSetId());

        assertNull(found.getAlternativeDataSharingPlanFile());
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
        consentDAO.insertConsentAssociation(consent.getConsentId(), ASSOCIATION_TYPE_TEST, dataset.getDataSetId());

        List<Dataset> datasets = datasetDAO.findDatasetsByIdList(List.of(dataset.getDataSetId()));
        assertFalse(datasets.isEmpty());
        assertEquals(1, datasets.size());
        assertEquals(dac.getDacId(), datasets.get(0).getDacId());
        assertEquals(consent.getConsentId(), datasets.get(0).getConsentId());
        assertEquals(consent.getTranslatedUseRestriction(), datasets.get(0).getTranslatedUseRestriction());
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
        consentDAO.insertConsentAssociation(consent.getConsentId(), ASSOCIATION_TYPE_TEST, dataset.getDataSetId());
        User user = createUser();
        createUserRole(UserRoles.CHAIRPERSON.getRoleId(), user.getUserId(), dac.getDacId());

        List<Dataset> datasets = datasetDAO.findDatasetsByAuthUserEmail(user.getEmail());
        assertFalse(datasets.isEmpty());
        List<Integer> datasetIds = datasets.stream().map(Dataset::getDataSetId).toList();
        assertTrue(datasetIds.contains(dataset.getDataSetId()));
    }

    @Test
    public void testFindNonDACDataSets() {
        Dataset dataset = insertDataset();
        Consent consent = insertConsent();
        consentDAO.insertConsentAssociation(consent.getConsentId(), ASSOCIATION_TYPE_TEST, dataset.getDataSetId());

        List<Dataset> datasets = datasetDAO.findNonDACDatasets();
        assertFalse(datasets.isEmpty());
        List<Integer> datasetIds = datasets.stream().map(Dataset::getDataSetId).toList();
        assertTrue(datasetIds.contains(dataset.getDataSetId()));
        // adding this here to ensure mapper does not return a false in place of a null for dacApproval
        datasets.forEach(d -> {
            assertTrue(d.getDacApproval() == null);
        });
    }

    @Test
    public void testFindDatasetAndDacIds() {
        Dataset dataset = insertDataset();
        Dac dac = insertDac();
        Consent consent = insertConsent();
        datasetDAO.updateDatasetDacId(dataset.getDataSetId(), dac.getDacId());
        consentDAO.insertConsentAssociation(consent.getConsentId(), ASSOCIATION_TYPE_TEST, dataset.getDataSetId());

        List<Pair<Integer, Integer>> pairs = datasetDAO.findDatasetAndDacIds();
        assertFalse(pairs.isEmpty());
        assertEquals(1, pairs.size());
        assertEquals(pairs.get(0).getLeft(), dataset.getDataSetId());
        assertEquals(pairs.get(0).getRight(), dac.getDacId());
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
        DatasetProperty newProperty = new DatasetProperty(d.getDataSetId(), 1, "Updated Value", DatasetPropertyType.String, new Date());
        List<DatasetProperty> updatedProperties = new ArrayList<>();
        updatedProperties.add(newProperty);
        datasetDAO.updateDatasetProperty(d.getDataSetId(), updatedProperties.get(0).getPropertyKey(), updatedProperties.get(0).getPropertyValue().toString());
        Set<DatasetProperty> returnedProperties = datasetDAO.findDatasetPropertiesByDatasetId(d.getDataSetId());
        DatasetProperty returnedProperty = returnedProperties.stream().toList().get(0);
        assertEquals(originalProperty.getPropertyKey(), returnedProperty.getPropertyKey());
        assertEquals(originalProperty.getPropertyId(), returnedProperty.getPropertyId());
        assertNotEquals(originalProperty.getPropertyValue(), returnedProperty.getPropertyValue());
    }

    @Test
    public void testCreateNumberTypedDatasetProperty() {
        Dataset d = insertDataset();

        Set<DatasetProperty> oldProperties = datasetDAO.findDatasetPropertiesByDatasetId(d.getDataSetId());
        DatasetProperty propertyToDelete = new ArrayList<>(oldProperties).get(0);
        datasetDAO.deleteDatasetPropertyByKey(d.getDataSetId(), propertyToDelete.getPropertyKey());

        List<DatasetProperty> newProps = List.of(
                new DatasetProperty(
                        d.getDataSetId(),
                        1,
                        "10",
                        DatasetPropertyType.Number,
                        new Date())
        );
        datasetDAO.insertDatasetProperties(newProps);

        Dataset dWithProps = datasetDAO.findDatasetById(d.getDataSetId());

        assertEquals(1, dWithProps.getProperties().size());
        DatasetProperty prop = new ArrayList<>(dWithProps.getProperties()).get(0);
        assertEquals(DatasetPropertyType.Number, prop.getPropertyType());
        assertEquals("10", prop.getPropertyValueAsString());
        assertEquals(10, prop.getPropertyValue());
    }

    @Test
    public void testCreateDateTypedDatasetProperty() {
        Dataset d = insertDataset();
        Instant now = Instant.now();

        Set<DatasetProperty> oldProperties = datasetDAO.findDatasetPropertiesByDatasetId(d.getDataSetId());
        DatasetProperty propertyToDelete = new ArrayList<>(oldProperties).get(0);
        datasetDAO.deleteDatasetPropertyByKey(d.getDataSetId(), propertyToDelete.getPropertyKey());

        List<DatasetProperty> newProps = List.of(
                new DatasetProperty(
                        d.getDataSetId(),
                        1,
                        now.toString(),
                        DatasetPropertyType.Date,
                        new Date())
        );
        datasetDAO.insertDatasetProperties(newProps);

        Dataset dWithProps = datasetDAO.findDatasetById(d.getDataSetId());

        assertEquals(1, dWithProps.getProperties().size());
        DatasetProperty prop = new ArrayList<>(dWithProps.getProperties()).get(0);
        assertEquals(DatasetPropertyType.Date, prop.getPropertyType());
        assertEquals(now.toString(), prop.getPropertyValueAsString());
        assertEquals(now, prop.getPropertyValue());
    }

    @Test
    public void testCreateBooleanTypedDatasetProperty() {
        Dataset d = insertDataset();
        Boolean bool = Boolean.FALSE;

        Set<DatasetProperty> oldProperties = datasetDAO.findDatasetPropertiesByDatasetId(d.getDataSetId());
        DatasetProperty propertyToDelete = new ArrayList<>(oldProperties).get(0);
        datasetDAO.deleteDatasetPropertyByKey(d.getDataSetId(), propertyToDelete.getPropertyKey());

        List<DatasetProperty> newProps = List.of(
                new DatasetProperty(
                        d.getDataSetId(),
                        1,
                        bool.toString(),
                        DatasetPropertyType.Boolean,
                        new Date())
        );
        datasetDAO.insertDatasetProperties(newProps);

        Dataset dWithProps = datasetDAO.findDatasetById(d.getDataSetId());

        assertEquals(1, dWithProps.getProperties().size());
        DatasetProperty prop = new ArrayList<>(dWithProps.getProperties()).get(0);
        assertEquals(DatasetPropertyType.Boolean, prop.getPropertyType());
        assertEquals(bool.toString(), prop.getPropertyValueAsString());
        assertEquals(Boolean.FALSE, prop.getPropertyValue());
    }

    @Test
    public void testCreateJsonTypedDatasetProperty() {
        Dataset d = insertDataset();
        JsonObject jsonObject = new JsonObject();
        jsonObject.add("test", new JsonObject());

        Set<DatasetProperty> oldProperties = datasetDAO.findDatasetPropertiesByDatasetId(d.getDataSetId());
        DatasetProperty propertyToDelete = new ArrayList<>(oldProperties).get(0);
        datasetDAO.deleteDatasetPropertyByKey(d.getDataSetId(), propertyToDelete.getPropertyKey());

        List<DatasetProperty> newProps = List.of(
                new DatasetProperty(
                        d.getDataSetId(),
                        1,
                        jsonObject.toString(),
                        DatasetPropertyType.Json,
                        new Date())
        );
        datasetDAO.insertDatasetProperties(newProps);

        Dataset dWithProps = datasetDAO.findDatasetById(d.getDataSetId());

        assertEquals(1, dWithProps.getProperties().size());
        DatasetProperty prop = new ArrayList<>(dWithProps.getProperties()).get(0);
        assertEquals(DatasetPropertyType.Json, prop.getPropertyType());
        assertEquals(jsonObject.toString(), prop.getPropertyValueAsString());
        assertEquals(jsonObject, prop.getPropertyValue());
    }

    @Test
    public void testCreateStringTypedDatasetProperty() {
        Dataset d = insertDataset();
        String value = "hi";

        Set<DatasetProperty> oldProperties = datasetDAO.findDatasetPropertiesByDatasetId(d.getDataSetId());
        DatasetProperty propertyToDelete = new ArrayList<>(oldProperties).get(0);
        datasetDAO.deleteDatasetPropertyByKey(d.getDataSetId(), propertyToDelete.getPropertyKey());

        List<DatasetProperty> newProps = List.of(
                new DatasetProperty(
                        d.getDataSetId(),
                        1,
                        value,
                        DatasetPropertyType.String,
                        new Date())
        );
        datasetDAO.insertDatasetProperties(newProps);

        Dataset dWithProps = datasetDAO.findDatasetById(d.getDataSetId());

        assertEquals(1, dWithProps.getProperties().size());
        DatasetProperty prop = new ArrayList<>(dWithProps.getProperties()).get(0);
        assertEquals(DatasetPropertyType.String, prop.getPropertyType());
        assertEquals(value, prop.getPropertyValueAsString());
        assertEquals(value, prop.getPropertyValue());
    }

    @Test
    public void testCreateTypedDatasetPropertyWithSchema() {
        Dataset d = insertDataset();
        String schemaValue = "test test test test";

        Set<DatasetProperty> oldProperties = datasetDAO.findDatasetPropertiesByDatasetId(d.getDataSetId());
        DatasetProperty propertyToDelete = new ArrayList<>(oldProperties).get(0);
        datasetDAO.deleteDatasetPropertyByKey(d.getDataSetId(), propertyToDelete.getPropertyKey());

        List<DatasetProperty> newProps = List.of(
                new DatasetProperty(
                        d.getDataSetId(),
                        1,
                        schemaValue,
                        "asdf",
                        DatasetPropertyType.String,
                        new Date())
        );
        datasetDAO.insertDatasetProperties(newProps);

        Dataset dWithProps = datasetDAO.findDatasetById(d.getDataSetId());

        assertEquals(1, dWithProps.getProperties().size());
        DatasetProperty prop = new ArrayList<>(dWithProps.getProperties()).get(0);
        assertEquals(DatasetPropertyType.String, prop.getPropertyType());
        assertEquals(schemaValue, prop.getSchemaProperty());
    }

    @Test
    public void testDeleteDatasetPropertyByKey() {
        Dataset d = insertDataset();
        Set<DatasetProperty> properties = datasetDAO.findDatasetPropertiesByDatasetId(d.getDataSetId());
        DatasetProperty propertyToDelete = properties.stream().toList().get(0);
        datasetDAO.deleteDatasetPropertyByKey(d.getDataSetId(), propertyToDelete.getPropertyKey());
        Set<DatasetProperty> returnedProperties = datasetDAO.findDatasetPropertiesByDatasetId(d.getDataSetId());
        assertNotEquals(properties.size(), returnedProperties.size());
    }

    @Test
    public void testFindAllDatasets() {
        List<Dataset> datasetList = IntStream.range(1, 5).mapToObj(i -> {
            Dataset dataset = insertDataset();
            Consent consent = insertConsent();
            consentDAO.insertConsentAssociation(consent.getConsentId(), ASSOCIATION_TYPE_TEST, dataset.getDataSetId());
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
        consentDAO.insertConsentAssociation(consent.getConsentId(), ASSOCIATION_TYPE_TEST, dataset.getDataSetId());

        Set<DatasetDTO> datasets = datasetDAO.findAllDatasetDTOs();
        assertFalse(datasets.isEmpty());
        List<Integer> datasetIds = datasets.stream().map(DatasetDTO::getDataSetId).toList();
        assertTrue(datasetIds.contains(dataset.getDataSetId()));
    }

    @Test
    public void testFindActiveDatasets() {
        Dataset dataset = insertDataset();
        Consent consent = insertConsent();

        consentDAO.insertConsentAssociation(consent.getConsentId(), ASSOCIATION_TYPE_TEST, dataset.getDataSetId());

        Set<DatasetDTO> datasets = datasetDAO.findActiveDatasetDTOs();
        assertFalse(datasets.isEmpty());
        List<Integer> datasetIds = datasets.stream().map(DatasetDTO::getDataSetId).toList();
        assertTrue(datasetIds.contains(dataset.getDataSetId()));
    }

    @Test
    public void testFindDatasetsByUser() {
        Dataset dataset = insertDataset();
        Dac dac = insertDac();
        Consent consent = insertConsent();
        datasetDAO.updateDatasetDacId(dataset.getDataSetId(), dac.getDacId());
        consentDAO.insertConsentAssociation(consent.getConsentId(), ASSOCIATION_TYPE_TEST, dataset.getDataSetId());
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

        createConsentAndAssociationWithDatasetId(dataset.getDataSetId());
        createConsentAndAssociationWithDatasetId(datasetTwo.getDataSetId());
        List<Integer> datasetIds = List.of(dataset.getDataSetId(), datasetTwo.getDataSetId());
        Set<DatasetDTO> datasets = datasetDAO.findDatasetsByDacIds(List.of(dac.getDacId(), dacTwo.getDacId()));
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

        createConsentAndAssociationWithDatasetId(dataset.getDataSetId());
        createConsentAndAssociationWithDatasetId(datasetTwo.getDataSetId());
        List<Integer> datasetIds = List.of(dataset.getDataSetId(), datasetTwo.getDataSetId());
        List<Dataset> datasets = datasetDAO.findDatasetListByDacIds(List.of(dac.getDacId(), dacTwo.getDacId()));
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
        consentDAO.insertConsentAssociation(consent.getConsentId(), ASSOCIATION_TYPE_TEST, dataset.getDataSetId());

        Set<Dataset> datasets = datasetDAO.findDatasetWithDataUseByIdList(Collections.singletonList(dataset.getDataSetId()));
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
            null, RandomStringUtils.randomAlphabetic(10), null, new Date(), new Date(),
            null, RandomStringUtils.randomAlphabetic(10));
        consentDAO.insertConsentAssociation(consentId, RandomStringUtils.randomAlphabetic(10), datasetId);

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
        datasetDAO.updateDatasetApproval(true, Instant.now(), updateUser.getUserId(), dataset.getDataSetId());
        Dataset updatedDataset = datasetDAO.findDatasetById(dataset.getDataSetId());
        assertNotNull(updatedDataset);
        assertTrue(updatedDataset.getDacApproval());
        datasetDAO.updateDatasetApproval(false, Instant.now(), updateUser.getUserId(), dataset.getDataSetId());
        Dataset updatedDatasetAfterApprovalFalse = datasetDAO.findDatasetById(dataset.getDataSetId());
        assertNotNull(updatedDatasetAfterApprovalFalse);
        assertEquals(dataset.getDataSetId(), updatedDatasetAfterApprovalFalse.getDataSetId());
        assertFalse(updatedDatasetAfterApprovalFalse.getDacApproval());

    }

    private DarCollection createDarCollectionWithDatasets(int dacId, User user, List<Dataset> datasets) {
        String darCode = "DAR-" + RandomUtils.nextInt(1, 999999);
        Integer collectionId = darCollectionDAO.insertDarCollection(darCode, user.getUserId(), new Date());
        IntStream.range(0, datasets.size()).forEach(index -> {
            String darSubCode = darCode + "-A-" + index;
            Dataset dataset = datasets.get(index);
            datasetDAO.updateDatasetDacId(dataset.getDataSetId(), dacId);
            createDataAccessRequestWithDatasetAndCollectionInfo(collectionId, dataset.getDataSetId(), user.getUserId(), darSubCode);
            createConsentAndAssociationWithDatasetId(dataset.getDataSetId());
        });
        return darCollectionDAO.findDARCollectionByCollectionId(collectionId);
    }

    private void createUserRole(Integer roleId, Integer userId, Integer dacId) {
        dacDAO.addDacMember(roleId, userId, dacId);
    }

    private FileStorageObject createFileStorageObject() {
        FileCategory category = List.of(FileCategory.values()).get(new Random().nextInt(FileCategory.values().length));
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
        Integer id = datasetDAO.insertDataset(name, now, user.getUserId(), objectId, true, dataUse.toString(), null);
        createDatasetProperties(id);
        return datasetDAO.findDatasetById(id);
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
                "{\"type\":\"everything\"}",
                "{\"generalUse\": true }",
                "dul",
                consentId,
                "dulName",
                new Date(),
                new Date(),
                "Everything",
                "Group");
        return consentDAO.findConsentById(consentId);
    }

}
