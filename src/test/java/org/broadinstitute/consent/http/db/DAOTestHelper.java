package org.broadinstitute.consent.http.db;

import static org.broadinstitute.consent.http.ConsentModule.DB_ENV;
import org.broadinstitute.consent.http.models.Institution;
import static org.junit.Assert.fail;

import io.dropwizard.jdbi3.JdbiFactory;
import io.dropwizard.setup.Environment;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.testing.DropwizardTestSupport;
import io.dropwizard.testing.ResourceHelpers;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.ConsentApplication;
import org.broadinstitute.consent.http.configurations.ConsentConfiguration;
import org.broadinstitute.consent.http.enumeration.ElectionStatus;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.enumeration.VoteType;
import org.broadinstitute.consent.http.models.ApprovalExpirationTime;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.DataSet;
import org.broadinstitute.consent.http.models.DataSetProperty;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.User;
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

public class DAOTestHelper {

    public static final String POSTGRES_IMAGE = "postgres:11.6-alpine";
    private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());
    private static final int maxConnections = 100;
    private static final ConfigOverride maxConnectionsOverride = ConfigOverride.config("database.maxSize", String.valueOf(maxConnections));

    private static DropwizardTestSupport<ConsentConfiguration> testApp;
    protected static ApprovalExpirationTimeDAO approvalExpirationTimeDAO;
    protected static ConsentDAO consentDAO;
    protected static CounterDAO counterDAO;
    protected static DacDAO dacDAO;
    protected static UserDAO userDAO;
    protected static DataSetDAO dataSetDAO;
    protected static ElectionDAO electionDAO;
    protected static UserRoleDAO userRoleDAO;
    protected static VoteDAO voteDAO;
    protected static DataAccessRequestDAO dataAccessRequestDAO;
    protected static MatchDAO matchDAO;
    protected static MailMessageDAO mailMessageDAO;
    protected static MetricsDAO metricsDAO;
    protected static UserPropertyDAO userPropertyDAO;
    protected static InstitutionDAO institutionDAO;

    private static final List<Integer> createdDataSetIds = new ArrayList<>();
    private static final List<Integer> createdDacIds = new ArrayList<>();
    private static final List<String> createdConsentIds = new ArrayList<>();
    private static final List<Integer> createdElectionIds = new ArrayList<>();
    private static final List<Integer> createdUserIds = new ArrayList<>();
    private static final List<String> createdDataAccessRequestReferenceIds = new ArrayList<>();
    private static final List<Integer> createdInstitutionIds = new ArrayList<>();

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
        approvalExpirationTimeDAO = jdbi.onDemand(ApprovalExpirationTimeDAO.class);
        consentDAO = jdbi.onDemand(ConsentDAO.class);
        counterDAO = jdbi.onDemand(CounterDAO.class);
        dacDAO = jdbi.onDemand(DacDAO.class);
        userDAO = jdbi.onDemand(UserDAO.class);
        dataSetDAO = jdbi.onDemand(DataSetDAO.class);
        electionDAO = jdbi.onDemand(ElectionDAO.class);
        userRoleDAO = jdbi.onDemand(UserRoleDAO.class);
        voteDAO = jdbi.onDemand(VoteDAO.class);
        dataAccessRequestDAO = jdbi.onDemand(DataAccessRequestDAO.class);
        matchDAO = jdbi.onDemand(MatchDAO.class);
        mailMessageDAO = jdbi.onDemand(MailMessageDAO.class);
        metricsDAO = jdbi.onDemand(MetricsDAO.class);
        userPropertyDAO = jdbi.onDemand(UserPropertyDAO.class);
        institutionDAO = jdbi.onDemand(InstitutionDAO.class);
    }

    @AfterClass
    public static void shutDown() {
        testApp.after();
    }

    @After
    public void tearDown() {
        ApprovalExpirationTime aet = approvalExpirationTimeDAO.findApprovalExpirationTime();
        if (Objects.nonNull(aet) && aet.getId() > 0) {
            approvalExpirationTimeDAO.deleteApprovalExpirationTime(aet.getId());
        }
        // Order is important for FK constraints
        createdConsentIds.forEach(id -> {
            matchDAO.deleteMatchByConsentId(id);
            voteDAO.deleteVotes(id);
            consentDAO.deleteAllAssociationsForConsent(id);
            consentDAO.deleteConsent(id);
        });
        createdElectionIds.forEach(id -> electionDAO.deleteAccessRP(id));
        createdElectionIds.forEach(id -> electionDAO.deleteElectionById(id));
        dataSetDAO.deleteDataSetsProperties(createdDataSetIds);
        dataSetDAO.deleteDataSets(createdDataSetIds);
        createdDacIds.forEach(id -> {
            dacDAO.deleteDacMembers(id);
            dacDAO.deleteDac(id);
        });
        createdInstitutionIds.forEach(id -> {
            institutionDAO.deleteInstitutionById(id);
        });
        createdUserIds.forEach(id -> {
            userPropertyDAO.deleteAllPropertiesByUser(id);
            userRoleDAO.findRolesByUserId(id).
                    forEach(ur -> userRoleDAO.removeSingleUserRole(ur.getUserId(), ur.getRoleId()));
            userDAO.deleteUserById(id);
        });
        createdDataAccessRequestReferenceIds.forEach(d ->
                dataAccessRequestDAO.deleteByReferenceId(d));
        counterDAO.deleteAll();
    }

    protected void createAssociation(String consentId, Integer datasetId) {
        consentDAO.insertConsentAssociation(consentId, ASSOCIATION_TYPE_TEST, datasetId);
    }

    protected Election createAccessElection(String referenceId, Integer datasetId) {
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

    protected Election createExtendedElection(String referenceId, Integer datasetId) {
        Integer electionId = electionDAO.insertElection(
                ElectionType.DATA_ACCESS.getValue(),
                ElectionStatus.OPEN.getValue(),
                new Date(),
                referenceId,
                Boolean.TRUE,
                "dataUseLetter",
                "dulName",
                datasetId
        );
        createdElectionIds.add(electionId);
        return electionDAO.findElectionById(electionId);
    }

    protected Election createRPElection(String referenceId, Integer datasetId) {
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

    protected Election createDULElection(String referenceId, Integer datasetId) {
        Integer electionId = electionDAO.insertElection(
                ElectionType.TRANSLATE_DUL.getValue(),
                ElectionStatus.OPEN.getValue(),
                new Date(),
                referenceId,
                datasetId
        );
        createdElectionIds.add(electionId);
        return electionDAO.findElectionById(electionId);
    }

    protected Election createDatasetElection(String referenceId, Integer datasetId) {
        Integer electionId = electionDAO.insertElection(
                ElectionType.DATA_SET.getValue(),
                ElectionStatus.OPEN.getValue(),
                new Date(),
                referenceId,
                datasetId
        );
        createdElectionIds.add(electionId);
        return electionDAO.findElectionById(electionId);
    }

    protected void closeElection(Election election) {
        electionDAO.updateElectionById(
                election.getElectionId(),
                ElectionStatus.CLOSED.getValue(),
                new Date());
    }

    protected Vote createDacVote(Integer userId, Integer electionId) {
        Integer voteId = voteDAO.insertVote(userId, electionId, VoteType.DAC.getValue());
        return voteDAO.findVoteById(voteId);
    }

    protected Vote createFinalVote(Integer userId, Integer electionId) {
        Integer voteId = voteDAO.insertVote(userId, electionId, VoteType.FINAL.getValue());
        return voteDAO.findVoteById(voteId);
    }

    protected Vote createPopulatedFinalVote(Integer userId, Integer electionId) {
        Integer voteId = voteDAO.insertVote(userId, electionId, VoteType.FINAL.getValue());
        voteDAO.updateVote(true, "rationale", new Date(), voteId, false, electionId, new Date(), false);
        return voteDAO.findVoteById(voteId);
    }

    protected Vote createChairpersonVote(Integer userId, Integer electionId) {
        Integer voteId = voteDAO.insertVote(userId, electionId, VoteType.CHAIRPERSON.getValue());
        return voteDAO.findVoteById(voteId);
    }

    protected Vote createPopulatedChairpersonVote(Integer userId, Integer electionId) {
        Integer voteId = voteDAO.insertVote(userId, electionId, VoteType.CHAIRPERSON.getValue());
        voteDAO.updateVote(true, "rationale", new Date(), voteId, false, electionId, new Date(), false);
        return voteDAO.findVoteById(voteId);
    }

    protected Vote createPopulatedDataOwnerVote(Integer userId, Integer electionId) {
        Integer voteId = voteDAO.insertVote(userId, electionId, VoteType.DATA_OWNER.getValue());
        voteDAO.updateVote(true, "rationale", new Date(), voteId, false, electionId, new Date(), false);
        return voteDAO.findVoteById(voteId);
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
                "Group",
                dacId);
        createdConsentIds.add(consentId);
        return consentDAO.findConsentById(consentId);
    }

    protected User createUser() {
        int i1 = RandomUtils.nextInt(5, 10);
        int i2 = RandomUtils.nextInt(5, 10);
        int i3 = RandomUtils.nextInt(3, 5);
        String email = RandomStringUtils.randomAlphabetic(i1) +
                "@" +
                RandomStringUtils.randomAlphabetic(i2) +
                "." +
                RandomStringUtils.randomAlphabetic(i3);
        Integer userId = userDAO.insertUser(email, "display name", new Date());
        createdUserIds.add(userId);
        return userDAO.findUserById(userId);
    }

    protected User createUserWithRole(Integer roleId) {
        int i1 = RandomUtils.nextInt(5, 10);
        int i2 = RandomUtils.nextInt(5, 10);
        int i3 = RandomUtils.nextInt(3, 5);
        String email = RandomStringUtils.randomAlphabetic(i1) +
                "@" +
                RandomStringUtils.randomAlphabetic(i2) +
                "." +
                RandomStringUtils.randomAlphabetic(i3);
        Integer userId = userDAO.insertUser(email, "display name", new Date());
        addUserRole(roleId, userId);
        createdUserIds.add(userId);
        return userDAO.findUserById(userId);
    }

    protected void addUserRole(int roleId, int userId) {
        userRoleDAO.insertSingleUserRole(roleId, userId);
    }

    protected User createUserWithRoleInDac(Integer roleId, Integer dacId) {
        User user = createUserWithRole(roleId);
        dacDAO.addDacMember(roleId, user.getDacUserId(), dacId);
        return user;
    }

    protected Dac createDac() {
        Integer id = dacDAO.createDac(
                "Test_" + RandomStringUtils.random(20, true, true),
                "Test_" + RandomStringUtils.random(20, true, true),
                new Date());
        createdDacIds.add(id);
        return dacDAO.findById(id);
    }

    protected Institution createInstitution() {
        Integer userId = createUser().getDacUserId();
        String email = RandomStringUtils.randomAlphabetic(6) +
                "@" +
                RandomStringUtils.randomAlphabetic(6) +
                "." +
                RandomStringUtils.randomAlphabetic(3);
        Integer id = institutionDAO.insertInstitution(
          "Test_" + RandomStringUtils.random(20, true, true),
          "Test_" + RandomStringUtils.random(20, true, true),
          email,
          userId,
          new Date());
        createdInstitutionIds.add(id);
        return institutionDAO.findInstitutionById(id);
    }

    protected void createDatasetProperties(Integer datasetId) {
        List<DataSetProperty> list = new ArrayList<>();
        DataSetProperty dsp = new DataSetProperty();
        dsp.setDataSetId(datasetId);
        dsp.setPropertyKey(1);
        dsp.setPropertyValue("Test_PropertyValue");
        dsp.setCreateDate(new Date());
        list.add(dsp);
        dataSetDAO.insertDataSetsProperties(list);
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
        createDatasetProperties(id);
        return dataSetDAO.findDataSetById(id);
    }

    protected Date yesterday() {
        final Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -1);
        return cal.getTime();
    }

    protected DataAccessRequest createDataAccessRequestV2() {
        DataAccessRequestData data;
        Date now = new Date();
        Integer userId = createUser().getDacUserId();
        try {
            String darDataString = FileUtils.readFileToString(
                    new File(ResourceHelpers.resourceFilePath("dataset/dar.json")),
                    Charset.defaultCharset());
            data = DataAccessRequestData.fromString(darDataString);
            String referenceId = UUID.randomUUID().toString();
            dataAccessRequestDAO.insertVersion2(referenceId, userId, now, now, now, now, data);
            createdDataAccessRequestReferenceIds.add(referenceId);
            return dataAccessRequestDAO.findByReferenceId(referenceId);
        } catch (IOException e) {
            logger.error("Exception parsing dar data: " + e.getMessage());
            fail("Unable to create a Data Access Request from sample data: " + e.getMessage());
        }
        return null;
    }

    protected DataAccessRequest createDraftDataAccessRequest() {
        DataAccessRequestData data;
        try {
            String darDataString = FileUtils.readFileToString(
                    new File(ResourceHelpers.resourceFilePath("dataset/dar.json")),
                    Charset.defaultCharset());
            data = DataAccessRequestData.fromString(darDataString);
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
