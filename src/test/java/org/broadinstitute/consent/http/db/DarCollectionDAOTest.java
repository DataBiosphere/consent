package org.broadinstitute.consent.http.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.enumeration.ElectionStatus;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.enumeration.OrganizationType;
import org.broadinstitute.consent.http.enumeration.UserFields;
import org.broadinstitute.consent.http.enumeration.VoteType;
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
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserProperty;
import org.broadinstitute.consent.http.models.Vote;
import org.junit.jupiter.api.Test;
import org.postgresql.util.PSQLException;
import org.postgresql.util.PSQLState;


class DarCollectionDAOTest extends DAOTestHelper {

  private void generateDatasetElectionForCollection(DarCollection collection) {
    DataAccessRequest dar = collection.getDars().values().stream()
        .filter(d -> !d.getElections().isEmpty()).findFirst().orElse(null);
    String referenceId = dar.getReferenceId();
    Election election = dar.getElections().values().stream().findFirst().orElse(null);
    Integer datasetId = election.getDataSetId();
    electionDAO.insertElection("DataSet", "Open", new Date(), referenceId, datasetId);
  }

  private List<Election> getElectionsFromCollection(DarCollection collection) {
    return collection.getDars().values().stream()
        .map(DataAccessRequest::getElections)
        .map(electionMap -> electionMap.values())
        .flatMap(Collection::stream)
        .collect(Collectors.toList());
  }

  private List<Election> getDatasetElectionsFromElection(List<Election> elections) {
    return elections.stream()
        .filter(e -> e.getElectionType().equalsIgnoreCase("dataset"))
        .collect(Collectors.toList());
  }

  @Test
  void testFindAllDARCollections() {
    DarCollection collection = createDarCollectionMultipleUserProperties();
    List<DarCollection> allAfter = darCollectionDAO.findAllDARCollections();
    assertTrue(allAfter.contains(collection));
    assertEquals(1, allAfter.size());
    DarCollection targetCollection = allAfter.get(0);
    generateDatasetElectionForCollection(targetCollection);
    List<UserProperty> userProperties = allAfter.get(0).getCreateUser().getProperties();
    assertFalse(userProperties.isEmpty());

    List<Election> elections = getElectionsFromCollection(targetCollection);
    assertNotNull(elections);
    assertTrue(elections.size() > 0);
    List<Election> datasetElections = getDatasetElectionsFromElection(elections);
    assertEquals(0, datasetElections.size());
    userProperties.forEach(p -> assertEquals(collection.getCreateUserId(),
        p.getUserId()));
  }

  @Test
  void testFindAllDarCollectionsMultipleUserProperties() {
    DarCollection collection = createDarCollectionMultipleUserProperties();
    List<DarCollection> allAfter = darCollectionDAO.findAllDARCollections();
    assertTrue(allAfter.contains(collection));
    assertEquals(1, allAfter.size());
    List<UserProperty> userProperties = allAfter.get(0).getCreateUser().getProperties();
    assertFalse(userProperties.isEmpty());
    userProperties.forEach(p -> assertEquals(collection.getCreateUserId(),
        p.getUserId()));
  }

  @Test
  void testFindDARCollectionByReferenceId() {
    DataAccessRequest dar = createDataAccessRequestV3();
    DarCollection collection = darCollectionDAO.findDARCollectionByReferenceId(
        dar.getReferenceId());
    assertNotNull(collection);
    assertEquals(dar.getCollectionId(), collection.getDarCollectionId());
    List<String> ids = collection.getDars().values().stream().map(DataAccessRequest::getReferenceId)
        .collect(Collectors.toList());
    assertTrue(ids.contains(dar.getReferenceId()));
  }

  @Test
  void testFindDARCollectionByReferenceIdNegative() {
    //dar without a collection ID
    DataAccessRequest dar = createDraftDataAccessRequest();
    DarCollection collection = darCollectionDAO.findDARCollectionByReferenceId(
        dar.getReferenceId());
    assertNull(collection);
  }

