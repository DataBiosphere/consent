package org.broadinstitute.consent.http.service.ontology;

/**
 * Result of reconciling ontology terms referenced by Datasets and DARs against the terms indexed in
 * the ontology_index table. Each result represents a single referenced term id that is either
 * missing from the index entirely or present but flagged unusable (obsolete/deprecated).
 *
 * @param termId The referenced ontology term id (full IRI).
 * @param issue MISSING_FROM_INDEX when no ontology_index row exists, or PRESENT_BUT_UNUSABLE when a
 *     row exists but is not usable.
 * @param ontology The ontology of the indexed term, or null when the term is missing.
 * @param referenceCount Total number of Dataset/DAR references to this term.
 * @param datasetRefs Number of Dataset references to this term.
 * @param darRefs Number of DAR references to this term.
 * @param referencedBy Comma-separated list of sources referencing the term (e.g. "DATASET:12,
 *     DAR:abc-123").
 */
public record OntologyReconciliationResult(
    String termId,
    String issue,
    String ontology,
    Long referenceCount,
    Long datasetRefs,
    Long darRefs,
    String referencedBy) {}
