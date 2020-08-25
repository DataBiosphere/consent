package org.broadinstitute.consent.http.service;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.broadinstitute.consent.http.db.DataSetDAO;

import javax.inject.Inject;
import java.util.Date;
import org.broadinstitute.consent.http.models.DataSet;
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

  public DataSet findDatasetByName(String name) {
    return dataSetDAO.findDatasetByName(name);
  }

  public DataSetDTO findDatasetDTOById(Integer id) {
    DataSetDTO result = new DataSetDTO();
    Set<DataSetDTO> set = dataSetDAO.findDatasetDTOWithPropsByDatasetId(id);

    for (DataSetDTO ds : set) {
      result = ds;
    }
    return result;
  }

  public List<DataSetPropertyDTO> findInvalidProperties(List<DataSetPropertyDTO> properties) {
    List<Dictionary> dictionaries = dataSetDAO.getMappedFieldsOrderByReceiveOrder();
    List<String> keys = dictionaries.stream().map(d -> d.getKey()).collect(Collectors.toList());

    List<DataSetPropertyDTO> invalidProperties = properties.stream().filter(p -> !keys.contains(p.getPropertyName())).collect(Collectors.toList());
    return invalidProperties;
  }

  public List<DataSetProperty> processDatasetProperties(Integer datasetId, Date now,
      List<DataSetPropertyDTO> properties) {
    List<Dictionary> dictionaries = dataSetDAO.getMappedFieldsOrderByReceiveOrder();

    return properties.stream()
        .filter(p -> !p.getPropertyName().equals("Dataset Name"))
        .map(
            p -> {
              Dictionary dictionary = dictionaries.stream()
                  .filter(d -> d.getKey().equals(p.getPropertyName()))
                  .findAny().get();
              return new DataSetProperty(datasetId, dictionary.getKeyId(), p.getPropertyValue(),
                  now);
            }
        ).collect(Collectors.toList());
  }

}
