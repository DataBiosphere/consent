package org.broadinstitute.consent.http.db;

import io.dropwizard.jdbi3.JdbiFactory;
import io.dropwizard.setup.Environment;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.testing.DropwizardTestSupport;
import io.dropwizard.testing.ResourceHelpers;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.ConsentApplication;
import org.broadinstitute.consent.http.configurations.ConsentConfiguration;
import org.broadinstitute.consent.http.enumeration.ElectionStatus;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.enumeration.VoteType;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.DACUser;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.DataSet;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.Vote;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.gson2.Gson2Plugin;
import org.jdbi.v3.guava.GuavaPlugin;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.PostgreSQLContainer;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.broadinstitute.consent.http.ConsentModule.DB_ENV;
import static org.junit.Assert.fail;

public class DAOTestHelper {

    private Logger logger = LoggerFactory.getLogger(this.getClass().getName());
    public static final String POSTGRES_IMAGE = "postgres:11.6-alpine";
    private static final int maxConnections = 100;
    private static ConfigOverride maxConnectionsOverride = ConfigOverride.config("database.maxSize", String.valueOf(maxConnections));

    private static DropwizardTestSupport<ConsentConfiguration> testApp;
    static ConsentDAO consentDAO;
    static CounterDAO counterDAO;
    static DacDAO dacDAO;
    static DACUserDAO userDAO;
    static DataSetDAO dataSetDAO;
    static ElectionDAO electionDAO;
    static UserRoleDAO userRoleDAO;
    static VoteDAO voteDAO;
    static DataAccessRequestDAO dataAccessRequestDAO;
    static MailMessageDAO mailMessageDAO;
    static ResearcherPropertyDAO researcherPropertyDAO;

    private static List<Integer> createdDataSetIds = new ArrayList<>();
    private static List<Integer> createdDacIds = new ArrayList<>();
    private static List<String> createdConsentIds = new ArrayList<>();
    private static List<Integer> createdElectionIds = new ArrayList<>();
    private static List<Integer> createdUserIds = new ArrayList<>();
    private static List<String> createdDataAccessRequestReferenceIds = new ArrayList<>();

    String ASSOCIATION_TYPE_TEST = RandomStringUtils.random(10, true, false);

    @BeforeClass
    public static void startUp() throws Exception {
        // Start the database
        @SuppressWarnings("rawtypes")
        PostgreSQLContainer postgres = new PostgreSQLContainer<>(POSTGRES_IMAGE).
                withCommand("postgres -c max_connections=" + maxConnections);
        postgres.start();
        ConfigOverride driverOverride = ConfigOverride.config("database.driverClass", postgres.getDriverClassName());
        ConfigOverride urlOverride = ConfigOverride.config("database.url", postgres.getJdbcUrl());
        ConfigOverride userOverride = ConfigOverride.config("database.user", postgres.getUsername());
        ConfigOverride passwordOverride = ConfigOverride.config("database.password", postgres.getPassword());
        ConfigOverride validationQueryOverride = ConfigOverride.config("database.validationQuery", postgres.getTestQueryString());

        // Start the app
        testApp = new DropwizardTestSupport<>(
                ConsentApplication.class,
                ResourceHelpers.resourceFilePath("consent-config.yml"),
                driverOverride, urlOverride,
                userOverride, passwordOverride,
                validationQueryOverride,
                maxConnectionsOverride);
        testApp.before();

        // Initialize DAOs
        String dbiExtension = "_" + RandomStringUtils.random(10, true, false);
        ConsentConfiguration configuration = testApp.getConfiguration();
        Environment environment = testApp.getEnvironment();
        Jdbi jdbi = new JdbiFactory().build(environment, configuration.getDataSourceFactory(), DB_ENV + dbiExtension);
        jdbi.installPlugin(new SqlObjectPlugin());
        jdbi.installPlugin(new Gson2Plugin());
        jdbi.installPlugin(new GuavaPlugin());
        consentDAO = jdbi.onDemand(ConsentDAO.class);
        counterDAO = jdbi.onDemand(CounterDAO.class);
        dacDAO = jdbi.onDemand(DacDAO.class);
        userDAO = jdbi.onDemand(DACUserDAO.class);
        dataSetDAO = jdbi.onDemand(DataSetDAO.class);
        electionDAO = jdbi.onDemand(ElectionDAO.class);
        userRoleDAO = jdbi.onDemand(UserRoleDAO.class);
        voteDAO = jdbi.onDemand(VoteDAO.class);
        dataAccessRequestDAO = jdbi.onDemand(DataAccessRequestDAO.class);
        mailMessageDAO = jdbi.onDemand(MailMessageDAO.class);
        researcherPropertyDAO = jdbi.onDemand(ResearcherPropertyDAO.class);
    }

    @AfterClass
    public static void shutDown() {
        testApp.after();
    }

    @After
    public void tearDown() {
        // Order is important for FK constraints
        createdConsentIds.forEach(id -> {
            voteDAO.deleteVotes(id);
            consentDAO.deleteAllAssociationsForConsent(id);
            consentDAO.deleteConsent(id);
        });
        createdElectionIds.forEach(id -> electionDAO.deleteAccessRP(id));
        createdElectionIds.forEach(id -> electionDAO.deleteElectionById(id));
        dataSetDAO.deleteDataSets(createdDataSetIds);
        createdDacIds.forEach(id -> {
            dacDAO.deleteDacMembers(id);
            dacDAO.deleteDac(id);
        });
        createdUserIds.forEach(id -> {
            researcherPropertyDAO.deleteAllPropertiesByUser(id);
            userRoleDAO.findRolesByUserId(id).
                    forEach(ur -> userRoleDAO.removeSingleUserRole(ur.getUserId(), ur.getRoleId()));
            userDAO.deleteDACUserById(id);
        });
        createdDataAccessRequestReferenceIds.forEach(d ->
                dataAccessRequestDAO.deleteByReferenceId(d));
        counterDAO.deleteAll();
    }

