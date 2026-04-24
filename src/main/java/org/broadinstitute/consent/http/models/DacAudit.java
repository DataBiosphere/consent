package org.broadinstitute.consent.http.models;

import java.time.Instant;
import org.broadinstitute.consent.http.enumeration.AuditActions;

public record DacAudit(
    Long id,
    Integer dacId,
    Integer userId,
    Integer affectedUserId,
    Integer roleId,
    AuditActions action,
    Instant actionDate) {}
