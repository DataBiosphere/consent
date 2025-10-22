package org.broadinstitute.consent.http.db;

import static org.broadinstitute.consent.http.ConsentModule.DB_ENV;

import io.dropwizard.core.setup.Environment;
import io.dropwizard.jdbi3.JdbiFactory;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.testing.DropwizardTestSupport;
import io.dropwizard.testing.ResourceHelpers;
import java.util.Date;
import java.util.Random;
import java.util.UUID;
import org.apache.commons.lang3.RandomStringUtils;
import org.broadinstitute.consent.http.AbstractTestHelper;
import org.broadinstitute.consent.http.ConsentApplication;
import org.broadinstitute.consent.http.configurations.ConsentConfiguration;
import org.broadinstitute.consent.http.enumeration.OrganizationType;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.Institution;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.util.gson.GsonUtil;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.gson2.Gson2Config;
import org.jdbi.v3.gson2.Gson2Plugin;
import org.jdbi.v3.guava.GuavaPlugin;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestPlan;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;

public class DAOTestHelper extends AbstractTestHelper implements TestExecutionListener {

  public static final String POSTGRES_IMAGE = "postgres:16.4-alpine";
  public static final String EMPTY_JSON_DOCUMENT = "{}";
  private static final int maxConnections = 100;
  private static final ConfigOverride maxConnectionsOverride = ConfigOverride.config(
      "database.maxSize", String.valueOf(maxConnections));
  protected static Jdbi jdbi;
  protected static CounterDAO counterDAO;
  protected static DacDAO dacDAO;
  protected static DaaDAO daaDAO;
  protected static UserDAO userDAO;
  protected static DatasetAuthorizationReaderDAO datasetAuthorizationReaderDAO;
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
  protected static FileStorageObjectDAO fileStorageObjectDAO;
  protected static AcknowledgementDAO acknowledgementDAO;
  protected static DraftDAO draftDAO;
  protected static DACAutomationRuleDAO dacAutomationRuleDAO;
  private static DropwizardTestSupport<ConsentConfiguration> testApp;
  // This is a test-only DAO class where we manage the deletion
  // of all records between test runs.
  private static TestingDAO testingDAO;

  @SuppressWarnings("rawtypes")
  private static PostgreSQLContainer postgresContainer;

  @Override
  public void testPlanExecutionStarted(TestPlan testPlan) {
    // The tests that extend this class make the necessary assumption that the app will be started
    // and backed by a running database.  When we do not need the containers, we are also
    // indicating we do not need the application within this class, hence we can early return and
    // skip resource construction.
    // This became a necessary optimization because of command line generated output that could not
    // be properly handled by a CI/CD process. */
    try {
      if (enableTestContainers()) {
        startUp();
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public void startUp() throws Exception {
    // Start the database
    postgresContainer = new PostgreSQLContainer<>(POSTGRES_IMAGE).
        withCommand("postgres -c max_connections=" + maxConnections).
        waitingFor(Wait.forListeningPorts());
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
    String dbiExtension = "_" + RandomStringUtils.secureStrong().nextAlphabetic(10);
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
    datasetAuthorizationReaderDAO = jdbi.onDemand(DatasetAuthorizationReaderDAO.class);
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
    fileStorageObjectDAO = jdbi.onDemand(FileStorageObjectDAO.class);
    acknowledgementDAO = jdbi.onDemand(AcknowledgementDAO.class);
    draftDAO = jdbi.onDemand(DraftDAO.class);
    dacAutomationRuleDAO = jdbi.onDemand(DACAutomationRuleDAO.class);
    testingDAO = jdbi.onDemand(TestingDAO.class);
  }

  @BeforeEach()
  void before() {
    testingDAO.truncateAllTables();
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
    return createUserWithRole(UserRoles.RESEARCHER.getRoleId());
  }

  /**
   * This method creates a number of DARs under a DarCollection and only returns the last DAR
   * created.
   *
   * @return Last DataAccessRequest of a DarCollection
   */
  protected DataAccessRequest createDataAccessRequestV3() {
    User user = createUserWithInstitution();
    String darCode = "DAR-" + randomInt(1, 999999999);
    Integer collection_id = darCollectionDAO.insertDarCollection(darCode, user.getUserId(),
        new Date());
    for (int i = 0; i < 4; i++) {
      createDataAccessRequest(user.getUserId(), collection_id);
    }
    return createDataAccessRequest(user.getUserId(), collection_id);
  }

  /**
   * Creates a new user, dataset, data access request, and dar collection
   *
   * @return Populated DataAccessRequest
   */
  private DataAccessRequest createDataAccessRequest(Integer userId, Integer collectionId) {
    DataAccessRequestData data = new DataAccessRequestData();
    data.setProjectTitle("Project Title: " + randomAlphabetic(50));
    data.setHmb(true);
    data.setMethods(false);
    String referenceId = UUID.randomUUID().toString();
    Date now = new Date();
    dataAccessRequestDAO.insertDataAccessRequest(
        collectionId,
        referenceId,
        userId,
        now, now, now,
        data,
        randomAlphabetic(10));
    return dataAccessRequestDAO.findByReferenceId(referenceId);
  }

  protected User createUserWithRoleInDac(Integer roleId, Integer dacId) {
    User user = createUserWithRole(roleId);
    dacDAO.addDacMember(roleId, user.getUserId(), dacId);
    return user;
  }

  protected User createUserWithRole(Integer roleId) {
    return createUserWithRole(roleId, null);
  }

  protected User createUserWithRole(Integer roleId, Integer institutionId) {
    int i1 = randomInt(5, 10);
    int i2 = randomInt(5, 10);
    int i3 = randomInt(3, 5);
    String email = randomAlphabetic(i1) + "@" + randomAlphabetic(i2) + "." + randomAlphabetic(i3);
    Integer userId = userDAO.insertUser(email, "display name", institutionId, new Date());
    userRoleDAO.insertSingleUserRole(roleId, userId);
    return userDAO.findUserById(userId);
  }

  protected User createUserWithInstitution() {
    User admin = createUserWithRole(UserRoles.ADMIN.getRoleId());
    Integer adminId = admin.getUserId();
    Integer institutionId = institutionDAO.insertInstitution(randomAlphabetic(20),
        "itDirectorName",
        "itDirectorEmail",
        randomAlphabetic(10),
        new Random().nextInt(),
        randomAlphabetic(10),
        randomAlphabetic(10),
        randomAlphabetic(10),
        OrganizationType.NON_PROFIT.getValue(),
        adminId,
        new Date());
    User user = createUserWithRole(UserRoles.SIGNINGOFFICIAL.getRoleId(), institutionId);
    return userDAO.findUserById(user.getUserId());
  }

  protected Institution getUserInstitution(User user) {
    return institutionDAO.findInstitutionById(user.getInstitutionId());
  }

  protected void updateVote(Boolean vote, String rationale, Date updateDate, Integer voteId,
      boolean reminder, Integer electionId, Date createDate, Boolean hasConcerns) {
    jdbi.useHandle(handle -> {
      String sql = """
              UPDATE vote
              SET vote = :vote, update_date = :updateDate, rationale = :rationale, reminder_sent = :reminderSent, create_date = :createDate, has_concerns = :hasConcerns
              WHERE vote_id = :voteId
          """;
      handle.createUpdate(sql)
          .bind("vote", vote)
          .bind("rationale", rationale)
          .bind("updateDate", updateDate)
          .bind("voteId", voteId)
          .bind("reminderSent", reminder)
          .bind("electionId", electionId)
          .bind("createDate", createDate)
          .bind("hasConcerns", hasConcerns)
          .execute();
    });
  }

}