    void createAssociation(String consentId, Integer datasetId) {
        consentDAO.insertConsentAssociation(consentId, ASSOCIATION_TYPE_TEST, datasetId);
    }

    Election createElection(String referenceId, Integer datasetId) {
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

    Election createRPElection(String referenceId, Integer datasetId) {
        Integer electionId = electionDAO.insertElection(
                ElectionType.RP.getValue(),
                ElectionStatus.OPEN.getValue(),
                new Date(),
                referenceId,
                datasetId
        );
        createdElectionIds.add(electionId);
        return electionDAO.findElectionById(electionId);
    }

    void closeElection(Election election) {
        electionDAO.updateElectionById(
                election.getElectionId(),
                ElectionStatus.CLOSED.getValue(),
                new Date());
    }

    Vote createDacVote(Integer userId, Integer electionId) {
        Integer voteId = voteDAO.insertVote(userId, electionId, VoteType.DAC.getValue());
        return voteDAO.findVoteById(voteId);
    }

    Vote createFinalVote(Integer userId, Integer electionId) {
        Integer voteId = voteDAO.insertVote(userId, electionId, VoteType.FINAL.getValue());
        return voteDAO.findVoteById(voteId);
    }

    Vote createChairpersonVote(Integer userId, Integer electionId) {
        Integer voteId = voteDAO.insertVote(userId, electionId, VoteType.CHAIRPERSON.getValue());
        return voteDAO.findVoteById(voteId);
    }

    @SuppressWarnings("SameParameterValue")
    Consent createConsent(Integer dacId) {
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

    DACUser createUser() {
        int i1 = RandomUtils.nextInt(5, 10);
        int i2 = RandomUtils.nextInt(5, 10);
        int i3 = RandomUtils.nextInt(3, 5);
        String email = RandomStringUtils.randomAlphabetic(i1) +
                "@" +
                RandomStringUtils.randomAlphabetic(i2) +
                "." +
                RandomStringUtils.randomAlphabetic(i3);
        Integer userId = userDAO.insertDACUser(email, "display name", new Date());
        createdUserIds.add(userId);
        return userDAO.findDACUserById(userId);
    }

    DACUser createUserWithRole(Integer roleId) {
        int i1 = RandomUtils.nextInt(5, 10);
        int i2 = RandomUtils.nextInt(5, 10);
        int i3 = RandomUtils.nextInt(3, 5);
        String email = RandomStringUtils.randomAlphabetic(i1) +
                "@" +
                RandomStringUtils.randomAlphabetic(i2) +
                "." +
                RandomStringUtils.randomAlphabetic(i3);
        Integer userId = userDAO.insertDACUser(email, "display name", new Date());
        userRoleDAO.insertSingleUserRole(roleId, userId);
        createdUserIds.add(userId);
        return userDAO.findDACUserById(userId);
    }

    DACUser createUserWithRoleInDac(Integer roleId, Integer dacId) {
        DACUser user = createUserWithRole(roleId);
        dacDAO.addDacMember(roleId, user.getDacUserId(), dacId);
        return user;
    }

    Dac createDac() {
        Integer id = dacDAO.createDac(
                "Test_" + RandomStringUtils.random(20, true, true),
                "Test_" + RandomStringUtils.random(20, true, true),
                new Date());
        createdDacIds.add(id);
        return dacDAO.findById(id);
    }

    DataSet createDataset() {
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

    Date yesterday() {
        final Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -1);
        return cal.getTime();
    }

    DataAccessRequest createDataAccessRequest() {
        DataAccessRequestData data;
        try {
            String darDataString = FileUtils.readFileToString(
                    new File(ResourceHelpers.resourceFilePath("dataset/dar.json")),
                    Charset.defaultCharset());
            data = DataAccessRequestData.fromString(darDataString);
            String referenceId = UUID.randomUUID().toString();
            dataAccessRequestDAO.insert(referenceId, data);
            createdDataAccessRequestReferenceIds.add(referenceId);
            return dataAccessRequestDAO.findByReferenceId(referenceId);
        } catch (IOException e) {
            logger.error("Exception parsing dar data: " + e.getMessage());
            fail("Unable to create a Data Access Request from sample data: " + e.getMessage());
        }
        return null;
    }

    DataAccessRequest createDraftDataAccessRequest() {
        DataAccessRequestData data;
        try {
            String darDataString = FileUtils.readFileToString(
                    new File(ResourceHelpers.resourceFilePath("dataset/dar.json")),
                    Charset.defaultCharset());
            data = DataAccessRequestData.fromString(darDataString);
            data.setPartialDarCode("temp_" + data.getDarCode());
            String referenceId = UUID.randomUUID().toString();
            dataAccessRequestDAO.insertDraft(referenceId, data);
            createdDataAccessRequestReferenceIds.add(referenceId);
            return dataAccessRequestDAO.findByReferenceId(referenceId);
        } catch (IOException e) {
            logger.error("Exception parsing dar data: " + e.getMessage());
            fail("Unable to create a Data Access Request from sample data: " + e.getMessage());
        }
        return null;
    }

}
