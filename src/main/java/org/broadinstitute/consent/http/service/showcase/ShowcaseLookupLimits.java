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

  public static final int MAX_IDENTIFIER_LENGTH = 32;

  private ShowcaseLookupLimits() {}
}
