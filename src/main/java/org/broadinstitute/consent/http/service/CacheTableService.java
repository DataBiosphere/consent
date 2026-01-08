package org.broadinstitute.consent.http.service;

import com.google.gson.JsonArray;
import jakarta.ws.rs.core.Response;
import org.broadinstitute.consent.http.db.CacheTableDAO;
import org.broadinstitute.consent.http.db.DacDAO;
import org.broadinstitute.consent.http.db.DatasetDAO;
import org.broadinstitute.consent.http.db.InstitutionDAO;
import org.broadinstitute.consent.http.db.StudyDAO;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.models.CacheDocument;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.DatasetProperty;
import org.broadinstitute.consent.http.models.Institution;
import org.broadinstitute.consent.http.models.Study;
import org.broadinstitute.consent.http.models.StudyProperty;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.elastic_search.DacTerm;
import org.broadinstitute.consent.http.models.elastic_search.DatasetTerm;
import org.broadinstitute.consent.http.models.elastic_search.InstitutionTerm;
import org.broadinstitute.consent.http.models.elastic_search.StudyTerm;
import org.broadinstitute.consent.http.models.elastic_search.UserTerm;
import org.broadinstitute.consent.http.models.ontology.DataUseSummary;
import org.broadinstitute.consent.http.service.dao.DatasetServiceDAO;
import org.broadinstitute.consent.http.util.ConsentLogger;
import org.broadinstitute.consent.http.util.DataLibraryCacheUtils;
import org.broadinstitute.consent.http.util.gson.GsonUtil;
import org.jdbi.v3.core.Jdbi;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class CacheTableService implements ConsentLogger {

  private final DacDAO dacDAO;
  private final UserDAO userDAO;
  private final OntologyService ontologyService;
  private final InstitutionDAO institutionDAO;
  private final DatasetDAO datasetDAO;
  private final DatasetServiceDAO datasetServiceDAO;
  private final StudyDAO studyDAO;
  private final CacheTableDAO cacheTableDAO;

  public CacheTableService(Jdbi jdbi, OntologyService ontologyService) {
    this.dacDAO = jdbi.onDemand(DacDAO.class);
    this.userDAO = jdbi.onDemand(UserDAO.class);
    this.ontologyService = ontologyService;
    this.institutionDAO = jdbi.onDemand(InstitutionDAO.class);
    this.datasetDAO = jdbi.onDemand(DatasetDAO.class);
    this.datasetServiceDAO = jdbi.onDemand(DatasetServiceDAO.class);
    this.studyDAO = jdbi.onDemand(StudyDAO.class);
    this.cacheTableDAO = jdbi.onDemand(CacheTableDAO.class);
  }

private DatasetTerm toDatasetTerm(Dataset dataset) {
    return DataLibraryCacheUtils.
}

  public Response indexDatasets(List<Integer> datasetIds){
    List<DatasetTerm> datasetTerms =
        datasetIds.stream().map(datasetDAO::findDatasetById).map(this::toDatasetTerm).toList();
    return indexDatasetTerms(datasetTerms);
  }

  public Response indexStudy(Integer studyId) {
    Study study = studyDAO.findStudyById(studyId);
    if (study != null && !study.getDatasetIds().isEmpty()) {
      return indexDatasets(study.getDatasetIds().stream().toList());
    }
    return Response.status(404).build();
  }

  protected void updateDatasetIndexDate(Integer datasetId, Integer userId, Instant indexDate) {
    // It is possible that a dataset has been deleted. If so, we don't want to try and update it.
    Dataset dataset = datasetDAO.findDatasetById(datasetId);
    if (dataset != null) {
      try {
        datasetServiceDAO.updateDatasetIndex(datasetId, userId, indexDate);
      } catch (SQLException e) {
        // We don't want to send these to Sentry, but we do want to log them for follow up off cycle
        logWarn("Error updating dataset indexed date for dataset id: %d ".formatted(datasetId), e);
      }
    }
  }

  private Response indexDatasetTerms(List<DatasetTerm> datasetTermList) {
    List<CacheDocument> writeQueue = new ArrayList<>();
    AtomicInteger docsWritten = new AtomicInteger();
    datasetTermList.forEach(
        dsTerm -> {
          writeQueue.add(
              new CacheDocument(
                  UUID.randomUUID().toString(), GsonUtil.getInstance().toJson(dsTerm)));

          if (writeQueue.size() / 250 == 1) {
            cacheTableDAO.insert(writeQueue);
            writeQueue.clear();
            docsWritten.addAndGet(250);
          }
        });
    cacheTableDAO.insert(writeQueue);
    docsWritten.addAndGet(writeQueue.size());
    return Response.status(200).entity(docsWritten.get()).build();
  }
}
