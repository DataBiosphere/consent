package org.broadinstitute.consent.http.models.draft;

public record FileTypeObject(
    FileType fileType,
    String functionalEquivalence
) {}