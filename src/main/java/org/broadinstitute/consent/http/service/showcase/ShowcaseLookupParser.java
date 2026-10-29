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
 * <p>The {@code all} and {@code stats} modes follow in later pull requests and are unknown modes
 * until then.
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

  private static final Set<String> IDENTIFIERS_FIELDS = Set.of(MODE, SECTION, IDENTIFIERS);
  private static final Set<String> RANKING_FIELDS =
      Set.of(MODE, SECTION, RANKING, SCOPE, LIMIT, PIN, EXCLUDE);

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
      default -> throw new ShowcaseLookupException("Unknown mode \"%s\"".formatted(echoed(mode)));
    };
  }

  private static ShowcaseIdentifiersLookup identifiersLookup(ShowcaseRequestFields request) {
    request.rejectUnrecognized(IDENTIFIERS_FIELDS);
    ShowcaseSection section = section(request);
    if (!section.isEditorial()) {
      throw unknownSection(section.getSlug(), request);
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
