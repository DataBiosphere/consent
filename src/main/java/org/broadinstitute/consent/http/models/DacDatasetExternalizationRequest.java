package org.broadinstitute.consent.http.models;

public record DacDatasetExternalizationRequest(
    String reason,
    Boolean dryRun,
    Boolean revokeApprovedAccess,
    Boolean cancelOpenElections,
    Boolean convertOpenAccessDatasets) {

  public DacDatasetExternalizationRequest {
    if (reason == null || reason.isBlank()) {
      throw new IllegalArgumentException("Reason is required");
    }
  }

  public boolean isDryRun() {
    return Boolean.TRUE.equals(dryRun);
  }

  public boolean shouldRevokeApprovedAccess() {
    return revokeApprovedAccess == null || revokeApprovedAccess;
  }

  public boolean shouldCancelOpenElections() {
    return cancelOpenElections == null || cancelOpenElections;
  }

  public boolean shouldConvertOpenAccessDatasets() {
    return Boolean.TRUE.equals(convertOpenAccessDatasets);
  }
}
