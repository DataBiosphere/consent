package org.broadinstitute.consent.http.models.passport;

import java.util.Optional;
import org.broadinstitute.consent.http.models.LibraryCard;
import org.broadinstitute.consent.http.models.User;

/**
 * https://github.com/ga4gh-duri/ga4gh-duri.github.io/blob/master/researcher_ids/ga4gh_passport_v1.md#affiliationandrole
 */
public class AffiliationAndRole implements VisaClaimType {

  private final User user;

  public AffiliationAndRole(User user) {
    this.user = user;
  }

  @Override
  public String type() {
    return "AffiliationAndRole";
  }

  @Override
  public Integer asserted() {
    if (user.getLibraryCards() != null) {
      Optional<LibraryCard> maybeLc = user.getLibraryCards().stream().findFirst();
      return maybeLc
          .map(libraryCard -> Long.valueOf(libraryCard.getCreateDate().getTime()).intValue())
          .orElseGet(() -> Long.valueOf(user.getCreateDate().getTime()).intValue());
    }
    return null;
  }

  // TODO Look for a better way to get the user's institutional domain. This is
  // not captured currently in our Institution model so we use email domain as
  // a proxy for institutional domain
  @Override
  public String value() {
    String[] splitEmail = user.getEmail().split("@");
    if (splitEmail.length > 1) {
      String domain = splitEmail[splitEmail.length - 1];
      return String.format("duos.researcher@%s", domain);
    }
    return "duos.researcher@no.organization";
  }

  @Override
  public String source() {
    return "https://duos.org/";
  }

  @Override
  public String by() {
    if (user.getLibraryCards() == null || user.getLibraryCards().isEmpty()) {
      return VisaBy.system.name();
    }
    return VisaBy.so.name();
  }
}
