package org.broadinstitute.consent.http.models.dto.registration.template;

/**
 * The draft a valid template produced. The type travels with the id so a client knows what document
 * it is about to load rather than inferring it.
 */
public record StudyDatasetDraftReference(String id, String draftType) {}
