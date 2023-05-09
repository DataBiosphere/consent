package org.broadinstitute.consent.http.service.dao;

import com.google.cloud.storage.BlobId;
import org.apache.commons.lang3.RandomStringUtils;
import org.broadinstitute.consent.http.db.DAOTestHelper;
import org.broadinstitute.consent.http.enumeration.FileCategory;
import org.broadinstitute.consent.http.enumeration.PropertyType;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DataUseBuilder;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.DatasetProperty;
import org.broadinstitute.consent.http.models.FileStorageObject;
import org.broadinstitute.consent.http.models.Study;
import org.broadinstitute.consent.http.models.StudyProperty;
import org.broadinstitute.consent.http.models.User;
import org.junit.Before;
import org.junit.Test;

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
        serviceDAO = new DatasetServiceDAO(jdbi, datasetDAO);
    }

    @Test
    public void testInsertDatasets() throws Exception {

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
        file1.setBlobId(BlobId.of(RandomStringUtils.randomAlphabetic(10), RandomStringUtils.randomAlphabetic(10)));
        file1.setFileName(RandomStringUtils.randomAlphabetic(10));

        DatasetServiceDAO.DatasetInsert insert = new DatasetServiceDAO.DatasetInsert(
                RandomStringUtils.randomAlphabetic(20),
                dac.getDacId(),
                new DataUseBuilder().setAddiction(true).setGeneralUse(true).build(),
                user.getUserId(),
                List.of(prop1, prop2),
                List.of(file1));

        List<Integer> createdIds = serviceDAO.insertDatasetRegistration(null, List.of(insert));

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

        assertNotNull(created.getNihInstitutionalCertificationFile());

        assertEquals(file1.getFileName(), created.getNihInstitutionalCertificationFile().getFileName());
        assertEquals(file1.getBlobId(), created.getNihInstitutionalCertificationFile().getBlobId());
    }


    @Test
    public void testInsertMultipleDatasets() throws Exception {

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

        List<Integer> createdIds = serviceDAO.insertDatasetRegistration(null, List.of(insert1, insert2));

        List<Dataset> datasets = datasetDAO.findDatasetsByIdList(createdIds);

        assertEquals(2, datasets.size());

        Optional<Dataset> ds1Optional = datasets.stream().filter(d -> d.getName().equals(insert1.name())).findFirst();
        assertTrue(ds1Optional.isPresent());
        Dataset dataset1 = ds1Optional.get();

        assertEquals(insert1.name(), dataset1.getName());
        assertEquals(insert1.dacId(), dataset1.getDacId());
        assertEquals(true, dataset1.getDataUse().getGeneralUse());
        assertEquals(1, dataset1.getProperties().size()); // dataset name property auto created
        assertNull(dataset1.getNihInstitutionalCertificationFile());

        Optional<Dataset> ds2Optional = datasets.stream().filter(d -> d.getName().equals(insert2.name())).findFirst();
        assertTrue(ds2Optional.isPresent());
        Dataset dataset2 = ds2Optional.get();

        assertEquals(insert2.name(), dataset2.getName());
        assertEquals(insert2.dacId(), dataset2.getDacId());
        assertEquals(true, dataset2.getDataUse().getIllegalBehavior());
        assertEquals(2, dataset2.getProperties().size());
        assertNull(dataset2.getNihInstitutionalCertificationFile());
    }

    @Test
    public void testInsertStudyWithDatasets() throws Exception {
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

        List<Integer> createdIds = serviceDAO.insertDatasetRegistration(studyInsert, List.of(datasetInsert));

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

        assertTrue(s.getProperties().isEmpty());
        assertNull(s.getAlternativeDataSharingPlan());
    }

    @Test
    public void testInsertStudyWithProps() throws Exception {
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

        List<Integer> createdIds = serviceDAO.insertDatasetRegistration(studyInsert, List.of(datasetInsert));

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


        StudyProperty createdProp1 = dataset1.getStudy().getProperties().stream().filter((p) -> p.getKey().equals(prop1.getKey())).findFirst().get();
        StudyProperty createdProp2 = dataset1.getStudy().getProperties().stream().filter((p) -> p.getKey().equals(prop2.getKey())).findFirst().get();

        assertEquals(prop1.getType(), createdProp1.getType());
        assertEquals(prop1.getValue(), createdProp1.getValue());
        assertEquals(prop2.getType(), createdProp2.getType());
        assertEquals(prop2.getValue(), createdProp2.getValue());

        assertNull(s.getAlternativeDataSharingPlan());
    }

    @Test
    public void testInsertStudyWithAlternativeDataSharingFile() throws Exception {
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
        file.setBlobId(BlobId.of(RandomStringUtils.randomAlphabetic(10), RandomStringUtils.randomAlphabetic(10)));
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

        List<Integer> createdIds = serviceDAO.insertDatasetRegistration(studyInsert, List.of(datasetInsert));

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


        StudyProperty createdProp1 = dataset1.getStudy().getProperties().stream().filter((p) -> p.getKey().equals(prop1.getKey())).findFirst().get();
        StudyProperty createdProp2 = dataset1.getStudy().getProperties().stream().filter((p) -> p.getKey().equals(prop2.getKey())).findFirst().get();

        assertEquals(prop1.getType(), createdProp1.getType());
        assertEquals(prop1.getValue(), createdProp1.getValue());
        assertEquals(prop2.getType(), createdProp2.getType());
        assertEquals(prop2.getValue(), createdProp2.getValue());

        assertNotNull(s.getAlternativeDataSharingPlan());

        assertEquals(file.getBlobId(), s.getAlternativeDataSharingPlan().getBlobId());
        assertEquals(file.getFileName(), s.getAlternativeDataSharingPlan().getFileName());
        assertEquals(file.getCategory(), s.getAlternativeDataSharingPlan().getCategory());
    }
}