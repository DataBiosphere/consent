package org.broadinstitute.consent.http.models.mail;

import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import org.broadinstitute.consent.http.enumeration.EmailType;

/**
 * What to search email sends for: a case-insensitive {@code LIKE} pattern over recipient names,
 * entity references, DAR codes and DUOS-IDs, and the email types that match. A null pattern means
 * no search.
 */
public record MailSendSearch(String pattern, List<Integer> types) {

  public static final int MAX_LENGTH = 200;

  public static final MailSendSearch NONE = new MailSendSearch(null, List.of());

  /**
   * Matches {@code text} anywhere, taking it literally rather than as LIKE wildcards. The types are
   * those named in {@code labelledTypes}, for callers with their own labels, plus those whose enum
   * name reads as containing the text.
   */
  public static MailSendSearch of(String text, List<Integer> labelledTypes) {
    if (text == null || text.isBlank()) {
      return NONE;
    }
    String term = text.strip();
    String literal = term.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    String needle = term.toLowerCase(Locale.ROOT);
    List<Integer> types =
        Stream.concat(
                labelledTypes == null ? Stream.empty() : labelledTypes.stream(),
                Stream.of(EmailType.values())
                    .filter(
                        type ->
                            type.name().replace('_', ' ').toLowerCase(Locale.ROOT).contains(needle))
                    .map(EmailType::getTypeInt))
            .distinct()
            .toList();
    return new MailSendSearch("%" + literal + "%", types);
  }
}
