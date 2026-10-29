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
 * <p>The {@code ranking}, {@code all} and {@code stats} modes follow in later pull requests and are
 * unknown modes until then.
 */
public final class ShowcaseLookupParser {

  private static final String MODE = "mode";
  private static final String SECTION = "section";
  private static final String IDENTIFIERS = "identifiers";

  private static final Set<String> IDENTIFIERS_FIELDS = Set.of(MODE, SECTION, IDENTIFIERS);

  private ShowcaseLookupParser() {}

  /**
   * @throws ShowcaseLookupException when the request breaks the contract
   */
  public static ShowcaseLookup parse(byte[] body) {
    Map<String, JsonElement> fields = ShowcaseJsonBody.read(body);
    String mode = new ShowcaseRequestFields(fields, null).requiredString(MODE);
    ShowcaseRequestFields request = new ShowcaseRequestFields(fields, mode);
    if (IDENTIFIERS.equals(mode)) {
      return identifiersLookup(request);
    }
    throw new ShowcaseLookupException("Unknown mode \"%s\"".formatted(echoed(mode)));
  }

  private static ShowcaseIdentifiersLookup identifiersLookup(ShowcaseRequestFields request) {
    request.rejectUnrecognized(IDENTIFIERS_FIELDS);
    String slug = request.requiredString(SECTION);
    ShowcaseSection section =
        ShowcaseSection.fromSlug(slug)
            .filter(ShowcaseSection::isEditorial)
            .orElseThrow(
                () ->
                    new ShowcaseLookupException(
                        "Unknown section \"%s\" for mode \"%s\""
                            .formatted(echoed(slug), request.mode())));
    return new ShowcaseIdentifiersLookup(
        section, request.requiredIdentifiers(IDENTIFIERS, ShowcaseLookupLimits.MAX_IDENTIFIERS));
  }
}
