package org.broadinstitute.consent.http.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.broadinstitute.consent.http.configurations.NihConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class EmailDenyListValidatorTest {

  // Representative entries from the shared global.nih.denyEmailPatterns list.
  private static final String HMAIL_PATTERN = "(?i).+@(.+\\.)*hmail\\.[^.]+";
  private static final String XYZ_TLD_PATTERN = "(?i).+@(.+\\.)+xyz";
  private static final String EXACT_ADDRESS_PATTERN = "(?i)exact_user@msu\\.edu";

  private EmailDenyListValidator validatorWith(List<String> patterns) {
    NihConfiguration config = new NihConfiguration();
    config.setDenyEmailPatterns(patterns);
    return new EmailDenyListValidator(config);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "user@hmail.com",
        "User@HMAIL.COM",
        "user@mail.hmail.com",
      })
  void deniesEmailMatchingPattern(String email) {
    EmailDenyListValidator validator = validatorWith(List.of(HMAIL_PATTERN));
    assertTrue(validator.isDenied(email));
  }

  @Test
  void allowsEmailNotMatchingPattern() {
    EmailDenyListValidator validator = validatorWith(List.of(HMAIL_PATTERN));
    assertFalse(validator.isDenied("user@non-restricted.org"));
  }

  @Test
  void allowsLookalikeDomain() {
    EmailDenyListValidator validator = validatorWith(List.of(HMAIL_PATTERN));
    assertFalse(validator.isDenied("user@nothmail.com"));
  }

  @Test
  void deniesCountryOfConcernTld() {
    EmailDenyListValidator validator = validatorWith(List.of(XYZ_TLD_PATTERN));
    assertTrue(validator.isDenied("researcher@university.xyz"));
    assertFalse(validator.isDenied("researcher@university.edu"));
  }

  @Test
  void deniesExactBannedAddressOnly() {
    EmailDenyListValidator validator = validatorWith(List.of(EXACT_ADDRESS_PATTERN));
    assertTrue(validator.isDenied("exact_user@msu.edu"));
    // Full-match semantics: a string merely containing the banned address is not denied.
    assertFalse(validator.isDenied("exact_user@msu.edu.good.com"));
  }

  @Test
  void matchesAgainstAnyConfiguredPattern() {
    EmailDenyListValidator validator =
        validatorWith(List.of(HMAIL_PATTERN, XYZ_TLD_PATTERN, EXACT_ADDRESS_PATTERN));
    assertTrue(validator.isDenied("user@hmail.com"));
    assertTrue(validator.isDenied("user@host.xyz"));
    assertTrue(validator.isDenied("exact_user@msu.edu"));
    assertFalse(validator.isDenied("user@non-restricted.org"));
  }

  @Test
  void emptyDenyListDeniesNothing() {
    EmailDenyListValidator validator = validatorWith(List.of());
    assertFalse(validator.isDenied("user@hmail.com"));
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"   "})
  void nullOrBlankEmailIsNotDenied(String email) {
    EmailDenyListValidator validator = validatorWith(List.of(HMAIL_PATTERN));
    assertFalse(validator.isDenied(email));
  }

  @Test
  void trimsWhitespaceBeforeMatching() {
    EmailDenyListValidator validator = validatorWith(List.of(HMAIL_PATTERN));
    assertTrue(validator.isDenied("  user@hmail.com  "));
  }

  @Test
  void invalidPatternIsSkippedAndDoesNotFailConstruction() {
    // An unclosed group is an invalid regex; it should be skipped while valid patterns still apply.
    EmailDenyListValidator validator = validatorWith(List.of("(unclosed", HMAIL_PATTERN));
    assertTrue(validator.isDenied("user@hmail.com"));
    assertFalse(validator.isDenied("user@non-restricted.org"));
  }
}
