package org.broadinstitute.consent.http.service.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.db.DAOTestHelper;
import org.broadinstitute.consent.http.enumeration.ElectionStatus;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.enumeration.OrganizationType;
import org.broadinstitute.consent.http.enumeration.VoteType;
import org.broadinstitute.consent.http.models.DarCollection;
import org.broadinstitute.consent.http.models.DarDataset;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.models.DataUseBuilder;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.DatasetEntry;
import org.broadinstitute.consent.http.models.DatasetProperty;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.Vote;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DataAccessRequestServiceDAOTest extends DAOTestHelper {

  DataAccessRequestServiceDAO serviceDAO;

  @BeforeEach
  void initService() {
    serviceDAO = new DataAccessRequestServiceDAO(dataAccessRequestDAO, jdbi, darCollectionDAO);
  }

  @Test
  void testUpdateByReferenceId() throws Exception {

    Dataset datasetOne = createDataset();
    Dataset datasetTwo = createDataset();
    Dataset datasetThree = createDataset();
    User user = createUser();
    Calendar cal = Calendar.getInstance();
    cal.set(Calendar.YEAR, 2020);
    cal.set(Calendar.MONTH, Calendar.JANUARY);
    cal.set(Calendar.DAY_OF_MONTH, 1);
    Date old = cal.getTime();

    String referenceId = RandomStringUtils.randomAlphanumeric(10);
    DarDataset oldDarDataset = new DarDataset(referenceId, datasetOne.getDataSetId());
    DarDataset oldDarDatasetTwo = new DarDataset(referenceId, datasetTwo.getDataSetId());
    DarCollection collection = createDarCollection();
    Integer collectionId = collection.getDarCollectionId();
    dataAccessRequestDAO.insertDataAccessRequest(collectionId, referenceId, user.getUserId(), old,
        old, old, old, new DataAccessRequestData());
    dataAccessRequestDAO.insertAllDarDatasets(List.of(oldDarDataset, oldDarDatasetTwo));

    DataAccessRequest dar = new DataAccessRequest();
    dar.setReferenceId(referenceId);
    dar.setCollectionId(collectionId);
    DataAccessRequestData data = new DataAccessRequestData();
    data.setOtherText("This is a test value");
    List<Integer> newDatasetIds = List.of(datasetThree.getDataSetId());
    dar.setDatasetIds(newDatasetIds);
    dar.setData(data);

    initService();

    DataAccessRequest updatedDar = serviceDAO.updateByReferenceId(user, dar);

    Timestamp oldTimestamp = new Timestamp(old.getTime());
    assertFalse(oldTimestamp.equals(updatedDar.getSortDate()));
    assertFalse(oldTimestamp.equals(updatedDar.getUpdateDate()));
    assertEquals(newDatasetIds, updatedDar.getDatasetIds());
    DataAccessRequestData updatedData = updatedDar.getData();
    assertEquals(data.getOtherText(), updatedData.getOtherText());

    DarCollection targetCollection = darCollectionDAO.findDARCollectionByCollectionId(collectionId);
    assertEquals(user.getUserId(), targetCollection.getUpdateUserId());

    // collection should have the same update date as the updated dar
    assertEquals(dar.getUpdateDate(), collection.getUpdateDate());
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
