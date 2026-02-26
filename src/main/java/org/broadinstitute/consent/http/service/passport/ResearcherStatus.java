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
    return assertedDate.getTime();
  }

  @Override
  public String value() {
    // TODO Collect public URL for the user's profile such as an ORCID or institutional profile.
    return PassportService.ISS;
  }

  @Override
  public String source() {
    return PassportService.ISS;
  }

  @Override
  public String by() {
    return VisaBy.SO.name();
  }
}
