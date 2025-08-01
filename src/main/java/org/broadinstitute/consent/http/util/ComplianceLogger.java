package org.broadinstitute.consent.http.util;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.User;
import org.glassfish.jersey.server.ContainerRequest;

public class ComplianceLogger implements ConsentLogger {

  private ComplianceLogger() {
  }

  private static ComplianceLogger getInstance() {
    return new ComplianceLogger();
  }

  public enum ComplianceEvent {
    DAR_SUBMISSION,
    DAR_APPROVAL,
    RADAR_APPROVAL,
    DAR_REJECTION,
    DAR_CANCELLATION,
    CLOSEOUT_SO_APPROVAL
  }

  private static final String MESSAGE = """
      _time: %s; \
      src_ip: %s; \
      dest_ip: %s; \
      dest_port: 443; \
      user_name: %s; \
      user_id: %s; \
      user_id_provider: %s; \
      url: %s; \
      app: DUOS; \
      http_user_agent: %s; \
      status: %s; \
      http_content_type: %s; \
      cadr_name: DUOS; \
      user_org: %s; \
      user_email: %s; \
      associated_study: %s; \
      eRA_commons_id: %s; \
      event_type: %s; \
      """;

  private void logEvent(User user, List<Dataset> datasets, ContainerRequest request,
      int responseStatusCode, ComplianceEvent event) {
    Instant now = Instant.now();
    String sourceIp = Objects.requireNonNullElse(request.getHeaderString("X-Forwarded-For"), "-");
    String destinationIp = Objects.requireNonNullElse(request.getHeaderString("X-Forwarded-Server"),
        "-");
    String userId = Objects.requireNonNullElse(request.getHeaderString("oidc_claim_user_id"), "-");
    String userAgent = Objects.requireNonNullElse(request.getHeaderString(HttpHeaders.USER_AGENT),
        "-");
    String userIdProvider = user.getEraCommonsId() == null ? "-" : "RAS";
    String urlString = request.getRequestUri() == null ? "-"
        : request.getRequestUri().toString();
    String responseContentType = MediaType.APPLICATION_JSON;
    String institutionName = user.getInstitution() == null ? "-" : user.getInstitution().getName();
    datasets.forEach(dataset -> {
      String logMessage = MESSAGE
          .formatted(
              now,
              sourceIp,
              destinationIp,
              user.getDisplayName(),
              userId,
              userIdProvider,
              urlString,
              userAgent,
              responseStatusCode,
              responseContentType,
              institutionName,
              user.getEmail(),
              dataset.getDatasetIdentifier(),
              user.getEraCommonsId(),
              event);
      logInfo(logMessage);
    });
  }

  public static void logDARApproval(User user, List<Dataset> datasets, ContainerRequest request,
      int responseStatusCode) {
    getInstance().logEvent(user, datasets, request, responseStatusCode, ComplianceEvent.DAR_APPROVAL);
  }

  public static void logRadarApproval(User user, List<Dataset> datasets, ContainerRequest request,
      int responseStatusCode) {
    getInstance().logEvent(user, datasets, request, responseStatusCode, ComplianceEvent.RADAR_APPROVAL);
  }

  public static void logDARRejection(User user, List<Dataset> datasets, ContainerRequest request,
      int responseStatusCode) {
    getInstance().logEvent(user, datasets, request, responseStatusCode, ComplianceEvent.DAR_REJECTION);
  }

  public static void logDARSubmission(User user, List<Dataset> datasets, ContainerRequest request,
      int responseStatusCode) {
    getInstance().logEvent(user, datasets, request, responseStatusCode, ComplianceEvent.DAR_SUBMISSION);
  }

  public static void logDARCancellation(User user, List<Dataset> datasets, ContainerRequest request,
      int responseStatusCode) {
    getInstance().logEvent(user, datasets, request, responseStatusCode, ComplianceEvent.DAR_CANCELLATION);
  }

  public static void logCloseoutApprovalBySigningOfficial(User user, List<Dataset> datasets,
      ContainerRequest request, int responseStatusCode) {
    getInstance().logEvent(user, datasets, request, responseStatusCode, ComplianceEvent.CLOSEOUT_SO_APPROVAL);
  }

}
