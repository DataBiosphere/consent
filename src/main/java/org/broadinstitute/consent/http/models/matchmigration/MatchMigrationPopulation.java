package org.broadinstitute.consent.http.models.matchmigration;

/**
 * The match rows the dataset_id migration still has to deal with, counted in one pass.
 *
 * <p>Figures drift: the legacy set drains on its own as DARs are re-matched, which is why nothing
 * here is ever hardcoded and why the run recounts rather than trusting an earlier report.
 *
 * @param affectedMatches rows the constraints would reject: no dataset_id, or a v1/v2/absent stamp
 * @param affectedPurposes distinct purposes those rows belong to, the unit a reprocess works by
 * @param versionV1 affected rows still stamped v1
 * @param versionV2 affected rows still stamped v2
 * @param versionNull affected rows with no stamp at all, which the acceptance criteria ask for
 * @param missingDatasetId affected rows with no dataset_id, the non-null constraint's blockers
 * @param resolvablePurposes affected purposes whose DAR still resolves, so a reprocess can rebuild
 * @param archivedOrMissingPurposes affected purposes whose DAR does not; skipped, never reprocessed
 * @param purposesWithMultipleAffectedMatches purposes holding more than one affected row, where a
 *     rebuild could collapse two rows onto one (purpose, dataset_id) pair
 * @param duplicatePurposeDatasetPairs existing (purpose, dataset_id) collisions, which block the
 *     uniqueness constraint whether or not this migration created them
 */
public record MatchMigrationPopulation(
    int affectedMatches,
    int affectedPurposes,
    int versionV1,
    int versionV2,
    int versionNull,
    int missingDatasetId,
    int resolvablePurposes,
    int archivedOrMissingPurposes,
    int purposesWithMultipleAffectedMatches,
    int duplicatePurposeDatasetPairs) {

  /**
   * Whether the non-null and uniqueness constraints would still be refused. The changeset that
   * applies them halts on the same conditions, so this is the report an operator reads before
   * releasing it rather than a second, softer opinion.
   */
  public boolean blocksConstraints() {
    return affectedMatches > 0 || duplicatePurposeDatasetPairs > 0;
  }
}
