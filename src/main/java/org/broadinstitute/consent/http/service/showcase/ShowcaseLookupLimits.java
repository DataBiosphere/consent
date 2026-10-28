package org.broadinstitute.consent.http.service.showcase;

/**
 * The hard input limits of the public Data Showcase lookup (S2.2). They are contract values: the
 * BFF proxy and the duos-ui guard ({@code LOOKUP_LIMITS}) enforce the same numbers, and the OpenAPI
 * document publishes them. Change them only through a reviewed contract change.
 */
public final class ShowcaseLookupLimits {

  /** The largest request body, in bytes. A larger body gets a 413 before it is parsed. */
  public static final int MAX_BODY_BYTES = 64 * 1024;

  /** Submitted {@code identifiers}, counted before duplicates are removed. */
  public static final int MAX_IDENTIFIERS = 200;

  /** Submitted {@code pin} entries, counted before duplicates are removed. */
  public static final int MAX_PINS = 50;

  /** Submitted {@code exclude} entries, counted before duplicates are removed. */
  public static final int MAX_EXCLUSIONS = 200;

  /** Submitted values for one {@code scope} key. */
  public static final int MAX_SCOPE_VALUES = 20;

  public static final int MAX_IDENTIFIER_LENGTH = 32;

  public static final int DEFAULT_RANKING_LIMIT = 12;
  public static final int MAX_RANKING_LIMIT = 50;

  private ShowcaseLookupLimits() {}
}
