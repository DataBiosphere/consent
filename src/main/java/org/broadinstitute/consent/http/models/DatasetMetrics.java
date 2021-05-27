package org.broadinstitute.consent.http.models;


import java.util.List;

import org.broadinstitute.consent.http.models.dto.DatasetDTO;

public class DatasetMetrics {

  private DatasetDTO dataset;
  private List<Object> dars;
  private List<Election> elections;

  public DatasetDTO getDataset() {
    return dataset;
  }

  public void setDataset(DatasetDTO dataset) {
    this.dataset = dataset;
  }

  public List<Object> getDars() {
    return dars;
  }

  public void setDars(List<Object> dars) {
    this.dars = dars;
  }

  public List<Election> getElections() {
    return elections;
  }

  public void setElections(List<Election> elections) {
    this.elections = elections;
  }
}
