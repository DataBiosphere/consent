package org.broadinstitute.consent.http.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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
import org.junit.Test;
import org.postgresql.util.PSQLException;
import org.postgresql.util.PSQLState;

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
    List<String> ids = collection.getDars().stream().map(DataAccessRequest::getReferenceId).collect(Collectors.toList());
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
    DataAccessRequest dar = c.getDars().get(0);
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
    DataAccessRequest dar = c.getDars().get(0);
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

    DataAccessRequest darOne = collectionsResult.get(0).getDars().get(0);
    DataAccessRequest darTwo = collectionsResult.get(1).getDars().get(0);
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
    DataAccessRequest targetDar = targetCollection.getDars().get(0);
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
    DataAccessRequest targetDar = targetCollection.getDars().get(0);
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
    DataAccessRequest targetDar = targetCollection.getDars().get(0);
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
    DataAccessRequest targetDar = targetCollection.getDars().get(0);
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
    DataAccessRequest targetDar = targetCollection.getDars().get(0);
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

    DataAccessRequest darResultOne = collectionsResult.get(0).getDars().get(0);
    DataAccessRequest darResultTwo = collectionsResult.get(1).getDars().get(0);
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

    DataAccessRequest darResultOne = collectionsResult.get(0).getDars().get(0);
    DataAccessRequest darResultTwo = collectionsResult.get(1).getDars().get(0);
    DataAccessRequest expectedDarOne = collections.get(0).getDars().get(0);
    DataAccessRequest expectedDarTwo = collections.get(1).getDars().get(0);
    assertEquals(expectedDarOne.getData().getDarCode(), darResultOne.getData().getDarCode());
    assertEquals(expectedDarTwo.getData().getDarCode(), darResultTwo.getData().getDarCode());
  }

  @Test
  public void testFindAllDARCollectionsCreatedByUserId(){
    DarCollection collection = createDarCollection();
    List<DataAccessRequest> dars = collection.getDars();
    createDarCollection(); //create new collection associated with different user
    Integer userId = collection.getCreateUserId();
    List<DarCollection> collectionResult = darCollectionDAO.findDARCollectionsCreatedByUserId(userId);
    assertEquals(1, collectionResult.size());
    assertEquals(userId, collectionResult.get(0).getCreateUserId());
    Map<String, List<Election>> electionMap = collectionResult.get(0).getElectionMap();
    assertEquals(1, electionMap.size());
    String referenceId = collectionResult.get(0).getDars().get(0).getReferenceId();
    assertEquals(1, electionMap.get(referenceId).size());

    List<DataAccessRequest> darsResult = collectionResult.get(0).getDars();
    assertEquals(dars.size(), darsResult.size());

    for (int i = 0; i < dars.size(); i++) {
      DataAccessRequest darOriginal = dars.get(i);
      DataAccessRequest darResults = darsResult.get(i);
      assertEquals(darOriginal.getId(), darResults.getId());
      assertEquals(collection.getDarCode(), darResults.getData().getDarCode());
    }
  }

  private String generateTestTerm(String targetString) {
    return "(?=.*" + targetString.substring(0, 4) + ")";
  }
}
