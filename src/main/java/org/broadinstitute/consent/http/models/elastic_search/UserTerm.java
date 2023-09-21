package org.broadinstitute.consent.http.models.elastic_search;

public record UserTerm(Integer userId, String displayName, InstitutionTerm institution) {}
