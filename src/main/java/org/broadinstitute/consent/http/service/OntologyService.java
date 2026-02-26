package org.broadinstitute.consent.http.service;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.inject.Inject;
import jakarta.ws.rs.core.StreamingOutput;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import org.broadinstitute.consent.http.enumeration.DataUseTranslationType;
import org.broadinstitute.consent.http.enumeration.OntologyType;
import org.broadinstitute.consent.http.matching.TranslationUtil;
import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.ontology.DataUseSummary;
import org.broadinstitute.consent.http.service.ontology.OntologyDAO;
import org.broadinstitute.consent.http.service.ontology.OntologyIndexService;
import org.broadinstitute.consent.http.service.ontology.OntologyTerm;
import org.broadinstitute.consent.http.util.ConsentLogger;
import org.broadinstitute.consent.http.util.ThreadUtils;
import org.jspecify.annotations.NonNull;

public class OntologyService implements ConsentLogger {

  private final ExecutorService executorService =
      new ThreadUtils().getExecutorService(OntologyService.class);
  private final OntologyDAO ontologyDAO;
  private final OntologyIndexService indexService;
  private final TranslationUtil translationUtil;

  @Inject
  public OntologyService(OntologyDAO ontologyDAO, OntologyIndexService indexService) {
    this.ontologyDAO = ontologyDAO;
    this.indexService = indexService;
    this.translationUtil = new TranslationUtil(ontologyDAO);
  }

  public DataUseSummary translateDataUseSummary(DataUse dataUse) {
    return translationUtil.translateSummary(dataUse);
  }

  public String translateDataUse(DataUse dataUse, DataUseTranslationType type) {
    return translationUtil.translate(dataUse, type);
  }

  public void deleteOntologyTerms(OntologyType ontologyType) {
    ontologyDAO.deleteByOntology(ontologyType.name());
    logInfo("Deleted ontology terms for ontology: %s".formatted(ontologyType.name()));
  }

  public void indexOntology(User user, OntologyType ontologyType) {
    ListeningExecutorService listeningExecutorService =
        MoreExecutors.listeningDecorator(executorService);
    ListenableFuture<Collection<OntologyTerm>> syncFuture =
        listeningExecutorService.submit(
            () -> {
              Collection<OntologyTerm> terms = indexService.generateTerms(ontologyType);
              ontologyDAO.batchInsertTerms(terms, user.getUserId());
              return terms;
            });
    Futures.addCallback(
        syncFuture,
        new FutureCallback<>() {
          @Override
          public void onSuccess(Collection<OntologyTerm> terms) {
            logInfo("Successfully indexed %s ontology terms".formatted(terms.size()));
          }

          @Override
          public void onFailure(@NonNull Throwable t) {
            logThrowable(t);
          }
        },
        listeningExecutorService);
  }

  public StreamingOutput findByTermIds(String[] termIds) {
    return ontologyDAO.findByTermIds(termIds);
  }

  /**
   * Find ontology terms matching the provided query string and optional filters for ontology type
   * and result count. If no ontology type is provided, results will be returned for all ontology
   * types. If no count is provided, a default value of 20 will be used.
   *
   * @param q Required query string to search for in the ontology index.
   * @param ontologyType Optional ontology type to filter results by. If null, results will be
   *     returned for all ontology types.
   * @param count Optional maximum number of results to return. If null, a default value of 20 will
   *     be used.
   * @return A StreamingOutput containing the search results matching the query and filters
   *     provided.
   */
  public StreamingOutput findByQuery(String q, OntologyType ontologyType, Integer count) {
    return ontologyDAO.findByQuery(q, ontologyType, count);
  }
}
