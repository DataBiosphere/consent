package org.broadinstitute.consent.http.db;

import io.dropwizard.jdbi3.JdbiFactory;
import io.dropwizard.setup.Environment;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.testing.DropwizardTestSupport;
import io.dropwizard.testing.ResourceHelpers;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.ConsentApplication;
import org.broadinstitute.consent.http.configurations.ConsentConfiguration;
import org.broadinstitute.consent.http.enumeration.ElectionStatus;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.enumeration.MatchAlgorithm;
import org.broadinstitute.consent.http.enumeration.UserFields;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.enumeration.VoteType;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DarCollection;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.models.DataUseBuilder;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.DatasetEntry;
import org.broadinstitute.consent.http.models.DatasetProperty;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.Institution;
import org.broadinstitute.consent.http.models.LibraryCard;
import org.broadinstitute.consent.http.models.Match;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserProperty;
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

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.broadinstitute.consent.http.ConsentModule.DB_ENV;

public class DAOTestHelper {

    public static final String POSTGRES_IMAGE = "postgres:11.6-alpine";
    private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());
    private static final int maxConnections = 100;
    private static final ConfigOverride maxConnectionsOverride = ConfigOverride.config("database.maxSize", String.valueOf(maxConnections));

    private static DropwizardTestSupport<ConsentConfiguration> testApp;

    protected static Jdbi jdbi;

    protected static ConsentAuditDAO consentAuditDAO;

    protected static ConsentDAO consentDAO;
    protected static CounterDAO counterDAO;
    protected static DacDAO dacDAO;
    protected static UserDAO userDAO;
    protected static DatasetDAO datasetDAO;
    protected static ElectionDAO electionDAO;
    protected static UserRoleDAO userRoleDAO;
    protected static VoteDAO voteDAO;
    protected static DataAccessRequestDAO dataAccessRequestDAO;
    protected static MatchDAO matchDAO;
    protected static MailMessageDAO mailMessageDAO;
    protected static UserPropertyDAO userPropertyDAO;
    protected static InstitutionDAO institutionDAO;
    protected static LibraryCardDAO libraryCardDAO;
    protected static DarCollectionDAO darCollectionDAO;
    protected static DarCollectionSummaryDAO darCollectionSummaryDAO;

    // This is a test-only DAO class where we manage the deletion
    // of all records between test runs.
    private static TestingDAO testingDAO;

    public String ASSOCIATION_TYPE_TEST = RandomStringUtils.random(10, true, false);

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
        jdbi = new JdbiFactory().build(environment, configuration.getDataSourceFactory(), DB_ENV + dbiExtension);
        jdbi.installPlugin(new SqlObjectPlugin());
        jdbi.installPlugin(new Gson2Plugin());
        jdbi.installPlugin(new GuavaPlugin());
        consentAuditDAO = jdbi.onDemand(ConsentAuditDAO.class);
        consentDAO = jdbi.onDemand(ConsentDAO.class);
        counterDAO = jdbi.onDemand(CounterDAO.class);
        dacDAO = jdbi.onDemand(DacDAO.class);
        userDAO = jdbi.onDemand(UserDAO.class);
        datasetDAO = jdbi.onDemand(DatasetDAO.class);
        electionDAO = jdbi.onDemand(ElectionDAO.class);
        userRoleDAO = jdbi.onDemand(UserRoleDAO.class);
        voteDAO = jdbi.onDemand(VoteDAO.class);
        dataAccessRequestDAO = jdbi.onDemand(DataAccessRequestDAO.class);
        matchDAO = jdbi.onDemand(MatchDAO.class);
        mailMessageDAO = jdbi.onDemand(MailMessageDAO.class);
        userPropertyDAO = jdbi.onDemand(UserPropertyDAO.class);
        institutionDAO = jdbi.onDemand(InstitutionDAO.class);
        libraryCardDAO = jdbi.onDemand(LibraryCardDAO.class);
        darCollectionDAO = jdbi.onDemand(DarCollectionDAO.class);
        darCollectionSummaryDAO = jdbi.onDemand(DarCollectionSummaryDAO.class);
        testingDAO = jdbi.onDemand(TestingDAO.class);
    }

    @AfterClass
    public static void shutDown() {
        testApp.after();
    }

    @After
    public void tearDown() {
        // Order is important for FK constraints
        testingDAO.deleteAllDARDataset();
        testingDAO.deleteAllApprovalTimes();
        testingDAO.deleteAllVotes();
        testingDAO.deleteAllConsentAudits();
        testingDAO.deleteAllMatchEntityFailureReasons();
        testingDAO.deleteAllMatchEntities();
        testingDAO.deleteAllConsentAssociations();
        testingDAO.deleteAllConsents();
        testingDAO.deleteAllAccessRps();
        testingDAO.deleteAllElections();
        testingDAO.deleteAllDatasetProperties();
        testingDAO.deleteAllDatasets();
        testingDAO.deleteAllDacUserRoles();
        testingDAO.deleteAllDacs();
        testingDAO.deleteAllLibraryCards();
        testingDAO.deleteAllInstitutions();
        testingDAO.deleteAllUserProperties();
        testingDAO.deleteAllUserRoles();
        testingDAO.deleteAllUsers();
        testingDAO.deleteAllDARs();
        testingDAO.deleteAllDARCollections();
        testingDAO.deleteAllCounters();
    }

    /*
       Utility methods in this class need to be complete from the perspective of the
       entity. When testing, if you need a specific modification to an object, call
       dao methods directly to do any manipulation.
     */

    /**
     * Create a DataAccess Election with "Open" status.
     *
     * @param referenceId A DAR's reference id
     * @param datasetId A dataset id
     * @return DataAccess Election
     */
    protected Election createDataAccessElection(String referenceId, Integer datasetId) {
        Integer electionId = electionDAO.insertElection(
                ElectionType.DATA_ACCESS.getValue(),
                ElectionStatus.OPEN.getValue(),
                new Date(),
                referenceId,
                datasetId
        );
        return electionDAO.findElectionById(electionId);
    }

    protected Election createCancelledAccessElection(String referenceId, Integer datasetId) {
        Integer electionId = electionDAO.insertElection(
                ElectionType.DATA_ACCESS.getValue(),
                ElectionStatus.CANCELED.getValue(),
                new Date(),
                referenceId,
                datasetId
        );
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
        return electionDAO.findElectionById(electionId);
    }

    protected Vote createDacVote(Integer userId, Integer electionId) {
        Integer voteId = voteDAO.insertVote(userId, electionId, VoteType.DAC.getValue());
        return voteDAO.findVoteById(voteId);
    }

    protected Vote createFinalVote(Integer userId, Integer electionId) {
        Integer voteId = voteDAO.insertVote(userId, electionId, VoteType.FINAL.getValue());
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

    @SuppressWarnings("SameParameterValue")
    protected Consent createConsent() {
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
                "Group");
        return consentDAO.findConsentById(consentId);
    }

    protected Match createMatch() {
        DataAccessRequest dar = createDataAccessRequestV3();
        Dac dac = createDac();
        Dataset dataset = createDataset();
        Integer matchId =
        matchDAO.insertMatch(
            dataset.getDatasetIdentifier(),
            dar.getReferenceId(),
            RandomUtils.nextBoolean(),
            false,
            new Date(),
            MatchAlgorithm.V2.getVersion());
        return matchDAO.findMatchById(matchId);
    }

    /**
     * Creates a user with default role of Researcher and random user properties
     *
     * @return Created User
     */
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
        userRoleDAO.insertSingleUserRole(UserRoles.RESEARCHER.getRoleId(), userId);
        UserProperty prop = new UserProperty();
        prop.setUserId(userId);
        prop.setPropertyKey(UserFields.SUGGESTED_INSTITUTION.getValue());
        prop.setPropertyValue("test");
        userPropertyDAO.insertAll(List.of(prop));
        return userDAO.findUserById(userId);
    }

    private void createUserProperty(Integer userId, String field) {
        UserProperty property = new UserProperty();
        property.setPropertyKey(field);
        property.setPropertyValue(UUID.randomUUID().toString());
        property.setUserId(userId);
        userPropertyDAO.insertAll(List.of(property));
    }

    protected User createUserWithInstitution() {
        int i1 = RandomUtils.nextInt(5, 10);
        String email = RandomStringUtils.randomAlphabetic(i1);
        String name = RandomStringUtils.randomAlphabetic(10);
        Integer userId = userDAO.insertUser(email, name, new Date());
        Integer institutionId = institutionDAO.insertInstitution(RandomStringUtils.randomAlphanumeric(10), "itdirectorName", "itDirectorEmail", userId, new Date());
        userDAO.updateUser(name, userId, institutionId);
        userRoleDAO.insertSingleUserRole(7, userId);
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
        userRoleDAO.insertSingleUserRole(roleId, userId);
        return userDAO.findUserById(userId);
    }

    protected User createUserWithRoleInDac(Integer roleId, Integer dacId) {
        User user = createUserWithRole(roleId);
        dacDAO.addDacMember(roleId, user.getUserId(), dacId);
        return user;
    }

    protected Dac createDac() {
        Integer id = dacDAO.createDac(
                "Test_" + RandomStringUtils.random(20, true, true),
                "Test_" + RandomStringUtils.random(20, true, true),
                new Date());
        return dacDAO.findById(id);
    }

    protected Institution createInstitution() {
        User createUser = createUser();
        Integer id = institutionDAO.insertInstitution(
          "Test_" + RandomStringUtils.random(20, true, true),
          "Test_" + RandomStringUtils.random(20, true, true),
          createUser.getEmail(),
          createUser.getUserId(),
          new Date());
        Institution institution = institutionDAO.findInstitutionById(id);
        User updateUser = createUser();
        institutionDAO.updateInstitutionById(
                institution.getId(),
                institution.getName(),
                institution.getItDirectorName(),
                institution.getItDirectorEmail(),
                updateUser.getUserId(),
                new Date());
        return institutionDAO.findInstitutionById(id);
    }

    protected void createDatasetProperties(Integer datasetId) {
        List<DatasetProperty> list = new ArrayList<>();
        DatasetProperty dsp = new DatasetProperty();
        dsp.setDataSetId(datasetId);
        dsp.setPropertyKey(1);
        dsp.setPropertyValue("Test_PropertyValue");
        dsp.setCreateDate(new Date());
        list.add(dsp);
        datasetDAO.insertDatasetProperties(list);
    }

    protected Dataset createDataset() {
        User user = createUser();
        String name = "Name_" + RandomStringUtils.random(20, true, true);
        Timestamp now = new Timestamp(new Date().getTime());
        String objectId = "Object ID_" + RandomStringUtils.random(20, true, true);
        DataUse dataUse = new DataUseBuilder().setGeneralUse(true).build();
        Integer id = datasetDAO.insertDataset(name, now, user.getUserId(), objectId, true, dataUse.toString());
        createDatasetProperties(id);
        return datasetDAO.findDatasetById(id);
    }

    protected LibraryCard createLibraryCard() {
        Integer institutionId = createInstitution().getId();
        String email = RandomStringUtils.randomAlphabetic(11);
        Integer userId = userDAO.insertUser(email, "displayName", new Date());
        userDAO.updateUser(email, userId, institutionId);
        String stringValue = "value";
        Integer id = libraryCardDAO.insertLibraryCard(userId, institutionId, stringValue, stringValue, stringValue, userId, new Date());
        return libraryCardDAO.findLibraryCardById(id);
    }

    protected LibraryCard createLibraryCard(User user) {
        Integer institutionId = createInstitution().getId();
        String stringValue = "value";
        Integer id = libraryCardDAO.insertLibraryCard(user.getUserId(), institutionId, stringValue, user.getDisplayName(), user.getEmail(), user.getUserId(), new Date());
        return libraryCardDAO.findLibraryCardById(id);
    }

    protected LibraryCard createLCForUnregisteredUser(Integer institutionId) {
        Integer createUserId = createUser().getUserId();
        String email = RandomStringUtils.randomAlphabetic(10);
        Integer id = libraryCardDAO.insertLibraryCard(null, institutionId, null, null, email, createUserId, new Date());
        return libraryCardDAO.findLibraryCardById(id);
    }

    //overloaded method, helper for INDEX SQL call
    //createInstitution called outside of helper for institution reference/data checks
    protected LibraryCard createLibraryCardForIndex(Integer institutionId) {
        Integer userId = createUser().getUserId();
        String stringValue = "value";
        Integer id = libraryCardDAO.insertLibraryCard(userId, institutionId, stringValue, stringValue, stringValue,
                userId, new Date());
        return libraryCardDAO.findLibraryCardById(id);
    }

    /**
     * This method creates a number of DARs under a DarCollection and only returns the
     * last DAR created.
     *
     * @return Last DataAccessRequest of a DarCollection
     */
    protected DataAccessRequest createDataAccessRequestV3() {
        User user = createUserWithInstitution();
        String darCode = "DAR-" + RandomUtils.nextInt(1, 999999999);
        Integer collection_id = darCollectionDAO.insertDarCollection(darCode, user.getUserId(), new Date());
        for(int i = 0; i < 4; i++) {
            createDataAccessRequest(user.getUserId(), collection_id, darCode);
        }
        return createDataAccessRequest(user.getUserId(), collection_id, darCode);
    }

    protected DataAccessRequest createDataAccessRequestWithUserIdV3(Integer userId) {
        String darCode = "DAR-" + RandomUtils.nextInt(100, 1000);
        Integer collectionId = darCollectionDAO.insertDarCollection(darCode, userId, new Date());
        for(int i = 0; i < 4; i++) {
            createDataAccessRequest(userId, collectionId, darCode);
        }
        return createDataAccessRequest(userId, collectionId, darCode);
    }

    protected Integer createDataAccessRequestUserWithInstitute() {
        User user = createUserWithInstitution();
        String darCode = "DAR-" + RandomUtils.nextInt(100, 1000);
        Integer collectionId = darCollectionDAO.insertDarCollection(darCode, user.getUserId(), new Date());
        createDataAccessRequest(user.getUserId(), collectionId, darCode);
        return user.getInstitutionId();
    }

    protected DataAccessRequest createDataAccessRequestWithDatasetAndCollectionInfo(int collectionId, int datasetId, int userId, String darCode) {
        DataAccessRequestData data = new DataAccessRequestData();
        data.setProjectTitle(RandomStringUtils.random(10));
        String referenceId = RandomStringUtils.randomAlphanumeric(20);
        dataAccessRequestDAO.insertDataAccessRequest(collectionId, referenceId, userId, new Date(), new Date(), new Date(), new Date(), data);
        dataAccessRequestDAO.insertDARDatasetRelation(referenceId, datasetId);
        return dataAccessRequestDAO.findByReferenceId(referenceId);
    }

    /**
     * Creates a new user, dataset, data access request, and dar collection
     *
     * @return Populated DataAccessRequest
     */
    protected DataAccessRequest createDataAccessRequest(Integer userId, Integer collectionId, String darCode) {
        DataAccessRequestData data = new DataAccessRequestData();
        data.setProjectTitle("Project Title: " + RandomStringUtils.random(50, true, false));
        data.setDarCode(darCode);
        DatasetEntry entry = new DatasetEntry();
        entry.setKey("key");
        entry.setValue("value");
        entry.setLabel("label");
        data.setDatasets(List.of(entry));
        data.setHmb(true);
        data.setMethods(false);
        String referenceId = UUID.randomUUID().toString();
        Date now = new Date();
        dataAccessRequestDAO.insertDataAccessRequest(
            collectionId,
            referenceId,
            userId,
            now, now, now, now,
            data);
        return dataAccessRequestDAO.findByReferenceId(referenceId);
    }

    protected DataAccessRequest createDraftDataAccessRequest() {
        User user = createUser();
        String darCode = "DAR-" + RandomUtils.nextInt(100, 1000);
        DataAccessRequestData data = new DataAccessRequestData();
        data.setProjectTitle("Project Title: " + RandomStringUtils.random(50, true, false));
        data.setDarCode(darCode);
        String referenceId = UUID.randomUUID().toString();
        Date now = new Date();
        dataAccessRequestDAO.insertDraftDataAccessRequest(
            referenceId,
            user.getUserId(),
            now,
            now,
            now,
            now,
            data
        );
        return dataAccessRequestDAO.findByReferenceId(referenceId);
    }

    protected DarCollection createDarCollection() {
        User user = createUserWithInstitution();
        String darCode = "DAR-" + RandomUtils.nextInt(1, 10000);
        Integer collection_id = darCollectionDAO.insertDarCollection(darCode, user.getUserId(), new Date());
        Dataset dataset = createDataset();
        DataAccessRequest dar = createDataAccessRequest(user.getUserId(), collection_id, darCode);
        dataAccessRequestDAO.insertDARDatasetRelation(dar.getReferenceId(), dataset.getDataSetId());
        Election cancelled = createCancelledAccessElection(dar.getReferenceId(), dataset.getDataSetId());
        Election access = createDataAccessElection(dar.getReferenceId(), dataset.getDataSetId());
        createFinalVote(user.getUserId(), cancelled.getElectionId());
        createFinalVote(user.getUserId(), access.getElectionId());
        createDataAccessRequest(user.getUserId(), collection_id, darCode);
        createDataAccessRequest(user.getUserId(), collection_id, darCode);
        return darCollectionDAO.findDARCollectionByCollectionId(collection_id);
    }

    protected void createConsentAndAssociationWithDatasetIdAndDACId(int datasetId, int dacId ) {
        Consent consent = createConsent(dacId);
        consentDAO.insertConsentAssociation(consent.getConsentId(), ASSOCIATION_TYPE_TEST, datasetId);
    }

    protected DarCollection createDarCollectionWithDatasetsAndConsentAssociation(int dacId, User user, List<Dataset> datasets) {
        String darCode = "DAR-" + RandomUtils.nextInt(100, 1000);
        Integer collectionId = darCollectionDAO.insertDarCollection(darCode, user.getUserId(), new Date());
        datasets.stream()
                .forEach(dataset-> {
                    DataAccessRequest dar = createDataAccessRequestWithDatasetAndCollectionInfo(collectionId, dataset.getDataSetId(), user.getUserId(), darCode);
                    Election cancelled = createCancelledAccessElection(dar.getReferenceId(), dataset.getDataSetId());
                    Election access = createDataAccessElection(dar.getReferenceId(), dataset.getDataSetId());
                    createFinalVote(user.getUserId(), cancelled.getElectionId());
                    createFinalVote(user.getUserId(), access.getElectionId());
                    createConsentAndAssociationWithDatasetIdAndDACId(dataset.getDataSetId(), dacId);
                });
        return darCollectionDAO.findDARCollectionByCollectionId(collectionId);
    }

    protected DarCollection createDarCollectionMultipleUserProperties() {
        User user = createUser();
        Integer userId = user.getUserId();
        createUserProperty(userId, UserFields.SUGGESTED_SIGNING_OFFICIAL.getValue());
        createUserProperty(userId, UserFields.SUGGESTED_INSTITUTION.getValue());
        createUserProperty(userId, UserFields.ERA_STATUS.getValue());
        String darCode = "DAR-" + RandomUtils.nextInt(100, 1000);
        Integer collection_id = darCollectionDAO.insertDarCollection(darCode, user.getUserId(), new Date());
        Dataset dataset = createDataset();
        DataAccessRequest dar = createDataAccessRequest(user.getUserId(), collection_id, darCode);
        dataAccessRequestDAO.insertDARDatasetRelation(dar.getReferenceId(), dataset.getDataSetId());
        Election cancelled = createCancelledAccessElection(dar.getReferenceId(), dataset.getDataSetId());
        Election access = createDataAccessElection(dar.getReferenceId(), dataset.getDataSetId());
        createFinalVote(user.getUserId(), cancelled.getElectionId());
        createFinalVote(user.getUserId(), access.getElectionId());
        createDataAccessRequest(user.getUserId(), collection_id, darCode);
        createDataAccessRequest(user.getUserId(), collection_id, darCode);
        return darCollectionDAO.findDARCollectionByCollectionId(collection_id);
    }

    protected DataAccessRequest createDarForCollection(User user, Integer collectionId, Dataset dataset) {
        Date now = new Date();
        DataAccessRequest dar = new DataAccessRequest();
        dar.setReferenceId(UUID.randomUUID().toString());
        DataAccessRequestData data = new DataAccessRequestData();
        dar.setData(data);
        dataAccessRequestDAO.insertDraftDataAccessRequest(dar.getReferenceId(), user.getUserId(), now, now, now, now, data);
        dataAccessRequestDAO.updateDraftForCollection(collectionId, dar.getReferenceId());
        dataAccessRequestDAO.updateDraftByReferenceId(dar.getReferenceId(), false);
        dataAccessRequestDAO.insertDARDatasetRelation(dar.getReferenceId(), dataset.getDataSetId());
        return dataAccessRequestDAO.findByReferenceId(dar.getReferenceId());
    }
}
