package org.broadinstitute.consent.http.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.commons.lang3.tuple.Pair;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.DatasetProperty;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.dto.DatasetDTO;
import org.junit.Test;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class DatasetDAOTest extends DAOTestHelper {

    // User -> UserRoles -> DACs -> Consents -> Consent Associations -> DataSets
    @Test
    public void testFindDataSetsByAuthUserEmail() {
        Dataset dataset = createDataset();
        Dac dac = createDac();
        Consent consent = createConsent(dac.getDacId());
        createAssociation(consent.getConsentId(), dataset.getDatasetId());
        User user = createUser();
        createUserRole(UserRoles.CHAIRPERSON.getRoleId(), user.getDacUserId(), dac.getDacId());

        List<Dataset> datasets = dataSetDAO.findDataSetsByAuthUserEmail(user.getEmail());
        assertFalse(datasets.isEmpty());
        List<Integer> datasetIds = datasets.stream().map(Dataset::getDatasetId).collect(Collectors.toList());
        assertTrue(datasetIds.contains(dataset.getDatasetId()));
    }

    @Test
    public void testFindNonDACDataSets() {
        Dataset dataset = createDataset();
        Consent consent = createConsent(null);
        createAssociation(consent.getConsentId(), dataset.getDatasetId());

        List<Dataset> datasets = dataSetDAO.findNonDACDataSets();
        assertFalse(datasets.isEmpty());
        List<Integer> datasetIds = datasets.stream().map(Dataset::getDatasetId).collect(Collectors.toList());
        assertTrue(datasetIds.contains(dataset.getDatasetId()));
    }

    @Test
    public void testFindDatasetAndDacIds() {
        Dataset dataset = createDataset();
        Dac dac = createDac();
        Consent consent = createConsent(dac.getDacId());
        createAssociation(consent.getConsentId(), dataset.getDatasetId());

        List<Pair<Integer, Integer>> pairs = dataSetDAO.findDatasetAndDacIds();
        assertFalse(pairs.isEmpty());
        assertEquals(1, pairs.size());
        assertEquals(pairs.get(0).getLeft(), dataset.getDatasetId());
        assertEquals(pairs.get(0).getRight(), dac.getDacId());
    }

    @Test
    public void testFindDatasetPropertiesByDatasetId() {
        Dataset d = createDataset();
        Set<DatasetProperty> properties = dataSetDAO.findDatasetPropertiesByDatasetId(d.getDatasetId());
        assertEquals(properties.size(), 1);
    }

    @Test
    public void testUpdateDataset() {
        Dataset d = createDataset();
        Timestamp now = new Timestamp(new Date().getTime());
        dataSetDAO.updateDatasetUpdateUserAndDate(d.getDatasetId(), now, d.getCreateUserId());
        Dataset updated = dataSetDAO.findDataSetById(d.getDatasetId());
        assertEquals(updated.getUpdateDate(), now);
    }

    @Test
    public void testUpdateDatasetProperty() {
        Dataset d = createDataset();
        Set<DatasetProperty> properties = dataSetDAO.findDatasetPropertiesByDatasetId(d.getDatasetId());
        DatasetProperty originalProperty = properties.stream().collect(Collectors.toList()).get(0);
        DatasetProperty newProperty = new DatasetProperty(d.getDatasetId(), 1, "Updated Value", new Date());
        List<DatasetProperty> updatedProperties = new ArrayList<>();
        updatedProperties.add(newProperty);
        dataSetDAO.updateDatasetProperty(d.getDatasetId(), updatedProperties.get(0).getPropertyKey(), updatedProperties.get(0).getPropertyValue());
        Set<DatasetProperty> returnedProperties = dataSetDAO.findDatasetPropertiesByDatasetId(d.getDatasetId());
        DatasetProperty returnedProperty = returnedProperties.stream().collect(Collectors.toList()).get(0);
        assertEquals(originalProperty.getPropertyKey(), returnedProperty.getPropertyKey());
        assertEquals(originalProperty.getPropertyId(), returnedProperty.getPropertyId());
        assertNotEquals(originalProperty.getPropertyValue(), returnedProperty.getPropertyValue());
    }

    @Test
    public void testDeleteDatasetPropertyByKey() {
        Dataset d = createDataset();
        Set<DatasetProperty> properties = dataSetDAO.findDatasetPropertiesByDatasetId(d.getDatasetId());
        DatasetProperty propertyToDelete = properties.stream().collect(Collectors.toList()).get(0);
        dataSetDAO.deleteDatasetPropertyByKey(d.getDatasetId(), propertyToDelete.getPropertyKey());
        Set<DatasetProperty> returnedProperties = dataSetDAO.findDatasetPropertiesByDatasetId(d.getDatasetId());
        assertNotEquals(properties.size(), returnedProperties.size());
    }

    @Test
    public void testFindAllDatasets() {
        Dataset dataset = createDataset();
        Consent consent = createConsent(null);
        createAssociation(consent.getConsentId(), dataset.getDatasetId());

        Set<DatasetDTO> datasets = dataSetDAO.findAllDatasets();
        assertFalse(datasets.isEmpty());
        List<Integer> datasetIds = datasets.stream().map(DatasetDTO::getDatasetId).collect(Collectors.toList());
        assertTrue(datasetIds.contains(dataset.getDatasetId()));
    }

    @Test
    public void testFindActiveDatasets() {
        Dataset dataset = createDataset();
        Consent consent = createConsent(null);
        createAssociation(consent.getConsentId(), dataset.getDatasetId());

        Set<DatasetDTO> datasets = dataSetDAO.findActiveDatasets();
        assertFalse(datasets.isEmpty());
        List<Integer> datasetIds = datasets.stream().map(DatasetDTO::getDatasetId).collect(Collectors.toList());
        assertTrue(datasetIds.contains(dataset.getDatasetId()));
    }

    @Test
    public void testFindDatasetsByUser() {
        Dataset dataset = createDataset();
        Dac dac = createDac();
        Consent consent = createConsent(dac.getDacId());
        createAssociation(consent.getConsentId(), dataset.getDatasetId());
        User user = createUser();
        createUserRole(UserRoles.CHAIRPERSON.getRoleId(), user.getDacUserId(), dac.getDacId());

        Set<DatasetDTO> datasets = dataSetDAO.findDatasetsByUserId(user.getDacUserId());
        assertFalse(datasets.isEmpty());
        List<Integer> datasetIds = datasets.stream().map(DatasetDTO::getDatasetId).collect(Collectors.toList());
        assertTrue(datasetIds.contains(dataset.getDatasetId()));
    }

    @Test
    public void testFindDatasetWithDataUseByIdList() {
        Dataset dataset = createDataset();
        Dac dac = createDac();
        Consent consent = createConsent(dac.getDacId());
        createAssociation(consent.getConsentId(), dataset.getDatasetId());

        Set<Dataset> datasets = dataSetDAO.findDatasetWithDataUseByIdList(Collections.singletonList(dataset.getDatasetId()));
        assertFalse(datasets.isEmpty());
        List<Integer> datasetIds = datasets.stream().map(Dataset::getDatasetId).collect(Collectors.toList());
        assertTrue(datasetIds.contains(dataset.getDatasetId()));
    }

    private void createUserRole(Integer roleId, Integer userId, Integer dacId) {
        dacDAO.addDacMember(roleId, userId, dacId);
    }
}
