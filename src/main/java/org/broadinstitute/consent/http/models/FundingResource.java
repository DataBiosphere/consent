package org.broadinstitute.consent.http.models;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

public record FundingResource(
    String fundingId,
    String studyId,
    String funderName,
    String funderProgram,
    String grantNumber,
    String projectTitle,
    LocalDate startDate,
    LocalDate endDate,
    URI url,
    List<String> tags) {}
