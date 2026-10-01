package org.broadinstitute.consent.http.models;

import java.time.Instant;
import org.broadinstitute.consent.http.enumeration.DarKind;
import org.broadinstitute.consent.http.enumeration.SoApprovalStatus;

/**
 * DARs of one kind and status submitted in one bucket. Days to SO approval are summarized for
 * approved ones only; {@code unmeasured} approvals predate their submission date, so they are
 * counted but not timed. The mode is over whole days.
 */
public record SoApprovalBucket(
    Instant bucketStart,
    DarKind kind,
    SoApprovalStatus status,
    Long count,
    Long unmeasured,
    Double meanDays,
    Double medianDays,
    Integer modeDays) {}
