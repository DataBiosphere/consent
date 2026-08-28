package org.broadinstitute.consent.http.service;

import com.google.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import java.util.List;
import org.broadinstitute.consent.http.db.DataAccessRequestDAO;
import org.broadinstitute.consent.http.db.DatasetDAO;
import org.broadinstitute.consent.http.models.DarMetricsSummary;
import org.jdbi.v3.core.Jdbi;

public class MetricsService {

  private final DatasetDAO dataSetDAO;
  private final DataAccessRequestDAO darDAO;

  @Inject
  public MetricsService(Jdbi jdbi) {
    this.dataSetDAO = jdbi.onDemand(DatasetDAO.class);
    this.darDAO = jdbi.onDemand(DataAccessRequestDAO.class);
  }

  public List<DarMetricsSummary> generateDarSummaries(Integer datasetId) {
    // Only the dataset's existence matters here, so avoid assembling the full dataset.
    Integer existingDatasetId = dataSetDAO.findDatasetIdById(datasetId);
    if (existingDatasetId == null) {
      throw new NotFoundException("Dataset with specified ID does not exist.");
    }
    return darDAO.findSummaryMetricApprovedDARsByDatasetIdIncludesExpired(datasetId);
  }
}
