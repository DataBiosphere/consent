package org.broadinstitute.consent.http.db;

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
import org.junit.Test;
import org.postgresql.util.PSQLException;
import org.postgresql.util.PSQLState;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


public class DarCollectionDAOTest extends DAOTestHelper  {

  private void generateDatasetElectionForCollection(DarCollection collection) {
    DataAccessRequest dar = collection.getDars().values().stream().filter(d -> !d.getElections().isEmpty()).findFirst().orElse(null);
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
    userProperties.forEach(p -> assertEquals(collection.getCreateUserId(), p.getUserId()));
  }

  @Test
  public void testFindAllDarCollectionsMultipleUserProperties() {
    DarCollection collection = createDarCollectionMultipleUserProperties();
    List<DarCollection> allAfter = darCollectionDAO.findAllDARCollections();
    assertTrue(allAfter.contains(collection));
    assertEquals(1, allAfter.size());
    List<UserProperty> userProperties = allAfter.get(0).getCreateUser().getProperties();
    assertFalse(userProperties.isEmpty());
    userProperties.forEach(p -> assertEquals(collection.getCreateUserId(), p.getUserId()));
  }

  @Test
  public void testFindDARCollectionByReferenceId() {
    DataAccessRequest dar = createDataAccessRequestV3();
    DarCollection collection = darCollectionDAO.findDARCollectionByReferenceId(dar.getReferenceId());
    assertNotNull(collection);
    assertEquals(dar.getCollectionId(), collection.getDarCollectionId());
    List<String> ids = collection.getDars().values().stream().map(DataAccessRequest::getReferenceId).collect(Collectors.toList());
    assertTrue(ids.contains(dar.getReferenceId()));
  }

  @Test
  public void testFindDARCollectionByReferenceIdNegative() {
    //dar without a collection ID
    DataAccessRequest dar = createDraftDataAccessRequest();
    DarCollection collection = darCollectionDAO.findDARCollectionByReferenceId(dar.getReferenceId());
    assertNull(collection);
  }

  @Test
  public void testFindDARCollectionsByReferenceIds() {
    DataAccessRequest dar1 = createDataAccessRequestV3();
    DataAccessRequest dar2 = createDataAccessRequestV3();
    DataAccessRequest dar3 = createDataAccessRequestV3();
    List<DarCollection> darCollections = darCollectionDAO.findDARCollectionsByReferenceIds(List.of(dar1.getReferenceId(), dar2.getReferenceId()));
    assertNotNull(darCollections);
    assertEquals(2, darCollections.size());
    List<Integer> darCollectionIds = darCollections.stream().map(DarCollection::getDarCollectionId).collect(Collectors.toList());
    assertTrue(darCollectionIds.contains(dar1.collectionId));
    assertTrue(darCollectionIds.contains(dar2.collectionId));
    assertFalse(darCollectionIds.contains(dar3.collectionId));
  }

  @Test
  public void testFindDARCollectionByCollectionId() {
    DarCollection collection = createDarCollectionMultipleUserProperties();
    DarCollection returned = darCollectionDAO.findDARCollectionByCollectionId(collection.getDarCollectionId());
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
    userProperties.forEach(p -> assertEquals(collection.getCreateUserId(), p.getUserId()));

    assertNull(returned.getCreateUser().getLibraryCards());
  }

  @Test
  public void testFindDARCollectionByCollectionIdMultipleUserProperties() {
    DarCollection collection = createDarCollectionMultipleUserProperties();
    DarCollection returned = darCollectionDAO.findDARCollectionByCollectionId(collection.getDarCollectionId());
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
    Integer collectionId = darCollectionDAO.insertDarCollection(darCode, user.getUserId(), new Date());
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
    DarCollection returned = darCollectionDAO.findDARCollectionByCollectionId(RandomUtils.nextInt(1000, 2000));
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
    dataAccessRequestDAO.updateDataByReferenceId(dar.getReferenceId(), dar.getUserId(), new Date(), new Date(), new Date(), dar.getData());
    Dac dac = createDac();
    Consent consent = createConsent();
    datasetDAO.updateDatasetDacId(dataset.getDataSetId(), dac.getDacId());
    consentDAO.insertConsentAssociation(consent.getConsentId(), ASSOCIATION_TYPE_TEST, dataset.getDataSetId());

    List<Integer> collectionIds = darCollectionDAO.findDARCollectionIdsByDacIds(List.of(dac.getDacId()));
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

    List<Integer> collectionIds = darCollectionDAO.findDARCollectionIdsByInstitutionId(institution.getId());
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
      assertEquals(PSQLState.FOREIGN_KEY_VIOLATION.getState(), ((PSQLException)e.getCause()).getSQLState());
    }
    try {
      darCollectionDAO.insertDarCollection("darCode", userId, new Date());
      darCollectionDAO.insertDarCollection("darCode", userId, new Date());
    } catch (Exception e) {
      assertEquals(PSQLState.UNIQUE_VIOLATION.getState(), ((PSQLException)e.getCause()).getSQLState());
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
    DarCollection updated = darCollectionDAO.findDARCollectionByCollectionId(collection.getDarCollectionId());
    assertEquals(user.getUserId(), updated.getUpdateUserId());
    assertEquals(date, updated.getUpdateDate());
  }

