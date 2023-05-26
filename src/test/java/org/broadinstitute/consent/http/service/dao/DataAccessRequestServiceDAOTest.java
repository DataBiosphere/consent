package org.broadinstitute.consent.http.service.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import org.apache.commons.lang3.RandomStringUtils;
import org.broadinstitute.consent.http.db.DAOTestHelper;
import org.broadinstitute.consent.http.models.DarCollection;
import org.broadinstitute.consent.http.models.DarDataset;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.models.DataUseBuilder;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.DatasetProperty;
import org.broadinstitute.consent.http.models.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DataAccessRequestServiceDAOTest extends DAOTestHelper {

  public DataAccessRequestServiceDAO serviceDAO;

  @BeforeEach
  public void initService() {
    serviceDAO = new DataAccessRequestServiceDAO(dataAccessRequestDAO, jdbi, darCollectionDAO);
  }

  @Test
  public void testUpdateByReferenceId() throws Exception {

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
    Integer id = datasetDAO.insertDataset(name, now, user.getUserId(), objectId, false,
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

}
