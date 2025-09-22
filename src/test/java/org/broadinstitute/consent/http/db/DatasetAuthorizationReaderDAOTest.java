package org.broadinstitute.consent.http.db;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.models.DataUseBuilder;
import org.broadinstitute.consent.http.models.DatasetAuthorizationReader;
import org.broadinstitute.consent.http.models.DatasetProperty;
import org.broadinstitute.consent.http.models.User;
import org.junit.jupiter.api.Test;

class DatasetAuthorizationReaderDAOTest extends DAOTestHelper {

  @Test
  void testInsertDatasetAuthorizationRecord() {
    User testUser = createUserWithInstitution();
    User operator = createUserWithInstitution();
    int datasetId = createDataset(testUser);
    long authRecordId = datasetAuthorizationReaderDAO.addAuthorizedReaderToDataset(datasetId, testUser.getUserId(), operator.getUserId());
    assertTrue(authRecordId > 0);
    List<DatasetAuthorizationReader> authorizationReaderList = datasetAuthorizationReaderDAO.findAuthorizedReadersByDatasetId(datasetId);
    assertNotNull(authorizationReaderList);
    assertEquals(1, authorizationReaderList.size());
    DatasetAuthorizationReader authorizationReader = authorizationReaderList.get(0);
    assertEquals(datasetId, authorizationReader.datasetId());
    assertEquals(testUser.getUserId().intValue(), authorizationReader.userId());
    assertEquals(operator.getUserId().intValue(), authorizationReader.createdBy());
    datasetAuthorizationReaderDAO.deleteByDatasetId(datasetId);
    List<DatasetAuthorizationReader> authorizationReaderList2 = datasetAuthorizationReaderDAO.findAuthorizedReadersByDatasetId(datasetId);
    assertNotNull(authorizationReaderList2);
    assertEquals(0, authorizationReaderList2.size());
  }

  @Test
  void testDeleteByDatasetAndUserId() {
    User testUser = createUserWithInstitution();
    User operator = createUserWithInstitution();
    int datasetId1 = createDataset(testUser);
    int datasetId2 = createDataset(testUser);
    datasetAuthorizationReaderDAO.addAuthorizedReaderToDataset(datasetId1, testUser.getUserId(), operator.getUserId());
    datasetAuthorizationReaderDAO.addAuthorizedReaderToDataset(datasetId2, testUser.getUserId(), operator.getUserId());
    assertEquals(1, datasetAuthorizationReaderDAO.findAuthorizedReadersByDatasetId(datasetId1).size());
    assertEquals(1, datasetAuthorizationReaderDAO.findAuthorizedReadersByDatasetId(datasetId2).size());

    datasetAuthorizationReaderDAO.deleteByDatasetAndUserId(datasetId1, testUser.getUserId());
    assertEquals(0, datasetAuthorizationReaderDAO.findAuthorizedReadersByDatasetId(datasetId1).size());
    assertEquals(1, datasetAuthorizationReaderDAO.findAuthorizedReadersByDatasetId(datasetId2).size());

    datasetAuthorizationReaderDAO.deleteByDatasetAndUserId(datasetId2, testUser.getUserId());
    assertEquals(0, datasetAuthorizationReaderDAO.findAuthorizedReadersByDatasetId(datasetId1).size());
    assertEquals(0, datasetAuthorizationReaderDAO.findAuthorizedReadersByDatasetId(datasetId2).size());
  }

  @Test
  void testDeleteByDatasetId() {
    User testUser = createUserWithInstitution();
    User testUser2 = createUserWithInstitution();
    User operator = createUserWithInstitution();
    int datasetId1 = createDataset(testUser);
    int datasetId2 = createDataset(testUser);
    long record1 = datasetAuthorizationReaderDAO.addAuthorizedReaderToDataset(datasetId1, testUser.getUserId(), operator.getUserId());
    datasetAuthorizationReaderDAO.addAuthorizedReaderToDataset(datasetId2, testUser.getUserId(), operator.getUserId());
    datasetAuthorizationReaderDAO.addAuthorizedReaderToDataset(datasetId1, testUser2.getUserId(), operator.getUserId());

    DatasetAuthorizationReader authReader = datasetAuthorizationReaderDAO.findAuthorizedReaderByRecordId(record1);
    assertNotNull(authReader);
    assertEquals(record1, authReader.id());
    assertEquals(datasetId1, authReader.datasetId());
    assertEquals(testUser.getUserId().intValue(), authReader.userId());
    assertEquals(operator.getUserId().intValue(), authReader.createdBy());
    assertEquals(2, datasetAuthorizationReaderDAO.findAuthorizedReadersByDatasetId(datasetId1).size());
    DatasetAuthorizationReader reader = datasetAuthorizationReaderDAO.findAuthorizedReadersByDatasetIdAndUserId(datasetId1, testUser.getUserId());
    assertNotNull(reader);
    assertEquals(record1, reader.id());
    assertEquals(datasetId1, reader.datasetId());
    assertEquals(testUser.getUserId().intValue(), reader.userId());
    assertEquals(operator.getUserId().intValue(), reader.createdBy());
    datasetAuthorizationReaderDAO.deleteByDatasetId(datasetId1);
    assertEquals(0, datasetAuthorizationReaderDAO.findAuthorizedReadersByDatasetId(datasetId1).size());

    assertEquals(1, datasetAuthorizationReaderDAO.findAuthorizedReadersByDatasetId(datasetId2).size());
  }

  @Test
  void testDeleteByUserId() {
    User testUser1 = createUserWithInstitution();
    User testUser2 = createUserWithInstitution();
    User operator = createUserWithInstitution();
    int datasetId1 = createDataset(testUser1);
    int datasetId2 = createDataset(testUser2);

    datasetAuthorizationReaderDAO.addAuthorizedReaderToDataset(datasetId1, testUser1.getUserId(), operator.getUserId());
    datasetAuthorizationReaderDAO.addAuthorizedReaderToDataset(datasetId2, testUser1.getUserId(), operator.getUserId());
    datasetAuthorizationReaderDAO.addAuthorizedReaderToDataset(datasetId2, testUser2.getUserId(), operator.getUserId());

    assertEquals(1,datasetAuthorizationReaderDAO.findAuthorizedReadersByDatasetId(datasetId1).size());
    assertEquals(2,datasetAuthorizationReaderDAO.findAuthorizedReadersByDatasetId(datasetId2).size());
    datasetAuthorizationReaderDAO.deleteByUserId(testUser1.getUserId());
    assertEquals(0,datasetAuthorizationReaderDAO.findAuthorizedReadersByDatasetId(datasetId1).size());
    assertEquals(1,datasetAuthorizationReaderDAO.findAuthorizedReadersByDatasetId(datasetId2).size());

  }

  private int createDataset(User user) {
    String name = "Name_" + randomAlphanumeric(20);
    Timestamp now = new Timestamp(new Date().getTime());
    String objectId = "Object ID_" + randomAlphanumeric(20);
    DataUse dataUse = new DataUseBuilder().setGeneralUse(true).build();
    Integer id = datasetDAO.insertDataset(name, now, user.getUserId(), objectId,
        dataUse.toString(), null);
    createDatasetProperties(id);
    return id;
  }

  private void createDatasetProperties(Integer datasetId) {
    List<DatasetProperty> list = new ArrayList<>();
    DatasetProperty dsp = new DatasetProperty();
    dsp.setDatasetId(datasetId);
    dsp.setPropertyKey(1);
    dsp.setPropertyValue("Test_PropertyValue");
    dsp.setCreateDate(new Date());
    list.add(dsp);
    datasetDAO.insertDatasetProperties(list);
  }
}