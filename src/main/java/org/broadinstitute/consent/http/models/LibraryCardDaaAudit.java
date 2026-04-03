package org.broadinstitute.consent.http.models;

import java.time.Instant;
import org.broadinstitute.consent.http.enumeration.AuditActions;

public record LibraryCardDaaAudit(
    Long id,
    Integer daaId,
    Integer lcId,
    Integer userId,
    AuditActions action,
    Instant actionDate) {}
