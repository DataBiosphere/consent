package org.broadinstitute.consent.http.models;

/**
 * The public result for a submitted identifier that has no showcase summary. Nonexistent, private,
 * deleted and section-ineligible datasets all get this same result, so the response never shows
 * which case applies.
 *
 * @param identifier the identifier as submitted
 * @param status always {@link #UNAVAILABLE}
 */
public record ShowcaseUnavailableResult(String identifier, String status) {

  public static final String UNAVAILABLE = "unavailable";

  public ShowcaseUnavailableResult(String identifier) {
    this(identifier, UNAVAILABLE);
  }
}
