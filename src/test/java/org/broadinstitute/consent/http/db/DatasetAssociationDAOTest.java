package org.broadinstitute.consent.http.db;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.DatasetAssociation;
import org.broadinstitute.consent.http.models.User;
import org.junit.jupiter.api.Test;

public class DatasetAssociationDAOTest extends DAOTestHelper {

    @Test
    public void testInsertDatasetUserAssociation() {
        Dataset dataset = createDataset();
        User user = createUserWithRole(UserRoles.ALUMNI.getRoleId());
        datasetAssociationDAO.insertDatasetUserAssociation(DatasetAssociation.createDatasetAssociations(dataset.getDataSetId(), List.of(user.getUserId())));
        List<DatasetAssociation> associationList = datasetAssociationDAO.getDatasetAssociation(dataset.getDataSetId());
        assertFalse(associationList.isEmpty());
    }

    @Test
    public void testGetDatasetAssociation() {
        Dataset dataset1 = createDataset();
        Dataset dataset2 = createDataset();
        User user = createUserWithRole(UserRoles.ALUMNI.getRoleId());
        datasetAssociationDAO.insertDatasetUserAssociation(DatasetAssociation.createDatasetAssociations(dataset1.getDataSetId(), List.of(user.getUserId())));
        List<DatasetAssociation> associationList = datasetAssociationDAO.getDatasetAssociation(dataset1.getDataSetId());
        assertFalse(associationList.isEmpty());
        List<DatasetAssociation> emptyAssociationList = datasetAssociationDAO.getDatasetAssociation(dataset2.getDataSetId());
        assertTrue(emptyAssociationList.isEmpty());
    }

    @Test
    public void testExist() {
        Dataset dataset = createDataset();
        User user = createUserWithRole(UserRoles.ALUMNI.getRoleId());
        datasetAssociationDAO.insertDatasetUserAssociation(DatasetAssociation.createDatasetAssociations(dataset.getDataSetId(), List.of(user.getUserId())));
        Boolean associationExists = datasetAssociationDAO.exist(dataset.getDataSetId());
        assertTrue(associationExists);
    }

    @Test
    public void testGetDataOwnersOfDataSet() {
        Dataset dataset = createDataset();
        User user = createUserWithRole(UserRoles.ALUMNI.getRoleId());
        datasetAssociationDAO.insertDatasetUserAssociation(DatasetAssociation.createDatasetAssociations(dataset.getDataSetId(), List.of(user.getUserId())));
        List<Integer> userIds = datasetAssociationDAO.getDataOwnersOfDataSet(dataset.getDataSetId());
        assertTrue(userIds.contains(user.getUserId()));
    }

    @Test
    public void testDeleteDatasetAssociationsByUserId() {
        Dataset dataset1 = createDataset();
        Integer datasetId = dataset1.getDataSetId();
        Integer userId = dataset1.getCreateUserId();

        datasetAssociationDAO.insertDatasetUserAssociation(DatasetAssociation.createDatasetAssociations(datasetId, List.of(userId)));
        List<Integer> userIds1 = datasetAssociationDAO.getDataOwnersOfDataSet(datasetId);
        assertTrue(userIds1.contains(userId));

        datasetAssociationDAO.deleteAllDatasetUserAssociationsByUser(userId);

        List<Integer> userIds2 = datasetAssociationDAO.getDataOwnersOfDataSet(datasetId);
        assertTrue(userIds2.isEmpty());
    }
}
