package org.broadinstitute.consent.http.db;

import static org.broadinstitute.consent.http.ConsentModule.DB_ENV;

import io.dropwizard.core.setup.Environment;
import io.dropwizard.jdbi3.JdbiFactory;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.testing.DropwizardTestSupport;
import io.dropwizard.testing.ResourceHelpers;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.ConsentApplication;
import org.broadinstitute.consent.http.configurations.ConsentConfiguration;
import org.broadinstitute.consent.http.enumeration.OrganizationType;
import org.broadinstitute.consent.http.enumeration.UserFields;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.DatasetEntry;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserProperty;
import org.broadinstitute.consent.http.util.gson.GsonUtil;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.gson2.Gson2Config;
import org.jdbi.v3.gson2.Gson2Plugin;
import org.jdbi.v3.guava.GuavaPlugin;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.PostgreSQLContainer;

public class DAOTestHelper {

  public static final String POSTGRES_IMAGE = "postgres:11.6-alpine";
  private static final int maxConnections = 100;
  private static final ConfigOverride maxConnectionsOverride = ConfigOverride.config(
      "database.maxSize", String.valueOf(maxConnections));

  private static DropwizardTestSupport<ConsentConfiguration> testApp;

  protected static Jdbi jdbi;

  protected static CounterDAO counterDAO;
  protected static DacDAO dacDAO;
  protected static DaaDAO daaDAO;
  protected static UserDAO userDAO;
  protected static DatasetDAO datasetDAO;
  protected static ElectionDAO electionDAO;
  protected static UserRoleDAO userRoleDAO;
  protected static VoteDAO voteDAO;
  protected static StudyDAO studyDAO;
  protected static DataAccessRequestDAO dataAccessRequestDAO;
  protected static MatchDAO matchDAO;
  protected static MailMessageDAO mailMessageDAO;
  protected static UserPropertyDAO userPropertyDAO;
  protected static InstitutionDAO institutionDAO;
  protected static LibraryCardDAO libraryCardDAO;
  protected static DarCollectionDAO darCollectionDAO;
  protected static DarCollectionSummaryDAO darCollectionSummaryDAO;
  protected static DatasetAssociationDAO datasetAssociationDAO;
  protected static FileStorageObjectDAO fileStorageObjectDAO;
  protected static AcknowledgementDAO acknowledgementDAO;

  // This is a test-only DAO class where we manage the deletion
  // of all records between test runs.
  private static TestingDAO testingDAO;

  @SuppressWarnings("rawtypes")
  private static PostgreSQLContainer postgresContainer;

  @BeforeAll
  public static void startUp() throws Exception {
    // Start the database
    postgresContainer = new PostgreSQLContainer<>(POSTGRES_IMAGE).
        withCommand("postgres -c max_connections=" + maxConnections);
    postgresContainer.start();
    ConfigOverride driverOverride = ConfigOverride.config("database.driverClass",
        postgresContainer.getDriverClassName());
    ConfigOverride urlOverride = ConfigOverride.config("database.url",
        postgresContainer.getJdbcUrl());
    ConfigOverride userOverride = ConfigOverride.config("database.user",
        postgresContainer.getUsername());
    ConfigOverride passwordOverride = ConfigOverride.config("database.password",
        postgresContainer.getPassword());
    ConfigOverride validationQueryOverride = ConfigOverride.config("database.validationQuery",
        postgresContainer.getTestQueryString());

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
    jdbi = new JdbiFactory().build(environment, configuration.getDataSourceFactory(),
        DB_ENV + dbiExtension);
    jdbi.installPlugin(new SqlObjectPlugin());
    jdbi.installPlugin(new Gson2Plugin());
    jdbi.installPlugin(new GuavaPlugin());
    jdbi.getConfig().get(Gson2Config.class).setGson(
        GsonUtil.buildGson()
    );

    counterDAO = jdbi.onDemand(CounterDAO.class);
    dacDAO = jdbi.onDemand(DacDAO.class);
    daaDAO = jdbi.onDemand(DaaDAO.class);
    userDAO = jdbi.onDemand(UserDAO.class);
    datasetDAO = jdbi.onDemand(DatasetDAO.class);
    electionDAO = jdbi.onDemand(ElectionDAO.class);
    userRoleDAO = jdbi.onDemand(UserRoleDAO.class);
    voteDAO = jdbi.onDemand(VoteDAO.class);
    studyDAO = jdbi.onDemand(StudyDAO.class);
    dataAccessRequestDAO = jdbi.onDemand(DataAccessRequestDAO.class);
    matchDAO = jdbi.onDemand(MatchDAO.class);
    mailMessageDAO = jdbi.onDemand(MailMessageDAO.class);
    userPropertyDAO = jdbi.onDemand(UserPropertyDAO.class);
    institutionDAO = jdbi.onDemand(InstitutionDAO.class);
    libraryCardDAO = jdbi.onDemand(LibraryCardDAO.class);
    darCollectionDAO = jdbi.onDemand(DarCollectionDAO.class);
    darCollectionSummaryDAO = jdbi.onDemand(DarCollectionSummaryDAO.class);
    datasetAssociationDAO = jdbi.onDemand(DatasetAssociationDAO.class);
    fileStorageObjectDAO = jdbi.onDemand(FileStorageObjectDAO.class);
    acknowledgementDAO = jdbi.onDemand(AcknowledgementDAO.class);
    testingDAO = jdbi.onDemand(TestingDAO.class);
  }