  @Test
  void testFindDARCollectionByCollectionId() {
    DarCollection collection = createDarCollectionMultipleUserProperties();
    DarCollection returned = darCollectionDAO.findDARCollectionByCollectionId(
        collection.getDarCollectionId());
    assertNotNull(returned);
    assertEquals(collection.getDarCode(), returned.getDarCode());
    assertEquals(collection.getCreateUserId(), returned.getCreateUserId());
    generateDatasetElectionForCollection(collection);
    List<Election> elections = getElectionsFromCollection(collection);
    assertEquals(1, elections.size());
    List<Election> datasetElections = getDatasetElectionsFromElection(elections);
    assertTrue(datasetElections.isEmpty());

    Election election = elections.get(0);
    List<Vote> votes = new ArrayList<>(election.getVotes().values());
    Vote vote = votes.get(0);
    assertEquals(1, votes.size());
    assertEquals("Open", election.getStatus());
    assertEquals(election.getElectionId(), vote.getElectionId());

    List<UserProperty> userProperties = returned.getCreateUser().getProperties();
    assertFalse(userProperties.isEmpty());
    userProperties.forEach(p -> assertEquals(collection.getCreateUserId(),
        p.getUserId()));

    assertNull(returned.getCreateUser().getLibraryCards());
  }

  @Test
  void testFindDARCollectionByCollectionIdMultipleUserProperties() {
    DarCollection collection = createDarCollectionMultipleUserProperties();
    DarCollection returned = darCollectionDAO.findDARCollectionByCollectionId(
        collection.getDarCollectionId());
    assertNotNull(returned);

    List<UserProperty> userProperties = returned.getCreateUser().getProperties();
    Integer userId = collection.getCreateUser().getUserId();
    assertFalse(userProperties.isEmpty());
    userProperties.forEach(p -> assertEquals(userId, p.getUserId()));
  }

  @Test
  void testFindDARCollectionByCollectionIdLibraryCard() {
    User user = createUser();
    LibraryCard libraryCard = createLibraryCard(user);
    String darCode = "DAR-" + RandomUtils.nextInt(100, 1000);
    Integer collectionId = darCollectionDAO.insertDarCollection(darCode, user.getUserId(),
        new Date());
    createDataAccessRequest(user.getUserId(), collectionId, darCode);

    DarCollection collection = darCollectionDAO.findDARCollectionByCollectionId(collectionId);
    User returnedUser = collection.getCreateUser();
    assertEquals(user, returnedUser);

    List<LibraryCard> returnedLibraryCards = returnedUser.getLibraryCards();
    assertEquals(1, returnedLibraryCards.size());
    assertEquals(libraryCard, returnedLibraryCards.get(0));
    assertEquals(user.getUserId(), returnedLibraryCards.get(0).getUserId());
  }

  @Test
  void testFindDARCollectionByCollectionIdNegative() {
    DarCollection returned = darCollectionDAO.findDARCollectionByCollectionId(
        RandomUtils.nextInt(1000, 2000));
    assertNull(returned);
  }

  @Test
  void testInsertDARCollection() {
    List<DarCollection> allBefore = darCollectionDAO.findAllDARCollections();
    assertTrue(allBefore.isEmpty());
    DarCollection collection = createDarCollection();
    assertNotNull(collection);
    List<DarCollection> allAfter = darCollectionDAO.findAllDARCollections();
    assertTrue(allAfter.contains(collection));
  }

  @Test
  void testInsertDarCollectionNegative() {
    Integer userId = createUser().getUserId();
    try {
      darCollectionDAO.insertDarCollection("darCode", 0, new Date());
    } catch (Exception e) {
      assertEquals(PSQLState.FOREIGN_KEY_VIOLATION.getState(),
          ((PSQLException) e.getCause()).getSQLState());
    }
    try {
      darCollectionDAO.insertDarCollection("darCode", userId, new Date());
      darCollectionDAO.insertDarCollection("darCode", userId, new Date());
    } catch (Exception e) {
      assertEquals(PSQLState.UNIQUE_VIOLATION.getState(),
          ((PSQLException) e.getCause()).getSQLState());
    }
  }

  @Test
  void testUpdateDARCollection() {
    DarCollection collection = createDarCollection();
    assertNotNull(collection);
    assertNull(collection.getUpdateDate());
    assertNull(collection.getUpdateUserId());
    User user = createUser();
    Date date = new Date();
    darCollectionDAO.updateDarCollection(collection.getDarCollectionId(), user.getUserId(), date);
    DarCollection updated = darCollectionDAO.findDARCollectionByCollectionId(
        collection.getDarCollectionId());
    assertEquals(user.getUserId(), updated.getUpdateUserId());
    assertEquals(date, updated.getUpdateDate());
  }

  @Test
  void testUpdateDarCollectionNegative() {
    Integer userId = createUser().getUserId();
    try {
      darCollectionDAO.updateDarCollection(0, userId, new Date());
    } catch (Exception e) {
      assertEquals(PSQLState.FOREIGN_KEY_VIOLATION.getState(),
          ((PSQLException) e.getCause()).getSQLState());
    }
    try {
      DarCollection collection = createDarCollection();
      darCollectionDAO.updateDarCollection(collection.getDarCollectionId(), 0, new Date());
    } catch (Exception e) {
      assertEquals(PSQLState.FOREIGN_KEY_VIOLATION.getState(),
          ((PSQLException) e.getCause()).getSQLState());
    }
  }

