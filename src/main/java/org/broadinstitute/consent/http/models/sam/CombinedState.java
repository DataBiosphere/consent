package org.broadinstitute.consent.http.models.sam;

/**
 * This class is intended to be a wrapper for the various states that are returned by Sam. See
 * original implementation in Sam: <a
 * href="https://sam.dsde-prod.broadinstitute.org/#/Users/getSamUserCombinedState">
 * https://sam.dsde-dev.broadinstitute.org/#/Users/getSamUserCombinedState</a> For the purposes of
 * DUOS, we only need the `samUser` and `termsOfServiceDetails` fields, but we can add more fields
 * as needed in the future. Note that the Sam swagger docs are not up to date with the actual
 * implementation, so we are using the actual implementation.
 */
@SuppressWarnings("unused")
public class CombinedState {

  private SamUser samUser;
  private TermsOfServiceDetails termsOfServiceDetails;

  public SamUser getSamUser() {
    return samUser;
  }

  public CombinedState setSamUser(SamUser samUser) {
    this.samUser = samUser;
    return this;
  }

  public TermsOfServiceDetails getTermsOfServiceDetails() {
    return termsOfServiceDetails;
  }

  public CombinedState setTermsOfServiceDetails(TermsOfServiceDetails termsOfServiceDetails) {
    this.termsOfServiceDetails = termsOfServiceDetails;
    return this;
  }

  /// Example response from Sam:
  /// "samUser": {
  ///   "azureB2CId": "string",
  ///   "createdAt": "2026-02-20T14:09:06.715Z",
  ///   "email": "user@example.com",
  ///   "enabled": true,
  ///   "googleSubjectId": "string",
  ///   "id": "string",
  ///   "updatedAt": "2026-02-20T14:09:06.715Z"
  /// }
  public record SamUser(
      String azureB2CId,
      String createdAt,
      String email,
      Boolean enabled,
      String googleSubjectId,
      String id,
      String updatedAt) {}

  /// Example response from Sam:
  /// "termsOfServiceDetails": {
  ///   "acceptedOn": "2026-02-20T14:09:06.715Z",
  ///   "isCurrentVersion": true,
  ///   "latestAcceptedVersion": "string",
  ///   "permitsSystemUsage": true,
  /// }
  public record TermsOfServiceDetails(
      String acceptedOn,
      Boolean isCurrentVersion,
      String latestAcceptedVersion,
      Boolean permitsSystemUsage) {}
}
