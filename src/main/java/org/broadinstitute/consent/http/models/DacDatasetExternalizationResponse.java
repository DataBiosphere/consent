package org.broadinstitute.consent.http.models;

import java.time.Instant;

public record DacDatasetExternalizationResponse(
    Integer dacId,
    boolean dryRun,
    String reason,
    Instant startedAt,
    Instant completedAt,
    int datasetsTotalInDac,
    int datasetsConvertedToExternal,
    int datasetsAlreadyExternal,
    int darDatasetApprovalsRevoked,
    int openElectionsCanceled,
    int usersWithAccessRemoved) {}
