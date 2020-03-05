package org.broadinstitute.consent.http.db;

import org.apache.commons.lang3.tuple.Pair;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.DACUser;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DataSet;
import org.broadinstitute.consent.http.models.dto.DataSetDTO;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class DataSetDAOTest extends DAOTestFramework {

    @Test
    public void testInsertDataset() {
        // No-op ... tested in `createDataset()`
    }

    @Test
    public void testFindDatasetById() {
        // No-op ... tested in `createDataset()`
    }

    @Test
    public void testDeleteDataSets() {
        // No-op ... tested in `tearDown()`
    }

    // User -> UserRoles -> DACs -> Consents -> Consent Associations -> DataSets
    @Test
    public void testFindDataSetsByAuthUserEmail() {
        DataSet dataset = createDataset();
        Dac dac = createDac();
        Consent consent = createConsent(dac.getDacId());
        createAssociation(consent.getConsentId(), dataset.getDataSetId());
        DACUser user = createUser();
        createUserRole(UserRoles.CHAIRPERSON.getRoleId(), user.getDacUserId(), dac.getDacId());

        List<DataSet> datasets = dataSetDAO.findDataSetsByAuthUserEmail(user.getEmail());
        Assert.assertFalse(datasets.isEmpty());
        List<Integer> datasetIds = datasets.stream().map(DataSet::getDataSetId).collect(Collectors.toList());
        Assert.assertTrue(datasetIds.contains(dataset.getDataSetId()));
    }

    @Test
    public void testFindNonDACDataSets() {
        DataSet dataset = createDataset();
        Consent consent = createConsent(null);
        createAssociation(consent.getConsentId(), dataset.getDataSetId());

        List<DataSet> datasets = dataSetDAO.findNonDACDataSets();
        Assert.assertFalse(datasets.isEmpty());
        List<Integer> datasetIds = datasets.stream().map(DataSet::getDataSetId).collect(Collectors.toList());
        Assert.assertTrue(datasetIds.contains(dataset.getDataSetId()));
    }

    @Test
    public void testFindDatasetsByDac() {
        DataSet dataset = createDataset();
        DataSet dataset2 = createDataset();
        Dac dac = createDac();
        Consent consent = createConsent(dac.getDacId());
        createAssociation(consent.getConsentId(), dataset.getDataSetId());

        Set<DataSetDTO> datasets = dataSetDAO.findDatasetsByDac(dac.getDacId());
        Assert.assertFalse(datasets.isEmpty());
        List<Integer> datasetIds = datasets.stream().map(DataSetDTO::getDataSetId).collect(Collectors.toList());
        Assert.assertTrue(datasetIds.contains(dataset.getDataSetId()));
        Assert.assertFalse(datasetIds.contains(dataset2.getDataSetId()));
    }

    @Test
    public void testFindDatasetAndDacIds() {
        DataSet dataset = createDataset();
        Dac dac = createDac();
        Consent consent = createConsent(dac.getDacId());
        createAssociation(consent.getConsentId(), dataset.getDataSetId());

        List<Pair<Integer, Integer>> pairs = dataSetDAO.findDatasetAndDacIds();
        Assert.assertFalse(pairs.isEmpty());
        Assert.assertEquals(1, pairs.size());
        Assert.assertEquals(pairs.get(0).getLeft(), dataset.getDataSetId());
        Assert.assertEquals(pairs.get(0).getRight(), dac.getDacId());
    }

    @Test
    public void testFindDacForDataset() {
        DataSet dataset = createDataset();
        Dac dac = createDac();
        Consent consent = createConsent(dac.getDacId());
        createAssociation(consent.getConsentId(), dataset.getDataSetId());

        Dac foundDac = dataSetDAO.findDacForDataset(dataset.getDataSetId());
        Assert.assertNotNull(foundDac);
        Assert.assertEquals(dac.getDacId(), foundDac.getDacId());
    }

    @Test
    public void testFindDacForDatasetNotFound() {
        DataSet dataset = createDataset();

        Dac foundDac = dataSetDAO.findDacForDataset(dataset.getDataSetId());
        Assert.assertNull(foundDac);
    }

    @Test
    public void testFindDatasetsForConsentId_case0() {
        Dac dac = createDac();
        Consent c = createConsent(dac.getDacId());

        Set<DataSet> datasets = dataSetDAO.findDatasetsForConsentId(c.getConsentId());
        Assert.assertEquals(datasets.size(), 0);
    }

    @Test
    public void testFindDatasetsForConsentId_case1() {
        Dac dac = createDac();
        Consent c = createConsent(dac.getDacId());
        DataSet d = createDataset();
        createAssociation(c.getConsentId(), d.getDataSetId());

        Set<DataSet> datasets = dataSetDAO.findDatasetsForConsentId(c.getConsentId());
        Assert.assertEquals(datasets.size(), 1);
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
        Assert.assertEquals(datasets.size(), 2);
    }

    private void createUserRole(Integer roleId, Integer userId, Integer dacId) {
        dacDAO.addDacMember(roleId, userId, dacId);
    }

}
