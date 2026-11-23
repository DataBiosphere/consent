package org.broadinstitute.consent.http.models.support;

import java.util.Locale;
import java.util.Objects;

/**
 * Who sent a support request, as a note at the end of the ticket for support staff. The requester
 * sees the ticket too, so the note never names a sign-in email: the form's email may belong to
 * someone else.
 */
public record SupportRequester(String note) {

  static final String UNVERIFIED =
      "Requester: not signed in. The name and email were entered in the form and are not verified"
          + " by DUOS. Treat attachments with care.";

  /** A request from the open Contact Us form, sent without signing in. */
  public static SupportRequester unverified() {
    return new SupportRequester(UNVERIFIED);
  }

  /**
   * A request from a signed-in user.
   *
   * @param signInEmail the email the sign-in verified
   * @param duosUserId the user's DUOS account, or null if they have not registered
   * @param formEmail the email entered in the form, which the ticket replies to
   */
  public static SupportRequester signedIn(
      String signInEmail, Integer duosUserId, String formEmail) {
    String who =
        duosUserId == null
            ? "Requester: signed in, without a DUOS account."
            : "Requester: signed in as DUOS user %d.".formatted(duosUserId);
    String email =
        sameEmail(signInEmail, formEmail)
            ? " The form's email is the sign-in email."
            : " The form's email differs from the sign-in email and is not verified.";
    return new SupportRequester(who + email);
  }

  private static boolean sameEmail(String a, String b) {
    return a != null && b != null && normalized(a).equals(normalized(b));
  }

  private static String normalized(String email) {
    return Objects.requireNonNull(email).trim().toLowerCase(Locale.ROOT);
  }
}
