package org.broadinstitute.consent.http.service.passport;

import java.util.Optional;
import org.broadinstitute.consent.http.models.LibraryCard;
import org.broadinstitute.consent.http.models.User;

/**
 * <a
 * href="https://github.com/ga4gh-duri/ga4gh-duri.github.io/tree/master/researcher_ids#researcherstatus">ResearcherStatus</a>
 */
public class ResearcherStatus implements VisaClaimType {

  private final User user;

  public ResearcherStatus(User user) {
    this.user = user;
  }

  @Override
  public String type() {
    return VisaClaimTypes.RESEARCHER_STATUS.type;
  }

  @Override
  public Long asserted() {
    var assertedDate =
        Optional.ofNullable(user.getLibraryCard())
            .map(LibraryCard::getCreateDate)
            .orElse(user.getCreateDate());
    if (assertedDate == null) {
      return PassportService.getEpochSeconds(java.time.Instant.now());
    }
    // java.sql.Date#toInstant throws UnsupportedOperationException; use epoch millis instead.
    return PassportService.getEpochSeconds(java.time.Instant.ofEpochMilli(assertedDate.getTime()));
  }

  @Override
  public Object value() {
    // See https://broadworkbench.atlassian.net/browse/DT-2863
    // This will be replaced with an external profile link.
    return PassportService.ISS;
  }

  @Override
  public String source() {
    return PassportService.ISS;
  }

  @Override
  public String by() {
    return VisaBy.SO.name().toLowerCase();
  }
}
