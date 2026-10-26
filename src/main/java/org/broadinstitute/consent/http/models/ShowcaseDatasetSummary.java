package org.broadinstitute.consent.http.models;

import java.util.List;

/**
 * The public summary of one Data Showcase dataset. Its components mirror {@link
 * org.broadinstitute.consent.http.enumeration.ShowcasePublicField} exactly.
 *
 * @param identifier canonical {@code DUOS-} identifier
 * @param title dataset name
 * @param accessManagement {@code open}, {@code controlled} or {@code external}; null when the
 *     dataset has no recognized value
 * @param dacName name of the reviewing DAC
 * @param consentCodes consent codes such as {@code GRU} or {@code HMB}, without free-text values
 * @param participantCount number of participants
 * @param availableDate "new in DUOS" date, {@code YYYY-MM-DD}, the date part of the stored
 *     timestamp
 * @param accessUrl http(s) provider link for open and external datasets; null for others
 * @param species study species
 */
public record ShowcaseDatasetSummary(
    String identifier,
    String title,
    String accessManagement,
    String dacName,
    List<String> consentCodes,
    Integer participantCount,
    String availableDate,
    String accessUrl,
    String species) {}