  @Test
  void testDeleteByCollectionId() {
    DarCollection collection = createDarCollection();
    collection.getDars().keySet()
        .forEach(k -> dataAccessRequestDAO.deleteDARDatasetRelationByReferenceId(k));
    dataAccessRequestDAO.deleteByCollectionId(collection.getDarCollectionId());
    darCollectionDAO.deleteByCollectionId(collection.getDarCollectionId());
    assertNull(
        darCollectionDAO.findDARCollectionByCollectionId(collection.getDarCollectionId()));
  }

  @Test
  void testDeleteByCollectionIdNegative() {
    try {
      darCollectionDAO.deleteByCollectionId(RandomUtils.nextInt(100, 1000));
    } catch (Exception e) {
      assertEquals(PSQLState.UNIQUE_VIOLATION.getState(),
          ((PSQLException) e.getCause()).getSQLState());
    }
  }

  // local method to create a test DAR
  DataAccessRequest createDAR(User user, Dataset dataset, Integer collectionId) {
    Timestamp now = new Timestamp(new Date().getTime());
    DataAccessRequest testDar = new DataAccessRequest();
    testDar.setCollectionId(collectionId);
    testDar.setReferenceId(UUID.randomUUID().toString());
    testDar.setUserId(user.getUserId());
    testDar.setCreateDate(now);
    testDar.setSortDate(now);
    testDar.setSubmissionDate(now);
    testDar.setUpdateDate(now);
    DataAccessRequestData contents = new DataAccessRequestData();
    testDar.setData(contents);

    dataAccessRequestDAO.insertDataAccessRequest(
        testDar.getCollectionId(),
        testDar.getReferenceId(),
        testDar.getUserId(),
        testDar.getCreateDate(),
        testDar.getSortDate(),
        testDar.getSubmissionDate(),
        testDar.getUpdateDate(),
        testDar.getData()
    );
    dataAccessRequestDAO.insertDARDatasetRelation(testDar.getReferenceId(), dataset.getDataSetId());
    return testDar;
  }

  // local method to create a test DAC
  Dac createDAC() {
    Integer id = dacDAO.createDac(
        "Test_" + RandomStringUtils.random(20, true, true),
        "Test_" + RandomStringUtils.random(20, true, true),
        new Date());
    return dacDAO.findById(id);
  }

  // local method to create a test DAR Collection and dataset
  // takes in user as a parameter so we can test multiple collections with the same user
  // this method returns a list that includes: user, now, dataset, collectionId, testDar, dac, testDarCollection
  List<Object> createDarCollectionWithDataset(User user) {
    Timestamp now = new Timestamp(new Date().getTime());
    String darCode = "DAR-" + RandomStringUtils.randomAlphanumeric(25);
    Dataset dataset = createDataset();

    // creating a collection and DAR
    Integer collectionId = darCollectionDAO.insertDarCollection(darCode, user.getUserId(), now);
    DataAccessRequest testDar = createDAR(user, dataset, collectionId);

    // create a DAC
    Dac dac = createDAC();
    datasetDAO.updateDatasetDacId(dataset.getDataSetId(), dac.getDacId());
    DarCollection testDarCollection = darCollectionDAO.findDARCollectionByCollectionId(
        collectionId);

    return List.of(user, now, dataset, collectionId, testDar, dac, testDarCollection);
  }

  // findDARCollectionByCollectionIds should exclude archived collections
  @Test
  void testFindDARCollectionIdsByCollectionIdsArchived() {
    User user = createUserWithInstitution();
    List<Object> newDarCollection1 = createDarCollectionWithDataset(user);
    List<Object> newDarCollection2 = createDarCollectionWithDataset(user);

    DataAccessRequest testDar1 = (DataAccessRequest) newDarCollection1.get(4);
    DarCollection testDarCollection1 = (DarCollection) newDarCollection1.get(6);
    DarCollection testDarCollection2 = (DarCollection) newDarCollection2.get(6);

    dataAccessRequestDAO.archiveByReferenceIds(List.of(testDar1.getReferenceId()));

    List<Integer> collectionIds = List.of(
        testDarCollection1.getDarCollectionId(),
        testDarCollection2.getDarCollectionId()
    );

    List<DarCollection> returnedCollections = darCollectionDAO.findDARCollectionByCollectionIds(
        collectionIds);

    assertEquals(1, returnedCollections.size());
    assertFalse(returnedCollections.contains(testDarCollection1));
    assertTrue(returnedCollections.contains(testDarCollection2));
  }