  @Test
  public void testUpdateDarCollectionNegative() {
    Integer userId = createUser().getUserId();
    try {
      darCollectionDAO.updateDarCollection(0, userId, new Date());
    } catch (Exception e) {
      assertEquals(PSQLState.FOREIGN_KEY_VIOLATION.getState(), ((PSQLException)e.getCause()).getSQLState());
    }
    try {
      DarCollection collection = createDarCollection();
      darCollectionDAO.updateDarCollection(collection.getDarCollectionId(), 0, new Date());
    } catch (Exception e) {
      assertEquals(PSQLState.FOREIGN_KEY_VIOLATION.getState(), ((PSQLException)e.getCause()).getSQLState());
    }
  }

  @Test
  public void testDeleteByCollectionId() {
    DarCollection collection = createDarCollection();
    collection.getDars().keySet().forEach(k -> dataAccessRequestDAO.deleteDARDatasetRelationByReferenceId(k));
    dataAccessRequestDAO.deleteByCollectionId(collection.getDarCollectionId());
    darCollectionDAO.deleteByCollectionId(collection.getDarCollectionId());
    assertNull(darCollectionDAO.findDARCollectionByCollectionId(collection.getDarCollectionId()));
  }

  @Test
  public void testDeleteByCollectionIdNegative() {
    try{
      darCollectionDAO.deleteByCollectionId(RandomUtils.nextInt(100, 1000));
    } catch (Exception e) {
      assertEquals(PSQLState.UNIQUE_VIOLATION.getState(), ((PSQLException)e.getCause()).getSQLState());
    }
  }

  @Test
  public void testGetFilteredListForResearcher_NegativeDatasetElectionTest() {
    DarCollection collection = createDarCollectionMultipleUserProperties();
    generateDatasetElectionForCollection(collection);
    List<DarCollection> collections = darCollectionDAO.getFilteredListForResearcher("dar_code", "ASC", collection.getCreateUserId(), "");
    assertEquals(1, collections.size());
    DarCollection targetCollection = collections.get(0);
    List<Election> elections = getElectionsFromCollection(targetCollection);
    List<Election> datasetElections = getDatasetElectionsFromElection(elections);
    assertTrue(datasetElections.isEmpty());
  }

  @Test
  public void testGetFilteredListForResearcher_SortField() {
    DataAccessRequest dar1 = createDataAccessRequestV3(); // create first collection w DAR
    createDataAccessRequestWithUserIdV3(dar1.getUserId()); // create second collection w DAR

    List<DarCollection> collectionsResult = darCollectionDAO.getFilteredListForResearcher("dar_code", "ASC", dar1.getUserId(), "");
    assertEquals(2, collectionsResult.size());

    DarCollection collectionOne = collectionsResult.get(0);
    DarCollection collectionTwo = collectionsResult.get(1);
    int comparatorValue = collectionOne.getDarCode().compareTo(collectionTwo.getDarCode());
    assertTrue(comparatorValue < 0);
  }

  @Test
  public void testGetFilteredListForResearcher_ProjectTitleTerm() {
    User user = createUserWithInstitution();
    String darCode = "DAR-" + RandomUtils.nextInt(1, 999999999);
    Integer collectionId = darCollectionDAO.insertDarCollection(darCode, user.getUserId(), new Date());
    DataAccessRequest dar = createDataAccessRequest(user.getUserId(), collectionId, darCode);
    DataAccessRequestData data = dar.getData();
    String projectTitle = data.getProjectTitle();
    String testTerm = generateTestTerm(projectTitle);

    List<DarCollection> collections = darCollectionDAO.getFilteredListForResearcher("dar_code", "ASC", dar.getUserId(), testTerm);
    assertEquals(1, collections.size());
    DarCollection targetCollection = collections.get(0);
    assertEquals(1, targetCollection.getDars().size());
    assertEquals(darCode, targetCollection.getDarCode());
  }

@Test
public void testGetFilteredListForResearcher_InstitutionTerm() {
    User user = createUserWithInstitution();
    String darCode = "DAR-" + RandomUtils.nextInt(1, 999999999);
    Integer collectionId = darCollectionDAO.insertDarCollection(darCode, user.getUserId(), new Date());
    DataAccessRequest dar = createDataAccessRequest(user.getUserId(), collectionId, darCode);
    Institution institution = institutionDAO.findInstitutionById(user.getInstitutionId());
    String testTerm = generateTestTerm(institution.getName());

    List<DarCollection> collections = darCollectionDAO.getFilteredListForResearcher("dar_code", "ASC", dar.getUserId(), testTerm);
    assertEquals(1, collections.size());
    DarCollection targetCollection = collections.get(0);
    assertEquals(1, targetCollection.getDars().size());
    assertEquals(darCode, targetCollection.getDarCode());
  }

