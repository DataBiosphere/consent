package org.broadinstitute.consent.http.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.broadinstitute.consent.http.enumeration.ElectionStatus;
import org.broadinstitute.consent.http.enumeration.ElectionType;
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
import org.broadinstitute.consent.http.models.LibraryCard;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserProperty;
import org.broadinstitute.consent.http.models.Vote;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.postgresql.util.PSQLException;
import org.postgresql.util.PSQLState;

@ExtendWith(MockitoExtension.class)
class DarCollectionDAOTest extends DAOTestHelper {

  private void generateDatasetElectionForCollection(DarCollection collection) {
    DataAccessRequest dar = collection.getDars().values().stream()
        .filter(d -> !d.getElections().isEmpty()).findFirst().orElseThrow();
    String referenceId = dar.getReferenceId();
    Election election = dar.getElections().values().stream().findFirst().orElse(null);
    assertNotNull(election);
    Integer datasetId = election.getDatasetId();
    electionDAO.insertElection("DataSet", "Open", new Date(), referenceId, datasetId);
  }

  private List<Election> getElectionsFromCollection(DarCollection collection) {
    return collection.getDars().values().stream()
        .map(DataAccessRequest::getElections)
        .map(Map::values)
        .flatMap(Collection::stream)
        .toList();
  }

  private List<Election> getDatasetElectionsFromElection(List<Election> elections) {
    return elections.stream()
        .filter(e -> e.getElectionType().equalsIgnoreCase("dataset"))
        .toList();
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
    assertFalse(elections.isEmpty());
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
        .toList();
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
    assertNotNull(returned.getMostRecentDar().getEraCommonsId());
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

    assertNull(returned.getCreateUser().getLibraryCard());
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
    createLibraryCard(user);
    User updatedUser = userDAO.findUserById(user.getUserId());
    String darCode = "DAR-" + randomInt(100, 1000);
    Integer collectionId = darCollectionDAO.insertDarCollection(darCode, updatedUser.getUserId(),
        new Date());
    createDataAccessRequest(updatedUser.getUserId(), collectionId);

    DarCollection collection = darCollectionDAO.findDARCollectionByCollectionId(collectionId);
    User returnedUser = collection.getCreateUser();
    assertEquals(updatedUser, returnedUser);

    LibraryCard returnedLibraryCard = returnedUser.getLibraryCard();
    assertNotNull(returnedLibraryCard);
    assertEquals(updatedUser.getLibraryCard(), returnedLibraryCard);
  }

  @Test
  void testFindDARCollectionByCollectionIdNegative() {
    DarCollection returned = darCollectionDAO.findDARCollectionByCollectionId(
        randomInt(1000, 2000));
    assertNull(returned);
  }

  @Test
  void testFindDarCollectionByIdWithSigningOfficial() {
    User user = createUser();
    var collectionWithDataset = createDarCollectionWithDataset(user);
    DataAccessRequest testDar1 = (DataAccessRequest) collectionWithDataset.get(4);
    Dataset dataset = (Dataset) collectionWithDataset.get(2);
    DataAccessRequest testDar2 = createDAR(user, dataset, testDar1.getCollectionId());
    dataAccessRequestDAO.updateDarCloseoutSO(user.getUserId(), testDar2.getReferenceId());

    DataAccessRequest testDar2Stored = dataAccessRequestDAO.findByReferenceId(
        testDar2.getReferenceId());
    assertNotNull(testDar2Stored.getCloseoutSigningOfficialApprovedUserId());
    assertNotNull(testDar2Stored.getCloseoutSigningOfficialApprovedDate());

    DarCollection darCollection = darCollectionDAO.findDARCollectionByCollectionId(
        testDar2.getCollectionId());

    assertNotNull(darCollection.getMostRecentDar().getCloseoutSigningOfficialApprovedDate());
    assertEquals(user.getUserId(),
        darCollection.getMostRecentDar().getCloseoutSigningOfficialApprovedUserId());

    DarCollection darCollectionByReferenceId = darCollectionDAO.findDARCollectionByReferenceId(
        testDar2.getReferenceId());
    assertNotNull(
        darCollectionByReferenceId.getMostRecentDar().getCloseoutSigningOfficialApprovedDate());
    assertEquals(user.getUserId(),
        darCollectionByReferenceId.getMostRecentDar().getCloseoutSigningOfficialApprovedUserId());

    DarCollection darCollectionWithElectionsById = darCollectionDAO.findCollectionWithAllElectionsByCollectionId(
        testDar2.getCollectionId());
    assertNotNull(
        darCollectionWithElectionsById.getMostRecentDar().getCloseoutSigningOfficialApprovedDate());
    assertEquals(user.getUserId(), darCollectionWithElectionsById.getMostRecentDar()
        .getCloseoutSigningOfficialApprovedUserId());

    List<DarCollection> darCollectionList = darCollectionDAO.findDARCollectionByCollectionIds(
        List.of(testDar2.getCollectionId()));
    assertEquals(1, darCollectionList.size());
    assertNotNull(
        darCollectionList.get(0).getMostRecentDar().getCloseoutSigningOfficialApprovedDate());
    assertEquals(user.getUserId(),
        darCollectionList.get(0).getMostRecentDar().getCloseoutSigningOfficialApprovedUserId());
  }

