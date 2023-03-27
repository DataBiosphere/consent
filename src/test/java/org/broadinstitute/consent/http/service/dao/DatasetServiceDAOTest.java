package org.broadinstitute.consent.http.service.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.cloud.storage.BlobId;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import org.apache.commons.lang3.RandomStringUtils;
import org.broadinstitute.consent.http.db.DAOTestHelper;
import org.broadinstitute.consent.http.enumeration.DatasetPropertyType;
import org.broadinstitute.consent.http.enumeration.FileCategory;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DataUseBuilder;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.DatasetProperty;
import org.broadinstitute.consent.http.models.FileStorageObject;
import org.broadinstitute.consent.http.models.User;
import org.junit.Before;
import org.junit.Test;

public class DatasetServiceDAOTest extends DAOTestHelper {

    private DatasetServiceDAO serviceDAO;

    @Before
    public void setUp() {
        serviceDAO = new DatasetServiceDAO(jdbi, datasetDAO);
    }

    @Test
    public void testInsertDatasets() throws Exception{

        Dac dac = createDac();
        User user = createUser();

        DatasetProperty prop1 = new DatasetProperty();
        prop1.setSchemaProperty(RandomStringUtils.randomAlphabetic(10));
        prop1.setPropertyName(RandomStringUtils.randomAlphabetic(10));
        prop1.setPropertyType(DatasetPropertyType.Number);
        prop1.setPropertyValue(new Random().nextInt());

        DatasetProperty prop2 = new DatasetProperty();
        prop2.setSchemaProperty(RandomStringUtils.randomAlphabetic(10));
        prop2.setPropertyName(RandomStringUtils.randomAlphabetic(10));
        prop2.setPropertyType(DatasetPropertyType.Date);
        prop2.setPropertyValueAsString("2000-10-20");

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

        Optional<Dataset> ds1Optional = datasets.stream().filter(d -> d.getName().equals(insert1.name())).findFirst();
        assertTrue(ds1Optional.isPresent());
        Dataset dataset1 = ds1Optional.get();

        assertEquals(insert1.name(), dataset1.getName());
        assertEquals(insert1.dacId(), dataset1.getDacId());
        assertEquals(true, dataset1.getDataUse().getGeneralUse());
        assertEquals(1, dataset1.getProperties().size()); // dataset name property auto created
        assertNull(dataset1.getNihInstitutionalCertificationFile());
        assertNull(dataset1.getAlternativeDataSharingPlanFile());

        Optional<Dataset> ds2Optional = datasets.stream().filter(d -> d.getName().equals(insert2.name())).findFirst();
        assertTrue(ds2Optional.isPresent());
        Dataset dataset2 = ds2Optional.get();

        assertEquals(insert2.name(), dataset2.getName());
        assertEquals(insert2.dacId(), dataset2.getDacId());
        assertEquals(true, dataset2.getDataUse().getIllegalBehavior());
        assertEquals(2, dataset2.getProperties().size());
        assertNull(dataset2.getNihInstitutionalCertificationFile());
        assertNull(dataset2.getAlternativeDataSharingPlanFile());

    }

}