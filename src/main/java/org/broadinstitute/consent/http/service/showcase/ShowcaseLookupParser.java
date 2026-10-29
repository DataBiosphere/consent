package org.broadinstitute.consent.http.service.showcase;

import static org.broadinstitute.consent.http.service.showcase.ShowcaseLookupException.echoed;

import com.google.gson.JsonElement;
import java.util.Map;
import java.util.Set;

/**
 * Reads a public Data Showcase lookup request and checks it against the contract before any data is
 * read. The body must be one strict JSON object in valid UTF-8 with no repeated field ({@link
 * ShowcaseJsonBody}). Every field must belong to the request's mode, and every value must have the
 * agreed type and stay within {@link ShowcaseLookupLimits}.
 *
 * <p>The {@code stats} mode follows in a later pull request and is an unknown mode until then.
 */
public final class ShowcaseLookupParser {

  private static final String MODE = "mode";
  private static final String SECTION = "section";
  private static final String IDENTIFIERS = "identifiers";
  private static final String RANKING = "ranking";
  private static final String SCOPE = "scope";
  private static final String LIMIT = "limit";
  private static final String PIN = "pin";
  private static final String EXCLUDE = "exclude";
  private static final String ALL = "all";
  private static final String PAGE_SIZE = "pageSize";
  private static final String CURSOR = "cursor";

  private static final Set<String> IDENTIFIERS_FIELDS = Set.of(MODE, SECTION, IDENTIFIERS);
  private static final Set<String> RANKING_FIELDS =
      Set.of(MODE, SECTION, RANKING, SCOPE, LIMIT, PIN, EXCLUDE);
  private static final Set<String> ALL_FIELDS = Set.of(MODE, PAGE_SIZE, CURSOR);

  private ShowcaseLookupParser() {}

  /**
   * @throws ShowcaseLookupException when the request breaks the contract
   */
  public static ShowcaseLookup parse(byte[] body) {
    Map<String, JsonElement> fields = ShowcaseJsonBody.read(body);
    String mode = new ShowcaseRequestFields(fields, null).requiredString(MODE);
    ShowcaseRequestFields request = new ShowcaseRequestFields(fields, mode);
    return switch (mode) {
      case IDENTIFIERS -> identifiersLookup(request);
      case RANKING -> rankingLookup(request);
      case ALL -> allLookup(request);
      default -> throw new ShowcaseLookupException("Unknown mode \"%s\"".formatted(echoed(mode)));
    };
  }

  private static ShowcaseIdentifiersLookup identifiersLookup(ShowcaseRequestFields request) {
    request.rejectUnrecognized(IDENTIFIERS_FIELDS);
    ShowcaseSection section = section(request);
    if (!section.isEditorial()) {
      throw new ShowcaseLookupException(
          "Section \"%s\" does not take mode \"%s\"".formatted(section.getSlug(), request.mode()));
    }
    return new ShowcaseIdentifiersLookup(
        section, request.requiredIdentifiers(IDENTIFIERS, ShowcaseLookupLimits.MAX_IDENTIFIERS));
  }

  private static ShowcaseRankingLookup rankingLookup(ShowcaseRequestFields request) {
    request.rejectUnrecognized(RANKING_FIELDS);
    ShowcaseSection section = section(request);
    String rankingSlug = request.requiredString(RANKING);
    ShowcaseRanking ranking =
        ShowcaseRanking.fromSlug(rankingSlug)
            .filter(section::accepts)
            .orElseThrow(
                () ->
                    new ShowcaseLookupException(
                        "Unknown ranking \"%s\" for section \"%s\""
                            .formatted(echoed(rankingSlug), section.getSlug())));
    return new ShowcaseRankingLookup(
        section,
        ranking,
        request.optionalScope(SCOPE).orElse(null),
        request.optionalCount(
            LIMIT,
            ShowcaseLookupLimits.MAX_RANKING_LIMIT,
            ShowcaseLookupLimits.DEFAULT_RANKING_LIMIT),
        request.optionalIdentifiers(PIN, ShowcaseLookupLimits.MAX_PINS),
        request.optionalIdentifiers(EXCLUDE, ShowcaseLookupLimits.MAX_EXCLUSIONS));
  }

  private static ShowcaseAllLookup allLookup(ShowcaseRequestFields request) {
    request.rejectUnrecognized(ALL_FIELDS);
    int pageSize =
        request.optionalCount(
            PAGE_SIZE, ShowcaseLookupLimits.MAX_PAGE_SIZE, ShowcaseLookupLimits.DEFAULT_PAGE_SIZE);
    Integer afterAlias =
        request.optionalString(CURSOR).map(ShowcaseLookupParser::cursor).orElse(null);
    return new ShowcaseAllLookup(pageSize, afterAlias);
  }

  private static int cursor(String cursor) {
    if (cursor.length() > ShowcaseLookupLimits.MAX_CURSOR_LENGTH) {
      throw new ShowcaseLookupException(
          "Field \"%s\" is longer than %d characters"
              .formatted(CURSOR, ShowcaseLookupLimits.MAX_CURSOR_LENGTH));
    }
    return ShowcaseCursor.decode(cursor);
  }

  private static ShowcaseSection section(ShowcaseRequestFields request) {
    String slug = request.requiredString(SECTION);
    return ShowcaseSection.fromSlug(slug).orElseThrow(() -> unknownSection(slug, request));
  }

  private static ShowcaseLookupException unknownSection(
      String slug, ShowcaseRequestFields request) {
    return new ShowcaseLookupException(
        "Unknown section \"%s\" for mode \"%s\"".formatted(echoed(slug), request.mode()));
  }
}
