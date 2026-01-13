package org.broadinstitute.consent.http.service;

import jakarta.ws.rs.core.Response;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.lang3.tuple.Pair;
import org.broadinstitute.consent.http.db.CacheTableDAO;
import org.broadinstitute.consent.http.db.DAOContainer;
import org.broadinstitute.consent.http.db.DacDAO;
import org.broadinstitute.consent.http.db.DatasetDAO;
import org.broadinstitute.consent.http.db.InstitutionDAO;
import org.broadinstitute.consent.http.db.StudyDAO;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.models.CacheDocument;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.Study;
import org.broadinstitute.consent.http.models.elastic_search.DatasetTerm;
import org.broadinstitute.consent.http.service.dao.DatasetServiceDAO;
import org.broadinstitute.consent.http.util.ConsentLogger;
import org.broadinstitute.consent.http.util.DataLibraryCacheUtils;
import org.broadinstitute.consent.http.util.gson.GsonUtil;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.result.ResultIterable;

public class CacheTableService implements ConsentLogger {
  private final Jdbi jdbi;
  private final DacDAO dacDAO;
  private final UserDAO userDAO;
  private final OntologyService ontologyService;
  private final InstitutionDAO institutionDAO;
  private final DatasetDAO datasetDAO;
  private final DatasetServiceDAO datasetServiceDAO;
  private final StudyDAO studyDAO;
  private final CacheTableDAO cacheTableDAO;
  private final DataLibraryCacheUtils dataLibraryCacheUtils;

  public CacheTableService(
      Jdbi jdbi,
      DAOContainer daoContainer,
      DatasetServiceDAO datasetServiceDAO,
      OntologyService ontologyService) {

    this.jdbi = jdbi;
    this.dacDAO = daoContainer.getDacDAO();
    this.userDAO = daoContainer.getUserDAO();
    this.ontologyService = ontologyService;
    this.institutionDAO = daoContainer.getInstitutionDAO();
    this.datasetDAO = daoContainer.getDatasetDAO();
    this.datasetServiceDAO = datasetServiceDAO;
    this.studyDAO = daoContainer.getStudyDAO();
    this.cacheTableDAO = daoContainer.getCacheTableDAO();
    this.dataLibraryCacheUtils = new DataLibraryCacheUtils();
  }

  private DatasetTerm toDatasetTerm(Dataset dataset) {
    return dataLibraryCacheUtils.toDatasetTerm(
        dataset, userDAO, dacDAO, institutionDAO, ontologyService);
  }

  public Response indexDatasets(List<Integer> datasetIds) {
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

          if (writeQueue.size() / 50 == 1) {
            cacheTableDAO.insert(writeQueue);
            writeQueue.clear();
            docsWritten.addAndGet(50);
          }
        });
    cacheTableDAO.insert(writeQueue);
    docsWritten.addAndGet(writeQueue.size());
    return Response.status(200).entity(docsWritten.get()).build();
  }

  public Pair<ResultIterable<CacheDocument>, Handle> searchDatasets(String query) {
    Handle handle = jdbi.open();
    CacheTableDAO dao = handle.attach(CacheTableDAO.class);
    ResultIterable<CacheDocument> iterable = dao.streamDocuments();
    return Pair.of(iterable, handle);
  }
}