  @Test
  public void testGEtFilteredListForResearcher_DatasetTerm() {
    User user = createUserWithInstitution();
    String darCode = "DAR-" + RandomUtils.nextInt(1, 999999999);
    Integer collectionId = darCollectionDAO.insertDarCollection(darCode, user.getUserId(), new Date());
    DataAccessRequest dar = createDataAccessRequest(user.getUserId(), collectionId, darCode);
    DataAccessRequestData data = dar.getData();
    String datasetLabel = data.getDatasets().get(0).getLabel();
    String testTerm = generateTestTerm(datasetLabel);

    List<DarCollection> collections = darCollectionDAO.getFilteredListForResearcher("dar_code", "ASC", dar.getUserId(), testTerm);
    assertEquals(1, collections.size());
    DarCollection targetCollection = collections.get(0);
    assertEquals(1, targetCollection.getDars().size());
    assertEquals(darCode, targetCollection.getDarCode());
  }

  @Test
  public void testGetFilteredListForResearcher_DarCodeTerm() {
    User user = createUserWithInstitution();
    String darCode = "DAR-" + RandomUtils.nextInt(1, 999999999);
    Integer collectionId = darCollectionDAO.insertDarCollection(darCode, user.getUserId(), new Date());
    DataAccessRequest dar = createDataAccessRequest(user.getUserId(), collectionId, darCode);
    String testTerm = generateTestTerm(darCode);

    List<DarCollection> collections = darCollectionDAO.getFilteredListForResearcher("dar_code", "ASC", dar.getUserId(), testTerm);
    assertEquals(1, collections.size());
    DarCollection targetCollection = collections.get(0);
    assertEquals(1, targetCollection.getDars().size());
    assertEquals(darCode, targetCollection.getDarCode());
  }

  @Test
  public void testGetFilteredListForResearcher_ResearcherTerm() {
    User user = createUserWithInstitution();
    String darCode = "DAR-" + RandomUtils.nextInt(1, 999999999);
    Integer collectionId = darCollectionDAO.insertDarCollection(darCode, user.getUserId(), new Date());
    DataAccessRequest dar = createDataAccessRequest(user.getUserId(), collectionId, darCode);
    String researcherTerm = user.getDisplayName();
    String testTerm = generateTestTerm(researcherTerm);

    List<DarCollection> collections = darCollectionDAO.getFilteredListForResearcher("dar_code", "ASC", dar.getUserId(), testTerm);
    assertEquals(1, collections.size());
    DarCollection targetCollection = collections.get(0);
    assertEquals(1, targetCollection.getDars().size());
    assertEquals(darCode, targetCollection.getDarCode());
  }

  @Test
  public void testGetFilteredListForResearcher_sortDirectionTerm_DESC() {
    User user = createUserWithInstitution();
    String darCodeOne = "DAR-" + RandomUtils.nextInt(1, 999999999);
    String darCodeTwo = "DAR-" + RandomUtils.nextInt(1, 999999999);
    Integer collectionIdOne = darCollectionDAO.insertDarCollection(darCodeOne, user.getUserId(), new Date());
    Integer collectionIdTwo = darCollectionDAO.insertDarCollection(darCodeTwo, user.getUserId(), new Date());
    createDataAccessRequest(user.getUserId(), collectionIdOne, darCodeOne);
    createDataAccessRequest(user.getUserId(), collectionIdTwo, darCodeTwo);

    List<DarCollection> collections =  darCollectionDAO.findAllDARCollections();
    collections.sort((a,b) -> (a.getDarCode().compareToIgnoreCase(b.getDarCode())));
    Collections.reverse(collections);
    List<DarCollection> collectionsResult = darCollectionDAO.getFilteredListForResearcher("dar_code", "DESC", user.getUserId(), "");

    assertEquals(2, collectionsResult.size());
    assertEquals(collections.get(0).getDarCode(), collectionsResult.get(0).getDarCode());
    assertEquals(collections.get(1).getDarCode(), collectionsResult.get(1).getDarCode());
    assertEquals(1, collectionsResult.get(0).getDars().size());
    assertEquals(1, collectionsResult.get(1).getDars().size());
  }

