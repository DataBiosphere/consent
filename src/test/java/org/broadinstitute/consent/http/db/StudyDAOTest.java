package org.broadinstitute.consent.http.db;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.apache.commons.lang3.RandomStringUtils;
import org.broadinstitute.consent.http.enumeration.FileCategory;
import org.broadinstitute.consent.http.enumeration.PropertyType;
import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.models.DataUseBuilder;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.FileStorageObject;
import org.broadinstitute.consent.http.models.Study;
import org.broadinstitute.consent.http.models.User;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

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

        Assertions.assertEquals(id, study.getStudyId());
        Assertions.assertEquals(name, study.getName());
        Assertions.assertEquals(description, study.getDescription());
        Assertions.assertEquals(piName, study.getPiName());
        Assertions.assertEquals(dataTypes, study.getDataTypes());
        Assertions.assertEquals(publicVisibility, study.getPublicVisibility());
        Assertions.assertEquals(u.getUserId(), study.getCreateUserId());
        Assertions.assertEquals(uuid, study.getUuid());
        Assertions.assertNotNull(u.getCreateDate());
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

        Assertions.assertEquals(study.getProperties().size(), 2);

        study.getProperties().forEach((prop) -> {
            if (prop.getStudyPropertyId().equals(prop1Id)) {
                Assertions.assertEquals("prop1", prop.getKey());
                Assertions.assertEquals(PropertyType.String, prop.getType());
                Assertions.assertEquals("asdf", prop.getValue());
            } else if (prop.getStudyPropertyId().equals(prop2Id)) {
                Assertions.assertEquals("prop2", prop.getKey());
                Assertions.assertEquals(PropertyType.Number, prop.getType());
                Assertions.assertEquals(1, prop.getValue());
            } else {
                Assertions.fail("Unexpected property");
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

        Assertions.assertEquals(fso.getFileStorageObjectId(),
            study.getAlternativeDataSharingPlan().getFileStorageObjectId());
        Assertions.assertEquals(fso.getBlobId(), study.getAlternativeDataSharingPlan().getBlobId());

    }

    @Test
    public void testGetAlternativeDataSharingFile() {
        Study study = insertStudyWithProperties();

        // create unrelated file with the same id as dataset id but different category, timestamp before
        createFileStorageObject(
                study.getUuid().toString(),
                FileCategory.NIH_INSTITUTIONAL_CERTIFICATION
        );

        FileStorageObject altFile = createFileStorageObject(
                study.getUuid().toString(),
                FileCategory.ALTERNATIVE_DATA_SHARING_PLAN
        );

        // create unrelated files with timestamp later than the ADSP file: one attached to dataset, one
        // completely separate from the dataset. ensures that the Mapper is selecting only the right file.
        createFileStorageObject("asdfasdf", FileCategory.ALTERNATIVE_DATA_SHARING_PLAN);
        createFileStorageObject(
                study.getUuid().toString(),
                FileCategory.DATA_USE_LETTER
        );

        Study found = studyDAO.findStudyById(study.getStudyId());

        Assertions.assertEquals(altFile, found.getAlternativeDataSharingPlan());
        Assertions.assertEquals(altFile.getBlobId(),
            found.getAlternativeDataSharingPlan().getBlobId());
    }

    @Test
    public void testGetAlternativeDataSharingPlanFile_AlwaysLatestCreated() throws InterruptedException {
        Study study = insertStudyWithProperties();

        String fileName = org.apache.commons.lang3.RandomStringUtils.randomAlphabetic(10);
        String bucketName = org.apache.commons.lang3.RandomStringUtils.randomAlphabetic(10);
        String gcsFileUri = org.apache.commons.lang3.RandomStringUtils.randomAlphabetic(10);
        User createUser = createUser();

        Integer altFileIdCreatedFirst = fileStorageObjectDAO.insertNewFile(
                fileName,
                FileCategory.ALTERNATIVE_DATA_SHARING_PLAN.getValue(),
                bucketName,
                gcsFileUri,
                study.getUuid().toString(),
                createUser.getUserId(),
                Instant.ofEpochMilli(100)
        );

        User updateUser = createUser();

        fileStorageObjectDAO.updateFileById(
                altFileIdCreatedFirst,
                org.apache.commons.lang3.RandomStringUtils.randomAlphabetic(20),
                org.apache.commons.lang3.RandomStringUtils.randomAlphabetic(20),
                updateUser.getUserId(),
                Instant.ofEpochMilli(120));

        Integer altFileIdCreatedSecond = fileStorageObjectDAO.insertNewFile(
                fileName,
                FileCategory.ALTERNATIVE_DATA_SHARING_PLAN.getValue(),
                bucketName,
                gcsFileUri,
                study.getUuid().toString(),
                createUser.getUserId(),
                Instant.ofEpochMilli(130)
        );


        Study found = studyDAO.findStudyById(study.getStudyId());

        // returns last updated file
        Assertions.assertEquals(altFileIdCreatedSecond,
            found.getAlternativeDataSharingPlan().getFileStorageObjectId());
    }

    @Test
    public void testGetAlternativeDataSharingPlanFile_NotDeleted() {
        Study study = insertStudyWithProperties();

        FileStorageObject altFile = createFileStorageObject(
                study.getUuid().toString(),
                FileCategory.ALTERNATIVE_DATA_SHARING_PLAN
        );

        User deleteUser = createUser();

        fileStorageObjectDAO.deleteFileById(
                altFile.getFileStorageObjectId(),
                deleteUser.getUserId(),
                Instant.now()
        );

        Study found = studyDAO.findStudyById(study.getStudyId());

        Assertions.assertNull(found.getAlternativeDataSharingPlan());
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

        Assertions.assertEquals(2, s.getDatasetIds().size());
        Assertions.assertTrue(s.getDatasetIds().contains(ds1.getDataSetId()));
        Assertions.assertTrue(s.getDatasetIds().contains(ds2.getDataSetId()));
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