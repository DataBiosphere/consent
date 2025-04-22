package org.broadinstitute.consent.http.util;

import io.netty.handler.codec.http.HttpHeaderNames;
import jakarta.ws.rs.core.MediaType;
import java.time.Instant;
import java.util.List;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.User;
import org.glassfish.jersey.server.ContainerRequest;

public class ComplianceLogger implements ConsentLogger {

  private ComplianceLogger() {
  }

  public static ComplianceLogger getInstance() {
    return new ComplianceLogger();
  }

  public enum ComplianceEvent {
    DAR_SUBMISSION,
    DAR_APPROVAL,
    DAR_REJECTION,
    DAR_CANCELLATION
  }

  private static final String MESSAGE = """
      _time: %s;
      src_ip: -;
      dest_ip: -;
      dest_port: 443;
      user_name: %s;
      user_id: %s;
      user_id_provider: %s;
      session_id: -;
      url: %s;
      app: DUOS;
      http_user_agent: %s;
      status: %s;
      http_content_type: %s;
      bytes: -;
      duration: -;
      nih_ico: -;
      cadr_name: DUOS;
      user_country_name: -;
      user_org: %s;
      user_email: %s;
      associated_study: %s;
      eRA_commons_id: %s;
      user_permission_group: -;
      event_type: %s;
      """;

  public void logDARSubmission(User user, List<Dataset> datasets, ContainerRequest request,
      int responseStatusCode) {
    Instant now = Instant.now();
    String userId = request.getHeaderString("oidc_claim_user_id") == null ? "-"
        : request.getHeaderString("oidc_claim_user_id");
    String userAgent = request.getHeaderString(HttpHeaderNames.USER_AGENT.toString()) == null ? "-"
        : request.getHeaderString(HttpHeaderNames.USER_AGENT.toString());
    String userIdProvider = user.getEraCommonsId() == null ? "-" : "RAS";
    String responseContentType = MediaType.APPLICATION_JSON;
    String institutionName = user.getInstitution() == null ? "-" : user.getInstitution().getName();
    datasets.forEach(dataset -> {
      String logMessage = MESSAGE
          .formatted(
              now,
              user.getDisplayName(),
              userId,
              userIdProvider,
              request.getRequestUri().toString(),
              userAgent,
              responseStatusCode,
              responseContentType,
              institutionName,
              user.getEmail(),
              dataset.getDatasetIdentifier(),
              user.getEraCommonsId(),
              ComplianceEvent.DAR_SUBMISSION)
          .replace("\\R", " ");
      logInfo(logMessage);
    });
  }

}