  @Test
  void testFindCollectionWithAllElectionsByCollectionId() {
    // creates 3 dars, one dar has 2 elections, one cancelled, one open, the rest with no elections
    // each election has one vote (final)
    // uses findDarCollectionById internally
    DarCollection collection = createDarCollectionMultipleUserProperties();
    DarCollection returned = darCollectionDAO.findCollectionWithAllElectionsByCollectionId(
        collection.getDarCollectionId());
    assertNotNull(returned);
    // all values should be the same as the collection returned by findDarCollectionById
    // except the collection returned by findCollectionWithAllDataAccessElectionsById will include
    // both the created elections
    assertCollectionEqualExceptForElections(collection, returned);
    List<Election> elections = getElectionsFromCollection(returned)
        .stream()
        .sorted(Comparator.comparing(Election::getCreateDate))
        .toList();
    assertEquals(2, elections.size());
    Election firstElection = elections.get(0);
    assertExpectedElection(firstElection, ElectionStatus.CANCELED.getValue());
    Election secondElection = elections.get(1);
    assertExpectedElection(secondElection, ElectionStatus.OPEN.getValue());

    List<UserProperty> userProperties = returned.getCreateUser().getProperties();
    assertFalse(userProperties.isEmpty());
    userProperties.forEach(p -> assertEquals(collection.getCreateUserId(),
        p.getUserId()));

    assertNull(returned.getCreateUser().getLibraryCard());
  }

  @Test
  void testFindCollectionWithAllElectionsByCollectionIdLC() {
    User user = createUser();
    createLibraryCard(user);
    User updatedUser = userDAO.findUserById(user.getUserId());
    String darCode = "DAR-" + randomInt(100, 1000);
    Integer collectionId = darCollectionDAO.insertDarCollection(darCode, updatedUser.getUserId(),
        new Date());
    createDataAccessRequest(updatedUser.getUserId(), collectionId);

    DarCollection collection = darCollectionDAO.findCollectionWithAllElectionsByCollectionId(
        collectionId);
    User returnedUser = collection.getCreateUser();
    assertEquals(updatedUser, returnedUser);

    LibraryCard returnedLibraryCard = returnedUser.getLibraryCard();
    assertNotNull(returnedLibraryCard);
    assertEquals(updatedUser.getLibraryCard(), returnedLibraryCard);
  }

  @Test
  void testFindCollectionWithAllElectionsByCollectionIdNegative() {
    DarCollection returned = darCollectionDAO.findCollectionWithAllElectionsByCollectionId(
        randomInt(1000, 2000));
    assertNull(returned);
  }

