package org.broadinstitute.consent.http.db;

import org.apache.commons.lang3.RandomStringUtils;
import org.broadinstitute.consent.http.enumeration.FileCategory;
import org.broadinstitute.consent.http.enumeration.PropertyType;
import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.models.DataUseBuilder;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.FileStorageObject;
import org.broadinstitute.consent.http.models.Study;
import org.broadinstitute.consent.http.models.User;
import org.junit.Test;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class StudyDAOTest extends DAOTestHelper {

    @Test
    public void testCreateAndFindStudy() {
        User u = createUser();

        String name = RandomStringUtils.randomAlphabetic(20);
        String description = RandomStringUtils.randomAlphabetic(20);
        List<String> dataTypes = List.of(
                RandomStringUtils.randomAlphabetic(20),
                RandomStringUtils.randomAlphabetic(20),
                RandomStringUtils.randomAlphabetic(20)
        );
        String piName = RandomStringUtils.randomAlphabetic(20);
        Boolean publicVisibility = true;
        UUID uuid = UUID.randomUUID();

        Integer id = studyDAO.insertStudy(
                name,
                description,
                piName,
                dataTypes,
                publicVisibility,
                u.getUserId(),
                Instant.now(),
                uuid
        );

        studyDAO.insertStudy(
                RandomStringUtils.randomAlphabetic(20),
                description,
                piName,
                dataTypes,
                publicVisibility,
                u.getUserId(),
                Instant.now(),
                UUID.randomUUID()
        );

        Study study = studyDAO.findStudyById(id);

        assertEquals(id, study.getStudyId());
        assertEquals(name, study.getName());
        assertEquals(description, study.getDescription());
        assertEquals(piName, study.getPiName());
        assertEquals(dataTypes, study.getDataTypes());
        assertEquals(publicVisibility, study.getPublicVisibility());
        assertEquals(u.getUserId(), study.getCreateUserId());
        assertEquals(uuid, study.getUuid());
        assertNotNull(u.getCreateDate());
    }

    @Test
    public void testStudyProps() {
        User u = createUser();

        String name = RandomStringUtils.randomAlphabetic(20);
        String description = RandomStringUtils.randomAlphabetic(20);
        List<String> dataTypes = List.of(
                RandomStringUtils.randomAlphabetic(20),
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
                u.getUserId(),
                Instant.now(),
                UUID.randomUUID()
        );

        Integer id2 = studyDAO.insertStudy(
                RandomStringUtils.randomAlphabetic(20),
                description,
                piName,
                dataTypes,
                publicVisibility,
                u.getUserId(),
                Instant.now(),
                UUID.randomUUID()
        );

        Integer prop1Id = studyDAO.insertStudyProperty(
                id,
                "prop1",
                PropertyType.String.toString(),
                "asdf"
        );

        Integer prop2Id = studyDAO.insertStudyProperty(
                id,
                "prop2",
                PropertyType.Number.toString(),
                "1"
        );

        // create some random, other property
        studyDAO.insertStudyProperty(
                id2,
                "unrelated",
                PropertyType.String.toString(),
                "asdfasdfasdf"
        );

        Study study = studyDAO.findStudyById(id);

        assertEquals(study.getProperties().size(), 2);

        study.getProperties().forEach((prop) -> {
            if (prop.getStudyPropertyId().equals(prop1Id)) {
                assertEquals("prop1", prop.getName());
                assertEquals(PropertyType.String, prop.getType());
                assertEquals("asdf", prop.getValue());
            } else if (prop.getStudyPropertyId().equals(prop2Id)) {
                assertEquals("prop2", prop.getName());
                assertEquals(PropertyType.Number, prop.getType());
                assertEquals(1, prop.getValue());
            } else {
                fail("Unexpected property");
            }
        });
    }


    @Test
    public void testAlternativeDataSharingPlan() {
        User u = createUser();

        UUID uuid = UUID.randomUUID();
        Integer id = studyDAO.insertStudy(
                "name",
                "description",
                "asdf",
                List.of(),
                true,
                u.getUserId(),
                Instant.now(),
                uuid
        );

        FileStorageObject fso = createFileStorageObject(uuid.toString(), FileCategory.ALTERNATIVE_DATA_SHARING_PLAN);

        Study study = studyDAO.findStudyById(id);

        assertEquals(fso.getFileStorageObjectId(), study.getAlternativeDataSharingPlan().getFileStorageObjectId());
        assertEquals(fso.getBlobId(), study.getAlternativeDataSharingPlan().getBlobId());

    }

    @Test
    public void testIncludesDatasetIds() {
        Study s = insertStudyWithProperties();

        insertDataset();
        Dataset ds1 = insertDatasetForStudy(s.getStudyId());
        insertDataset();
        Dataset ds2 = insertDatasetForStudy(s.getStudyId());
        insertDataset();

        s = studyDAO.findStudyById(s.getStudyId());

        assertEquals(2, s.getDatasetIds().size());
        assertTrue(s.getDatasetIds().contains(ds1.getDataSetId()));
        assertTrue(s.getDatasetIds().contains(ds2.getDataSetId()));
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

    private Study insertStudyWithProperties() {
        User u = createUser();

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

    private Dataset insertDatasetForStudy(Integer studyId) {
        User user = createUser();
        String name = "Name_" + RandomStringUtils.random(20, true, true);
        Timestamp now = new Timestamp(new Date().getTime());
        String objectId = "Object ID_" + RandomStringUtils.random(20, true, true);
        DataUse dataUse = new DataUseBuilder().setGeneralUse(true).build();
        Integer id = datasetDAO.insertDataset(name, now, user.getUserId(), objectId, true, dataUse.toString(), null);

        datasetDAO.updateStudyId(id, studyId);

        return datasetDAO.findDatasetById(id);
    }

    private Dataset insertDataset() {
        User user = createUser();
        String name = "Name_" + RandomStringUtils.random(20, true, true);
        Timestamp now = new Timestamp(new Date().getTime());
        String objectId = "Object ID_" + RandomStringUtils.random(20, true, true);
        DataUse dataUse = new DataUseBuilder().setGeneralUse(true).build();
        Integer id = datasetDAO.insertDataset(name, now, user.getUserId(), objectId, true, dataUse.toString(), null);
        return datasetDAO.findDatasetById(id);
    }

}