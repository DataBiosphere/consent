package org.broadinstitute.consent.http.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.broadinstitute.consent.http.db.DarCollectionDAO;
import org.broadinstitute.consent.http.db.DataAccessRequestDAO;
import org.broadinstitute.consent.http.db.DatasetDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.models.DarCollection;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.DatasetMetrics;
import org.broadinstitute.consent.http.models.Election;

public class MetricsService {

  private final DatasetDAO dataSetDAO;
  private final DataAccessRequestDAO darDAO;
  private final DarCollectionDAO darCollectionDAO;
  private final ElectionDAO electionDAO;

  @Inject
  public MetricsService(DatasetDAO dataSetDAO, DataAccessRequestDAO darDAO,
      DarCollectionDAO darCollectionDAO, ElectionDAO electionDAO) {
    this.dataSetDAO = dataSetDAO;
    this.darDAO = darDAO;
    this.darCollectionDAO = darCollectionDAO;
    this.electionDAO = electionDAO;
  }

  public static class DarMetricsSummary {

    final Timestamp updateDate;
    @JsonProperty
    final String projectTitle;
    @JsonProperty
    final String darCode;
    @JsonProperty
    final String nonTechRus;
    @JsonProperty
    final String referenceId;

    public DarMetricsSummary(DataAccessRequest dar, String darCode) {
      if (dar != null && dar.data != null) {
        this.updateDate = dar.getUpdateDate();
        this.projectTitle = dar.data.getProjectTitle();
        this.darCode = darCode;
        this.nonTechRus = dar.data.getNonTechRus();
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

    //get datasetDTO with properties and data use restrictions
    Dataset dataset = dataSetDAO.findDatasetById(datasetId);
    if (dataset == null) {
      throw new NotFoundException("Dataset with specified ID does not exist.");
    }

    //find dars with the given datasetId in their list of datasetIds, datasetId is a String so it can be converted to jsonb in query
    //convert all dars into smaller objects that only contain the information needed
    List<DataAccessRequest> dars = darDAO.findApprovedDARsByDatasetId(datasetId);
    List<Integer> darCollectionIds = dars.stream().map(DataAccessRequest::getCollectionId).toList();
    List<DarCollection> darCollections = darCollectionIds.isEmpty() ? List.of() :
        darCollectionDAO.findDARCollectionByCollectionIds(darCollectionIds);
    Map<Integer, DarCollection> collectionMap = darCollections.stream()
        .collect(Collectors.toMap(DarCollection::getDarCollectionId, Function.identity()));

    List<DarMetricsSummary> darInfo = dars.stream().map(dar -> {
      DarCollection collection = collectionMap.get(dar.getCollectionId());
      String darCode = Objects.nonNull(collection) ? collection.getDarCode() : null;
      return new DarMetricsSummary(dar, darCode);
    }).collect(Collectors.toList());

    //if there are associated dars, find associated access elections so we know how many and which dars are approved/denied
    List<String> referenceIds = dars.stream().map(dar -> (dar.referenceId))
        .collect(Collectors.toList());
    if (!referenceIds.isEmpty()) {
      List<Election> elections = electionDAO.findLastElectionsByReferenceIdsAndType(referenceIds,
          "DataAccess");
      metrics.setElections(elections);
    } else {
      metrics.setElections(Collections.emptyList());
    }
    metrics.setDataset(dataset);
    metrics.setDars(darInfo);
    return metrics;
  }

}