  @Test
  void testFindCollectionWithElectionsByCollectionIdAndDatasetIds() {
    // creates 3 dars, one dar has 2 datasets, each with 2 elections, one cancelled, one open,
    // the other two dars in the collections have no elections
    // each election has one vote (final)
    // uses findDarCollectionById internally
    DarCollection collection = createDarCollectionMultipleDatasetElections();
    List<Integer> datasetIds = collection.getDatasets().stream()
        .sorted(Comparator.comparing(Dataset::getDatasetId))
        .map(Dataset::getDatasetId).toList();
    DarCollection returnedAll = darCollectionDAO.findCollectionWithElectionsByCollectionIdAndDatasetIds(
        datasetIds, collection.getDarCollectionId());
    assertNotNull(returnedAll);
    // all values should be the same as the collection returned by findDarCollectionById
    // except the collection returned by findCollectionWithDataAccessElectionsByCollectionIdAndDatasetIds will include
    // all the created elections for the specified datasets in the collection
    assertCollectionEqualExceptForElections(collection, returnedAll);
    List<Election> elections = getElectionsFromCollection(returnedAll)
        .stream()
        .sorted(Comparator.comparing(Election::getCreateDate))
        .toList();
    assertEquals(4, elections.size());
    Election firstElection = elections.get(0);
    assertEquals(datasetIds.get(0), firstElection.getDatasetId());
    assertExpectedElection(firstElection, ElectionStatus.CANCELED.getValue());
    Election secondElection = elections.get(1);
    assertEquals(datasetIds.get(0), secondElection.getDatasetId());
    assertExpectedElection(secondElection, ElectionStatus.OPEN.getValue());
    Election thirdElection = elections.get(2);
    assertEquals(datasetIds.get(1), thirdElection.getDatasetId());
    assertExpectedElection(thirdElection, ElectionStatus.CANCELED.getValue());
    Election fourthElection = elections.get(3);
    assertEquals(datasetIds.get(1), fourthElection.getDatasetId());
    assertExpectedElection(fourthElection, ElectionStatus.OPEN.getValue());

    List<UserProperty> userProperties = returnedAll.getCreateUser().getProperties();
    assertFalse(userProperties.isEmpty());
    userProperties.forEach(p -> assertEquals(collection.getCreateUserId(),
        p.getUserId()));

    assertNull(returnedAll.getCreateUser().getLibraryCard());
  }

  @Test
  void testFindCollectionWithElectionsByCollectionIdAndDatasetIdsOneDataset() {
    // creates 3 dars, one dar has 2 datasets, each with 2 elections, one cancelled, one open,
    // the other two dars in the collections have no elections
    // each election has one vote (final)
    // uses findDarCollectionById internally
    DarCollection collection = createDarCollectionMultipleDatasetElections();
    List<Integer> datasetId = List.of(collection.getDatasets().stream()
        .map(Dataset::getDatasetId).toList().get(0));
    DarCollection returnedAll = darCollectionDAO.findCollectionWithElectionsByCollectionIdAndDatasetIds(
        datasetId, collection.getDarCollectionId());
    assertNotNull(returnedAll);
    // all values should be the same as the collection returned by findDarCollectionById
    // except the collection returned by findCollectionWithAllDataAccessElectionsByCollectionIdAndDatasetIds
    // will include all the created elections for the specified datasets in the collection
    assertCollectionEqualExceptForElections(collection, returnedAll);
    List<Election> elections = getElectionsFromCollection(returnedAll)
        .stream()
        .sorted(Comparator.comparing(Election::getCreateDate))
        .toList();
    assertEquals(2, elections.size());
    Election firstElection = elections.get(0);
    assertEquals(datasetId.get(0), firstElection.getDatasetId());
    assertExpectedElection(firstElection, ElectionStatus.CANCELED.getValue());
    Election secondElection = elections.get(1);
    assertEquals(datasetId.get(0), secondElection.getDatasetId());
    assertExpectedElection(secondElection, ElectionStatus.OPEN.getValue());

    List<UserProperty> userProperties = returnedAll.getCreateUser().getProperties();
    assertFalse(userProperties.isEmpty());
    userProperties.forEach(p -> assertEquals(collection.getCreateUserId(),
        p.getUserId()));

    assertNull(returnedAll.getCreateUser().getLibraryCard());
  }

  @Test
  void testFindCollectionWithElectionsByCollectionIdAndDatasetIdsNoDataset() {
    // creates 3 dars, one dar has 2 datasets, each with 2 elections, one cancelled, one open,
    // the other two dars in the collections have no elections
    // each election has one vote (final)
    // uses findDarCollectionById internally
    DarCollection collection = createDarCollectionMultipleDatasetElections();
    List<Integer> datasetIds = List.of();
    DarCollection returnedAll = darCollectionDAO.findCollectionWithElectionsByCollectionIdAndDatasetIds(
        datasetIds, collection.getDarCollectionId());
    assertNotNull(returnedAll);
    // all values should be the same as the collection returned by findDarCollectionById
    // except the collection returned by findCollectionWithDataAccessElectionsByCollectionIdAndDatasetIds will include
    // no elections because no dataset was specified
    assertCollectionEqualExceptForElections(collection, returnedAll);
    List<Election> elections = getElectionsFromCollection(returnedAll);
    assertEquals(0, elections.size());
    List<UserProperty> userProperties = returnedAll.getCreateUser().getProperties();
    assertFalse(userProperties.isEmpty());
    userProperties.forEach(p -> assertEquals(collection.getCreateUserId(),
        p.getUserId()));

    assertNull(returnedAll.getCreateUser().getLibraryCard());
  }

