package org.broadinstitute.consent.http.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.DatasetAssociation;
import org.broadinstitute.consent.http.models.User;
import org.junit.Test;

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
    public void testGetDatasetAssociations() {
        Dataset dataset1 = createDataset();
        Dataset dataset2 = createDataset();
        User user = createUserWithRole(UserRoles.ALUMNI.getRoleId());
        datasetAssociationDAO.insertDatasetUserAssociation(DatasetAssociation.createDatasetAssociations(dataset1.getDataSetId(), List.of(user.getUserId())));
        datasetAssociationDAO.insertDatasetUserAssociation(DatasetAssociation.createDatasetAssociations(dataset2.getDataSetId(), List.of(user.getUserId())));
        List<DatasetAssociation> associationList = datasetAssociationDAO.getDatasetAssociations(List.of(dataset1.getDataSetId(), dataset2.getDataSetId()));
        assertEquals(associationList.stream().count(), 2);
        ArrayList<Integer> datasetIdList = new ArrayList<>();
        associationList.forEach(association -> datasetIdList.add(association.getDatasetId()));
        assertTrue(datasetIdList.containsAll(List.of(dataset1.getDataSetId(), dataset2.getDataSetId())));
    }

    @Test
    public void testDelete() {
        Dataset dataset1 = createDataset();
        Dataset dataset2 = createDataset();
        User user = createUserWithRole(UserRoles.ALUMNI.getRoleId());
        datasetAssociationDAO.insertDatasetUserAssociation(DatasetAssociation.createDatasetAssociations(dataset1.getDataSetId(), List.of(user.getUserId())));
        datasetAssociationDAO.insertDatasetUserAssociation(DatasetAssociation.createDatasetAssociations(dataset2.getDataSetId(), List.of(user.getUserId())));
        List<DatasetAssociation> associationList = datasetAssociationDAO.getDatasetAssociations(List.of(dataset1.getDataSetId(), dataset2.getDataSetId()));
        assertEquals(associationList.stream().count(), 2);
        datasetAssociationDAO.delete(dataset2.getDataSetId());
        List<DatasetAssociation> newAssociationList = datasetAssociationDAO.getDatasetAssociations(List.of(dataset1.getDataSetId(), dataset2.getDataSetId()));
        ArrayList<Integer> datasetIdList = new ArrayList<>();
        newAssociationList.forEach(association -> datasetIdList.add(association.getDatasetId()));
        assertTrue(datasetIdList.contains(dataset1.getDataSetId()));
        assertFalse(datasetIdList.contains(dataset2.getDataSetId()));
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
}