  @Test
  public void testGetFilteredListForResearcher_sortDirectionTerm_ASC() {
    User user = createUserWithInstitution();
    String darCodeOne = "DAR-" + RandomUtils.nextInt(1, 999999999);
    String darCodeTwo = "DAR-" + RandomUtils.nextInt(1, 999999999);
    Integer collectionIdOne = darCollectionDAO.insertDarCollection(darCodeOne, user.getUserId(), new Date());
    Integer collectionIdTwo = darCollectionDAO.insertDarCollection(darCodeTwo, user.getUserId(), new Date());
    createDataAccessRequest(user.getUserId(), collectionIdOne, darCodeOne);
    createDataAccessRequest(user.getUserId(), collectionIdTwo, darCodeTwo);

    List<DarCollection> collections = darCollectionDAO.findAllDARCollections();
    collections.sort((a, b) -> (a.getDarCode().compareToIgnoreCase(b.getDarCode())));

    List<DarCollection> collectionsResult = darCollectionDAO.getFilteredListForResearcher("dar_code", "ASC", user.getUserId(), "");
    assertEquals(2, collectionsResult.size());
    assertEquals(collections.get(0).getDarCode(), collectionsResult.get(0).getDarCode());
    assertEquals(collections.get(1).getDarCode(), collectionsResult.get(1).getDarCode());
    assertEquals(1, collectionsResult.get(0).getDars().size());
    assertEquals(1, collectionsResult.get(1).getDars().size());
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
  public void testFindAllDARCollectionsCreatedByUserId(){
    DarCollection collection = createDarCollectionMultipleUserProperties();
    Map<String, DataAccessRequest> dars = collection.getDars();
    createDarCollection(); //create new collection associated with different user
    Integer userId = collection.getCreateUserId();
    List<DarCollection> collectionResult = darCollectionDAO.findDARCollectionsCreatedByUserId(userId);
    assertEquals(1, collectionResult.size());
    assertEquals(userId, collectionResult.get(0).getCreateUserId());

    DarCollection collectionRecord = collectionResult.get(0);
    List<Election> electionList = dars.values().stream()
      .map(d-> d.getElections().values())
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
  public void testFindAllDARCollectionsCreatedByUserIdWithMultipleProperties(){
    DarCollection collection = createDarCollectionMultipleUserProperties();
    createDarCollection(); //create new collection associated with different user
    Integer userId = collection.getCreateUserId();
    List<DarCollection> collectionResult = darCollectionDAO.findDARCollectionsCreatedByUserId(userId);
    assertEquals(1, collectionResult.size());

    DarCollection collectionRecord = collectionResult.get(0);
    List<UserProperty> userProperties = collectionRecord.getCreateUser().getProperties();
    assertFalse(userProperties.isEmpty());
    userProperties.forEach(p -> assertEquals(userId, p.getUserId()));
  }

  @Test
  public void testReturnUnfilteredCollectionCount() {
    createDarCollection();
    createDarCollection();
    int count = darCollectionDAO.returnUnfilteredCollectionCount();
    assertEquals(2, count);
  }

  @Test
  public void testReturnUnfilteredCountForInstitution() {
    DarCollection collection = createDarCollection();
    User user = userDAO.findUserById(collection.getCreateUserId());
    createDarCollection();
    int count = darCollectionDAO.returnUnfilteredCountForInstitution(user.getInstitutionId());
    assertEquals(1, count);
  }

  @Test
  public void testReturnUnfilteredResearcherCollectionCount() {
    DarCollection collection = createDarCollection();
    User user = userDAO.findUserById(collection.getCreateUserId());
    createDarCollection();
    int count = darCollectionDAO.returnUnfilteredResearcherCollectionCount(user.getUserId());
    assertEquals(1, count);
  }

  @Test
  public void testGetFilteredListForSigningOfficial_NegativeDatasetElection() {
    DarCollection collection = createDarCollection();
    generateDatasetElectionForCollection(collection);
    Integer institutionId = userDAO.findUserById(collection.getCreateUserId()).getInstitutionId();
    List<DarCollection> collections = darCollectionDAO.getFilteredCollectionsForSigningOfficial("dar_code", "DESC",
      institutionId, "");
    assertEquals(1, collections.size());
    DarCollection targetCollection = collections.get(0);
    List<Election> elections = getElectionsFromCollection(targetCollection);
    List<Election> datasetElections = getDatasetElectionsFromElection(elections);
    assertTrue(datasetElections.isEmpty());
  }

  @Test
  public void testGetFilteredListForSigningOfficialOnDarCode() {
    DarCollection collection = createDarCollection();
    User user = userDAO.findUserById((collection.getCreateUserId()));
    createDarCollection();
    createDarCollection();
    String filterTerm = collection.getDarCode();
    String testTerm = generateTestTerm(filterTerm);
    List<DarCollection> collections = darCollectionDAO.getFilteredCollectionsForSigningOfficial("dar_code", "DESC", user.getInstitutionId(), testTerm);
    assertEquals(1, collections.size());
    List<DarCollection> negativeTestCollection = darCollectionDAO.getFilteredCollectionsForSigningOfficial("dar_code", "DESC", user.getInstitutionId(), "negativeTerm");
    assertEquals(0, negativeTestCollection.size());
  }

  @Test
  public void testGetFilteredListForSigningOfficialOnResearcher() {
    DarCollection collection = createDarCollection();
    User user = userDAO.findUserById((collection.getCreateUserId()));
    createDarCollection();
    createDarCollection();
    String filterTerm = user.getDisplayName();
    String testTerm = generateTestTerm(filterTerm);
    List<DarCollection> collections = darCollectionDAO.getFilteredCollectionsForSigningOfficial("researcher", "DESC", user.getInstitutionId(), testTerm);
    assertEquals(1, collections.size());
    List<DarCollection> negativeTestCollection = darCollectionDAO.getFilteredCollectionsForSigningOfficial("researcher", "DESC", user.getInstitutionId(), "negativeTerm");
    assertEquals(0, negativeTestCollection.size());
  }

  @Test
  public void testGetFilteredListForSigningOfficialOnProjectTitle() {
    DarCollection collection = createDarCollection();
    User user = userDAO.findUserById((collection.getCreateUserId()));
    assertTrue(collection.getDars().values().stream().findAny().isPresent());
    DataAccessRequest targetDar = collection.getDars().values().stream().findAny().get();
    String filterTerm = targetDar.getData().getProjectTitle();
    String testTerm = generateTestTerm(filterTerm);
    createDarCollection();
    createDarCollection();
    List<DarCollection> collections = darCollectionDAO.getFilteredCollectionsForSigningOfficial("projectTitle", "DESC", user.getInstitutionId(), testTerm);
    assertEquals(1, collections.size());
    List<DarCollection> negativeTestCollection = darCollectionDAO.getFilteredCollectionsForSigningOfficial("projectTitle", "DESC", user.getInstitutionId(), "negativeTerm");
    assertEquals(0, negativeTestCollection.size());
  }

  @Test
  public void testGetFilteredListForSigningOfficialOnInstitution() {
    DarCollection collection = createDarCollection();
    User user = userDAO.findUserById((collection.getCreateUserId()));
    String institutionName = user.getInstitution().getName();
    String testTerm = generateTestTerm(institutionName);
    createDarCollection();
    createDarCollection();
    List<DarCollection> collections = darCollectionDAO.getFilteredCollectionsForSigningOfficial("institution_name", "DESC", user.getInstitutionId(), testTerm);
    assertEquals(1, collections.size());
    List<DarCollection> negativeTestCollection = darCollectionDAO.getFilteredCollectionsForSigningOfficial("institution_name", "DESC", user.getInstitutionId(), "negativeTerm");
    assertEquals(0, negativeTestCollection.size());
  }

  @Test
  public void testGetFilteredListForDac_NegativeDatasetElection() {
    DarCollection collection = createDarCollection();
    generateDatasetElectionForCollection(collection);
    List<DarCollection> collections = darCollectionDAO.getFilteredCollectionsForDACByCollectionIds("dar_code", "DESC",
        List.of(collection.getDarCollectionId()), "");
    assertEquals(1, collections.size());
    DarCollection targetCollection = collections.get(0);
    List<Election> elections = getElectionsFromCollection(targetCollection);
    List<Election> datasetElections = getDatasetElectionsFromElection(elections);
    assertTrue(datasetElections.isEmpty());
  }

   @Test
   public void testGetFilteredListForDacByCollectionIdsOnDarCode() {
     Dac dac = createDac();
     User user = createUser();
     Dataset dataset = createDatasetWithDac(dac.getDacId());
     DarCollection collection = createDarCollectionWithDatasetsAndConsentAssociation(user, Collections.singletonList(dataset));
     createDarCollection();
     String testTerm = generateTestTerm(collection.getDarCode());
     List<Integer> collectionIds = Collections.singletonList(collection.getDarCollectionId());
     List<DarCollection> collections = darCollectionDAO.getFilteredCollectionsForDACByCollectionIds("dar_code", "DESC", collectionIds, testTerm);
     assertEquals(1, collections.size());
     List<DarCollection> negativeTest = darCollectionDAO.getFilteredCollectionsForDACByCollectionIds("dar_code", "DESC", collectionIds, "negativetest");
     assertEquals(0, negativeTest.size());
   }

  @Test
  public void testGetFilteredListForDacByCollectionIdsOnProjectTitle() {
    Dac dac = createDac();
    User user = createUser();
    Dataset dataset = createDatasetWithDac(dac.getDacId());
    Dataset secondDataset = createDatasetWithDac(dac.getDacId());
    DarCollection collection = createDarCollectionWithDatasetsAndConsentAssociation(user, Collections.singletonList(dataset));
    DarCollection negativeCollection = createDarCollectionWithDatasetsAndConsentAssociation(user, Collections.singletonList(secondDataset));
    assertTrue(collection.getDars().values().stream().findAny().isPresent());
    DataAccessRequest targetDar = collection.getDars().values().stream().findAny().get();
    assertTrue(negativeCollection.getDars().values().stream().findAny().isPresent());
    DataAccessRequest negativeDar = negativeCollection.getDars().values().stream().findAny().get();
    String testTerm = generateTestTerm(targetDar.getData().getProjectTitle());
    String negativeTerm = generateTestTerm(negativeDar.getData().getProjectTitle());
    List<Integer> collectionIds = Collections.singletonList(collection.getDarCollectionId());
    List<DarCollection> collections = darCollectionDAO.getFilteredCollectionsForDACByCollectionIds("projectTitle", "DESC", collectionIds, testTerm);
    assertEquals(1, collections.size());
    List<DarCollection> negativeTest = darCollectionDAO.getFilteredCollectionsForDACByCollectionIds("projectTitle", "DESC", collectionIds, negativeTerm);
    assertEquals(0, negativeTest.size());
  }

  @Test
  public void testGetFilteredListForDacByCollectionIdsOnResearcher() {
    Dac dac = createDac();
    User user = createUser();
    Dataset dataset = createDatasetWithDac(dac.getDacId());
    DarCollection collection = createDarCollectionWithDatasetsAndConsentAssociation(user, Collections.singletonList(dataset));
    createDarCollection();
    String testTerm = generateTestTerm(user.getDisplayName());
    List<Integer> collectionIds = Collections.singletonList(collection.getDarCollectionId());
    List<DarCollection> collections = darCollectionDAO.getFilteredCollectionsForDACByCollectionIds("researcher", "DESC", collectionIds, testTerm);
    assertEquals(1, collections.size());
    List<DarCollection> negativeTest = darCollectionDAO.getFilteredCollectionsForDACByCollectionIds("researcher", "DESC", collectionIds, "negativetest");
    assertEquals(0, negativeTest.size());
  }

  @Test
  public void testGetFilteredListForDacByCollectionIdsOnInstitution() {
    DarCollection collection = createDarCollection();
    User user = userDAO.findUserById(collection.getCreateUserId());
    String testTerm = generateTestTerm(user.getInstitution().getName());
    List<Integer> collectionIds = Collections.singletonList(collection.getDarCollectionId());
    List<DarCollection> collections = darCollectionDAO.getFilteredCollectionsForDACByCollectionIds("institution_name", "DESC", collectionIds, testTerm);
    assertEquals(1, collections.size());
    List<DarCollection> negativeTest = darCollectionDAO.getFilteredCollectionsForDACByCollectionIds("institution_name", "DESC", collectionIds, "negativetest");
    assertEquals(0, negativeTest.size());
  }

  @Test
  public void testGetFilteredListForAdmin_NegativeDatasetElection() {
    DarCollection collection = createDarCollection();
    generateDatasetElectionForCollection(collection);
    List<DarCollection> collections = darCollectionDAO.getFilteredCollectionsForAdmin("dar_code", "DESC", "");
    assertEquals(1, collections.size());
    DarCollection targetCollection = collections.get(0);
    List<Election> elections = getElectionsFromCollection(targetCollection);
    List<Election> datasetElections = getDatasetElectionsFromElection(elections);
    assertTrue(datasetElections.isEmpty());
  }

  @Test
  public void testGetFilteredListForAdminOnDarCode() {
    DarCollection collection = createDarCollection();
    createDarCollection();
    String testTerm = "(?=.*" + collection.getDarCode() + ")";
    List<DarCollection> collections = darCollectionDAO.getFilteredCollectionsForAdmin("dar_code", "DESC", testTerm);
    List<DarCollection> negativeCollections = darCollectionDAO.getFilteredCollectionsForAdmin("dar_code", "DESC", "negativeTest");
    assertEquals(1, collections.size());
    assertEquals(0, negativeCollections.size());
    List<DarCollection> emptyTermCollections = darCollectionDAO.getFilteredCollectionsForAdmin("dar_code", "DESC", "");
    assertEquals(2, emptyTermCollections.size());
  }

  @Test
  public void testGetFilteredListForAdminOnProjectTitle() {
    DarCollection collection = createDarCollection();
    createDarCollection();
    assertTrue(collection.getDars().values().stream().findAny().isPresent());
    String projectTitle = collection.getDars().values().stream().findAny().get().getData().getProjectTitle();
    String testTerm = generateTestTerm(projectTitle);
    List<DarCollection> collections = darCollectionDAO.getFilteredCollectionsForAdmin("projectTitle", "DESC", testTerm);
    List<DarCollection> negativeCollections = darCollectionDAO.getFilteredCollectionsForAdmin("projectTitle", "DESC", "negativeTest");
    List<DarCollection> emptyTermCollections = darCollectionDAO.getFilteredCollectionsForAdmin("projectTitle", "DESC", "");
    assertEquals(2, collections.size());
    assertEquals(0, negativeCollections.size());
    assertEquals(2, emptyTermCollections.size());
  }

  @Test
  public void testGetFilteredListForAdminOnInstitution() {
    DarCollection collection = createDarCollection();
    createDarCollection();
    User user = userDAO.findUserById(collection.getCreateUserId());
    String testTerm = generateTestTerm(user.getInstitution().getName());
    List<DarCollection> collections = darCollectionDAO.getFilteredCollectionsForAdmin("institution_name", "DESC", testTerm);
    List<DarCollection> negativeCollections = darCollectionDAO.getFilteredCollectionsForAdmin("institution_name", "DESC", "negativeTest");
    List<DarCollection> emptyTermCollections = darCollectionDAO.getFilteredCollectionsForAdmin("institution_name", "DESC", "");
    assertEquals(1, collections.size());
    assertEquals(0, negativeCollections.size());
    assertEquals(2, emptyTermCollections.size());
  }

  @Test
  public void testGetFilteredListForAdminOnResearcher() {
    DarCollection collection = createDarCollection();
    createDarCollection();
    User user = userDAO.findUserById(collection.getCreateUserId());
    String testTerm = generateTestTerm(user.getDisplayName());
    List<DarCollection> collections = darCollectionDAO.getFilteredCollectionsForAdmin("researcher", "DESC", testTerm);
    List<DarCollection> negativeCollections = darCollectionDAO.getFilteredCollectionsForAdmin("researcher", "DESC", "negativeTest");
    List<DarCollection> emptyTermCollections = darCollectionDAO.getFilteredCollectionsForAdmin("researcher", "DESC", "");
    assertEquals(1, collections.size());
    assertEquals(0, negativeCollections.size());
    assertEquals(2, emptyTermCollections.size());
  }

   //NOTE: this is needed to format terms for queries
   // formatting should happen in the service class, but it needs to be reproduced here for DAO testing
  private String generateTestTerm(String targetString) {
    return "(?=.*" + targetString.substring(0, 4) + ")";
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
    consentDAO.insertConsentAssociation(consent.getConsentId(), ASSOCIATION_TYPE_TEST, dataset.getDataSetId());

    DarCollection testDarCollection = darCollectionDAO.findDARCollectionByCollectionId(collectionId);

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

    List<Integer> collectionIds = darCollectionDAO.findDARCollectionIdsByDacIds(List.of(dac.getDacId()));

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

    List<Integer> collectionIds = darCollectionDAO.findDARCollectionIdsByInstitutionId(user.getInstitutionId());

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

    List<DarCollection> returnedCollections = darCollectionDAO.findDARCollectionByCollectionIds(collectionIds);

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

    List<DarCollection> returnedCollections = darCollectionDAO.findDARCollectionsCreatedByUserId(user.getUserId());

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

    DarCollection archivedCollection = darCollectionDAO.findDARCollectionByReferenceId(testDar1.getReferenceId());
    DarCollection validCollection = darCollectionDAO.findDARCollectionByReferenceId(testDar2.getReferenceId());

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

    DarCollection returnedCollection = darCollectionDAO.findDARCollectionByCollectionId(collectionId);
    assertNull(returnedCollection);
  }

  // returnUnfilteredCollectionCount should exclude archived collections
  @Test
  public void testReturnUnfilteredCollectionCountArchived() {
    User user = createUserWithInstitution();
    List<Object> newDarCollection1 = createDarCollectionWithDataset(user);
    List<Object> newDarCollection2 = createDarCollectionWithDataset(user);

    DataAccessRequest testDar1 = (DataAccessRequest) newDarCollection1.get(4);

    dataAccessRequestDAO.archiveByReferenceIds(List.of(testDar1.getReferenceId()));

    int count = darCollectionDAO.returnUnfilteredCollectionCount();

    assertEquals(1, count);
  }

  // returnUnfilteredResearcherCollectionCount should exclude archived collections
  @Test
  public void testReturnUnfilteredResearcherCollectionCountArchived() {
    User user = createUserWithInstitution();

    List<Object> newDarCollection1 = createDarCollectionWithDataset(user);
    List<Object> newDarCollection2 = createDarCollectionWithDataset(user);
    DataAccessRequest testDar1 = (DataAccessRequest) newDarCollection1.get(4);

    dataAccessRequestDAO.archiveByReferenceIds(List.of(testDar1.getReferenceId()));

    int count = darCollectionDAO.returnUnfilteredResearcherCollectionCount(user.getUserId());

    assertEquals(1, count);
  }

  // returnUnfilteredCountForInstitution should exclude archived collections
  @Test
  public void testReturnUnfilteredCountForInstitutionArchived() {
    User user = createUserWithInstitution();

    List<Object> newDarCollection1 = createDarCollectionWithDataset(user);
    List<Object> newDarCollection2 = createDarCollectionWithDataset(user);

    DataAccessRequest testDar1 = (DataAccessRequest) newDarCollection1.get(4);

    dataAccessRequestDAO.archiveByReferenceIds(List.of(testDar1.getReferenceId()));
    int count = darCollectionDAO.returnUnfilteredCountForInstitution(user.getInstitutionId());
    assertEquals(1, count);
  }

  // getFilteredCollectionsForAdmin should exclude archived collections
  @Test
  public void testGetFilteredCollectionsForAdminArchived() {
    User user = createUserWithInstitution();
    List<Object> newDarCollection = createDarCollectionWithDataset(user);

    DataAccessRequest testDar = (DataAccessRequest) newDarCollection.get(4);
    DarCollection testDarCollection = (DarCollection) newDarCollection.get(6);

    dataAccessRequestDAO.archiveByReferenceIds(List.of(testDar.getReferenceId()));

    String filterTerm = testDarCollection.getDarCode();
    String testTerm = generateTestTerm(filterTerm);
    List<DarCollection> collections = darCollectionDAO.getFilteredCollectionsForAdmin("dar_code","ASC", testTerm);

    assertTrue(collections.isEmpty());
    assertFalse(collections.contains(testDarCollection));
  }

  // getFilteredCollectionsForSigningOfficial should exclude archived collections
  @Test
  public void testGetFilteredCollectionsForSigningOfficialArchived() {
    User user = createUserWithInstitution();

    List<Object> newDarCollection = createDarCollectionWithDataset(user);

    DataAccessRequest testDar = (DataAccessRequest) newDarCollection.get(4);
    DarCollection testDarCollection = (DarCollection) newDarCollection.get(6);

    dataAccessRequestDAO.archiveByReferenceIds(List.of(testDar.getReferenceId()));

    String filterTerm = testDarCollection.getDarCode();
    List<DarCollection> collections = darCollectionDAO.getFilteredCollectionsForSigningOfficial("dar_code","ASC", user.getInstitutionId(), filterTerm);

    assertTrue(collections.isEmpty());
    assertFalse(collections.contains(testDarCollection));
  }

  // getFilteredListForResearcher should exclude archived collections
  @Test
  public void testGetFilteredListForResearcherArchived() {
    User user = createUserWithInstitution();
    List<Object> newDarCollection = createDarCollectionWithDataset(user);

    DataAccessRequest testDar = (DataAccessRequest) newDarCollection.get(4);
    DarCollection testDarCollection = (DarCollection) newDarCollection.get(6);

    dataAccessRequestDAO.archiveByReferenceIds(List.of(testDar.getReferenceId()));

    String filterTerm = testDarCollection.getDarCode();
    List<DarCollection> collections = darCollectionDAO.getFilteredListForResearcher("dar_code","ASC", user.getUserId(), filterTerm);

    assertTrue(collections.isEmpty());
    assertFalse(collections.contains(testDarCollection));
  }

  // getFilteredCollectionsForDACByCollectionIds should exclude archived collections
  @Test
  public void testGetFilteredCollectionsForDACByCollectionIdsArchived() {
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

    String filterTerm = "";

    List<DarCollection> collections = darCollectionDAO.getFilteredCollectionsForDACByCollectionIds("dar_code","ASC", collectionIds, filterTerm);

    assertFalse(collections.contains(testDarCollection1));
    assertTrue(collections.contains(testDarCollection2));
  }
}