  @Test
  void testFindCollectionWithElectionsByCollectionIdAndDatasetIdsOtherDataset() {
    // creates 3 dars, one dar has 2 datasets, each with 2 elections, one cancelled, one open,
    // the other two dars in the collections have no elections
    // each election has one vote (final)
    // uses findDarCollectionById internally
    DarCollection collection = createDarCollectionMultipleDatasetElections();
    List<Integer> datasetIds = List.of(randomInt(1000, 2000));
    DarCollection returnedAll = darCollectionDAO.findCollectionWithElectionsByCollectionIdAndDatasetIds(
        datasetIds, collection.getDarCollectionId());
    assertNotNull(returnedAll);
    // all values should be the same as the collection returned by findDarCollectionById
    // except the collection returned by findCollectionWithDataAccessElectionsByCollectionIdAndDatasetIds will include
    // no elections since the specified dataset is not in the collection
    assertCollectionEqualExceptForElections(collection, returnedAll);
    List<Election> elections = getElectionsFromCollection(returnedAll);
    assertEquals(0, elections.size());
    List<UserProperty> userProperties = returnedAll.getCreateUser().getProperties();
    assertFalse(userProperties.isEmpty());
    userProperties.forEach(p -> assertEquals(collection.getCreateUserId(),
        p.getUserId()));

    assertNull(returnedAll.getCreateUser().getLibraryCard());
  }

  @Test
  void testFindCollectionWithElectionsByCollectionIdAndDatasetsLC() {
    User user = createUser();
    createLibraryCard(user);
    User updatedUser = userDAO.findUserById(user.getUserId());
    String darCode = "DAR-" + randomInt(100, 1000);
    Integer collectionId = darCollectionDAO.insertDarCollection(darCode, updatedUser.getUserId(),
        new Date());
    createDataAccessRequest(updatedUser.getUserId(), collectionId);

    DarCollection collection = darCollectionDAO.findCollectionWithElectionsByCollectionIdAndDatasetIds(List.of(), collectionId);
    User returnedUser = collection.getCreateUser();
    assertEquals(updatedUser, returnedUser);

    LibraryCard returnedLibraryCard = returnedUser.getLibraryCard();
    assertNotNull(returnedLibraryCard);
    assertEquals(updatedUser.getLibraryCard(), returnedLibraryCard);
  }

