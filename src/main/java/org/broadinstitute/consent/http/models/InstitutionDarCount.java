package org.broadinstitute.consent.http.models;

public record InstitutionDarCount(
    Integer institutionId, String institutionName, Long darCount, Long researcherCount) {}
