package org.broadinstitute.consent.http.service;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Date;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.broadinstitute.consent.http.db.DataSetDAO;
import org.broadinstitute.consent.http.models.DataSet;
import org.broadinstitute.consent.http.models.DataSetProperty;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class DatasetServiceTest {

  private DatasetService datasetService;

  @Mock
  DataSetDAO datasetDAO;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  private void initService() {
    datasetService = new DatasetService(datasetDAO);
  }

  @Test
  public void testGetDatasetProperties() {
    when(datasetDAO.findDatasetPropertiesByDatasetId(anyInt())).thenReturn(Collections.emptySet());
    initService();

    Assert.assertTrue(datasetService.getDatasetProperties(1).isEmpty());
  }

  @Test
  public void testGetDatasetWithPropertiesById() {
    when(datasetDAO.findDatasetPropertiesByDatasetId(anyInt())).thenReturn(getDatasetProperties());
    when(datasetDAO.findDataSetById(anyInt())).thenReturn(getDataset());
    initService();

    Assert.assertTrue(datasetService.getDatasetProperties(1).equals(datasetDAO.findDatasetPropertiesByDatasetId(1)));
  }

  private DataSet getDataset() {
    DataSet dataset = new DataSet();
    dataset.setName("Test Dataset");
    dataset.setActive(true);
    dataset.setNeedsApproval(false);
    dataset.setProperties(Collections.emptySet());
    return dataset;
  }

  private Set<DataSetProperty> getDatasetProperties() {
    return IntStream.range(1, 11)
        .mapToObj(i ->
            new DataSetProperty(1, i, "Test Value", new Date())
        ).collect(Collectors.toSet());
  }

}