  @Test
  void testFindCollectionWithElectionsByCollectionIdAndDatasetsNegative() {
    DarCollection returned = darCollectionDAO.findCollectionWithElectionsByCollectionIdAndDatasetIds(List.of(1),
        randomInt(1000, 2000));
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
    testDar.setEraCommonsId(user.getEraCommonsId());
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
        testDar.getData(),
        user.getEraCommonsId()
    );
    dataAccessRequestDAO.insertDARDatasetRelation(testDar.getReferenceId(), dataset.getDatasetId());
    return testDar;
  }

  // local method to create a test DAC
  Dac createDAC() {
    Integer id = dacDAO.createDac(
        "Test_" + randomAlphanumeric(20),
        "Test_" + randomAlphanumeric(20),
        new Date());
    return dacDAO.findById(id);
  }

  // local method to create a test DAR Collection and dataset
  // takes in user as a parameter so we can test multiple collections with the same user
  // this method returns a list that includes: user, now, dataset, collectionId, testDar, dac, testDarCollection
  List<Object> createDarCollectionWithDataset(User user) {
    Timestamp now = new Timestamp(new Date().getTime());
    String darCode = "DAR-" + randomAlphanumeric(25);
    Dataset dataset = createDataset();

    // creating a collection and DAR
    Integer collectionId = darCollectionDAO.insertDarCollection(darCode, user.getUserId(), now);
    DataAccessRequest testDar = createDAR(user, dataset, collectionId);

    // create a DAC
    Dac dac = createDAC();
    datasetDAO.updateDatasetDacId(dataset.getDatasetId(), dac.getDacId());
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
    assertEquals(validCollection, testDarCollection2);
  }

  @Test
  void testFindDARCollectionByReferenceIdIncludesERACommonsId() {
    User user = createUserWithInstitution();
    String eraCommonsId = randomAlphabetic(20);
    user.setEraCommonsId(eraCommonsId);
    List<Object> newDarCollection1 = createDarCollectionWithDataset(user);
    DataAccessRequest testDar1 = (DataAccessRequest) newDarCollection1.get(4);
    DarCollection testDarCollection = darCollectionDAO.findDARCollectionByReferenceId(testDar1.getReferenceId());
    assertNotNull(testDarCollection.getMostRecentDar().getEraCommonsId());
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

  @Test
  void testFindMostRecentDarInCollection() {
    User user = createUserWithInstitution();
    List<Object> newDarCollection1 = createDarCollectionWithDataset(user);
    DataAccessRequest testDar1 = (DataAccessRequest) newDarCollection1.get(4);
    Dataset dataset = (Dataset) newDarCollection1.get(2);
    DataAccessRequest testDar2 = createDAR(user, dataset, testDar1.getCollectionId());
    DarCollection returnedCollection = darCollectionDAO.findDARCollectionByCollectionId(testDar1.getCollectionId());
    assertTrue(testDar2.getSubmissionDate().after(testDar1.getSubmissionDate()));
    assertNotEquals(testDar2.getReferenceId(), testDar1.getReferenceId());
    assertEquals(testDar2.getReferenceId(), returnedCollection.getMostRecentDar().getReferenceId());
  }

  /**
   * Creates a new user, dataset, data access request, and dar collection
   *
   * @return Populated DataAccessRequest
   */
  private DataAccessRequest createDataAccessRequest(Integer userId, Integer collectionId) {
    DataAccessRequestData data = new DataAccessRequestData();
    data.setProjectTitle("Project Title: " + randomAlphabetic(50));
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
        data,
        randomAlphabetic(10));
    return dataAccessRequestDAO.findByReferenceId(referenceId);
  }

  private Dataset createDataset() {
    User user = createUser();
    String name = "Name_" + randomAlphanumeric(20);
    Timestamp now = new Timestamp(new Date().getTime());
    String objectId = "Object ID_" + randomAlphanumeric(20);
    DataUse dataUse = new DataUseBuilder().setGeneralUse(true).build();
    Integer id = datasetDAO.insertDataset(name, now, user.getUserId(), objectId,
        dataUse.toString(), null);
    createDatasetProperties(id);
    return datasetDAO.findDatasetById(id);
  }

  private void createDatasetProperties(Integer datasetId) {
    List<DatasetProperty> list = new ArrayList<>();
    DatasetProperty dsp = new DatasetProperty();
    dsp.setDatasetId(datasetId);
    dsp.setPropertyKey(1);
    dsp.setPropertyValue("Test_PropertyValue");
    dsp.setCreateDate(new Date());
    list.add(dsp);
    datasetDAO.insertDatasetProperties(list);
  }

  private LibraryCard createLibraryCard(User user) {
    Integer id = libraryCardDAO.insertLibraryCard(user.getUserId(),
        user.getDisplayName(), user.getEmail(), user.getUserId(), new Date());
    return libraryCardDAO.findLibraryCardById(id);
  }

  private DarCollection createDarCollectionMultipleUserProperties() {
    User user = createUser();
    Integer userId = user.getUserId();
    createUserProperty(userId, UserFields.ERA_STATUS.getValue());
    String darCode = "DAR-" + randomInt(100, 1000);
    Integer collectionId = darCollectionDAO.insertDarCollection(darCode, user.getUserId(),
        new Date());
    Dataset dataset = createDataset();
    DataAccessRequest dar = createDataAccessRequest(user.getUserId(), collectionId);
    dataAccessRequestDAO.insertDARDatasetRelation(dar.getReferenceId(), dataset.getDatasetId());
    Election cancelled = createCancelledAccessElection(dar.getReferenceId(),
        dataset.getDatasetId());
    Election access = createDataAccessElection(dar.getReferenceId(), dataset.getDatasetId());
    createFinalVote(user.getUserId(), cancelled.getElectionId());
    createFinalVote(user.getUserId(), access.getElectionId());
    createDataAccessRequest(user.getUserId(), collectionId);
    createDataAccessRequest(user.getUserId(), collectionId);
    return darCollectionDAO.findDARCollectionByCollectionId(collectionId);
  }

  private DarCollection createDarCollectionMultipleDatasetElections() {
    User user = createUser();
    Integer userId = user.getUserId();
    createUserProperty(userId, UserFields.ERA_STATUS.getValue());
    String darCode = "DAR-" + randomInt(100, 1000);
    Integer collectionId = darCollectionDAO.insertDarCollection(darCode, user.getUserId(),
        new Date());
    DataAccessRequest dar = createDataAccessRequest(user.getUserId(), collectionId);

    Dataset dataset1 = createDataset();
    dataAccessRequestDAO.insertDARDatasetRelation(dar.getReferenceId(), dataset1.getDatasetId());
    Election cancelled1 = createCancelledAccessElection(dar.getReferenceId(),
        dataset1.getDatasetId());
    Election access1 = createDataAccessElection(dar.getReferenceId(), dataset1.getDatasetId());
    createFinalVote(user.getUserId(), cancelled1.getElectionId());
    createFinalVote(user.getUserId(), access1.getElectionId());

    Dataset dataset2 = createDataset();
    dataAccessRequestDAO.insertDARDatasetRelation(dar.getReferenceId(), dataset2.getDatasetId());
    Election cancelled2 = createCancelledAccessElection(dar.getReferenceId(),
        dataset2.getDatasetId());
    Election access2 = createDataAccessElection(dar.getReferenceId(), dataset2.getDatasetId());
    createFinalVote(user.getUserId(), cancelled2.getElectionId());
    createFinalVote(user.getUserId(), access2.getElectionId());

    createDataAccessRequest(user.getUserId(), collectionId);
    createDataAccessRequest(user.getUserId(), collectionId);
    DarCollection darCollection = darCollectionDAO.findDARCollectionByCollectionId(collectionId);
    darCollection.setDatasets(new HashSet<>(List.of(dataset1, dataset2)));
    return darCollection;
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
    String darCode = "DAR-" + randomInt(1, 10000);
    Integer collectionId = darCollectionDAO.insertDarCollection(darCode, user.getUserId(),
        new Date());
    Dataset dataset = createDataset();
    DataAccessRequest dar = createDataAccessRequest(user.getUserId(), collectionId);
    dataAccessRequestDAO.insertDARDatasetRelation(dar.getReferenceId(), dataset.getDatasetId());
    Election cancelled = createCancelledAccessElection(dar.getReferenceId(),
        dataset.getDatasetId());
    Election access = createDataAccessElection(dar.getReferenceId(), dataset.getDatasetId());
    createFinalVote(user.getUserId(), cancelled.getElectionId());
    createFinalVote(user.getUserId(), access.getElectionId());
    createDataAccessRequest(user.getUserId(), collectionId);
    createDataAccessRequest(user.getUserId(), collectionId);
    return darCollectionDAO.findDARCollectionByCollectionId(collectionId);
  }

  private DataAccessRequest createDraftDataAccessRequest() {
    User user = createUser();
    DataAccessRequestData data = new DataAccessRequestData();
    data.setProjectTitle("Project Title: " + randomAlphabetic(50));
    String referenceId = UUID.randomUUID().toString();
    Date now = new Date();
    dataAccessRequestDAO.insertDraftDataAccessRequest(
        referenceId,
        user.getUserId(),
        now,
        now,
        now,
        data
    );
    return dataAccessRequestDAO.findByReferenceId(referenceId);
  }

  private void createFinalVote(Integer userId, Integer electionId) {
    Integer voteId = voteDAO.insertVote(userId, electionId, VoteType.FINAL.getValue());
    voteDAO.findVoteById(voteId);
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

  private void assertExpectedElection(Election e, String status) {
    assertNotNull(e.getElectionId());
    assertNotNull(e.getCreateDate());
    assertNotNull(e.getReferenceId());
    assertEquals(ElectionType.DATA_ACCESS.getValue(), e.getElectionType());
    assertEquals(status, e.getStatus());
    List<Vote> votes = e.getVotes().values().stream().toList();
    assertEquals(1, votes.size());
    Vote vote = votes.get(0);
    assertNotNull(vote.getVoteId());
    assertEquals(VoteType.FINAL.getValue(), vote.getType());
    assertNotNull(vote.getCreateDate());
    assertNotNull(vote.getUserId());
    assertEquals(vote.getElectionId(), e.getElectionId());
  }

  private void assertCollectionEqualExceptForElections(DarCollection collection, DarCollection returned) {
    DarCollection collectionCopy = collection.deepCopy();
    collectionCopy.getDars().values().forEach(dar -> dar.setElections(new HashMap<>()));
    DarCollection returnedCopy = returned.deepCopy();
    returnedCopy.getDars().values().forEach(dar -> dar.setElections(new HashMap<>()));
    assertEquals(collectionCopy, returnedCopy);
  }
}
