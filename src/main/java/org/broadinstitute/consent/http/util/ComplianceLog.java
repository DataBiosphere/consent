package org.broadinstitute.consent.http.util;

import java.time.Instant;
import java.util.List;
import org.broadinstitute.consent.http.models.Study;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.sam.UserStatusInfo;

public class ComplianceLog implements ConsentLogger {

  public enum ComplianceEvent {
    DAR_SUBMISSION,
    DAR_APPROVAL,
    DAR_REJECTION,
    DAR_CANCELLATION
  }

  private final ComplianceEvent event;
  private final UserStatusInfo statusInfo;
  private final User user;
  private final List<Study> studies;
  private static final String MESSAGE = """
          _time: %s;
          src_ip: '';
          dest_ip: '';
          dest_port: '';
          user_name: %s;
          user_id: %s;
          user_id_provider: '';
          session_id: '';
          url: '';
          app: DUOS;
          http_user_agent: '';
          status: '';
          http_content_type: '';
          bytes: '';
          duration: '';
          nih_ico: '';
          cadr_name: '';
          user_country_name: '';
          user_org: %s;
          user_email: %s;
          associated_study: %s;
          eRA_commons_id: %s;
          user_permission_group: '';
          event_type: %s;
          """;

  public ComplianceLog(ComplianceEvent event, UserStatusInfo statusInfo, User user,
      List<Study> studies) {
    this.event = event;
    this.statusInfo = statusInfo;
    this.user = user;
    this.studies = studies;
  }

  public void logComplianceEvent() {
    Instant now = Instant.now();
    studies.forEach(study -> {
      String logMessage = MESSAGE
          .formatted(
              now,
              user.getDisplayName(),
              statusInfo.getUserSubjectId(),
              user.getInstitution().getName(),
              user.getEmail(),
              study.getName(),
              user.getEraCommonsId(),
              event)
          .replace("\\R", " ");
      logInfo(logMessage);
    });
  }

}
