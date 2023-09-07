package org.broadinstitute.consent.http.db;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.models.DataUseBuilder;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.DatasetAssociation;
import org.broadinstitute.consent.http.models.DatasetProperty;
import org.broadinstitute.consent.http.models.User;
import org.junit.jupiter.api.Test;

public class DatasetAssociationDAOTest extends DAOTestHelper {

  @Test
  public void testInsertDatasetUserAssociation() {
    Dataset dataset = createDataset();
    User user = createUserWithRole(UserRoles.ALUMNI.getRoleId());
    datasetAssociationDAO.insertDatasetUserAssociation(
        DatasetAssociation.createDatasetAssociations(dataset.getDataSetId(),
            List.of(user.getUserId())));
    List<DatasetAssociation> associationList = datasetAssociationDAO.getDatasetAssociation(
        dataset.getDataSetId());
    assertFalse(associationList.isEmpty());
  }

  @Test
  public void testGetDatasetAssociation() {
    Dataset dataset1 = createDataset();
    Dataset dataset2 = createDataset();
    User user = createUserWithRole(UserRoles.ALUMNI.getRoleId());
    datasetAssociationDAO.insertDatasetUserAssociation(
        DatasetAssociation.createDatasetAssociations(dataset1.getDataSetId(),
            List.of(user.getUserId())));
    List<DatasetAssociation> associationList = datasetAssociationDAO.getDatasetAssociation(
        dataset1.getDataSetId());
    assertFalse(associationList.isEmpty());
    List<DatasetAssociation> emptyAssociationList = datasetAssociationDAO.getDatasetAssociation(
        dataset2.getDataSetId());
    assertTrue(emptyAssociationList.isEmpty());
  }

  @Test
  public void testExist() {
    Dataset dataset = createDataset();
    User user = createUserWithRole(UserRoles.ALUMNI.getRoleId());
    datasetAssociationDAO.insertDatasetUserAssociation(
        DatasetAssociation.createDatasetAssociations(dataset.getDataSetId(),
            List.of(user.getUserId())));
    Boolean associationExists = datasetAssociationDAO.exist(dataset.getDataSetId());
    assertTrue(associationExists);
  }

  @Test
  public void testGetDataOwnersOfDataSet() {
    Dataset dataset = createDataset();
    User user = createUserWithRole(UserRoles.ALUMNI.getRoleId());
    datasetAssociationDAO.insertDatasetUserAssociation(
        DatasetAssociation.createDatasetAssociations(dataset.getDataSetId(),
            List.of(user.getUserId())));
    List<Integer> userIds = datasetAssociationDAO.getDataOwnersOfDataSet(dataset.getDataSetId());
    assertTrue(userIds.contains(user.getUserId()));
  }

  @Test
  public void testDeleteDatasetAssociationsByUserId() {
    Dataset dataset1 = createDataset();
    Integer datasetId = dataset1.getDataSetId();
    Integer userId = dataset1.getCreateUserId();

    datasetAssociationDAO.insertDatasetUserAssociation(
        DatasetAssociation.createDatasetAssociations(datasetId, List.of(userId)));
    List<Integer> userIds1 = datasetAssociationDAO.getDataOwnersOfDataSet(datasetId);
    assertTrue(userIds1.contains(userId));

    datasetAssociationDAO.deleteAllDatasetUserAssociationsByUser(userId);

    List<Integer> userIds2 = datasetAssociationDAO.getDataOwnersOfDataSet(datasetId);
    assertTrue(userIds2.isEmpty());
  }

  private Dataset createDataset() {
    User user = createUser();
    String name = "Name_" + RandomStringUtils.random(20, true, true);
    Timestamp now = new Timestamp(new Date().getTime());
    String objectId = "Object ID_" + RandomStringUtils.random(20, true, true);
    DataUse dataUse = new DataUseBuilder().setGeneralUse(true).build();
    Integer id = datasetDAO.insertDataset(name, now, user.getUserId(), objectId,
        dataUse.toString(), null);
    createDatasetProperties(id);
    return datasetDAO.findDatasetById(id);
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

  private User createUserWithRole(Integer roleId) {
    int i1 = RandomUtils.nextInt(5, 10);
    int i2 = RandomUtils.nextInt(5, 10);
    int i3 = RandomUtils.nextInt(3, 5);
    String email = RandomStringUtils.randomAlphabetic(i1) +
        "@" +
        RandomStringUtils.randomAlphabetic(i2) +
        "." +
        RandomStringUtils.randomAlphabetic(i3);
    Integer userId = userDAO.insertUser(email, "display name", new Date());
    userRoleDAO.insertSingleUserRole(roleId, userId);
    return userDAO.findUserById(userId);
  }

}
