package org.broadinstitute.consent.http.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DarCollection;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.Institution;
import org.broadinstitute.consent.http.models.LibraryCard;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserProperty;
import org.broadinstitute.consent.http.models.Vote;
import org.junit.jupiter.api.Test;
import org.postgresql.util.PSQLException;
import org.postgresql.util.PSQLState;


public class DarCollectionDAOTest extends DAOTestHelper {

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
  public void testFindAllDARCollections() {
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
  public void testFindAllDarCollectionsMultipleUserProperties() {
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
  public void testFindDARCollectionByReferenceId() {
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
  public void testFindDARCollectionByReferenceIdNegative() {
    //dar without a collection ID
    DataAccessRequest dar = createDraftDataAccessRequest();
    DarCollection collection = darCollectionDAO.findDARCollectionByReferenceId(
        dar.getReferenceId());
    assertNull(collection);
  }

  @Test
  public void testFindDARCollectionsByReferenceIds() {
    DataAccessRequest dar1 = createDataAccessRequestV3();
    DataAccessRequest dar2 = createDataAccessRequestV3();
    DataAccessRequest dar3 = createDataAccessRequestV3();
    List<DarCollection> darCollections = darCollectionDAO.findDARCollectionsByReferenceIds(
        List.of(dar1.getReferenceId(), dar2.getReferenceId()));
    assertNotNull(darCollections);
    assertEquals(2, darCollections.size());
    List<Integer> darCollectionIds = darCollections.stream().map(DarCollection::getDarCollectionId)
        .collect(Collectors.toList());
    assertTrue(darCollectionIds.contains(dar1.collectionId));
    assertTrue(darCollectionIds.contains(dar2.collectionId));
    assertFalse(darCollectionIds.contains(dar3.collectionId));
  }

  @Test
  public void testFindDARCollectionByCollectionId() {
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
  public void testFindDARCollectionByCollectionIdMultipleUserProperties() {
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
  public void testFindDARCollectionByCollectionIdLibraryCard() {
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
  public void testFindDARCollectionByCollectionIdNegative() {
    DarCollection returned = darCollectionDAO.findDARCollectionByCollectionId(
        RandomUtils.nextInt(1000, 2000));
    assertNull(returned);
  }

  @Test
  public void testFindDARCollectionIdsByDacIds() {
    // Set up a DAR Collection with a DAR, Dataset, Consent, Consent Association,
    // and DAC in such a way that all are connected via the dataset id.
    Dataset dataset = createDataset();
    DarCollection c = createDarCollection();
    generateDatasetElectionForCollection(c);
    DataAccessRequest dar = new ArrayList<>(c.getDars().values()).get(0);
    if (Objects.isNull(dar)) {
      fail("DAR was not created in collection");
    }
    dataAccessRequestDAO.insertDARDatasetRelation(dar.getReferenceId(), dataset.getDataSetId());
    dataAccessRequestDAO.updateDataByReferenceId(dar.getReferenceId(), dar.getUserId(), new Date(),
        new Date(), new Date(), dar.getData());
    Dac dac = createDac();
    Consent consent = createConsent();
    datasetDAO.updateDatasetDacId(dataset.getDataSetId(), dac.getDacId());
    consentDAO.insertConsentAssociation(consent.getConsentId(), ASSOCIATION_TYPE_TEST,
        dataset.getDataSetId());

    List<Integer> collectionIds = darCollectionDAO.findDARCollectionIdsByDacIds(
        List.of(dac.getDacId()));
    assertFalse(collectionIds.isEmpty());
    assertEquals(c.getDarCollectionId(), collectionIds.get(0));
  }

  @Test
  public void testFindDARCollectionIdsByInstitutionId() {
    // Set up a DAR Collection with a DAR, User, and Institution
    DarCollection c = createDarCollection();
    DataAccessRequest dar = new ArrayList<>(c.getDars().values()).get(0);
    if (Objects.isNull(dar)) {
      fail("DAR was not created in collection");
    }
    User user = userDAO.findUserById(dar.getUserId());
    Institution institution = createInstitution();
    user.setInstitutionId(institution.getId());
    userDAO.updateUser(user.getDisplayName(), user.getUserId(), user.getInstitutionId());

    List<Integer> collectionIds = darCollectionDAO.findDARCollectionIdsByInstitutionId(
        institution.getId());
    assertFalse(collectionIds.isEmpty());
    assertEquals(c.getDarCollectionId(), collectionIds.get(0));
  }

  @Test
  public void testInsertDARCollection() {
    List<DarCollection> allBefore = darCollectionDAO.findAllDARCollections();
    assertTrue(allBefore.isEmpty());
    DarCollection collection = createDarCollection();
    assertNotNull(collection);
    List<DarCollection> allAfter = darCollectionDAO.findAllDARCollections();
    assertTrue(allAfter.contains(collection));
  }

  @Test
  public void testInsertDarCollectionNegative() {
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
  public void testUpdateDARCollection() {
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
  public void testUpdateDarCollectionNegative() {
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
  public void testDeleteByCollectionId() {
    DarCollection collection = createDarCollection();
    collection.getDars().keySet()
        .forEach(k -> dataAccessRequestDAO.deleteDARDatasetRelationByReferenceId(k));
    dataAccessRequestDAO.deleteByCollectionId(collection.getDarCollectionId());
    darCollectionDAO.deleteByCollectionId(collection.getDarCollectionId());
    assertNull(
        darCollectionDAO.findDARCollectionByCollectionId(collection.getDarCollectionId()));
  }

  @Test
  public void testDeleteByCollectionIdNegative() {
    try {
      darCollectionDAO.deleteByCollectionId(RandomUtils.nextInt(100, 1000));
    } catch (Exception e) {
      assertEquals(PSQLState.UNIQUE_VIOLATION.getState(),
          ((PSQLException) e.getCause()).getSQLState());
    }
  }

  @Test
  public void testFindAllDARCollectionsCreatedByUserId_NegativeDatasetElection() {
    DarCollection collection = createDarCollection();
    generateDatasetElectionForCollection(collection);
    List<DarCollection> collections = darCollectionDAO.findDARCollectionsCreatedByUserId(collection.getCreateUserId());
    assertEquals(1, collections.size());
    DarCollection targetCollection = collections.get(0);
    List<Election> elections = getElectionsFromCollection(targetCollection);
    List<Election> datasetElections = getDatasetElectionsFromElection(elections);
    assertTrue(datasetElections.isEmpty());
  }

  @Test
  public void testFindAllDARCollectionsCreatedByUserId() {
    DarCollection collection = createDarCollectionMultipleUserProperties();
    Map<String, DataAccessRequest> dars = collection.getDars();
    createDarCollection(); //create new collection associated with different user
    Integer userId = collection.getCreateUserId();
    List<DarCollection> collectionResult = darCollectionDAO.findDARCollectionsCreatedByUserId(
        userId);
    assertEquals(1, collectionResult.size());
    assertEquals(userId, collectionResult.get(0).getCreateUserId());

    DarCollection collectionRecord = collectionResult.get(0);
    List<Election> electionList = dars.values().stream()
        .map(d -> d.getElections().values())
        .flatMap(Collection::stream)
        .collect(Collectors.toList());

    assertEquals(1, electionList.size());
    List<String> referenceIds = collectionRecord.getDars().values().stream()
        .map(DataAccessRequest::getReferenceId)
        .collect(Collectors.toList());

    assertTrue(referenceIds.contains(electionList.get(0).getReferenceId()));

    List<UserProperty> userProperties = collectionResult.get(0).getCreateUser().getProperties();
    assertFalse(userProperties.isEmpty());
    userProperties.forEach(p -> assertEquals(userId, p.getUserId()));
  }

  @Test
  public void testFindAllDARCollectionsCreatedByUserIdWithMultipleProperties() {
    DarCollection collection = createDarCollectionMultipleUserProperties();
    createDarCollection(); //create new collection associated with different user
    Integer userId = collection.getCreateUserId();
    List<DarCollection> collectionResult = darCollectionDAO.findDARCollectionsCreatedByUserId(
        userId);
    assertEquals(1, collectionResult.size());

    DarCollection collectionRecord = collectionResult.get(0);
    List<UserProperty> userProperties = collectionRecord.getCreateUser().getProperties();
    assertFalse(userProperties.isEmpty());
    userProperties.forEach(p -> assertEquals(userId, p.getUserId()));
  }

    // local method to create a test DAR
    public DataAccessRequest createDAR(User user, Dataset dataset, Integer collectionId) {
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
  public Dac createDAC() {
    Integer id = dacDAO.createDac(
        "Test_" + RandomStringUtils.random(20, true, true),
        "Test_" + RandomStringUtils.random(20, true, true),
        new Date());
    return dacDAO.findById(id);
  }

  // local method to create a test DAR Collection and dataset
  // takes in user as a parameter so we can test multiple collections with the same user
  // this method returns a list that includes: user, now, dataset, collectionId, testDar, dac, testDarCollection
  public List<Object> createDarCollectionWithDataset(User user) {
    Timestamp now = new Timestamp(new Date().getTime());
    String darCode = "DAR-" + RandomStringUtils.randomAlphanumeric(25);
    Dataset dataset = createDataset();

    // creating a collection and DAR
    Integer collectionId = darCollectionDAO.insertDarCollection(darCode, user.getUserId(), now);
    DataAccessRequest testDar = createDAR(user, dataset, collectionId);

    // create a DAC
    Dac dac = createDAC();
    Consent consent = createConsent();
    datasetDAO.updateDatasetDacId(dataset.getDataSetId(), dac.getDacId());
    consentDAO.insertConsentAssociation(consent.getConsentId(), ASSOCIATION_TYPE_TEST,
        dataset.getDataSetId());

    DarCollection testDarCollection = darCollectionDAO.findDARCollectionByCollectionId(
        collectionId);

    return List.of(user, now, dataset, collectionId, testDar, dac, testDarCollection);
  }

  // findDARCollectionIdsByDacIds should exclude archived collections
  @Test
  public void testFindDARCollectionIdsByDacIdsArchived() {
    User user = createUserWithInstitution();

    List<Object> newDarCollection = createDarCollectionWithDataset(user);
    DataAccessRequest testDar = (DataAccessRequest) newDarCollection.get(4);
    Dac dac = (Dac) newDarCollection.get(5);
    DarCollection testDarCollection = (DarCollection) newDarCollection.get(6);

    dataAccessRequestDAO.archiveByReferenceIds(List.of(testDar.getReferenceId()));

    List<Integer> collectionIds = darCollectionDAO.findDARCollectionIdsByDacIds(
        List.of(dac.getDacId()));

    assertTrue(collectionIds.isEmpty());
    assertFalse(collectionIds.contains(testDarCollection));
  }

  // findDARCollectionIdsByInstitutionId should exclude archived collections
  @Test
  public void testFindDARCollectionIdsByInsitutionIdArchived() {
    User user = createUserWithInstitution();

    List<Object> newDarCollection = createDarCollectionWithDataset(user);
    DataAccessRequest testDar = (DataAccessRequest) newDarCollection.get(4);
    DarCollection testDarCollection = (DarCollection) newDarCollection.get(6);

    dataAccessRequestDAO.archiveByReferenceIds(List.of(testDar.getReferenceId()));

    List<Integer> collectionIds = darCollectionDAO.findDARCollectionIdsByInstitutionId(
        user.getInstitutionId());

    assertTrue(collectionIds.isEmpty());
    assertFalse(collectionIds.contains(testDarCollection));
  }

  // findDARCollectionByCollectionIds should exclude archived collections
  @Test
  public void testFindDARCollectionIdsByCollectionIdsArchived() {
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
  public void testFindAllDARCollectionsArchived() {
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

  // findDARCollectionsCreatedByUserId should exclude archived collections
  @Test
  public void testFindDARCollectionsCreatedByUserIdArchived() {
    User user = createUserWithInstitution();

    List<Object> newDarCollection1 = createDarCollectionWithDataset(user);
    List<Object> newDarCollection2 = createDarCollectionWithDataset(user);

    DataAccessRequest testDar1 = (DataAccessRequest) newDarCollection1.get(4);
    DarCollection testDarCollection1 = (DarCollection) newDarCollection1.get(6);
    DarCollection testDarCollection2 = (DarCollection) newDarCollection2.get(6);

    dataAccessRequestDAO.archiveByReferenceIds(List.of(testDar1.getReferenceId()));

    List<DarCollection> returnedCollections = darCollectionDAO.findDARCollectionsCreatedByUserId(
        user.getUserId());

    assertEquals(1, returnedCollections.size());
    assertFalse(returnedCollections.contains(testDarCollection1));
    assertTrue(returnedCollections.contains(testDarCollection2));
  }

  // findDARCollectionByReferenceId should exclude archived collections
  @Test
  public void testFindDARCollectionByReferenceIdArchived() {
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
  public void testFindDARCollectionByCollectionIdArchived() {
    User user = createUserWithInstitution();
    List<Object> newDarCollection = createDarCollectionWithDataset(user);

    Integer collectionId = (Integer) newDarCollection.get(3);
    DataAccessRequest testDar = (DataAccessRequest) newDarCollection.get(4);

    dataAccessRequestDAO.archiveByReferenceIds(List.of(testDar.getReferenceId()));

    DarCollection returnedCollection = darCollectionDAO.findDARCollectionByCollectionId(
        collectionId);
    assertNull(returnedCollection);
  }

}