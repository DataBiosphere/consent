package org.broadinstitute.consent.http.models;

import jakarta.ws.rs.core.Response;
import java.util.Objects;

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

  /** The study was deleted after the run listed it, so there was nothing to sync. */
  public boolean studyDeleted() {
    return Objects.equals(status, Response.Status.NOT_FOUND.getStatusCode());
  }

  /** The last line of a bulk resync; nextAfterStudyId is null once no studies remain. */
  public record Summary(
      int synced, int failed, int deleted, int indexFailed, Integer nextAfterStudyId) {}
}
