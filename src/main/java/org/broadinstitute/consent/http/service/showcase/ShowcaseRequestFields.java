package org.broadinstitute.consent.http.service.showcase;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.broadinstitute.consent.http.models.Dataset;

/**
 * Typed, limit-checked reads of one request's fields. Each failure is a {@link
 * ShowcaseLookupException} whose message names only the caller's input and the contract's limits.
 */
final class ShowcaseRequestFields {

  static final String DAC_SCOPE = "dac";

  /** Longer number text cannot be an integer within any lookup limit. */
  private static final int MAX_NUMBER_TEXT = 20;

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
    return string(name, required(name));
  }

  /** An integer from 1 to {@code max}, or {@code fallback} when the field is absent. */
  int optionalCount(String name, int max, int fallback) {
    JsonElement value = fields.get(name);
    if (value == null) {
      return fallback;
    }
    String rangeError = "Field \"%s\" must be an integer from 1 to %d".formatted(name, max);
    if (!isNumber(value) || value.getAsString().length() > MAX_NUMBER_TEXT) {
      throw new ShowcaseLookupException(rangeError);
    }
    BigDecimal number = value.getAsBigDecimal();
    if (number.signum() <= 0
        || number.compareTo(BigDecimal.valueOf(max)) > 0
        || number.stripTrailingZeros().scale() > 0) {
      throw new ShowcaseLookupException(rangeError);
    }
    return number.intValueExact();
  }

  /** Canonical identifiers without duplicates, in the order first submitted. */
  List<String> requiredIdentifiers(String name, int max) {
    return identifiers(name, required(name), max);
  }

  List<String> optionalIdentifiers(String name, int max) {
    JsonElement value = fields.get(name);
    return value == null ? List.of() : identifiers(name, value, max);
  }

  /** The scope, or empty when the request has none. Only the {@code dac} key exists so far. */
  Optional<ShowcaseScope> optionalScope(String name) {
    JsonElement value = fields.get(name);
    if (value == null) {
      return Optional.empty();
    }
    if (!value.isJsonObject()) {
      throw new ShowcaseLookupException("Field \"%s\" must be an object".formatted(name));
    }
    JsonObject scope = value.getAsJsonObject();
    scope.keySet().stream()
        .filter(scopeName -> !DAC_SCOPE.equals(scopeName))
        .findFirst()
        .ifPresent(
            scopeName -> {
              throw new ShowcaseLookupException(
                  "Unrecognized scope key \"%s\"".formatted(scopeName));
            });
    JsonElement dac = scope.get(DAC_SCOPE);
    if (dac == null) {
      return Optional.of(new ShowcaseScope(null));
    }
    String dacField = name + "." + DAC_SCOPE;
    List<String> dacNames = strings(dacField, dac);
    checkCount(dacField, dacNames.size(), ShowcaseLookupLimits.MAX_SCOPE_VALUES);
    return Optional.of(new ShowcaseScope(List.copyOf(new LinkedHashSet<>(dacNames))));
  }

  private JsonElement required(String name) {
    JsonElement value = fields.get(name);
    if (value == null) {
      throw new ShowcaseLookupException("Missing field \"%s\"".formatted(name));
    }
    return value;
  }

  private static String string(String name, JsonElement value) {
    if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
      throw new ShowcaseLookupException("Field \"%s\" must be a string".formatted(name));
    }
    return value.getAsString();
  }

  private static boolean isNumber(JsonElement value) {
    return value instanceof JsonPrimitive primitive && primitive.isNumber();
  }

  /**
   * Counts the submitted entries before duplicates are removed, so that repeats cannot get past the
   * cap.
   */
  private static List<String> identifiers(String name, JsonElement value, int max) {
    List<String> entries = strings(name, value);
    checkCount(name, entries.size(), max);
    Set<String> unique = new LinkedHashSet<>();
    for (int index = 0; index < entries.size(); index++) {
      unique.add(canonicalIdentifier(name, entries.get(index), index));
    }
    return List.copyOf(unique);
  }

  private static void checkCount(String name, int count, int max) {
    if (count > max) {
      throw new ShowcaseLookupException(
          "Field \"%s\" has more than %d entries".formatted(name, max));
    }
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
