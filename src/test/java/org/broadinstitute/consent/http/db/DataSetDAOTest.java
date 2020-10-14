package org.broadinstitute.consent.http.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.apache.commons.lang3.tuple.Pair;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DataSet;
import org.broadinstitute.consent.http.models.DataSetProperty;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.dto.DataSetDTO;
import org.junit.Test;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class DataSetDAOTest extends DAOTestHelper {

    // User -> UserRoles -> DACs -> Consents -> Consent Associations -> DataSets
    @Test
    public void testFindDataSetsByAuthUserEmail() {
        DataSet dataset = createDataset();
        Dac dac = createDac();
        Consent consent = createConsent(dac.getDacId());
        createAssociation(consent.getConsentId(), dataset.getDataSetId());
        User user = createUser();
        createUserRole(UserRoles.CHAIRPERSON.getRoleId(), user.getDacUserId(), dac.getDacId());

        List<DataSet> datasets = dataSetDAO.findDataSetsByAuthUserEmail(user.getEmail());
        assertFalse(datasets.isEmpty());
        List<Integer> datasetIds = datasets.stream().map(DataSet::getDataSetId).collect(Collectors.toList());
        assertTrue(datasetIds.contains(dataset.getDataSetId()));
    }

    @Test
    public void testFindNonDACDataSets() {
        DataSet dataset = createDataset();
        Consent consent = createConsent(null);
        createAssociation(consent.getConsentId(), dataset.getDataSetId());

        List<DataSet> datasets = dataSetDAO.findNonDACDataSets();
        assertFalse(datasets.isEmpty());
        List<Integer> datasetIds = datasets.stream().map(DataSet::getDataSetId).collect(Collectors.toList());
        assertTrue(datasetIds.contains(dataset.getDataSetId()));
    }

    @Test
    public void testFindDatasetsByDac() {
        DataSet dataset = createDataset();
        DataSet dataset2 = createDataset();
        Dac dac = createDac();
        Consent consent = createConsent(dac.getDacId());
        createAssociation(consent.getConsentId(), dataset.getDataSetId());

        Set<DataSetDTO> datasets = dataSetDAO.findDatasetsByDac(dac.getDacId());
        assertFalse(datasets.isEmpty());
        List<Integer> datasetIds = datasets.stream().map(DataSetDTO::getDataSetId).collect(Collectors.toList());
        assertTrue(datasetIds.contains(dataset.getDataSetId()));
        assertFalse(datasetIds.contains(dataset2.getDataSetId()));
    }

    @Test
    public void testFindDatasetsWithDacsCase1() {
        Set<DataSetDTO> datasets = dataSetDAO.findDatasetsWithDacs();
        assertTrue(datasets.isEmpty());
    }

    @Test
    public void testFindDatasetsWithDacsCase2() {
        DataSet dataset = createDataset();
        DataSet dataset2 = createDataset();
        Dac dac = createDac();
        Consent consent = createConsent(dac.getDacId());
        createAssociation(consent.getConsentId(), dataset.getDataSetId());
        createAssociation(consent.getConsentId(), dataset2.getDataSetId());
        Set<DataSetDTO> datasets = dataSetDAO.findDatasetsWithDacs();
        assertFalse(datasets.isEmpty());
        assertEquals(2, datasets.size());
    }

    @Test
    public void testFindDatasetAndDacIds() {
        DataSet dataset = createDataset();
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
    public void testFindDacForDataset() {
        DataSet dataset = createDataset();
        Dac dac = createDac();
        Consent consent = createConsent(dac.getDacId());
        createAssociation(consent.getConsentId(), dataset.getDataSetId());

        Dac foundDac = dataSetDAO.findDacForDataset(dataset.getDataSetId());
        assertNotNull(foundDac);
        assertEquals(dac.getDacId(), foundDac.getDacId());
    }


    @Test
    public void testFindDacForDatasetNotFound() {
        DataSet dataset = createDataset();

        Dac foundDac = dataSetDAO.findDacForDataset(dataset.getDataSetId());
        assertNull(foundDac);
    }

    @Test
    public void testFindDatasetsForConsentId_case0() {
        Dac dac = createDac();
        Consent c = createConsent(dac.getDacId());

        Set<DataSet> datasets = dataSetDAO.findDatasetsForConsentId(c.getConsentId());
        assertEquals(datasets.size(), 0);
    }

    @Test
    public void testFindDatasetsForConsentId_case1() {
        Dac dac = createDac();
        Consent c = createConsent(dac.getDacId());
        DataSet d = createDataset();
        createAssociation(c.getConsentId(), d.getDataSetId());

        Set<DataSet> datasets = dataSetDAO.findDatasetsForConsentId(c.getConsentId());
        assertEquals(datasets.size(), 1);
    }

    @Test
    public void testFindDatasetsForConsentId_case2() {
        Dac dac = createDac();
        Consent c = createConsent(dac.getDacId());
        DataSet d = createDataset();
        createAssociation(c.getConsentId(), d.getDataSetId());
        DataSet d2 = createDataset();
        createAssociation(c.getConsentId(), d2.getDataSetId());

        Set<DataSet> datasets = dataSetDAO.findDatasetsForConsentId(c.getConsentId());
        assertEquals(datasets.size(), 2);
    }

    @Test
    public void testFindDatasetPropertiesByDatasetId() {
        DataSet d = createDataset();
        Set<DataSetProperty> properties = dataSetDAO.findDatasetPropertiesByDatasetId(d.getDataSetId());
        assertEquals(properties.size(), 1);
    }

    @Test
    public void testUpdateDataset() {
        DataSet d = createDataset();
        Timestamp now = new Timestamp(new Date().getTime());
        dataSetDAO.updateDatasetUpdateUserAndDate(d.getDataSetId(), now, d.getCreateUserId());
        DataSet updated = dataSetDAO.findDataSetById(d.getDataSetId());
        assertEquals(updated.getUpdateDate(), now);
    }

    @Test
    public void testUpdateDatasetProperty() {
        DataSet d = createDataset();
        Set<DataSetProperty> properties = dataSetDAO.findDatasetPropertiesByDatasetId(d.getDataSetId());
        DataSetProperty originalProperty = properties.stream().collect(Collectors.toList()).get(0);
        DataSetProperty newProperty = new DataSetProperty(d.getDataSetId(), 1, "Updated Value", new Date());
        List<DataSetProperty> updatedProperties = new ArrayList<>();
        updatedProperties.add(newProperty);
        dataSetDAO.updateDatasetProperty(d.getDataSetId(), updatedProperties.get(0).getPropertyKey(), updatedProperties.get(0).getPropertyValue());
        Set<DataSetProperty> returnedProperties = dataSetDAO.findDatasetPropertiesByDatasetId(d.getDataSetId());
        DataSetProperty returnedProperty = returnedProperties.stream().collect(Collectors.toList()).get(0);
        assertEquals(originalProperty.getPropertyKey(), returnedProperty.getPropertyKey());
        assertEquals(originalProperty.getPropertyId(), returnedProperty.getPropertyId());
        assertNotEquals(originalProperty.getPropertyValue(), returnedProperty.getPropertyValue());
    }

    @Test
    public void testDeleteDatasetPropertyByKey() {
        DataSet d = createDataset();
        Set<DataSetProperty> properties = dataSetDAO.findDatasetPropertiesByDatasetId(d.getDataSetId());
        DataSetProperty propertyToDelete = properties.stream().collect(Collectors.toList()).get(0);
        dataSetDAO.deleteDatasetPropertyByKey(d.getDataSetId(), propertyToDelete.getPropertyKey());
        Set<DataSetProperty> returnedProperties = dataSetDAO.findDatasetPropertiesByDatasetId(d.getDataSetId());
        assertNotEquals(properties.size(), returnedProperties.size());
    }
    
    @Test
    public void testFindDatasetWithPropertiesByDatasetId() {
        Dac dac = createDac();
        Consent c = createConsent(dac.getDacId());
        DataSet d = createDataset();
        createAssociation(c.getConsentId(), d.getDataSetId());
        Set<DataSetDTO> dataSetDTOs = dataSetDAO.findDataSetWithPropertiesByDataSetId(d.getDataSetId());
        DataSetDTO dataSetDTO = dataSetDTOs.stream().findFirst().orElse(null);
        assertNotNull(dataSetDTO);
        assertNotNull(dataSetDTO.getDataUse());
        assertEquals(c.getDataUse().getGeneralUse(), dataSetDTO.getDataUse().getGeneralUse());
    }

    private void createUserRole(Integer roleId, Integer userId, Integer dacId) {
        dacDAO.addDacMember(roleId, userId, dacId);
    }

}
