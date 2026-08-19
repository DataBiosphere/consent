package org.broadinstitute.consent.http.models.dto.registration.template;

/**
 * The draft a valid template produced. The type is carried alongside the id because a client must
 * know what kind of document it is about to load rather than inferring one from the id or the route
 * that produced it.
 */
public record StudyDatasetDraftReference(String id, String draftType) {}
