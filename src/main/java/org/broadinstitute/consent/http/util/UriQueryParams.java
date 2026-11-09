package org.broadinstitute.consent.http.util;

import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.UriBuilder;

/**
 * Adds query parameters from a client to a {@link UriBuilder} as literal text. {@link
 * UriBuilder#queryParam} reads its names and values as URI templates, so a client value that holds
 * a brace either fails the build or fills in a template variable. This class escapes each name and
 * value first, so the builder finds no template in it.
 */
public final class UriQueryParams {

  private UriQueryParams() {}

  public static void addAll(UriBuilder builder, MultivaluedMap<String, String> parameters) {
    parameters.forEach(
        (name, values) ->
            builder.queryParam(
                literal(name), values.stream().map(UriQueryParams::literal).toArray()));
  }

  public static void add(UriBuilder builder, String name, String value) {
    builder.queryParam(literal(name), literal(value));
  }

  public static void replace(UriBuilder builder, String name, String value) {
    builder.replaceQueryParam(literal(name), literal(value));
  }

  /**
   * Encodes only the characters that the builder misreads: braces start a template, and the builder
   * keeps an existing "%XX" as an escape instead of literal text. The builder encodes all other
   * characters itself, so a space still becomes "+".
   */
  private static String literal(String text) {
    return text.replace("%", "%25").replace("{", "%7B").replace("}", "%7D");
  }
}
