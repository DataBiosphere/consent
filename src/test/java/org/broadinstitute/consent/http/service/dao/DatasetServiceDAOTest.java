package org.broadinstitute.consent.http.service.dao;

import com.google.cloud.storage.BlobId;
import org.apache.commons.lang3.RandomStringUtils;
import org.broadinstitute.consent.http.db.DAOTestHelper;
import org.broadinstitute.consent.http.enumeration.DatasetPropertyType;
import org.broadinstitute.consent.http.enumeration.FileCategory;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.models.DataUseBuilder;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.DatasetProperty;
import org.broadinstitute.consent.http.models.Dictionary;
import org.broadinstitute.consent.http.models.FileStorageObject;
import org.broadinstitute.consent.http.models.User;
import org.junit.Before;
import org.junit.Test;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class DatasetServiceDAOTest extends DAOTestHelper {

    private DatasetServiceDAO serviceDAO;

    @Before
    public void setUp() {
        serviceDAO = new DatasetServiceDAO(jdbi, datasetDAO, fileStorageObjectDAO);
    }

    /*
     * The following DatasetProperty synchronization test cases are covered here:
     *  1. Add new dataset property with an existing dictionary key
     *  2. Add new dataset property without an existing dictionary key
     *  3. Update existing dataset property
     *  4. Add new dataset property with an existing dictionary key + Delete existing property
     *  5. Add new dataset property without an existing dictionary key + Delete existing property
     *  6. Update existing dataset property + Delete existing property
     */

    @Test
    public void testSynchronizeDatasetProperties_case1() throws Exception {
        List<Dictionary> dictionaryTerms = datasetDAO.getDictionaryTerms();
        Dictionary dictionary = dictionaryTerms.get(0);
        Dataset dataset = createSampleDataset();
        // Creating a prop that will be added
        DatasetProperty prop = createUnsavedPropWithKeyName(dataset, dictionary.getKey());
        List<DatasetProperty> synchronizedProps = serviceDAO.synchronizeDatasetProperties(dataset.getDataSetId(), List.of(prop));
        assertEquals(1, synchronizedProps.size());
        assertEquals(dictionary.getKey(), synchronizedProps.get(0).getPropertyName());
    }

    @Test
    public void testSynchronizeDatasetProperties_case2() throws Exception {
        String newPropName = "New Prop Name";
        Dataset dataset = createSampleDataset();
        // Creating a prop that will be added
        DatasetProperty prop = createUnsavedPropWithKeyName(dataset, newPropName);
        List<DatasetProperty> synchronizedProps = serviceDAO.synchronizeDatasetProperties(dataset.getDataSetId(), List.of(prop));
        assertEquals(1, synchronizedProps.size());
        List<String> dictionaryTerms = datasetDAO.getDictionaryTerms().stream().map(Dictionary::getKey).toList();
        assertTrue(dictionaryTerms.contains(newPropName));
    }

    @Test
    public void testSynchronizeDatasetProperties_case3() throws Exception {
        Dataset dataset = createSampleDataset();
        List<Dictionary> dictionaryTerms = datasetDAO.getDictionaryTerms();
        Dictionary dictionary = dictionaryTerms.get(0);
        // Saving a prop that will be updated
        DatasetProperty prop = savePropWithDictionaryTerm(dataset, dictionary);
        String newPropVal = RandomStringUtils.randomAlphabetic(10);
        prop.setPropertyValue(newPropVal);
        List<DatasetProperty> synchronizedProps = serviceDAO.synchronizeDatasetProperties(dataset.getDataSetId(), List.of(prop));
        assertEquals(1, synchronizedProps.size());
        assertEquals(newPropVal, synchronizedProps.get(0).getPropertyValueAsString());
    }

    @Test
    public void testSynchronizeDatasetProperties_case4() throws Exception {
        List<Dictionary> dictionaryTerms = datasetDAO.getDictionaryTerms();
        Dictionary dict1 = dictionaryTerms.get(0);
        Dictionary dict2 = dictionaryTerms.get(1);
        Dataset dataset = createSampleDataset();
        // Saving a prop that will be deleted via synchronization
        savePropWithDictionaryTerm(dataset, dict1);
        // Creating a prop that will be added
        DatasetProperty propToAdd = createUnsavedPropWithKeyName(dataset, dict2.getKey());
        List<DatasetProperty> synchronizedProps = serviceDAO.synchronizeDatasetProperties(dataset.getDataSetId(), List.of(propToAdd));
        assertEquals(1, synchronizedProps.size());
        assertEquals(dict2.getKey(), synchronizedProps.get(0).getPropertyName());
    }

    @Test
    public void testSynchronizeDatasetProperties_case5() throws Exception {
        List<Dictionary> dictionaryTerms = datasetDAO.getDictionaryTerms();
        Dictionary dict1 = dictionaryTerms.get(0);
        String newPropName = "New Prop Name";
        Dataset dataset = createSampleDataset();
        // Saving a prop that will be deleted via synchronization
        savePropWithDictionaryTerm(dataset, dict1);
        // Creating a prop that will be added
        DatasetProperty propToAdd = createUnsavedPropWithKeyName(dataset, newPropName);
        List<DatasetProperty> synchronizedProps = serviceDAO.synchronizeDatasetProperties(dataset.getDataSetId(), List.of(propToAdd));
        assertEquals(1, synchronizedProps.size());
        assertEquals(newPropName, synchronizedProps.get(0).getPropertyName());
    }

    @Test
    public void testSynchronizeDatasetProperties_case6() throws Exception {
        List<Dictionary> dictionaryTerms = datasetDAO.getDictionaryTerms();
        Dictionary dict1 = dictionaryTerms.get(0);
        Dictionary dict2 = dictionaryTerms.get(1);
        Dataset dataset = createSampleDataset();
        // Saving a prop that will be deleted via synchronization
        savePropWithDictionaryTerm(dataset, dict1);
        // Saving a prop that will be updated
        DatasetProperty propToUpdate = savePropWithDictionaryTerm(dataset, dict2);
        String newPropVal = RandomStringUtils.randomAlphabetic(10);
        propToUpdate.setPropertyValue(newPropVal);
        List<DatasetProperty> synchronizedProps = serviceDAO.synchronizeDatasetProperties(dataset.getDataSetId(), List.of(propToUpdate));
        assertEquals(1, synchronizedProps.size());
        assertEquals(dict2.getKey(), synchronizedProps.get(0).getPropertyName());
        assertEquals(newPropVal, synchronizedProps.get(0).getPropertyValueAsString());
    }


    @Test
    public void testInsertDatasets() throws Exception{

        Dac dac = createDac();
        User user = createUser();

        DatasetProperty prop1 = new DatasetProperty();
        prop1.setSchemaProperty(RandomStringUtils.randomAlphabetic(10));
        prop1.setPropertyName(RandomStringUtils.randomAlphabetic(10));
        prop1.setPropertyValue(new Random().nextInt());
        prop1.setPropertyType(DatasetPropertyType.Number);

        DatasetProperty prop2 = new DatasetProperty();
        prop2.setSchemaProperty(RandomStringUtils.randomAlphabetic(10));
        prop2.setPropertyName(RandomStringUtils.randomAlphabetic(10));
        prop2.setPropertyValue(RandomStringUtils.randomAlphabetic(10));
        prop2.setPropertyType(DatasetPropertyType.String);

        FileStorageObject file1 = new FileStorageObject();
        file1.setMediaType(RandomStringUtils.randomAlphabetic(20));
        file1.setCategory(FileCategory.NIH_INSTITUTIONAL_CERTIFICATION);
        file1.setBlobId(BlobId.of(RandomStringUtils.randomAlphabetic(10), RandomStringUtils.randomAlphabetic(10)));
        file1.setFileName(RandomStringUtils.randomAlphabetic(10));

        FileStorageObject file2 = new FileStorageObject();
        file2.setMediaType(RandomStringUtils.randomAlphabetic(20));
        file2.setCategory(FileCategory.ALTERNATIVE_DATA_SHARING_PLAN);
        file2.setBlobId(BlobId.of(RandomStringUtils.randomAlphabetic(10), RandomStringUtils.randomAlphabetic(10)));
        file2.setFileName(RandomStringUtils.randomAlphabetic(10));

        DatasetServiceDAO.DatasetInsert insert = new DatasetServiceDAO.DatasetInsert(
                RandomStringUtils.randomAlphabetic(20),
                dac.getDacId(),
                new DataUseBuilder().setAddiction(true).setGeneralUse(true).build(),
                user.getUserId(),
                List.of(prop1, prop2),
                List.of(file1, file2));

        List<Integer> createdIds = serviceDAO.insertDatasets(List.of(insert));

        assertEquals(1, createdIds.size());

        Dataset created = datasetDAO.findDatasetById(createdIds.get(0));

        assertEquals(insert.name(), created.getName());
        assertEquals(insert.dacId(), created.getDacId());

        assertEquals(3, created.getProperties().size());

        DatasetProperty createdProp1 = created.getProperties().stream().filter((p) -> p.getPropertyName().equals(prop1.getPropertyName())).findFirst().get();
        DatasetProperty createdProp2 = created.getProperties().stream().filter((p) -> p.getPropertyName().equals(prop2.getPropertyName())).findFirst().get();
        DatasetProperty datasetNameProp = created.getProperties().stream().filter((p) -> p.getPropertyName().equals("Dataset Name")).findFirst().get();
        assertNotNull(datasetNameProp);

        assertEquals(created.getDataSetId(), createdProp1.getDataSetId());
        assertEquals(prop1.getPropertyValue(), createdProp1.getPropertyValue());
        assertEquals(prop1.getPropertyType(), createdProp1.getPropertyType());

        assertEquals(created.getDataSetId(), createdProp2.getDataSetId());
        assertEquals(prop2.getPropertyValue(), createdProp2.getPropertyValue());
        assertEquals(prop2.getPropertyType(), createdProp2.getPropertyType());

        assertNotNull(created.getAlternativeDataSharingPlanFile());
        assertNotNull(created.getNihInstitutionalCertificationFile());

        assertEquals(file1.getFileName(), created.getNihInstitutionalCertificationFile().getFileName());
        assertEquals(file1.getBlobId(), created.getNihInstitutionalCertificationFile().getBlobId());

        assertEquals(file2.getFileName(), created.getAlternativeDataSharingPlanFile().getFileName());
        assertEquals(file2.getBlobId(), created.getAlternativeDataSharingPlanFile().getBlobId());
    }


    @Test
    public void testInsertMultipleDatasets() throws Exception {

        Dac dac = createDac();
        User user = createUser();

        DatasetProperty prop1 = new DatasetProperty();
        prop1.setSchemaProperty(RandomStringUtils.randomAlphabetic(10));
        prop1.setPropertyName(RandomStringUtils.randomAlphabetic(10));
        prop1.setPropertyValue(new Random().nextInt());
        prop1.setPropertyType(DatasetPropertyType.Number);


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

        List<Integer> createdIds = serviceDAO.insertDatasets(List.of(insert1, insert2));

        List<Dataset> datasets = datasetDAO.findDatasetsByIdList(createdIds);

        assertEquals(2, datasets.size());

        Dataset dataset1 = datasets.get(0);

        assertEquals(insert1.name(), dataset1.getName());
        assertEquals(insert1.dacId(), dataset1.getDacId());
        assertEquals(true, dataset1.getDataUse().getGeneralUse());
        assertEquals(1, dataset1.getProperties().size()); // dataset name property auto created
        assertNull(dataset1.getNihInstitutionalCertificationFile());
        assertNull(dataset1.getAlternativeDataSharingPlanFile());


        Dataset dataset2 = datasets.get(1);

        assertEquals(insert2.name(), dataset2.getName());
        assertEquals(insert2.dacId(), dataset2.getDacId());
        assertEquals(true, dataset2.getDataUse().getIllegalBehavior());
        assertEquals(2, dataset2.getProperties().size());
        assertNull(dataset2.getNihInstitutionalCertificationFile());
        assertNull(dataset2.getAlternativeDataSharingPlanFile());

    }


        // Helper methods

    /**
     * Creates a new sample Dataset along with a User and a Dac
     *
     * @return Dataset
     */
    private Dataset createSampleDataset() {
        User user = createUser();
        Dac dac = createDac();
        DataUse dataUse = new DataUseBuilder().setHmbResearch(true).build();
        Timestamp now = new Timestamp(new Date().getTime());
        Integer id = datasetDAO.insertDataset(
                RandomStringUtils.randomAlphabetic(10),
                now,
                user.getUserId(),
                RandomStringUtils.randomAlphabetic(10),
                true,
                dataUse.toString(),
                dac.getDacId()
        );
        return datasetDAO.findDatasetById(id);
    }

    /**
     * Creates a saved DatasetProperty that uses an existing Dictionary term
     *
     * @param dataset    The Dataset
     * @param dictionary The Dictionary
     * @return The created DatasetProperty
     */
    private DatasetProperty savePropWithDictionaryTerm(Dataset dataset, Dictionary dictionary) {
        DatasetProperty prop = new DatasetProperty();
        prop.setDataSetId(dataset.getDataSetId());
        prop.setPropertyKey(dictionary.getKeyId());
        prop.setPropertyName(dictionary.getKey());
        prop.setPropertyValue(RandomStringUtils.randomAlphabetic(10));
        prop.setCreateDate(new Date());
        prop.setSchemaProperty("testSchemaProp");
        prop.setPropertyType(DatasetPropertyType.String);
        datasetDAO.insertDatasetProperties(List.of(prop));
        Optional<DatasetProperty> optional = datasetDAO.findDatasetPropertiesByDatasetId(dataset.getDataSetId())
            .stream()
            .filter(p -> p.getPropertyKey().equals(dictionary.getKeyId()))
            .findFirst();
        return optional.orElse(null);
    }

    /**
     * Populates an unsaved DatasetProperty that does not use an existing Dictionary term
     *
     * @param dataset    The Dataset
     * @return The populated DatasetProperty
     */
    private DatasetProperty createUnsavedPropWithKeyName(Dataset dataset, String keyName) {
        DatasetProperty prop = new DatasetProperty();
        prop.setDataSetId(dataset.getDataSetId());
        prop.setPropertyName(keyName);
        prop.setPropertyValue(RandomStringUtils.randomAlphabetic(10));
        prop.setCreateDate(new Date());
        prop.setSchemaProperty("testSchemaProp");
        prop.setPropertyType(DatasetPropertyType.String);
        return prop;
    }

}