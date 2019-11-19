package org.broadinstitute.consent.http.db;

import com.google.common.io.Resources;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.AbstractTest;
import org.broadinstitute.consent.http.ConsentApplication;
import org.broadinstitute.consent.http.configurations.ConsentConfiguration;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.ConsentDataSet;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DataSet;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class ConsentDAOTest extends AbstractTest {

    // TODO: We have to track what we add because there are tests that rely on existing data.
    // Once those are replaced, these can be replaced with a delete all.
    private List<Integer> createdDataSetIds = new ArrayList<>();
    private List<Integer> createdDacIds = new ArrayList<>();
    private List<String> createdConsentIds = new ArrayList<>();

    private ConsentDAO consentDAO;
    private DacDAO dacDAO;
    private DataSetDAO dataSetDAO;

    @SuppressWarnings("UnstableApiUsage")
    @ClassRule
    public static final DropwizardAppRule<ConsentConfiguration> RULE = new DropwizardAppRule<>(
            ConsentApplication.class, Resources.getResource("consent-config.yml").getFile());

    @Override
    public DropwizardAppRule<ConsentConfiguration> rule() {
        return RULE;
    }

    @Before
    public void setUp() {
        consentDAO = getApplicationJdbi().onDemand(ConsentDAO.class);
        dacDAO = getApplicationJdbi().onDemand(DacDAO.class);
        dataSetDAO = getApplicationJdbi().onDemand(DataSetDAO.class);
    }

    @After
    public void tearDown() {
        // Order is important for FK constraints
        createdConsentIds.forEach(id -> {
            consentDAO.deleteAllAssociationsForConsent(id);
            consentDAO.deleteConsent(id);
        });
        dataSetDAO.deleteDataSets(createdDataSetIds);
        createdDacIds.forEach(id -> dacDAO.deleteDac(id));
    }

    @Test
    public void testFindConsentById() {
        // no-op ... tested in `createConsent()`
    }

    @Test
    public void testFindConsentFromDatasetID() {
        DataSet dataset = createDataset();
        Consent consent = createConsent(null);
        createAssociation(consent.getConsentId(), dataset.getDataSetId());

        Consent foundConsent = consentDAO.findConsentFromDatasetID(dataset.getDataSetId());
        Assert.assertNotNull(foundConsent);
    }

    @Test
    public void testFindConsentNameFromDatasetID() {
        DataSet dataset = createDataset();
        Consent consent = createConsent(null);
        createAssociation(consent.getConsentId(), dataset.getDataSetId());

        String name = consentDAO.findConsentNameFromDatasetID(dataset.getDataSetId().toString());
        Assert.assertNotNull(name);
        Assert.assertEquals(consent.getName(), name);
    }

    @Test
    public void testFindConsentsFromConsentsIDs() {
        Consent consent1 = createConsent(null);
        Consent consent2 = createConsent(null);

        Collection<Consent> consents = consentDAO.findConsentsFromConsentsIDs(Arrays.asList(
                consent1.getConsentId(),
                consent2.getConsentId()));
        Collection<String> ids = consents.stream().map(Consent::getConsentId).collect(Collectors.toList());
        Assert.assertNotNull(consents);
        Assert.assertFalse(consents.isEmpty());
        Assert.assertEquals(2, consents.size());
        Assert.assertTrue(ids.contains(consent1.getConsentId()));
        Assert.assertTrue(ids.contains(consent2.getConsentId()));
    }

    @Test
    public void testFindConsentsFromConsentNames() {
        Consent consent1 = createConsent(null);
        Consent consent2 = createConsent(null);

        Collection<Consent> consents = consentDAO.findConsentsFromConsentNames(Arrays.asList(
                consent1.getName(),
                consent2.getName()));
        Collection<String> names = consents.stream().map(Consent::getName).collect(Collectors.toList());
        Assert.assertNotNull(consents);
        Assert.assertFalse(consents.isEmpty());
        Assert.assertEquals(2, consents.size());
        Assert.assertTrue(names.contains(consent1.getName()));
        Assert.assertTrue(names.contains(consent2.getName()));
    }

    @Test
    public void testGetConsentIdAndDataSets() {
        DataSet dataset1 = createDataset();
        Consent consent1 = createConsent(null);
        createAssociation(consent1.getConsentId(), dataset1.getDataSetId());

        DataSet dataset2 = createDataset();
        Consent consent2 = createConsent(null);
        createAssociation(consent2.getConsentId(), dataset2.getDataSetId());

        Set<ConsentDataSet> consentDataSets = consentDAO.getConsentIdAndDataSets(Arrays.asList(
                dataset1.getDataSetId(), dataset2.getDataSetId()));
        Collection<String> consentIds = consentDataSets.stream().map(ConsentDataSet::getConsentId).collect(Collectors.toList());

        Assert.assertNotNull(consentDataSets);
        Assert.assertFalse(consentDataSets.isEmpty());
        Assert.assertEquals(2, consentDataSets.size());
        consentDataSets.forEach(c -> {
            Assert.assertTrue(consentIds.contains(c.getConsentId()));
        });
    }

    @Test
    public void testInsertConsent() {
        // no-op ... tested in `createConsent()`
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