  // findAllDARCollections should exclude archived collections
  @Test
  void testFindAllDARCollectionsArchived() {
    User user = createUserWithInstitution();

    List<Object> newDarCollection1 = createDarCollectionWithDataset(user);
    List<Object> newDarCollection2 = createDarCollectionWithDataset(user);

    DataAccessRequest testDar1 = (DataAccessRequest) newDarCollection1.get(4);
    DarCollection testDarCollection1 = (DarCollection) newDarCollection1.get(6);
    DarCollection testDarCollection2 = (DarCollection) newDarCollection2.get(6);

    dataAccessRequestDAO.archiveByReferenceIds(List.of(testDar1.getReferenceId()));

    List<DarCollection> returnedCollections = darCollectionDAO.findAllDARCollections();

    assertEquals(1, returnedCollections.size());
    assertFalse(returnedCollections.contains(testDarCollection1));
    assertTrue(returnedCollections.contains(testDarCollection2));
  }

  // findDARCollectionByReferenceId should exclude archived collections
  @Test
  void testFindDARCollectionByReferenceIdArchived() {
    User user = createUserWithInstitution();
    List<Object> newDarCollection1 = createDarCollectionWithDataset(user);
    List<Object> newDarCollection2 = createDarCollectionWithDataset(user);

    DataAccessRequest testDar1 = (DataAccessRequest) newDarCollection1.get(4);
    DataAccessRequest testDar2 = (DataAccessRequest) newDarCollection2.get(4);
    DarCollection testDarCollection2 = (DarCollection) newDarCollection2.get(6);

    dataAccessRequestDAO.archiveByReferenceIds(List.of(testDar1.getReferenceId()));

    DarCollection archivedCollection = darCollectionDAO.findDARCollectionByReferenceId(
        testDar1.getReferenceId());
    DarCollection validCollection = darCollectionDAO.findDARCollectionByReferenceId(
        testDar2.getReferenceId());

    assertNull(archivedCollection);
    assertTrue(validCollection.equals(testDarCollection2));
  }

