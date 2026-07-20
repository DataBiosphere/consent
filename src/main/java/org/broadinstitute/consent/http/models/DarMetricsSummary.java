package org.broadinstitute.consent.http.models;

import java.sql.Timestamp;

public record DarMetricsSummary(
    Timestamp updateDate,
    String projectTitle,
    String darCode,
    String nonTechRus,
    String referenceId,
    Boolean expired) {}
