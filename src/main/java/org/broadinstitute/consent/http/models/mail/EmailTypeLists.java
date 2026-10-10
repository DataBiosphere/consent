package org.broadinstitute.consent.http.models.mail;

import java.util.List;
import org.broadinstitute.consent.http.enumeration.EmailReference;

/**
 * {@link EmailReference}'s kinds and roles, indexed by email type number, as the sends query reads
 * them.
 */
public record EmailTypeLists(List<String> kinds, List<String> roles) {

  public static final EmailTypeLists CURRENT =
      new EmailTypeLists(EmailReference.kindsByTypeInt(), EmailReference.rolesByTypeInt());
}
