package org.broadinstitute.consent.http.service.showcase;

import com.google.gson.JsonElement;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.broadinstitute.consent.http.models.Dataset;

/**
 * Typed, limit-checked reads of one request's fields. Each failure is a {@link
 * ShowcaseLookupException} whose message names only the caller's input and the contract's limits.
 */
final class ShowcaseRequestFields {

  private final Map<String, JsonElement> fields;
  private final String mode;

  ShowcaseRequestFields(Map<String, JsonElement> fields, String mode) {
    this.fields = fields;
    this.mode = mode;
  }

  String mode() {
    return mode;
  }

  void rejectUnrecognized(Set<String> allowed) {
    fields.keySet().stream()
        .filter(name -> !allowed.contains(name))
        .findFirst()
        .ifPresent(
            name -> {
              throw new ShowcaseLookupException(
                  "Unrecognized field \"%s\" for mode \"%s\"".formatted(name, mode));
            });
  }

  String requiredString(String name) {
    JsonElement value = required(name);
    if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
      throw new ShowcaseLookupException("Field \"%s\" must be a string".formatted(name));
    }
    return value.getAsString();
  }

  /**
   * Canonical identifiers without duplicates, in the order first submitted. The entries are counted
   * before duplicates are removed, so that repeats cannot get past the cap.
   */
  List<String> requiredIdentifiers(String name, int max) {
    List<String> entries = strings(name, required(name));
    if (entries.size() > max) {
      throw new ShowcaseLookupException(
          "Field \"%s\" has more than %d entries".formatted(name, max));
    }
    Set<String> unique = new LinkedHashSet<>();
    for (int index = 0; index < entries.size(); index++) {
      unique.add(canonicalIdentifier(name, entries.get(index), index));
    }
    return List.copyOf(unique);
  }

  private JsonElement required(String name) {
    JsonElement value = fields.get(name);
    if (value == null) {
      throw new ShowcaseLookupException("Missing field \"%s\"".formatted(name));
    }
    return value;
  }

  private static List<String> strings(String name, JsonElement value) {
    String typeError = "Field \"%s\" must be an array of strings".formatted(name);
    if (!value.isJsonArray()) {
      throw new ShowcaseLookupException(typeError);
    }
    return value.getAsJsonArray().asList().stream()
        .map(
            entry -> {
              if (!entry.isJsonPrimitive() || !entry.getAsJsonPrimitive().isString()) {
                throw new ShowcaseLookupException(typeError);
              }
              return entry.getAsString();
            })
        .toList();
  }

  /**
   * Accepts only the form that the lookup returns, so a configured identifier always matches its
   * result: {@code DUOS-123} is rejected because it comes back as {@code DUOS-000123}.
   */
  private static String canonicalIdentifier(String name, String identifier, int index) {
    if (identifier.length() > ShowcaseLookupLimits.MAX_IDENTIFIER_LENGTH) {
      throw new ShowcaseLookupException(
          "Field \"%s\" entry %d is longer than %d characters"
              .formatted(name, index, ShowcaseLookupLimits.MAX_IDENTIFIER_LENGTH));
    }
    String canonical = Dataset.parseAliasToIdentifier(alias(identifier));
    if (!canonical.equals(identifier)) {
      throw new ShowcaseLookupException(
          "Identifier \"%s\" is not canonical; use \"%s\"".formatted(identifier, canonical));
    }
    return identifier;
  }

  private static Integer alias(String identifier) {
    try {
      Integer alias = Dataset.parseIdentifierToAlias(identifier);
      if (alias >= 0) {
        return alias;
      }
    } catch (IllegalArgumentException e) {
      // Reported below with the same message as a negative alias.
    }
    throw new ShowcaseLookupException(
        "Identifier \"%s\" is not a DUOS identifier".formatted(identifier));
  }
}
