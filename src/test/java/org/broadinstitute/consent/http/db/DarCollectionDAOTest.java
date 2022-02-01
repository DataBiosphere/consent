package org.broadinstitute.consent.http.db;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DarCollection;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.DataSet;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.Institution;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.Vote;
import org.junit.Test;
import org.postgresql.util.PSQLException;
import org.postgresql.util.PSQLState;

import static org.junit.Assert.*;

public class DarCollectionDAOTest extends DAOTestHelper  {

  @Test
  public void testFindAllDARCollections() {
    DarCollection collection = createDarCollection();
    List<DarCollection> allAfter = darCollectionDAO.findAllDARCollections();
    assertTrue(allAfter.contains(collection));
    assertEquals(1, allAfter.size());
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
  public void testFindDARCollectionByCollectionId() {
    DarCollection collection = createDarCollection();
    DarCollection returned = darCollectionDAO.findDARCollectionByCollectionId(collection.getDarCollectionId());
    assertNotNull(returned);
    assertEquals(collection.getDarCode(), returned.getDarCode());
    assertEquals(collection.getCreateUserId(), returned.getCreateUserId());
    List<Election> elections = collection.getDars().values().stream()
      .map(d -> d.getElections().values())
      .flatMap(Collection::stream)
      .collect(Collectors.toList());
    
      assertEquals(1, elections.size());

      Election election = elections.get(0);
      List<Vote> votes = election.getVotes().values().stream()
        .collect(Collectors.toList());

      Vote vote = votes.get(0);
      
      assertEquals(1, votes.size());
      assertEquals("Open", election.getStatus());
      assertEquals(election.getElectionId(), vote.getElectionId());
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
    DataSet dataset = createDataset();
    DarCollection c = createDarCollection();
    DataAccessRequest dar = new ArrayList<DataAccessRequest>(c.getDars().values()).get(0);
    if (Objects.isNull(dar)) {
      fail("DAR was not created in collection");
    }
    dar.getData().setDatasetIds(List.of(dataset.getDataSetId()));
    dataAccessRequestDAO.updateDataByReferenceIdVersion2(dar.getReferenceId(), dar.getUserId(), new Date(), new Date(), new Date(), dar.getData());
    Dac dac = createDac();
    Consent consent = createConsent(dac.getDacId());
    createAssociation(consent.getConsentId(), dataset.getDataSetId());

    List<Integer> collectionIds = darCollectionDAO.findDARCollectionIdsByDacIds(List.of(dac.getDacId()));
    assertFalse(collectionIds.isEmpty());
    assertEquals(c.getDarCollectionId(), collectionIds.get(0));
  }

  @Test
  public void testFindDARCollectionIdsByInstitutionId() {
    // Set up a DAR Collection with a DAR, User, and Institution
    DarCollection c = createDarCollection();
    DataAccessRequest dar = new ArrayList<DataAccessRequest>(c.getDars().values()).get(0);
    if (Objects.isNull(dar)) {
      fail("DAR was not created in collection");
    }
    User user = userDAO.findUserById(dar.getUserId());
    Institution institution = createInstitution();
    user.setInstitutionId(institution.getId());
    userDAO.updateUser(user.getDisplayName(), user.getDacUserId(), user.getAdditionalEmail(), user.getInstitutionId());

    List<Integer> collectionIds = darCollectionDAO.findDARCollectionIdsByInstitutionId(institution.getId());
    assertFalse(collectionIds.isEmpty());
    assertEquals(c.getDarCollectionId(), collectionIds.get(0));
  }

  @Test
  public void testFindDARCollectionByCollectionIdWithOrder_ASC() {
    DarCollection collectionOne = createDarCollection();
    DarCollection collectionTwo = createDarCollection();
    List<Integer> collectionIds = List.of(
      collectionOne.getDarCollectionId(),
      collectionTwo.getDarCollectionId()
    );
    List<DarCollection> returnedCollections = darCollectionDAO.findDARCollectionByCollectionIdsWithOrder(collectionIds, "dar_code", "ASC");
    for(int i = 0; i < returnedCollections.size() - 1; i++) {
      String firstCode = returnedCollections.get(i).getDarCode();
      String secondCode = returnedCollections.get(i+1).getDarCode();
      assertTrue(firstCode.compareTo(secondCode) < 0);
    }
  }

  @Test
  public void testFindDARCollectionByCollectionIdWithOrder_DESC() {
    DarCollection collectionOne = createDarCollection();
    DarCollection collectionTwo = createDarCollection();
    List<Integer> collectionIds = List.of(collectionOne.getDarCollectionId(), collectionTwo.getDarCollectionId());
    List<DarCollection> returnedCollections = darCollectionDAO.findDARCollectionByCollectionIdsWithOrder(collectionIds,
        "dar_code", "DESC");
    for (int i = 0; i < returnedCollections.size() - 1; i++) {
      String firstCode = returnedCollections.get(i).getDarCode();
      String secondCode = returnedCollections.get(i + 1).getDarCode();
      assertTrue(firstCode.compareTo(secondCode) > 0);
    }
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
    Integer userId = createUser().getDacUserId();
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
    darCollectionDAO.updateDarCollection(collection.getDarCollectionId(), user.getDacUserId(), date);
    DarCollection updated = darCollectionDAO.findDARCollectionByCollectionId(collection.getDarCollectionId());
    assertEquals(user.getDacUserId(), updated.getUpdateUserId());
    assertEquals(date, updated.getUpdateDate());
  }

  @Test
  public void testUpdateDarCollectionNegative() {
    Integer userId = createUser().getDacUserId();
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
  public void testFindAllDARCollectionsWithFilters_SortField() {
    DataAccessRequest dar1 = createDataAccessRequestV3(); // create first collection w DAR
    createDataAccessRequestWithUserIdV3(dar1.getUserId()); // create second collection w DAR

    List<DarCollection> collectionsResult = darCollectionDAO.findAllDARCollectionsWithFiltersByUser("", dar1.getUserId(), "dar_code", "ASC");

    assertEquals(2, collectionsResult.size());

    DataAccessRequest darOne = new ArrayList<DataAccessRequest>(collectionsResult.get(0).getDars().values()).get(0);
    DataAccessRequest darTwo = new ArrayList<DataAccessRequest>(collectionsResult.get(1).getDars().values()).get(0);
    int comparatorValue = darOne.getData().getDarCode().compareTo(darTwo.getData().getDarCode());
    assertTrue(comparatorValue < 0);
  }

  @Test
  public void testFindAllDARCollectionsWithFilters_ProjectTitleTerm() {

    DataAccessRequest dar = createDataAccessRequestV3();
    DataAccessRequestData data = dar.getData();
    String projectTitle = data.getProjectTitle();
    String testTerm = generateTestTerm(projectTitle);

    List<DarCollection> collections = darCollectionDAO.findAllDARCollectionsWithFiltersByUser(testTerm, dar.getUserId(), "dar_code", "ASC");
    assertEquals(1, collections.size());
    DarCollection targetCollection = collections.get(0);
    assertEquals(5, targetCollection.getDars().size());
    DataAccessRequest targetDar = new ArrayList<DataAccessRequest>(targetCollection.getDars().values()).get(0);
    assertEquals(targetDar.getData().getDarCode(), targetCollection.getDarCode());
  }

@Test
public void testFindAllDARCollectionsWithFilters_InstitutionTerm() {

    DataAccessRequest dar = createDataAccessRequestV3();
    User user = userDAO.findUserById(dar.getUserId());
    Institution institution = institutionDAO.findInstitutionById(user.getInstitutionId());
    String testTerm = generateTestTerm(institution.getName());

    List<DarCollection> collections = darCollectionDAO.findAllDARCollectionsWithFiltersByUser(testTerm, user.getDacUserId(), "dar_code", "ASC");
    assertEquals(1, collections.size());
    DarCollection targetCollection = collections.get(0);
    assertEquals(5, targetCollection.getDars().size());
    DataAccessRequest targetDar = new ArrayList<DataAccessRequest>(targetCollection.getDars().values()).get(0);
    assertEquals(targetCollection.getDarCode(), targetDar.getData().getDarCode());
  }

  @Test
  public void testFindAllDARCollectionsWithFilters_DatasetTerm() {

    DataAccessRequest dar = createDataAccessRequestV3();
    DataAccessRequestData data = dar.getData();
    String datasetLabel = data.getDatasets().get(0).getLabel();
    String testTerm = generateTestTerm(datasetLabel);

    List<DarCollection> collections = darCollectionDAO.findAllDARCollectionsWithFiltersByUser(testTerm, dar.getUserId(), "dar_code", "ASC");
    assertEquals(1, collections.size());
    DarCollection targetCollection = collections.get(0);
    assertEquals(5, targetCollection.getDars().size());
    DataAccessRequest targetDar = new ArrayList<DataAccessRequest>(targetCollection.getDars().values()).get(0);
    assertEquals(targetCollection.getDarCode(), targetDar.getData().getDarCode());
  }

  @Test
  public void testFindAllDARCollectionsWithFilters_DarCodeTerm() {

    DataAccessRequest dar = createDataAccessRequestV3();
    DataAccessRequestData data = dar.getData();
    String darCode = data.getDarCode();
    String testTerm = generateTestTerm(darCode);

    List<DarCollection> collections = darCollectionDAO.findAllDARCollectionsWithFiltersByUser( testTerm, dar.getUserId(), "dar_code", "ASC");
    assertEquals(1, collections.size());
    DarCollection targetCollection = collections.get(0);
    assertEquals(5, targetCollection.getDars().size());
    DataAccessRequest targetDar = new ArrayList<DataAccessRequest>(targetCollection.getDars().values()).get(0);
    assertEquals(targetCollection.getDarCode(), targetDar.getData().getDarCode());
  }

  @Test
  public void testFindAllDARCollectionsWithFilters_ResearcherTerm() {
    DataAccessRequest dar = createDataAccessRequestV3();
    User user = userDAO.findUserById(dar.getUserId());
    String researcherTerm = user.getDisplayName();
    String testTerm = generateTestTerm(researcherTerm);

    List<DarCollection> collections = darCollectionDAO.findAllDARCollectionsWithFiltersByUser(testTerm, user.getDacUserId(), "dar_code", "ASC");
    assertEquals(1, collections.size());
    DarCollection targetCollection = collections.get(0);
    assertEquals(5, targetCollection.getDars().size());
    DataAccessRequest targetDar = new ArrayList<DataAccessRequest>(targetCollection.getDars().values()).get(0);
    assertEquals(targetDar.getData().getDarCode(), targetCollection.getDarCode());
  }

  @Test
  public void testFindAllDarCollectionsWithFilters_sortDirectionTerm_DESC() {
    DataAccessRequest dar1 = createDataAccessRequestV3(); //create first collection w DAR
    createDataAccessRequestWithUserIdV3(dar1.getUserId()); // create second collection w DAR

    List<DarCollection> collections =  darCollectionDAO.findAllDARCollections();
    collections.sort((a,b) -> (a.getDarCode().compareToIgnoreCase(b.getDarCode())));
    Collections.reverse(collections);

    List<DarCollection> collectionsResult = darCollectionDAO.findAllDARCollectionsWithFiltersByUser("", dar1.getUserId(), "dar_code", "DESC");

    assertEquals(2, collectionsResult.size());
    assertEquals(collections.get(0).getDarCode(), collectionsResult.get(0).getDarCode());

    DataAccessRequest darResultOne = new ArrayList<DataAccessRequest>(collectionsResult.get(0).getDars().values()).get(0);
    DataAccessRequest darResultTwo = new ArrayList<DataAccessRequest>(collectionsResult.get(1).getDars().values()).get(0);
    assertEquals(collections.get(0).getDarCode(), darResultOne.getData().getDarCode());
    assertEquals(collections.get(1).getDarCode(), darResultTwo.getData().getDarCode());
  }

  @Test
  public void testFindAllDarCollectionsWithFilters_sortDirectionTerm_ASC() {
    DataAccessRequest dar1 = createDataAccessRequestV3(); // create first collection w DAR
    createDataAccessRequestWithUserIdV3(dar1.getUserId()); // create second collection w DAR

    List<DarCollection> collections = darCollectionDAO.findAllDARCollections();
    collections.sort((a, b) -> (a.getDarCode().compareToIgnoreCase(b.getDarCode())));

    List<DarCollection> collectionsResult = darCollectionDAO.findAllDARCollectionsWithFiltersByUser("", dar1.getUserId(), "dar_code", "ASC");

    assertEquals(2, collectionsResult.size());
    assertEquals(collectionsResult.get(0).getDarCode(), collections.get(0).getDarCode());

    DataAccessRequest darResultOne = new ArrayList<DataAccessRequest>(collectionsResult.get(0).getDars().values()).get(0);
    DataAccessRequest darResultTwo = new ArrayList<DataAccessRequest>(collectionsResult.get(1).getDars().values()).get(0);
    DataAccessRequest expectedDarOne = new ArrayList<DataAccessRequest>(collections.get(0).getDars().values()).get(0);
    DataAccessRequest expectedDarTwo = new ArrayList<DataAccessRequest>(collections.get(1).getDars().values()).get(0);
    assertEquals(expectedDarOne.getData().getDarCode(), darResultOne.getData().getDarCode());
    assertEquals(expectedDarTwo.getData().getDarCode(), darResultTwo.getData().getDarCode());
  }

  @Test
  public void testFindAllDARCollectionsCreatedByUserId(){
    DarCollection collection = createDarCollection();
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
  }

  @Test
  public void findDARCollectionIdsByDacAndDatasetIds() {
    //set up a dataset
    //set up a dar, set datasetIds = [datasetId]
    //create a consent and consent association for the dataset link
    Dac dac = createDac();
    User user = createUser();
    DataSet dataset = createDataset();
    DarCollection collection = createDarCollectionWithDatasetAndConsentAssociation(dac.getDacId(), user, dataset);
    DarCollection collection2 = createDarCollection();
    List<Integer> dacIds = Collections.singletonList(dac.getDacId());
    List<Integer> datasetIds = Collections.singletonList(dataset.getDataSetId());
    List<Integer> collectionIds = Collections.singletonList(collection.getDarCollectionId());
    List<Integer> fetchedCollectionIds = darCollectionDAO.findDARCollectionIdsByDacAndDatasetIds(dacIds, datasetIds);
    assertNotNull(fetchedCollectionIds);
    assertEquals(collectionIds.size(), fetchedCollectionIds.size());
    assertEquals(collection.getDarCollectionId(), fetchedCollectionIds.get(0));
    assertNotEquals(collection2.getDarCollectionId(), fetchedCollectionIds.get(0));
  }

  @Test
  public void testReturnUnfilteredCollectionCount() {
    createDarCollection();
    createDarCollection();
    int count = darCollectionDAO.returnUnfilteredCollectionCount();
    assertEquals(count, 2);
  }

  @Test
  public void testReturnUnfilteredCountForInstitution() {
    DarCollection collection = createDarCollection();
    User user = userDAO.findUserById(collection.getCreateUserId());
    createDarCollection();
    int count = darCollectionDAO.returnUnfilteredCountForInstitution(user.getInstitutionId());
    assertEquals(count, 1);
  }

  @Test
  public void testReturnUnfilteredResearcherCollectionCount() {
    DarCollection collection = createDarCollection();
    User user = userDAO.findUserById(collection.getCreateUserId());
    createDarCollection();
    int count = darCollectionDAO.returnUnfilteredResearcherCollectionCount(user.getDacUserId());
    assertEquals(count, 1);
  }

  @Test
  public void testGetFilteredListForSigningOfficial() {
    DarCollection collection = createDarCollection();
    User user = userDAO.findUserById((collection.getCreateUserId()));
    createDarCollection();
    createDarCollection();
    String filterTerm = collection.getDarCode();
    List<DarCollection> collections = darCollectionDAO.getFilteredListForSigningOfficial("darCode", "DESC", user.getInstitutionId(), filterTerm);
    assertEquals(collections.size(), 1);
  }

  private String generateTestTerm(String targetString) {
    return "(?=.*" + targetString.substring(0, 4) + ")";
  }
}
