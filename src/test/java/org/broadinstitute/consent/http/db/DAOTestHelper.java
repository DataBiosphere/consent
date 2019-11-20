package org.broadinstitute.consent.http.db;

import com.google.common.io.Resources;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.AbstractTest;
import org.broadinstitute.consent.http.ConsentApplication;
import org.broadinstitute.consent.http.configurations.ConsentConfiguration;
import org.broadinstitute.consent.http.enumeration.ElectionStatus;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DataSet;
import org.broadinstitute.consent.http.models.Election;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class DAOTestHelper extends AbstractTest {

    protected List<Integer> createdDataSetIds = new ArrayList<>();
    protected List<Integer> createdDacIds = new ArrayList<>();
    protected List<String> createdConsentIds = new ArrayList<>();
    protected List<Integer> createdElectionIds = new ArrayList<>();

    protected ConsentDAO consentDAO;
    protected DacDAO dacDAO;
    protected DataSetDAO dataSetDAO;
    protected ElectionDAO electionDAO;

    protected String ASSOCIATION_TYPE_TEST;

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
        electionDAO = getApplicationJdbi().onDemand(ElectionDAO.class);
        ASSOCIATION_TYPE_TEST = RandomStringUtils.random(10, true, false);
    }

    @After
    public void tearDown() {
        // Order is important for FK constraints
        createdConsentIds.forEach(id -> {
            consentDAO.deleteAllAssociationsForConsent(id);
            consentDAO.deleteConsent(id);
        });
        createdElectionIds.forEach(id -> electionDAO.deleteElectionById(id));
        dataSetDAO.deleteDataSets(createdDataSetIds);
        createdDacIds.forEach(id -> dacDAO.deleteDac(id));
    }

    protected void createAssociation(String consentId, Integer datasetId) {
        consentDAO.insertConsentAssociation(consentId, ASSOCIATION_TYPE_TEST, datasetId);
    }

    protected Election createElection(String referenceId, Integer datasetId) {
        Integer electionId = electionDAO.insertElection(
                ElectionType.DATA_ACCESS.getValue(),
                ElectionStatus.OPEN.getValue(),
                new Date(),
                referenceId,
                datasetId
        );
        createdElectionIds.add(electionId);
        return electionDAO.findElectionById(electionId);
    }

    @SuppressWarnings("SameParameterValue")
    protected Consent createConsent(Integer dacId) {
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

    protected Dac createDac() {
        Integer id = dacDAO.createDac(
                "Test_" + RandomStringUtils.random(20, true, true),
                "Test_" + RandomStringUtils.random(20, true, true),
                new Date());
        createdDacIds.add(id);
        return dacDAO.findById(id);
    }

    protected DataSet createDataset() {
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

    protected Date yesterday() {
        final Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -1);
        return cal.getTime();
    }

}
