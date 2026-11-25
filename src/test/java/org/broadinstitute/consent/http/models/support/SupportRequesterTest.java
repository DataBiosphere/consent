package org.broadinstitute.consent.http.models.support;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SupportRequesterTest {

  @Test
  void testUnverified() {
    assertEquals(
        "Requester: not signed in. The name and email were entered in the form and are not"
            + " verified by DUOS. Treat attachments with care.",
        SupportRequester.unverified().note());
  }

  @Test
  void testSignedInWithTheSameEmail() {
    assertEquals(
        "Requester: signed in as DUOS user 12. The form's email is the sign-in email.",
        SupportRequester.signedIn("Ada@Example.org", 12, " ada@example.org ").note());
  }

  // The requester sees the ticket, so the note never names the sign-in email.
  @Test
  void testSignedInWithADifferentEmail() {
    String note = SupportRequester.signedIn("ada@example.org", 12, "bob@example.org").note();

    assertEquals(
        "Requester: signed in as DUOS user 12. The form's email differs from the sign-in email"
            + " and is not verified.",
        note);
  }

  @Test
  void testSignedInWithoutADuosAccount() {
    assertEquals(
        "Requester: signed in, without a DUOS account. The form's email is the sign-in email.",
        SupportRequester.signedIn("ada@example.org", null, "ada@example.org").note());
  }

  @Test
  void testAMissingFormEmailIsNotTheSignInEmail() {
    assertEquals(
        "Requester: signed in as DUOS user 12. The form's email differs from the sign-in email"
            + " and is not verified.",
        SupportRequester.signedIn("ada@example.org", 12, null).note());
  }
}