  // findDARCollectionByCollectionId should exclude archived collections
  @Test
  void testFindDARCollectionByCollectionIdArchived() {
    User user = createUserWithInstitution();
    List<Object> newDarCollection = createDarCollectionWithDataset(user);

    Integer collectionId = (Integer) newDarCollection.get(3);
    DataAccessRequest testDar = (DataAccessRequest) newDarCollection.get(4);

    dataAccessRequestDAO.archiveByReferenceIds(List.of(testDar.getReferenceId()));

    DarCollection returnedCollection = darCollectionDAO.findDARCollectionByCollectionId(
        collectionId);
    assertNull(returnedCollection);
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

  private Dataset createDataset() {
    User user = createUser();
    String name = "Name_" + RandomStringUtils.random(20, true, true);
    Timestamp now = new Timestamp(new Date().getTime());
    String objectId = "Object ID_" + RandomStringUtils.random(20, true, true);
    DataUse dataUse = new DataUseBuilder().setGeneralUse(true).build();
    Integer id = datasetDAO.insertDataset(name, now, user.getUserId(), objectId,
        dataUse.toString(), null);
    createDatasetProperties(id);
    return datasetDAO.findDatasetById(id);
  }

  private void createDatasetProperties(Integer datasetId) {
    List<DatasetProperty> list = new ArrayList<>();
    DatasetProperty dsp = new DatasetProperty();
    dsp.setDataSetId(datasetId);
    dsp.setPropertyKey(1);
    dsp.setPropertyValue("Test_PropertyValue");
    dsp.setCreateDate(new Date());
    list.add(dsp);
    datasetDAO.insertDatasetProperties(list);
  }

  private LibraryCard createLibraryCard(User user) {
    Integer institutionId = createInstitution().getId();
    String stringValue = "value";
    Integer id = libraryCardDAO.insertLibraryCard(user.getUserId(), institutionId, stringValue,
        user.getDisplayName(), user.getEmail(), user.getUserId(), new Date());
    return libraryCardDAO.findLibraryCardById(id);
  }

  private Institution createInstitution() {
    User createUser = createUser();
    Integer id = institutionDAO.insertInstitution(RandomStringUtils.randomAlphabetic(20),
        "itDirectorName",
        "itDirectorEmail",
        RandomStringUtils.randomAlphabetic(10),
        new Random().nextInt(),
        RandomStringUtils.randomAlphabetic(10),
        RandomStringUtils.randomAlphabetic(10),
        RandomStringUtils.randomAlphabetic(10),
        OrganizationType.NON_PROFIT.getValue(),
        createUser.getUserId(),
        createUser.getCreateDate());
    Institution institution = institutionDAO.findInstitutionById(id);
    User updateUser = createUser();
    institutionDAO.updateInstitutionById(
        id,
        institution.getName(),
        institution.getItDirectorEmail(),
        institution.getItDirectorName(),
        institution.getInstitutionUrl(),
        institution.getDunsNumber(),
        institution.getOrgChartUrl(),
        institution.getVerificationUrl(),
        institution.getVerificationFilename(),
        institution.getOrganizationType().getValue(),
        updateUser.getUserId(),
        new Date()
    );
    return institutionDAO.findInstitutionById(id);
  }

  private DarCollection createDarCollectionMultipleUserProperties() {
    User user = createUser();
    Integer userId = user.getUserId();
    createUserProperty(userId, UserFields.SUGGESTED_SIGNING_OFFICIAL.getValue());
    createUserProperty(userId, UserFields.SUGGESTED_INSTITUTION.getValue());
    createUserProperty(userId, UserFields.ERA_STATUS.getValue());
    String darCode = "DAR-" + RandomUtils.nextInt(100, 1000);
    Integer collection_id = darCollectionDAO.insertDarCollection(darCode, user.getUserId(),
        new Date());
    Dataset dataset = createDataset();
    DataAccessRequest dar = createDataAccessRequest(user.getUserId(), collection_id, darCode);
    dataAccessRequestDAO.insertDARDatasetRelation(dar.getReferenceId(), dataset.getDataSetId());
    Election cancelled = createCancelledAccessElection(dar.getReferenceId(),
        dataset.getDataSetId());
    Election access = createDataAccessElection(dar.getReferenceId(), dataset.getDataSetId());
    createFinalVote(user.getUserId(), cancelled.getElectionId());
    createFinalVote(user.getUserId(), access.getElectionId());
    createDataAccessRequest(user.getUserId(), collection_id, darCode);
    createDataAccessRequest(user.getUserId(), collection_id, darCode);
    return darCollectionDAO.findDARCollectionByCollectionId(collection_id);
  }

  private void createUserProperty(Integer userId, String field) {
    UserProperty property = new UserProperty();
    property.setPropertyKey(field);
    property.setPropertyValue(UUID.randomUUID().toString());
    property.setUserId(userId);
    userPropertyDAO.insertAll(List.of(property));
  }

  private Election createCancelledAccessElection(String referenceId, Integer datasetId) {
    Integer electionId = electionDAO.insertElection(
        ElectionType.DATA_ACCESS.getValue(),
        ElectionStatus.CANCELED.getValue(),
        new Date(),
        referenceId,
        datasetId
    );
    return electionDAO.findElectionById(electionId);
  }

  private DarCollection createDarCollection() {
    User user = createUserWithInstitution();
    String darCode = "DAR-" + RandomUtils.nextInt(1, 10000);
    Integer collection_id = darCollectionDAO.insertDarCollection(darCode, user.getUserId(),
        new Date());
    Dataset dataset = createDataset();
    DataAccessRequest dar = createDataAccessRequest(user.getUserId(), collection_id, darCode);
    dataAccessRequestDAO.insertDARDatasetRelation(dar.getReferenceId(), dataset.getDataSetId());
    Election cancelled = createCancelledAccessElection(dar.getReferenceId(),
        dataset.getDataSetId());
    Election access = createDataAccessElection(dar.getReferenceId(), dataset.getDataSetId());
    createFinalVote(user.getUserId(), cancelled.getElectionId());
    createFinalVote(user.getUserId(), access.getElectionId());
    createDataAccessRequest(user.getUserId(), collection_id, darCode);
    createDataAccessRequest(user.getUserId(), collection_id, darCode);
    return darCollectionDAO.findDARCollectionByCollectionId(collection_id);
  }

  private DataAccessRequest createDraftDataAccessRequest() {
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

  private Vote createFinalVote(Integer userId, Integer electionId) {
    Integer voteId = voteDAO.insertVote(userId, electionId, VoteType.FINAL.getValue());
    return voteDAO.findVoteById(voteId);
  }

  private Election createDataAccessElection(String referenceId, Integer datasetId) {
    Integer electionId = electionDAO.insertElection(
        ElectionType.DATA_ACCESS.getValue(),
        ElectionStatus.OPEN.getValue(),
        new Date(),
        referenceId,
        datasetId
    );
    return electionDAO.findElectionById(electionId);
  }

  private User createUserWithInstitution() {
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
    return userDAO.findUserById(userId);
  }

}
