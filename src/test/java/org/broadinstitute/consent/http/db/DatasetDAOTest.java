package org.broadinstitute.consent.http.db;

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
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class DatasetDAOTest extends DAOTestHelper {

    @Test
    public void testFindDatasetByIdWithDacAndConsent() {
        Dataset dataset = createDataset();
        Dac dac = createDac();
        Consent consent = createConsent(dac.getDacId());
        createAssociation(consent.getConsentId(), dataset.getDataSetId());

        Dataset foundDataset = dataSetDAO.findDatasetById(dataset.getDataSetId());
        assertNotNull(foundDataset);
        assertEquals(dac.getDacId(), foundDataset.getDacId());
        assertEquals(consent.getConsentId(), foundDataset.getConsentId());
        assertEquals(consent.getTranslatedUseRestriction(), foundDataset.getTranslatedUseRestriction());
        assertFalse(foundDataset.getProperties().isEmpty());
        assertTrue(foundDataset.getDeletable());
    }

    @Test
    public void testFindDatasetByIdWithDacAndConsentNotDeletable() {
        User user = createUser();
        Dataset dataset = createDataset();
        Dac dac = createDac();
        // This helper method creates a consent and association
        createDarCollectionWithDatasetsAndConsentAssociation(dac.getDacId(), user, List.of(dataset));

        Dataset foundDataset = dataSetDAO.findDatasetById(dataset.getDataSetId());
        assertNotNull(foundDataset);
        assertEquals(dac.getDacId(), foundDataset.getDacId());
        assertFalse(foundDataset.getProperties().isEmpty());
        assertFalse(foundDataset.getDeletable());
    }

    @Test
    public void testFindNeedsApprovalDataSetByDataSetId() {
        Dataset dataset = createDataset();
        dataSetDAO.updateDatasetNeedsApproval(dataset.getDataSetId(), true);
        Dac dac = createDac();
        Consent consent = createConsent(dac.getDacId());
        createAssociation(consent.getConsentId(), dataset.getDataSetId());

        List<Dataset> datasets = dataSetDAO.findNeedsApprovalDatasetByDatasetId(List.of(dataset.getDataSetId()));
        assertFalse(datasets.isEmpty());
        assertEquals(1, datasets.size());
        assertEquals(dac.getDacId(), datasets.get(0).getDacId());
        assertEquals(consent.getConsentId(), datasets.get(0).getConsentId());
        assertEquals(consent.getTranslatedUseRestriction(), datasets.get(0).getTranslatedUseRestriction());
        assertFalse(datasets.get(0).getProperties().isEmpty());
    }

    @Test
    public void testGetDataSetsForObjectIdList() {
        Dataset dataset = createDataset();
        Dac dac = createDac();
        Consent consent = createConsent(dac.getDacId());
        createAssociation(consent.getConsentId(), dataset.getDataSetId());

        List<Dataset> datasets = dataSetDAO.getDatasetsForObjectIdList(List.of(dataset.getObjectId()));
        assertFalse(datasets.isEmpty());
        assertEquals(1, datasets.size());
        assertEquals(dac.getDacId(), datasets.get(0).getDacId());
        assertEquals(consent.getConsentId(), datasets.get(0).getConsentId());
        assertEquals(consent.getTranslatedUseRestriction(), datasets.get(0).getTranslatedUseRestriction());
        assertFalse(datasets.get(0).getProperties().isEmpty());
    }

    @Test
    public void testFindDatasetsByIdList() {
        Dataset dataset = createDataset();
        Dac dac = createDac();
        Consent consent = createConsent(dac.getDacId());
        createAssociation(consent.getConsentId(), dataset.getDataSetId());

        List<Dataset> datasets = dataSetDAO.findDatasetsByIdList(List.of(dataset.getDataSetId()));
        assertFalse(datasets.isEmpty());
        assertEquals(1, datasets.size());
        assertEquals(dac.getDacId(), datasets.get(0).getDacId());
        assertEquals(consent.getConsentId(), datasets.get(0).getConsentId());
        assertEquals(consent.getTranslatedUseRestriction(), datasets.get(0).getTranslatedUseRestriction());
        assertFalse(datasets.get(0).getProperties().isEmpty());
    }

    @Test
    public void testFindDatasetsForConsentId() {
        Dataset dataset = createDataset();
        Dac dac = createDac();
        Consent consent = createConsent(dac.getDacId());
        createAssociation(consent.getConsentId(), dataset.getDataSetId());

        Set<Dataset> datasets = dataSetDAO.findDatasetsForConsentId(consent.getConsentId());
        assertFalse(datasets.isEmpty());
        assertEquals(1, datasets.size());
        Optional<Dataset> foundDataset = datasets.stream().findFirst();
        assertTrue(foundDataset.isPresent());
        assertEquals(dac.getDacId(), foundDataset.get().getDacId());
        assertEquals(consent.getConsentId(), foundDataset.get().getConsentId());
        assertEquals(consent.getTranslatedUseRestriction(), foundDataset.get().getTranslatedUseRestriction());
        assertFalse(foundDataset.get().getProperties().isEmpty());
    }

    // User -> UserRoles -> DACs -> Consents -> Consent Associations -> DataSets
    @Test
    public void testFindDataSetsByAuthUserEmail() {
        Dataset dataset = createDataset();
        Dac dac = createDac();
        Consent consent = createConsent(dac.getDacId());
        createAssociation(consent.getConsentId(), dataset.getDataSetId());
        User user = createUser();
        createUserRole(UserRoles.CHAIRPERSON.getRoleId(), user.getDacUserId(), dac.getDacId());

        List<Dataset> datasets = dataSetDAO.findDatasetsByAuthUserEmail(user.getEmail());
        assertFalse(datasets.isEmpty());
        List<Integer> datasetIds = datasets.stream().map(Dataset::getDataSetId).collect(Collectors.toList());
        assertTrue(datasetIds.contains(dataset.getDataSetId()));
    }

    @Test
    public void testFindNonDACDataSets() {
        Dataset dataset = createDataset();
        Consent consent = createConsent(null);
        createAssociation(consent.getConsentId(), dataset.getDataSetId());

        List<Dataset> datasets = dataSetDAO.findNonDACDatasets();
        assertFalse(datasets.isEmpty());
        List<Integer> datasetIds = datasets.stream().map(Dataset::getDataSetId).collect(Collectors.toList());
        assertTrue(datasetIds.contains(dataset.getDataSetId()));
    }

    @Test
    public void testFindDatasetAndDacIds() {
        Dataset dataset = createDataset();
        Dac dac = createDac();
        Consent consent = createConsent(dac.getDacId());
        createAssociation(consent.getConsentId(), dataset.getDataSetId());

        List<Pair<Integer, Integer>> pairs = dataSetDAO.findDatasetAndDacIds();
        assertFalse(pairs.isEmpty());
        assertEquals(1, pairs.size());
        assertEquals(pairs.get(0).getLeft(), dataset.getDataSetId());
        assertEquals(pairs.get(0).getRight(), dac.getDacId());
    }

    @Test
    public void testFindDatasetPropertiesByDatasetId() {
        Dataset d = createDataset();
        Set<DatasetProperty> properties = dataSetDAO.findDatasetPropertiesByDatasetId(d.getDataSetId());
        assertEquals(properties.size(), 1);
    }

    @Test
    public void testUpdateDataset() {
        Dataset d = createDataset();
        Timestamp now = new Timestamp(new Date().getTime());
        dataSetDAO.updateDatasetUpdateUserAndDate(d.getDataSetId(), now, d.getCreateUserId());
        Dataset updated = dataSetDAO.findDatasetById(d.getDataSetId());
        assertEquals(updated.getUpdateDate(), now);
    }

    @Test
    public void testUpdateDatasetProperty() {
        Dataset d = createDataset();
        Set<DatasetProperty> properties = dataSetDAO.findDatasetPropertiesByDatasetId(d.getDataSetId());
        DatasetProperty originalProperty = properties.stream().collect(Collectors.toList()).get(0);
        DatasetProperty newProperty = new DatasetProperty(d.getDataSetId(), 1, "Updated Value", new Date());
        List<DatasetProperty> updatedProperties = new ArrayList<>();
        updatedProperties.add(newProperty);
        dataSetDAO.updateDatasetProperty(d.getDataSetId(), updatedProperties.get(0).getPropertyKey(), updatedProperties.get(0).getPropertyValue());
        Set<DatasetProperty> returnedProperties = dataSetDAO.findDatasetPropertiesByDatasetId(d.getDataSetId());
        DatasetProperty returnedProperty = returnedProperties.stream().collect(Collectors.toList()).get(0);
        assertEquals(originalProperty.getPropertyKey(), returnedProperty.getPropertyKey());
        assertEquals(originalProperty.getPropertyId(), returnedProperty.getPropertyId());
        assertNotEquals(originalProperty.getPropertyValue(), returnedProperty.getPropertyValue());
    }

    @Test
    public void testDeleteDatasetPropertyByKey() {
        Dataset d = createDataset();
        Set<DatasetProperty> properties = dataSetDAO.findDatasetPropertiesByDatasetId(d.getDataSetId());
        DatasetProperty propertyToDelete = properties.stream().collect(Collectors.toList()).get(0);
        dataSetDAO.deleteDatasetPropertyByKey(d.getDataSetId(), propertyToDelete.getPropertyKey());
        Set<DatasetProperty> returnedProperties = dataSetDAO.findDatasetPropertiesByDatasetId(d.getDataSetId());
        assertNotEquals(properties.size(), returnedProperties.size());
    }

    @Test
    public void testFindAllDatasets() {
        Dataset dataset = createDataset();
        Consent consent = createConsent(null);
        createAssociation(consent.getConsentId(), dataset.getDataSetId());

        Set<DatasetDTO> datasets = dataSetDAO.findAllDatasets();
        assertFalse(datasets.isEmpty());
        List<Integer> datasetIds = datasets.stream().map(DatasetDTO::getDataSetId).collect(Collectors.toList());
        assertTrue(datasetIds.contains(dataset.getDataSetId()));
    }

    @Test
    public void testFindActiveDatasets() {
        Dataset dataset = createDataset();
        Consent consent = createConsent(null);
        createAssociation(consent.getConsentId(), dataset.getDataSetId());

        Set<DatasetDTO> datasets = dataSetDAO.findActiveDatasets();
        assertFalse(datasets.isEmpty());
        List<Integer> datasetIds = datasets.stream().map(DatasetDTO::getDataSetId).collect(Collectors.toList());
        assertTrue(datasetIds.contains(dataset.getDataSetId()));
    }

    @Test
    public void testFindDatasetsByUser() {
        Dataset dataset = createDataset();
        Dac dac = createDac();
        Consent consent = createConsent(dac.getDacId());
        createAssociation(consent.getConsentId(), dataset.getDataSetId());
        User user = createUser();
        createUserRole(UserRoles.CHAIRPERSON.getRoleId(), user.getDacUserId(), dac.getDacId());

        Set<DatasetDTO> datasets = dataSetDAO.findDatasetsByUserId(user.getDacUserId());
        assertFalse(datasets.isEmpty());
        List<Integer> datasetIds = datasets.stream().map(DatasetDTO::getDataSetId).collect(Collectors.toList());
        assertTrue(datasetIds.contains(dataset.getDataSetId()));
    }

    @Test
    public void testFindDatasetsByDacIds() {
        Dataset dataset = createDataset();
        Dac dac = createDac();

        Dataset datasetTwo = createDataset();
        Dac dacTwo = createDac();
        createConsentAndAssociationWithDatasetIdAndDACId(dataset.getDataSetId(), dac.getDacId());
        createConsentAndAssociationWithDatasetIdAndDACId(datasetTwo.getDataSetId(), dacTwo.getDacId());
        List<Integer> datasetIds = List.of(dataset.getDataSetId(), datasetTwo.getDataSetId());
        Set<DatasetDTO> datasets = dataSetDAO.findDatasetsByDacIds(List.of(dac.getDacId(), dacTwo.getDacId()));
        datasets.stream().forEach(d -> assertTrue(datasetIds.contains(d.getDataSetId())));
    }

    @Test
    public void testFindDatasetWithDataUseByIdList() {
        Dataset dataset = createDataset();
        Dac dac = createDac();
        Consent consent = createConsent(dac.getDacId());
        createAssociation(consent.getConsentId(), dataset.getDataSetId());

        Set<Dataset> datasets = dataSetDAO.findDatasetWithDataUseByIdList(Collections.singletonList(dataset.getDataSetId()));
        assertFalse(datasets.isEmpty());
        List<Integer> datasetIds = datasets.stream().map(Dataset::getDataSetId).collect(Collectors.toList());
        assertTrue(datasetIds.contains(dataset.getDataSetId()));
    }

    private void createUserRole(Integer roleId, Integer userId, Integer dacId) {
        dacDAO.addDacMember(roleId, userId, dacId);
    }
}
