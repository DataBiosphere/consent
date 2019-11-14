package org.broadinstitute.consent.http.db;

import com.google.common.io.Resources;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.AbstractTest;
import org.broadinstitute.consent.http.ConsentApplication;
import org.broadinstitute.consent.http.configurations.ConsentConfiguration;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.DACUser;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DataSet;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class DataSetDAOTest extends AbstractTest {

    // TODO: We have to track what we add because there are tests that rely on existing data.
    // Once those are replaced, these can be replaced with a delete all.
    private List<Integer> createdDataSetIds = new ArrayList<>();
    private List<Integer> createdDacIds = new ArrayList<>();
    private List<String> createdConsentIds = new ArrayList<>();
    private List<String> createdDacUserEmails = new ArrayList<>();

    @SuppressWarnings("UnstableApiUsage")
    @ClassRule
    public static final DropwizardAppRule<ConsentConfiguration> RULE = new DropwizardAppRule<>(
            ConsentApplication.class, Resources.getResource("consent-config.yml").getFile());

    @Override
    public DropwizardAppRule<ConsentConfiguration> rule() {
        return RULE;
    }

    private DacDAO dacDAO;
    private DACUserDAO dacUserDAO;
    private ConsentDAO consentDAO;
    private DataSetDAO dataSetDAO;
    private UserRoleDAO userRoleDAO;

    @Before
    public void setUp() {
        dacDAO = getApplicationJdbi().onDemand(DacDAO.class);
        dacUserDAO = getApplicationJdbi().onDemand(DACUserDAO.class);
        dataSetDAO = getApplicationJdbi().onDemand(DataSetDAO.class);
        consentDAO = getApplicationJdbi().onDemand(ConsentDAO.class);
        userRoleDAO = getApplicationJdbi().onDemand(UserRoleDAO.class);
    }

    @After
    public void tearDown() {
        // Order is important for FK constraints
        createdConsentIds.forEach(id -> {
            consentDAO.deleteAllAssociationsForConsent(id);
            consentDAO.deleteConsent(id);
        });
        dataSetDAO.deleteDataSets(createdDataSetIds);
        createdDacUserEmails.forEach(email -> {
            userRoleDAO.findRolesByUserEmail(email).
                    forEach(ur -> userRoleDAO.removeSingleUserRole(ur.getUserId(), ur.getRoleId()));
            dacUserDAO.deleteDACUserByEmail(email);
        });
        createdDacIds.forEach(id -> dacDAO.deleteDac(id));
    }

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
        DACUser user = createDacUser();
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

    private void createUserRole(Integer roleId, Integer userId, Integer dacId) {
        dacDAO.addDacMember(roleId, userId, dacId);
    }

    private DACUser createDacUser() {
        String name = RandomStringUtils.random(10, true, false);
        String email = name + "@test.org";
        createdDacUserEmails.add(email);
        Integer id = dacUserDAO.insertDACUser(email, name, new Date());
        return dacUserDAO.findDACUserById(id);
    }

    private void createAssociation(String consentId, Integer datasetId) {
        consentDAO.insertConsentAssociation(consentId, "sampleSet", datasetId);
    }

    private Consent createConsent(Integer dacId) {
        String consentId = UUID.randomUUID().toString();
        consentDAO.insertConsent(consentId,
                false,
                "{\"type\":\"everything\"}",
                "{\"generalUse\": true }",
                "dul",
                consentId,
                "dulName",
                new Date(),
                new Date(),
                "Everything",
                true,
                "Group",
                dacId);
        createdConsentIds.add(consentId);
        return consentDAO.findConsentById(consentId);
    }

    private Dac createDac() {
        Integer id = dacDAO.createDac(
                "Test_" + RandomStringUtils.random(20, true, true),
                "Test_" + RandomStringUtils.random(20, true, true),
                new Date());
        createdDacIds.add(id);
        return dacDAO.findById(id);
    }
    private DataSet createDataset() {
        DataSet ds = new DataSet();
        ds.setName("Name_" + RandomStringUtils.random(20, true, true));
        ds.setCreateDate(new Date());
        ds.setObjectId("Object ID_" + RandomStringUtils.random(20, true, true));
        ds.setActive(true);
        ds.setAlias(RandomUtils.nextInt(1, 1000));
        Integer id = dataSetDAO.insertDataset(ds.getName(), ds.getCreateDate(), ds.getObjectId(), ds.getActive(), ds.getAlias());
        createdDataSetIds.add(id);
        return dataSetDAO.findDataSetById(id);
    }

}
