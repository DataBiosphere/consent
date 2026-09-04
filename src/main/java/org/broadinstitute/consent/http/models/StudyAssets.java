package org.broadinstitute.consent.http.models;

import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.broadinstitute.consent.http.util.ConsentLogger;
import org.broadinstitute.consent.http.util.gson.GsonUtil;

/**
 * The study registration asset lists that backend behavior depends on.
 *
 * <p>These lists used to live as keys inside the client-managed {@code assets} object, which
 * DatasetRegistrationSchemaV1.yaml declares is "preserved and returned as-is by the backend; not
 * backend-validated". Once endpoints and dashboard counts began reading individual keys out of it,
 * that contract no longer held, so each key below is now a first-class registration field stored in
 * its own {@code study_property} row.
 *
 * <p>The {@code assets} object itself remains, deprecated, for any key that has not been promoted.
 * Until clients move to the top-level fields, reads fall back to it and writes are accepted through
 * it; see {@link #findAssetList} and {@link #stripPromoted}.
 */
public class StudyAssets implements ConsentLogger {

  public static final String MODELS = "models";
  public static final String WORKSPACES = "workspaces";
  public static final String PRESENTATIONS = "presentations";
  public static final String PUBLICATIONS = "publications";
  public static final String CLINICAL_TRIALS = "clinicalTrials";
  public static final String INTELLECTUAL_PROPERTIES = "intellectualProperties";
  public static final String BIOSPECIMENS = "biospecimens";
  public static final String FUNDING = "funding";

  /** The legacy client-managed object these lists were promoted out of. */
  public static final String ASSETS = "assets";

  public static final List<String> PROMOTED_KEYS =
      List.of(
          MODELS,
          WORKSPACES,
          PRESENTATIONS,
          PUBLICATIONS,
          CLINICAL_TRIALS,
          INTELLECTUAL_PROPERTIES,
          BIOSPECIMENS,
          FUNDING);

  private static final Type LIST_TYPE = new TypeToken<List<Object>>() {}.getType();
  private static final Type MAP_TYPE = new TypeToken<Map<String, Object>>() {}.getType();

  /**
   * The assets of one type recorded for a study. Reads the promoted property, falling back to the
   * legacy {@code assets} object for a study whose registration has not been rewritten since the
   * promotion. An absent or malformed value reads as "no assets of that type".
   *
   * <p>A promoted property that is present and parses is authoritative even when it holds an empty
   * list: a submitter who removed the last asset of a type must not have it restored from a stale
   * legacy copy.
   */
  public List<Object> findAssetList(Collection<StudyProperty> properties, String key) {
    if (properties == null) {
      return List.of();
    }
    Optional<List<Object>> promoted =
        properties.stream()
            .filter(property -> key.equalsIgnoreCase(property.getKey()))
            .map(StudyProperty::getValue)
            .<List<Object>>map(value -> parse(key, value, LIST_TYPE))
            .filter(Objects::nonNull)
            .findFirst();
    if (promoted.isPresent()) {
      return promoted.get();
    }
    Object legacy = findLegacyAssets(properties).get(key);
    return legacy instanceof Collection<?> collection
        ? collection.stream().map(Object.class::cast).toList()
        : List.of();
  }

  /**
   * The full assets object for a study: every promoted list, plus whatever keys remain in the
   * legacy object. This is the shape clients and the search index still expect.
   */
  public Map<String, Object> assemble(Collection<StudyProperty> properties) {
    Map<String, Object> assets = new LinkedHashMap<>(findLegacyAssets(properties));
    for (String key : PROMOTED_KEYS) {
      // Remove any legacy copy first. In particular, an authoritative empty promoted property
      // must remove a stale legacy list rather than accidentally resurrecting it.
      assets.keySet().removeIf(existingKey -> key.equalsIgnoreCase(existingKey));
      List<Object> values = findAssetList(properties, key);
      if (!values.isEmpty()) {
        assets.put(key, values);
      }
    }
    return assets;
  }

  /**
   * A client-supplied assets object with the promoted keys removed, so they are not stored twice.
   * Returns null when nothing is left to store.
   */
  public static Map<String, Object> stripPromoted(Map<String, Object> assets) {
    if (assets == null || assets.isEmpty()) {
      return null;
    }
    Map<String, Object> remaining = new LinkedHashMap<>(assets);
    PROMOTED_KEYS.forEach(remaining::remove);
    return remaining.isEmpty() ? null : remaining;
  }

  /**
   * The value a promoted field should be stored with: the top-level registration field when the
   * client sent one, otherwise the same key read out of the legacy assets object.
   *
   * <p>"Sent one" means present, not non-empty. Registration reads return every promoted list both
   * top-level and inside the legacy {@code assets} object, so an edit that clears the top-level
   * list arrives alongside the pre-edit legacy copy; treating {@code []} as "not provided" would
   * resurrect what the submitter just removed.
   */
  public static List<Object> promotedValue(
      List<Object> topLevel, Map<String, Object> assets, String key) {
    if (topLevel != null) {
      return topLevel;
    }
    Object legacy = assets == null ? null : assets.get(key);
    return legacy instanceof Collection<?> collection
        ? collection.stream().map(Object.class::cast).toList()
        : null;
  }

  private Map<String, Object> findLegacyAssets(Collection<StudyProperty> properties) {
    if (properties == null) {
      return Map.of();
    }
    return properties.stream()
        .filter(property -> ASSETS.equalsIgnoreCase(property.getKey()))
        .map(StudyProperty::getValue)
        .map(value -> parseMap(value))
        .filter(assets -> !assets.isEmpty())
        .findFirst()
        .orElseGet(Map::of);
  }

  private Map<String, Object> parseMap(Object value) {
    Map<String, Object> parsed = parse(ASSETS, value, MAP_TYPE);
    return parsed == null ? Map.of() : parsed;
  }

  /**
   * Study property values are JSON written by the client. A value that is malformed or of the wrong
   * shape is treated as absent rather than surfacing as a server error.
   */
  private <T> T parse(String key, Object value, Type type) {
    if (Objects.isNull(value)) {
      return null;
    }
    try {
      return GsonUtil.getInstance().fromJson(value.toString(), type);
    } catch (Exception e) {
      logWarn("Unable to parse the %s study property: %s".formatted(key, e.getMessage()));
      return null;
    }
  }
}
