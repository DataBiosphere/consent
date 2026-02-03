package org.broadinstitute.consent.http.service;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.inject.Inject;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import org.broadinstitute.consent.http.configurations.ServicesConfiguration;
import org.broadinstitute.consent.http.enumeration.DataUseTranslationType;
import org.broadinstitute.consent.http.enumeration.OntologyType;
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
  private final ServicesConfiguration servicesConfiguration;
  private final Client client;
  private final OntologyDAO ontologyDAO;
  private final OntologyIndexService indexService;

  @Inject
  public OntologyService(
      Client client,
      ServicesConfiguration config,
      OntologyDAO ontologyDAO,
      OntologyIndexService indexService) {
    this.client = client;
    this.servicesConfiguration = config;
    this.ontologyDAO = ontologyDAO;
    this.indexService = indexService;
  }

  public DataUseSummary translateDataUseSummary(DataUse dataUse) {
    WebTarget target = client.target(servicesConfiguration.getOntologyURL() + "translate/summary");
    try (Response response =
        target.request(MediaType.APPLICATION_JSON).post(Entity.json(dataUse.toString()))) {
      if (response.getStatus() >= 200 || response.getStatus() <= 299) {
        return response.readEntity(DataUseSummary.class);
      }
      logWarn("Error response from Ontology service: " + response.readEntity(String.class));
    } catch (Exception e) {
      logWarn("Error parsing response from Ontology service: " + e);
    }
    return null;
  }

  public String translateDataUse(DataUse dataUse, DataUseTranslationType type) {
    WebTarget target =
        client.target(servicesConfiguration.getOntologyURL() + "translate?for=" + type.getValue());
    try (Response response =
        target.request(MediaType.TEXT_PLAIN).post(Entity.json(dataUse.toString()))) {
      if (response.getStatus() == 200) {
        return response.readEntity(String.class);
      }

      throw new RuntimeException(
          "Error response from Ontology service: " + response.readEntity(String.class));
    } catch (Exception e) {
      logWarn("Error parsing response from Ontology service: " + e);
      throw e;
    }
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
}
