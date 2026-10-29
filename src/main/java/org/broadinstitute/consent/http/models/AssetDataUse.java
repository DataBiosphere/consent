package org.broadinstitute.consent.http.models;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import jakarta.ws.rs.BadRequestException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.broadinstitute.consent.http.models.dataset_registration_v1.ConsentGroup.AccessManagement;
import org.broadinstitute.consent.http.models.datause.DataUsePrimaryValidator;
import org.broadinstitute.consent.http.util.gson.GsonUtil;

/**
 * Data use and access management on study asset entries. Each entry is a client-supplied map, so
 * only these keys are read; every other key is kept exactly as sent.
 */
public final class AssetDataUse {

  public static final String DATA_USE = "dataUse";
  public static final String ACCESS_MANAGEMENT = "accessManagement";
  public static final String DATA_USE_SUMMARY = "dataUseSummary";

  private static final List<String> ASSET_KEYS =
      List.of(StudyAssets.MODELS, StudyAssets.WORKSPACES);

  private static final String ACCESS_MANAGEMENT_VALUES =
      Arrays.stream(AccessManagement.values())
          .map(AccessManagement::value)
          .collect(Collectors.joining(", "));

  private AssetDataUse() {}

  /**
   * One message per invalid field of one asset list, prefixed with its location. Never throws.
   *
   * @param storedEntries The study's entries as stored. Each stored access management and data use
   *     pair excuses one sent entry with the same pair, so values saved before this validation
   *     existed do not block unrelated edits. Entries are matched by pair, not identity, so only
   *     sending more copies of a pair than are stored is caught.
   */
  public static List<String> violations(
      String assetKey, List<Object> entries, List<Object> storedEntries) {
    if (entries == null || !ASSET_KEYS.contains(assetKey)) {
      return List.of();
    }
    List<StoredFields> unmatched =
        storedEntries.stream()
            .filter(Map.class::isInstance)
            .map(entry -> StoredFields.of((Map<?, ?>) entry))
            .collect(Collectors.toCollection(ArrayList::new));
    List<String> violations = new ArrayList<>();
    for (int i = 0; i < entries.size(); i++) {
      if (entries.get(i) instanceof Map<?, ?> map && !unmatched.remove(StoredFields.of(map))) {
        violations.addAll(entryViolations("%s[%d]".formatted(assetKey, i), map));
      }
    }
    return violations;
  }

  /**
   * The entries to persist: as sent, less any {@code dataUseSummary}, which is derived on read and
   * only arrives when a client sends back what a read returned. A null list stays null.
   */
  public static List<Object> withoutSummaries(String assetKey, List<Object> entries) {
    if (entries == null || !ASSET_KEYS.contains(assetKey)) {
      return entries;
    }
    return entries.stream()
        .map(entry -> entry instanceof Map<?, ?> map ? withoutSummary(map) : entry)
        .toList();
  }

  private record StoredFields(JsonElement accessManagement, JsonElement dataUse) {
    static StoredFields of(Map<?, ?> entry) {
      Gson gson = GsonUtil.getInstance();
      return new StoredFields(
          gson.toJsonTree(entry.get(ACCESS_MANAGEMENT)), gson.toJsonTree(entry.get(DATA_USE)));
    }
  }

  private static List<String> entryViolations(String location, Map<?, ?> entry) {
    List<String> violations = new ArrayList<>();
    Object rawAccess = entry.get(ACCESS_MANAGEMENT);
    Optional<AccessManagement> access = accessManagement(rawAccess);
    if (rawAccess != null && access.isEmpty()) {
      violations.add(
          "%s.%s: must be one of %s"
              .formatted(location, ACCESS_MANAGEMENT, ACCESS_MANAGEMENT_VALUES));
    }
    Object rawDataUse = entry.get(DATA_USE);
    if (rawDataUse == null) {
      return violations;
    }
    ReadDataUse dataUse = readDataUse(rawDataUse);
    if (dataUse.problem() != null) {
      violations.add("%s.%s: %s".formatted(location, DATA_USE, dataUse.problem()));
    } else if (rawAccess == null || access.isPresent()) {
      // An invalid access management is already reported, and the primary rule depends on it.
      try {
        DataUsePrimaryValidator.validate(dataUse.value(), access.orElse(null));
      } catch (BadRequestException e) {
        violations.add("%s.%s: %s".formatted(location, DATA_USE, e.getMessage()));
      }
    }
    return violations;
  }

  /** Matches case exactly, as consent groups do. */
  private static Optional<AccessManagement> accessManagement(Object raw) {
    if (!(raw instanceof String value)) {
      return Optional.empty();
    }
    try {
      return Optional.of(AccessManagement.fromValue(value));
    } catch (IllegalArgumentException e) {
      return Optional.empty();
    }
  }

  private record ReadDataUse(DataUse value, String problem) {}

  /**
   * The raw map is what gets stored, so it must read back exactly: Gson would otherwise coerce
   * {@code "yes"} to false and skip unknown keys, validating something other than what is stored.
   * On a mismatch each field is read on its own, so that one Gson rejects outright is still named.
   */
  private static ReadDataUse readDataUse(Object raw) {
    if (!(raw instanceof Map<?, ?>)) {
      return new ReadDataUse(null, "must be a data use object");
    }
    Gson gson = GsonUtil.getInstance();
    JsonObject sent = gson.toJsonTree(raw).getAsJsonObject();
    try {
      DataUse dataUse = gson.fromJson(sent, DataUse.class);
      if (sent.equals(gson.toJsonTree(dataUse))) {
        return new ReadDataUse(dataUse, null);
      }
    } catch (JsonParseException e) {
      // Fall through to name the fields that do not read back.
    }
    List<String> unread =
        sent.keySet().stream().filter(key -> !readsBack(gson, key, sent.get(key))).toList();
    return new ReadDataUse(null, "unknown or mistyped fields " + String.join(", ", unread));
  }

  private static boolean readsBack(Gson gson, String key, JsonElement value) {
    JsonObject field = new JsonObject();
    field.add(key, value);
    try {
      return value.equals(
          gson.toJsonTree(gson.fromJson(field, DataUse.class)).getAsJsonObject().get(key));
    } catch (JsonParseException e) {
      return false;
    }
  }

  private static Object withoutSummary(Map<?, ?> entry) {
    if (!entry.containsKey(DATA_USE_SUMMARY)) {
      return entry;
    }
    Map<Object, Object> copy = new LinkedHashMap<>(entry);
    copy.remove(DATA_USE_SUMMARY);
    return copy;
  }
}
