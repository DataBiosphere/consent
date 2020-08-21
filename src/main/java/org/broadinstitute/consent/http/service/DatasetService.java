package org.broadinstitute.consent.http.service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.broadinstitute.consent.http.db.DataSetDAO;

import javax.inject.Inject;
import java.util.Date;
import org.broadinstitute.consent.http.models.DataSetProperty;
import org.broadinstitute.consent.http.models.Dictionary;
import org.broadinstitute.consent.http.models.dto.DataSetDTO;
import org.broadinstitute.consent.http.models.dto.DataSetPropertyDTO;

public class DatasetService {

  private final DataSetDAO dataSetDAO;

  @Inject
  public DatasetService(DataSetDAO dataSetDAO) {
    this.dataSetDAO = dataSetDAO;
  }

  public DataSetDTO createDataset(DataSetDTO dataset, String name) {
    Date now = new Date();
    int lastAlias = dataSetDAO.findLastAlias();
    int alias = lastAlias + 1;

    Integer id = dataSetDAO
        .insertDataset(name, now, dataset.getObjectId(), dataset.getActive(), alias);

    List<DataSetProperty> propertyList = processDatasetProperties(id, now, dataset.getProperties());
    dataSetDAO.insertDataSetsProperties(propertyList);

    DataSetDTO result = new DataSetDTO();
    Set<DataSetDTO> set = dataSetDAO.findDatasetDTOWithPropsByDatasetId(id);

    for (DataSetDTO ds : set) {
      result = ds;
    }
    return result;
  }

  // return -1 if no ds found
  public Integer findDatasetByName(String name) {
    Integer result = dataSetDAO.getDataSetByName(name);
    if (result == null) {
      return -1;
    }
    return result;
  }

  // assumes that you will receive non-null values for all properties in receiveOrder
  public List<DataSetProperty> processDatasetProperties(Integer datasetId, Date now,
      List<DataSetPropertyDTO> properties) {
    List<Dictionary> dictionaries = dataSetDAO.getMappedFieldsOrderByReceiveOrder();
    // removes Dataset Name property, which is generated and doesn't need to be manually inserted
    dictionaries.remove(0);

    return properties.stream()
        .map(
            p -> {
              Dictionary dictionary = dictionaries.stream()
                  .filter(d -> d.getKey().equals(p.getPropertyName()))
                  .findAny()
                  .orElse(null);
              return new DataSetProperty(datasetId, dictionary.getKeyId(), p.getPropertyValue(),
                  now);
            }
        ).collect(Collectors.toList());
  }

}
