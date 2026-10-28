package org.broadinstitute.consent.http.models;

import com.google.gson.Gson;
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
   * @param stored The entries to persist: as sent, less any {@code dataUseSummary}, which is
   *     derived on read and only arrives when a client sends back what a read returned
   * @param violations One message per invalid field, prefixed with its location
   */
  public record Prepared(List<Object> stored, List<String> violations) {}

  /** Validates the entries of one asset list. Never throws; a null list passes through as null. */
  public static Prepared prepare(String assetKey, List<Object> entries) {
    if (entries == null || !ASSET_KEYS.contains(assetKey)) {
      return new Prepared(entries, List.of());
    }
    List<Object> stored = new ArrayList<>(entries.size());
    List<String> violations = new ArrayList<>();
    for (int i = 0; i < entries.size(); i++) {
      Object entry = entries.get(i);
      if (entry instanceof Map<?, ?> map) {
        violations.addAll(entryViolations("%s[%d]".formatted(assetKey, i), map));
        stored.add(withoutSummary(map));
      } else {
        stored.add(entry);
      }
    }
    return new Prepared(stored, violations);
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

  private static Optional<AccessManagement> accessManagement(Object raw) {
    return Arrays.stream(AccessManagement.values())
        .filter(value -> value.value().equals(raw))
        .findFirst();
  }

  private record ReadDataUse(DataUse value, String problem) {}

  /**
   * The raw map is what gets stored, so it must read back exactly: Gson would otherwise coerce
   * {@code "yes"} to false and skip unknown keys, validating something other than what is stored.
   */
  private static ReadDataUse readDataUse(Object raw) {
    if (!(raw instanceof Map<?, ?>)) {
      return new ReadDataUse(null, "must be a data use object");
    }
    Gson gson = GsonUtil.getInstance();
    JsonObject sent = gson.toJsonTree(raw).getAsJsonObject();
    DataUse dataUse;
    try {
      dataUse = gson.fromJson(sent, DataUse.class);
    } catch (JsonParseException e) {
      return new ReadDataUse(null, "must be a data use object");
    }
    JsonObject read = gson.toJsonTree(dataUse).getAsJsonObject();
    List<String> unread =
        sent.keySet().stream().filter(key -> !sent.get(key).equals(read.get(key))).toList();
    return unread.isEmpty()
        ? new ReadDataUse(dataUse, null)
        : new ReadDataUse(null, "unknown or mistyped fields " + String.join(", ", unread));
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
