package org.broadinstitute.consent.http.service;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import org.broadinstitute.consent.http.db.SigningOfficialDashboardDAO;
import org.broadinstitute.consent.http.db.SigningOfficialDashboardDAO.DashboardDatabaseCounts;
import org.broadinstitute.consent.http.models.SigningOfficialDashboardSummary;
import org.broadinstitute.consent.http.models.SigningOfficialDashboardSummary.DaaAssociations;
import org.broadinstitute.consent.http.models.SigningOfficialDashboardSummary.DarApprovals;
import org.broadinstitute.consent.http.models.SigningOfficialDashboardSummary.DarRequests;
import org.broadinstitute.consent.http.models.SigningOfficialDashboardSummary.DataSubmitters;
import org.broadinstitute.consent.http.models.SigningOfficialDashboardSummary.InstitutionLibrary;
import org.broadinstitute.consent.http.models.SigningOfficialDashboardSummary.ResearcherStatus;
import org.broadinstitute.consent.http.models.User;
import org.jdbi.v3.core.Jdbi;

public class SigningOfficialDashboardService {
  private final SigningOfficialDashboardDAO dashboardDAO;
  private final ElasticSearchService elasticSearchService;
  private final ExecutorService executorService;

  @Inject
  public SigningOfficialDashboardService(
      Jdbi jdbi, ElasticSearchService elasticSearchService, ExecutorService executorService) {
    this.dashboardDAO = jdbi.onDemand(SigningOfficialDashboardDAO.class);
    this.elasticSearchService = elasticSearchService;
    this.executorService = executorService;
  }

  public SigningOfficialDashboardSummary getSummary(User user) {
    if (user.getInstitutionId() == null) {
      throw new BadRequestException("Signing Official is not associated with an institution.");
    }
    CompletableFuture<DashboardDatabaseCounts> databaseCounts =
        CompletableFuture.supplyAsync(
            () ->
                dashboardDAO.getCounts(
                    user.getInstitutionId(), user.getUserId().toString(), user.getEmail()),
            executorService);
    CompletableFuture<InstitutionLibrary> libraryCounts =
        CompletableFuture.supplyAsync(
            () -> getInstitutionLibraryCounts(user.getInstitutionId()), executorService);
    try {
      DashboardDatabaseCounts db = databaseCounts.join();
      InstitutionLibrary library = libraryCounts.join();
      long inProcess = db.darTotal() - db.darApproved() - db.darCanceled();
      return new SigningOfficialDashboardSummary(
          new ResearcherStatus(db.activeResearchers(), db.inactiveResearchers()),
          new DarRequests(db.darTotal(), db.darApproved(), db.darCanceled(), inProcess),
          new DarApprovals(db.approvalTotal(), db.awaitingSoAction()),
          new DataSubmitters(db.approvedDataSubmitters()),
          library,
          new DaaAssociations(db.agreements(), db.researchersApproved()));
    } catch (CompletionException e) {
      if (e.getCause() instanceof RuntimeException runtimeException) throw runtimeException;
      throw e;
    }
  }

  /**
   * The dataset count has to match what the SO sees after following the tile to
   * /datalibrary/myinstitution, so this reuses that library's query verbatim — a bare institution
   * match, with no study-exists or DAC-approval narrowing. track_total_hits keeps hits.total.value
   * exact past Elasticsearch's default 10k cap.
   */
  private InstitutionLibrary getInstitutionLibraryCounts(Integer institutionId) {
    String query =
        """
        {
          "size": 0,
          "track_total_hits": true,
          "query": {"match_phrase": {"submitter.institution.id": %d}},
          "aggs": {"total_studies": {"cardinality": {"field": "study.studyId"}}}
        }
        """
            .formatted(institutionId);
    try (InputStream stream = elasticSearchService.searchDatasetsStream(query);
        InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
      JsonObject response = JsonParser.parseReader(reader).getAsJsonObject();
      long datasets =
          response.getAsJsonObject("hits").getAsJsonObject("total").get("value").getAsLong();
      long studies =
          response
              .getAsJsonObject("aggregations")
              .getAsJsonObject("total_studies")
              .get("value")
              .getAsLong();
      return new InstitutionLibrary(datasets, studies);
    } catch (Exception e) {
      throw new IllegalStateException("Unable to load institution library statistics", e);
    }
  }
}
