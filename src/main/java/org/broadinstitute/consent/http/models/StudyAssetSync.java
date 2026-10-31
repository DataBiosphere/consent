package org.broadinstitute.consent.http.models;

public record StudyAssetSync(Integer studyId, boolean synced, IndexOutcome index) {

  public enum IndexOutcome {
    UPDATED,
    /** None of the study's datasets is in the index, so there was nothing to update. */
    NOT_INDEXED,
    FAILED
  }

  public static StudyAssetSync synced(Integer studyId, IndexOutcome index) {
    return new StudyAssetSync(studyId, true, index);
  }
}
