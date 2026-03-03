package org.broadinstitute.consent.http.service.passport;

import java.util.Optional;
import org.broadinstitute.consent.http.models.LibraryCard;
import org.broadinstitute.consent.http.models.User;

/**
 * <a
 * href="https://github.com/ga4gh-duri/ga4gh-duri.github.io/blob/master/researcher_ids/ga4gh_passport_v1.md#affiliationandrole">AffiliationAndRole</a>
 */
public class AffiliationAndRole implements VisaClaimType {

  private final User user;
  public static final String DEFAULT_VALUE = "faculty@no.organization";

  public AffiliationAndRole(User user) {
    this.user = user;
  }

  @Override
  public String type() {
    return VisaClaimTypes.AFFILIATION_AND_ROLE.type;
  }

  @Override
  public Long asserted() {
    var assertedDate =
        Optional.ofNullable(user.getLibraryCard())
            .map(LibraryCard::getCreateDate)
            .orElse(user.getCreateDate());
    // GA4GH requires Unix timestamps in seconds; Date#getTime() returns milliseconds.
    return PassportService.getEpochSeconds(assertedDate.toInstant());
  }

  @Override
  public String value() {
    if (user.getEmail() == null) {
      return DEFAULT_VALUE;
    }
    String[] splitEmail = user.getEmail().split("@");
    if (splitEmail.length > 1) {
      String domain = splitEmail[splitEmail.length - 1];
      return String.format("faculty@%s", domain);
    }
    return DEFAULT_VALUE;
  }

  @Override
  public String source() {
    return PassportService.ISS;
  }

  @Override
  public String by() {
    if (user.getLibraryCard() == null) {
      return VisaBy.SYSTEM.name().toLowerCase();
    }
    return VisaBy.SO.name().toLowerCase();
  }
}
