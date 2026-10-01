package org.broadinstitute.consent.http.models;

import java.time.Instant;
import org.broadinstitute.consent.http.enumeration.DarKind;
import org.broadinstitute.consent.http.enumeration.SoApprovalStatus;

public record SoApproval(
    String referenceId,
    Integer collectionId,
    DarKind kind,
    Instant submissionDate,
    SoApprovalStatus status,
    Instant approvalDate,
    Double elapsedDays) {}
