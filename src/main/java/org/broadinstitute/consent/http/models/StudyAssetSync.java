package org.broadinstitute.consent.http.models;

public record StudyAssetSync(
    Integer studyId, boolean synced, IndexOutcome index, Integer status, String error) {

  public enum IndexOutcome {
    UPDATED,
    /** None of the study's datasets is in the index, so there was nothing to update. */
    NOT_INDEXED,
    FAILED
  }

  public static StudyAssetSync synced(Integer studyId, IndexOutcome index) {
    return new StudyAssetSync(studyId, true, index, null, null);
  }

  public static StudyAssetSync failed(Integer studyId, int status, String error) {
    return new StudyAssetSync(studyId, false, null, status, error);
  }

  /** The last line of a bulk resync. */
  public record Summary(int synced, int failed, int indexFailed) {}
}
