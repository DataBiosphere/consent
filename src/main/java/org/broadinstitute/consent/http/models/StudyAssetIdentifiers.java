package org.broadinstitute.consent.http.models;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import org.broadinstitute.consent.http.enumeration.AssetType;

/** Each model, workspace and biospecimen's DUOS identifier, added when served and never stored. */
public final class StudyAssetIdentifiers {

  public static final String DUOS_IDENTIFIER = "duosIdentifier";

  private record Kind(AssetType type, String idKey) {}

  private static final Map<String, Kind> KINDS =
      Map.of(
          StudyAssets.MODELS, new Kind(AssetType.MODEL, "modelId"),
          StudyAssets.WORKSPACES, new Kind(AssetType.WORKSPACE, "workspaceId"),
          StudyAssets.BIOSPECIMENS, new Kind(AssetType.BIOSPECIMEN, "biospecimenId"));

  private StudyAssetIdentifiers() {}

  /** The entries of one asset list, each with its identifier; other lists come back unchanged. */
  public static List<Object> decorate(String key, List<Object> entries) {
    Kind kind = KINDS.get(key);
    return kind == null ? entries : decorated(kind, entries);
  }

  /** An assets object, as {@link StudyAssets#assemble} builds it, with its entries decorated. */
  public static Map<String, Object> decorate(Map<String, Object> assets) {
    return mapLists(assets, StudyAssetIdentifiers::decorated);
  }

  /** The entries of one asset list without any {@code duosIdentifier}. */
  public static List<Object> strip(String key, List<Object> entries) {
    return KINDS.containsKey(key) ? stripped(entries) : entries;
  }

  /** An assets object with no {@code duosIdentifier} on any entry. */
  public static Map<String, Object> strip(Map<String, Object> assets) {
    return mapLists(assets, (kind, entries) -> stripped(entries));
  }

  private static Map<String, Object> mapLists(
      Map<String, Object> assets, BiFunction<Kind, List<?>, List<Object>> mapList) {
    Map<String, Object> mapped = new LinkedHashMap<>(assets);
    KINDS.forEach(
        (key, kind) -> {
          if (mapped.get(key) instanceof List<?> entries) {
            mapped.put(key, mapList.apply(kind, entries));
          }
        });
    return mapped;
  }

  private static List<Object> decorated(Kind kind, List<?> entries) {
    return entries.stream().map(e -> withIdentifier(kind, e)).toList();
  }

  private static List<Object> stripped(List<?> entries) {
    return entries.stream().map(StudyAssetIdentifiers::without).toList();
  }

  private static Object withIdentifier(Kind kind, Object entry) {
    if (!(entry instanceof Map<?, ?> map)) {
      return entry;
    }
    Map<Object, Object> copy = new LinkedHashMap<>(map);
    copy.remove(DUOS_IDENTIFIER);
    rowId(map.get(kind.idKey()))
        .ifPresent(id -> copy.put(DUOS_IDENTIFIER, AssetIdentifier.format(kind.type(), id)));
    return copy;
  }

  private static Object without(Object entry) {
    if (!(entry instanceof Map<?, ?> map) || !map.containsKey(DUOS_IDENTIFIER)) {
      return entry;
    }
    Map<Object, Object> copy = new LinkedHashMap<>(map);
    copy.remove(DUOS_IDENTIFIER);
    return copy;
  }

  /** A JSON number that is a whole number from 1 to 2147483647; a string id has none. */
  private static Optional<Integer> rowId(Object value) {
    if (!(value instanceof Number number)) {
      return Optional.empty();
    }
    double d = number.doubleValue();
    if (d != Math.rint(d) || d < 1 || d > Integer.MAX_VALUE) {
      return Optional.empty();
    }
    return Optional.of((int) d);
  }
}
