package org.broadinstitute.consent.http.service;

import com.google.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import java.util.List;
import org.broadinstitute.consent.http.db.DarCollectionSummaryDAO;
import org.broadinstitute.consent.http.db.DatasetDAO;
import org.broadinstitute.consent.http.models.DarCollectionSummary;
import org.broadinstitute.consent.http.models.DarMetricsSummary;
import org.broadinstitute.consent.http.models.Dataset;
import org.jdbi.v3.core.Jdbi;

public class MetricsService {

  private final DatasetDAO dataSetDAO;
  private final DarCollectionSummaryDAO darCollectionSummaryDAO;

  @Inject
  public MetricsService(Jdbi jdbi) {
    this.dataSetDAO = jdbi.onDemand(DatasetDAO.class);
    this.darCollectionSummaryDAO = jdbi.onDemand(DarCollectionSummaryDAO.class);
  }

  public List<DarMetricsSummary> generateDarSummaries(Integer datasetId) {
    Dataset dataset = dataSetDAO.findDatasetById(datasetId);
    if (dataset == null) {
      throw new NotFoundException("Dataset with specified ID does not exist.");
    }
    List<DarCollectionSummary> collectionSummaries =
        darCollectionSummaryDAO.getDarCollectionSummariesByDatasetId(datasetId);
    return collectionSummaries.stream().map(DarMetricsSummary::new).toList();
  }
}
