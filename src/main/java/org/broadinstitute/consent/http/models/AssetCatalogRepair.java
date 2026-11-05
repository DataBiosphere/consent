package org.broadinstitute.consent.http.models;

/**
 * What an asset catalog repair changed. {@code skipped} counts the datasets and assets still left
 * without an entry, such as collisions; {@code aliasSequenceAdvancedTo} is null when the sequence
 * needed no change.
 */
public record AssetCatalogRepair(
    int inserted, int relinked, int stamped, long skipped, Long aliasSequenceAdvancedTo) {}
