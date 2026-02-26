package org.broadinstitute.consent.http.service;

import com.google.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import java.util.List;
import org.broadinstitute.consent.http.db.DataAccessRequestDAO;
import org.broadinstitute.consent.http.db.DatasetDAO;
import org.broadinstitute.consent.http.models.DarMetricsSummary;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.Dataset;

public class MetricsService {

  private final DatasetDAO dataSetDAO;
  private final DataAccessRequestDAO darDAO;

  @Inject
  public MetricsService(DatasetDAO dataSetDAO, DataAccessRequestDAO darDAO) {
    this.dataSetDAO = dataSetDAO;
    this.darDAO = darDAO;
  }

  public List<DarMetricsSummary> generateDarSummaries(Integer datasetId) {
    Dataset dataset = dataSetDAO.findDatasetById(datasetId);
    if (dataset == null) {
      throw new NotFoundException("Dataset with specified ID does not exist.");
    }
    List<DataAccessRequest> dars =
        darDAO.findSummaryMetricApprovedDARsByDatasetIdIncludesExpired(datasetId);
    return dars.stream().map(DarMetricsSummary::new).toList();
  }
}