  @AfterAll
  public static void shutDown() {
    testApp.after();
    postgresContainer.stop();
  }

  @AfterEach
  public void tearDown() {
    // Order is important for FK constraints
    testingDAO.deleteAllDARDataset();
    testingDAO.deleteAllApprovalTimes();
    testingDAO.deleteAllVotes();
    testingDAO.deleteAllConsentAudits();
    testingDAO.deleteAllMatchEntityRationales();
    testingDAO.deleteAllMatchEntities();
    testingDAO.deleteAllConsentAssociations();
    testingDAO.deleteAllConsents();
    testingDAO.deleteAllAccessRps();
    testingDAO.deleteAllElections();
    testingDAO.deleteAllDatasetProperties();
    testingDAO.deleteAllDictionaryTerms();
    testingDAO.deleteAllDatasetAssociations();
    testingDAO.deleteAllDatasetAudits();
    testingDAO.deleteAllDatasets();
    testingDAO.deleteAllStudyProperties();
    testingDAO.deleteAllStudies();
    testingDAO.deleteAllDacUserRoles();
    testingDAO.deleteAllLibraryCardDAAs();
    testingDAO.deleteAllDACDAAs();
    testingDAO.deleteAllDataAccessAgreements();
    testingDAO.deleteAllDacs();
    testingDAO.deleteAllLibraryCards();
    testingDAO.deleteAllInstitutions();
    testingDAO.deleteAllUserProperties();
    testingDAO.deleteAllUserRoles();
    testingDAO.deleteAllAcknowledgements();
    testingDAO.deleteAllFileStorageObjects();
    testingDAO.deleteAllUsers();
    testingDAO.deleteAllDARs();
    testingDAO.deleteAllDARCollections();
    testingDAO.deleteAllCounters();
    testingDAO.deleteAllEmailEntities();
  }

    /*
       Utility methods in this class need to be complete from the perspective of the
       entity. When testing, if you need a specific modification to an object, call
       dao methods directly to do any manipulation.
     */

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

  /**
   * This method creates a number of DARs under a DarCollection and only returns the last DAR
   * created.
   *
   * @return Last DataAccessRequest of a DarCollection
   */
  protected DataAccessRequest createDataAccessRequestV3() {
    int i1 = RandomUtils.nextInt(5, 10);
    String email = RandomStringUtils.randomAlphabetic(i1);
    String name = RandomStringUtils.randomAlphabetic(10);
    Integer userId = userDAO.insertUser(email, name, new Date());
    Integer institutionId = institutionDAO.insertInstitution(RandomStringUtils.randomAlphabetic(20),
        "itDirectorName",
        "itDirectorEmail",
        RandomStringUtils.randomAlphabetic(10),
        new Random().nextInt(),
        RandomStringUtils.randomAlphabetic(10),
        RandomStringUtils.randomAlphabetic(10),
        RandomStringUtils.randomAlphabetic(10),
        OrganizationType.NON_PROFIT.getValue(),
        userId,
        new Date());
    userDAO.updateUser(name, userId, institutionId);
    userRoleDAO.insertSingleUserRole(7, userId);
    User user = userDAO.findUserById(userId);
    String darCode = "DAR-" + RandomUtils.nextInt(1, 999999999);
    Integer collection_id = darCollectionDAO.insertDarCollection(darCode, user.getUserId(),
        new Date());
    for (int i = 0; i < 4; i++) {
      createDataAccessRequest(user.getUserId(), collection_id, darCode);
    }
    return createDataAccessRequest(user.getUserId(), collection_id, darCode);
  }

  /**
   * Creates a new user, dataset, data access request, and dar collection
   *
   * @return Populated DataAccessRequest
   */
  private DataAccessRequest createDataAccessRequest(Integer userId, Integer collectionId,
      String darCode) {
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

}
