package org.broadinstitute.consent.http.models;

import java.time.Instant;
import org.broadinstitute.consent.http.enumeration.AuditActions;

/**
 * @param id The audit ID
 * @param daaId The Data Access Agreement ID
 * @param lcId The Library Card ID
 * @param lcUserId The Library Card User ID (the user associated with the library card at the time
 *     of the audit action)
 * @param userId The User ID who initiated the action
 * @param action The action performed (e.g., ADD, REMOVE)
 * @param actionDate The action date
 */
public record LibraryCardDaaAudit(
    Long id,
    Integer daaId,
    Integer lcId,
    Integer lcUserId,
    Integer userId,
    AuditActions action,
    Instant actionDate) {}
