package org.broadinstitute.consent.http.db;


import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.models.DarCollection;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.Institution;
import org.broadinstitute.consent.http.models.User;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.postgresql.util.PSQLException;
import org.postgresql.util.PSQLState;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;


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
    List<String> ids = collection.getDars().stream().map(d -> d.getReferenceId()).collect(Collectors.toList());
    assertTrue(ids.contains(dar.getReferenceId()));
  }

  @Test
  public void testFindDARCollectionByReferenceIdNegative() {
    //dar without a collection ID
    DataAccessRequest dar = createDataAccessRequestV2();
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
    createDataAccessRequestV3(); // create first collection w DAR
    createDataAccessRequestV3(); // create second collection w DAR

    List<DarCollection> collectionsResult = darCollectionDAO.findAllDARCollectionsWithFilters("", "dar_code", "ASC");

    assertEquals(2, collectionsResult.size());

    DataAccessRequest darOne = collectionsResult.get(0).getDars().get(0);
    DataAccessRequest darTwo = collectionsResult.get(1).getDars().get(0);
    Integer comparatorValue = darOne.getData().getDarCode().compareTo(darTwo.getData().getDarCode());
    assertTrue(comparatorValue < 0);
  }

  @Test
  public void testFindAllDARCollectionsWithFilters_ProjectTitleTerm() {

    DataAccessRequest dar = createDataAccessRequestV3();
    DataAccessRequestData data = dar.getData();
    String projectTitle = data.getProjectTitle();
    String testTerm = generateTestTerm(projectTitle);

    List<DarCollection> collections = darCollectionDAO.findAllDARCollectionsWithFilters(testTerm, "dar_code", "ASC");
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

    List<DarCollection> collections = darCollectionDAO.findAllDARCollectionsWithFilters(testTerm, "dar_code", "ASC");
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

    List<DarCollection> collections = darCollectionDAO.findAllDARCollectionsWithFilters(testTerm, "dar_code", "ASC");
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

    List<DarCollection> collections = darCollectionDAO.findAllDARCollectionsWithFilters( testTerm, "dar_code", "ASC");
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

    List<DarCollection> collections = darCollectionDAO.findAllDARCollectionsWithFilters(testTerm, "dar_code", "ASC");
    assertEquals(1, collections.size());
    DarCollection targetCollection = collections.get(0);
    assertEquals(5, targetCollection.getDars().size());
    DataAccessRequest targetDar = targetCollection.getDars().get(0);
    assertEquals(targetDar.getData().getDarCode(), targetCollection.getDarCode());
  }

  @Test
  public void testFindAllDarCollectionsWithFilters_sortDirectionTerm_DESC() {
    createDataAccessRequestV3(); //create first collection w DAR
    createDataAccessRequestV3(); //create second collection w DAR

    List<DarCollection> collections =  darCollectionDAO.findAllDARCollections();
    collections.sort((a,b) -> (a.getDarCode().compareToIgnoreCase(b.getDarCode())));
    Collections.reverse(collections);

    List<DarCollection> collectionsResult = darCollectionDAO.findAllDARCollectionsWithFilters("", "dar_code", "DESC");

    assertEquals(2, collectionsResult.size());
    assertEquals(collections.get(0).getDarCode(), collectionsResult.get(0).getDarCode());

    DataAccessRequest darResultOne = collectionsResult.get(0).getDars().get(0);
    DataAccessRequest darResultTwo = collectionsResult.get(1).getDars().get(0);
    assertEquals(collections.get(0).getDarCode(), darResultOne.getData().getDarCode());
    assertEquals(collections.get(1).getDarCode(), darResultTwo.getData().getDarCode());
  }

  @Test
  public void testFindAllDarCollectionsWithFilters_sortDirectionTerm_ASC() {
    createDataAccessRequestV3(); // create first collection w DAR
    createDataAccessRequestV3(); // create second collection w DAR

    List<DarCollection> collections = darCollectionDAO.findAllDARCollections();
    collections.sort((a, b) -> (a.getDarCode().compareToIgnoreCase(b.getDarCode())));

    List<DarCollection> collectionsResult = darCollectionDAO.findAllDARCollectionsWithFilters("", "dar_code", "ASC");

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

    List<DataAccessRequest> darsResult = collectionResult.get(0).getDars();
    assertEquals(dars.size(), darsResult.size());

    for (int i = 0; i < dars.size(); i++) {
      DataAccessRequest darOriginal = dars.get(i);
      DataAccessRequest darResults = darsResult.get(i);
      assertEquals(darOriginal.getId(), darResults.getId());
      assertEquals(collection.getDarCode(), darResults.getData().getDarCode());
    }
  }

  private final String generateTestTerm(String targetString) {
    return "(?=.*" + targetString.substring(0, 4) + ")";
  }
};