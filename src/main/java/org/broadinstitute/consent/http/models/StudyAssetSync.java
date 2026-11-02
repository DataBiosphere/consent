package org.broadinstitute.consent.http.models;

public record StudyAssetSync(Integer studyId, IndexOutcome index) {

  public enum IndexOutcome {
    UPDATED,
    /** None of the study's datasets is in the index, so there was nothing to update. */
    NOT_INDEXED,
    FAILED
  }
}
