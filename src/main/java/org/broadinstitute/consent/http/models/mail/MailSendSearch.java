package org.broadinstitute.consent.http.models.mail;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.broadinstitute.consent.http.enumeration.EmailType;

/**
 * What to search email sends for: a case-insensitive {@code LIKE} pattern over recipient names, DAR
 * codes and DUOS-IDs, the dataset alias a DUOS-ID names, and the email types that match. A null
 * pattern means no search.
 */
public record MailSendSearch(String pattern, Long alias, List<Integer> types) {

  public static final int MAX_LENGTH = 200;

  public static final MailSendSearch NONE = new MailSendSearch(null, null, List.of());

  private static final Pattern DUOS_ID = Pattern.compile("(?i)^DUOS-0*([0-9]{1,18})$");

  /**
   * Matches {@code text} anywhere, taking it literally rather than as LIKE wildcards. The types are
   * those named in {@code labelledTypes}, for callers with their own labels, plus those whose enum
   * name contains the text as whole words.
   */
  public static MailSendSearch of(String text, List<Integer> labelledTypes) {
    if (text == null || text.isBlank()) {
      return NONE;
    }
    String term = text.strip();
    String literal = term.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    Matcher duosId = DUOS_ID.matcher(term);
    Long alias = duosId.matches() ? Long.valueOf(duosId.group(1)) : null;
    List<String> words = words(term);
    List<Integer> types =
        Stream.concat(
                labelledTypes == null ? Stream.empty() : labelledTypes.stream(),
                Stream.of(EmailType.values())
                    .filter(type -> Collections.indexOfSubList(words(type.name()), words) >= 0)
                    .map(EmailType::getTypeInt))
            .distinct()
            .toList();
    return new MailSendSearch("%" + literal + "%", alias, types);
  }

  private static List<String> words(String text) {
    return Arrays.stream(text.toLowerCase(Locale.ROOT).split("[\\s_]+"))
        .filter(word -> !word.isEmpty())
        .toList();
  }
}
