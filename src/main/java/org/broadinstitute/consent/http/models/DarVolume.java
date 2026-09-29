package org.broadinstitute.consent.http.models;

import java.time.Instant;
import org.broadinstitute.consent.http.enumeration.InstitutionSource;

public record DarVolume(
    String referenceId,
    Integer collectionId,
    Integer userId,
    Instant submissionDate,
    Integer institutionId,
    String institutionName,
    InstitutionSource institutionSource,
    Integer datasetCount,
    Integer labStaffCount,
    Integer internalCollaboratorCount) {}
