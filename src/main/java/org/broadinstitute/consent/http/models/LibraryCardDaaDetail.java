package org.broadinstitute.consent.http.models;

/**
 * Lightweight representation of a Library Card's DAA association, enriched with the email of the
 * user who authorized (created) the association.
 *
 * @param daaId The Data Access Agreement ID
 * @param authorizedBy The email of the user who created the LC-DAA association
 */
public record LibraryCardDaaDetail(Integer daaId, String authorizedBy) {}
