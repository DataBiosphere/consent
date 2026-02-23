package org.broadinstitute.consent.http.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import java.sql.Timestamp;
import java.util.List;
import org.broadinstitute.consent.http.db.DataAccessRequestDAO;
import org.broadinstitute.consent.http.db.DatasetDAO;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.DatasetMetrics;

public class MetricsService {

  private final DatasetDAO dataSetDAO;
  private final DataAccessRequestDAO darDAO;

  @Inject
  public MetricsService(DatasetDAO dataSetDAO, DataAccessRequestDAO darDAO) {
    this.dataSetDAO = dataSetDAO;
    this.darDAO = darDAO;
  }

  public static class DarMetricsSummary {

    final Timestamp updateDate;
    @JsonProperty final String projectTitle;
    @JsonProperty final String darCode;
    @JsonProperty final String nonTechRus;
    @JsonProperty final String referenceId;

    public DarMetricsSummary(DataAccessRequest dar) {
      if (dar != null && dar.getData() != null) {
        this.updateDate = dar.getUpdateDate();
        this.projectTitle = dar.getData().getProjectTitle();
        this.darCode = dar.getDarCode();
        this.nonTechRus = dar.getData().getNonTechRus();
        this.referenceId = dar.getReferenceId();
      } else {
        this.updateDate = null;
        this.projectTitle = null;
        this.darCode = null;
        this.nonTechRus = null;
        this.referenceId = null;
      }
    }
  }

  public DatasetMetrics generateDatasetMetrics(Integer datasetId) {

    DatasetMetrics metrics = new DatasetMetrics();

    // get datasetDTO with properties and data use restrictions
    Dataset dataset = dataSetDAO.findDatasetById(datasetId);
    if (dataset == null) {
      throw new NotFoundException("Dataset with specified ID does not exist.");
    }

    // find dars with the given datasetId in their list of datasetIds, datasetId is a String so it
    // can be converted to jsonb in query
    // convert all dars into smaller objects that only contain the information needed
    List<DataAccessRequest> dars = darDAO.findApprovedDARsByDatasetId(datasetId);
    List<DarMetricsSummary> darMetricsSummaries =
        dars.stream().map(DarMetricsSummary::new).toList();
    metrics.setDars(darMetricsSummaries);
    return metrics;
  }
}
