package org.broadinstitute.consent.http.service.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.broadinstitute.consent.http.db.DAOTestHelper;
import org.broadinstitute.consent.http.enumeration.ElectionStatus;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.enumeration.VoteType;
import org.broadinstitute.consent.http.models.DarCollection;
import org.broadinstitute.consent.http.models.DarDataset;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.models.DataUseBuilder;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.DatasetProperty;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DataAccessRequestServiceDAOTest extends DAOTestHelper {

  DataAccessRequestServiceDAO serviceDAO;

  @BeforeEach
  void initService() {
    serviceDAO = new DataAccessRequestServiceDAO(jdbi, dataAccessRequestDAO, darCollectionDAO);
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

    String referenceId = randomAlphanumeric(10);
    DarDataset oldDarDataset = new DarDataset(referenceId, datasetOne.getDatasetId());
    DarDataset oldDarDatasetTwo = new DarDataset(referenceId, datasetTwo.getDatasetId());
    DarCollection collection = createDarCollection();
    Integer collectionId = collection.getDarCollectionId();
    dataAccessRequestDAO.insertDataAccessRequest(
        collectionId,
        referenceId,
        user.getUserId(),
        old,
        old,
        old,
        new DataAccessRequestData(),
        user.getEraCommonsId());
    dataAccessRequestDAO.insertAllDarDatasets(List.of(oldDarDataset, oldDarDatasetTwo));

    DataAccessRequest dar = new DataAccessRequest();
    dar.setReferenceId(referenceId);
    dar.setCollectionId(collectionId);
    DataAccessRequestData data = new DataAccessRequestData();
    data.setOtherText("This is a test value");
    List<Integer> newDatasetIds = List.of(datasetThree.getDatasetId());
    dar.setDatasetIds(newDatasetIds);
    dar.setData(data);

    initService();

    DataAccessRequest updatedDar = serviceDAO.updateByReferenceId(user, dar);

    Timestamp oldTimestamp = new Timestamp(old.getTime());
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
    String name = "Name_" + randomAlphanumeric(20);
    Timestamp now = new Timestamp(new Date().getTime());
    String objectId = "Object ID_" + randomAlphanumeric(20);
    DataUse dataUse = new DataUseBuilder().setGeneralUse(true).build();
    Integer id =
        datasetDAO.insertDataset(name, now, user.getUserId(), objectId, dataUse.toString(), null);
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

  private DarCollection createDarCollection() {
    User user = createUserWithInstitution();
    String darCode = "DAR-" + randomInt(1, 10000);
    Integer collectionId =
        darCollectionDAO.insertDarCollection(darCode, user.getUserId(), new Date());
    Dataset dataset = createDataset();
    DataAccessRequest dar = createDataAccessRequest(user.getUserId(), collectionId);
    dataAccessRequestDAO.insertDARDatasetRelation(dar.getReferenceId(), dataset.getDatasetId());
    Election cancelled =
        createCancelledAccessElection(dar.getReferenceId(), dataset.getDatasetId());
    Election access = createDataAccessElection(dar.getReferenceId(), dataset.getDatasetId());
    createFinalVote(user.getUserId(), cancelled.getElectionId());
    createFinalVote(user.getUserId(), access.getElectionId());
    createDataAccessRequest(user.getUserId(), collectionId);
    createDataAccessRequest(user.getUserId(), collectionId);
    return darCollectionDAO.findDARCollectionByCollectionId(collectionId);
  }

  private Election createCancelledAccessElection(String referenceId, Integer datasetId) {
    Integer electionId =
        electionDAO.insertElection(
            ElectionType.DATA_ACCESS.getValue(),
            ElectionStatus.CANCELED.getValue(),
            new Date(),
            referenceId,
            datasetId);
    return electionDAO.findElectionById(electionId);
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
        collectionId, referenceId, userId, now, now, now, data, randomAlphabetic(10));
    return dataAccessRequestDAO.findByReferenceId(referenceId);
  }

  private void createFinalVote(Integer userId, Integer electionId) {
    voteDAO.insertVote(userId, electionId, VoteType.FINAL.getValue());
  }

  private Election createDataAccessElection(String referenceId, Integer datasetId) {
    Integer electionId =
        electionDAO.insertElection(
            ElectionType.DATA_ACCESS.getValue(),
            ElectionStatus.OPEN.getValue(),
            new Date(),
            referenceId,
            datasetId);
    return electionDAO.findElectionById(electionId);
  }
}
