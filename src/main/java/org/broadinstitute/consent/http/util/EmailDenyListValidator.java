package org.broadinstitute.consent.http.util;

import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.broadinstitute.consent.http.configurations.NihConfiguration;

/**
 * Validates email addresses against the NIH deny-list configured in {@link NihConfiguration}.
 *
 * <p>The deny-list is a collection of regular expressions (sourced from the shared {@code
 * global.nih.denyEmailPatterns} helm value) describing restricted address patterns.
 *
 * <p>Patterns are compiled once at construction. An email is "denied" when it fully matches any
 * configured pattern.
 */
public class EmailDenyListValidator implements ConsentLogger {

  private final List<Pattern> denyPatterns;

  public EmailDenyListValidator(NihConfiguration nihConfiguration) {
    this.denyPatterns =
        nihConfiguration.getDenyEmailPatterns().stream()
            .map(this::compilePattern)
            .filter(Objects::nonNull)
            .toList();
  }

  /**
   * @param email the email address to check; may be null or blank
   * @return true if the email fully matches any configured deny pattern
   */
  public boolean isDenied(String email) {
    if (email == null || email.isBlank()) {
      return false;
    }
    String trimmed = email.trim();
    return denyPatterns.stream().anyMatch(pattern -> pattern.matcher(trimmed).matches());
  }

  private Pattern compilePattern(String regex) {
    try {
      return Pattern.compile(regex);
    } catch (PatternSyntaxException e) {
      // Skip an invalid pattern rather than failing startup; a single bad entry in config
      // should not take down the service or disable the rest of the deny-list.
      logWarn("Ignoring invalid deny email pattern '" + regex + "': " + e.getMessage());
      return null;
    }
  }
}
