package org.broadinstitute.consent.http.service.dao;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import jakarta.ws.rs.BadRequestException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.consent.http.db.StudyAssetRecordDAO;
import org.broadinstitute.consent.http.db.StudyDAO;
import org.broadinstitute.consent.http.enumeration.PropertyType;
import org.broadinstitute.consent.http.models.StudyAssetKey;
import org.broadinstitute.consent.http.models.StudyAssets;
import org.broadinstitute.consent.http.models.StudyBiospecimenRecord;
import org.broadinstitute.consent.http.models.StudyModelRecord;
import org.broadinstitute.consent.http.models.StudyProperty;
import org.broadinstitute.consent.http.models.StudyWorkspaceRecord;
import org.broadinstitute.consent.http.util.ConsentLogger;
import org.broadinstitute.consent.http.util.NulCharacters;
import org.jdbi.v3.core.Handle;

/**
 * Keeps a study's asset rows in step with the asset lists its registration stores, and writes each
 * row's id back into the entry it belongs to.
 *
 * <p>Entries are edited as a {@link JsonElement} tree and never mapped to a typed class, so unknown
 * client keys and the exact text of every number survive the write-back.
 */
public class StudyAssetRecordSync implements ConsentLogger {

  private static final String BIOSPECIMEN_ID = "biospecimenId";
  private static final String SAMPLE_ID = "sampleId";
  private static final Pattern ROW_ID = Pattern.compile("[1-9]\\d{0,9}");
  private static final BigDecimal MAX_ROW_ID = BigDecimal.valueOf(Integer.MAX_VALUE);
  // Code points, as SQL counts them; at 4 bytes each this fits the legacy_key index's row limit.
  private static final int MAX_LEGACY_KEY_LENGTH = 255;

  private static final List<AssetType<?>> TYPES =
      List.of(
          new AssetType<>(
              StudyAssets.MODELS,
              "modelId",
              (id, studyId, legacyKey, f) ->
                  new StudyModelRecord(
                      id,
                      studyId,
                      legacyKey,
                      f.text("name"),
                      f.text("description"),
                      f.text("url"),
                      f.texts("cloud"),
                      f.text("format"),
                      f.text("license"),
                      f.texts("trainedOnDatasets"),
                      f.object("maintainer"),
                      f.texts("tags"),
                      f.text("accessManagement"),
                      f.object("dataUse"),
                      null,
                      null,
                      null),
              StudyAssetRecordDAO::findLiveModels,
              StudyAssetRecordDAO::findUsedModelLegacyKeys,
              StudyAssetRecordDAO::insertModels,
              StudyAssetRecordDAO::updateModels,
              StudyAssetRecordDAO::retireModelsExcept),
          new AssetType<>(
              StudyAssets.WORKSPACES,
              "workspaceId",
              (id, studyId, legacyKey, f) ->
                  new StudyWorkspaceRecord(
                      id,
                      studyId,
                      legacyKey,
                      f.text("name"),
                      f.text("platform"),
                      f.text("url"),
                      f.texts("cloud"),
                      f.text("description"),
                      f.texts("tools"),
                      f.text("access"),
                      f.texts("tags"),
                      f.text("accessManagement"),
                      f.object("dataUse"),
                      null,
                      null,
                      null),
              StudyAssetRecordDAO::findLiveWorkspaces,
              StudyAssetRecordDAO::findUsedWorkspaceLegacyKeys,
              StudyAssetRecordDAO::insertWorkspaces,
              StudyAssetRecordDAO::updateWorkspaces,
              StudyAssetRecordDAO::retireWorkspacesExcept),
          new AssetType<>(
              StudyAssets.BIOSPECIMENS,
              BIOSPECIMEN_ID,
              (id, studyId, legacyKey, f) -> {
                Fields interval = f.nested("postMortemInterval");
                return new StudyBiospecimenRecord(
                    id,
                    studyId,
                    legacyKey,
                    nonBlank(f.text(SAMPLE_ID)),
                    f.text("donorId"),
                    f.text("specimenType"),
                    f.text("preservationMethod"),
                    f.text("preservationDetails"),
                    f.text("dateOfCollection"),
                    interval.number("value"),
                    interval.text("unit"),
                    f.text("sex"),
                    f.number("age"),
                    f.text("race"),
                    f.text("countryOfOrigin"),
                    f.texts("extractedDiagnoses"),
                    f.text("pathology"),
                    f.text("organization"),
                    f.text("sourceSite"),
                    f.text("optionalDataUse"),
                    null,
                    null,
                    null);
              },
              StudyAssetRecordDAO::findLiveBiospecimens,
              StudyAssetRecordDAO::findUsedBiospecimenLegacyKeys,
              StudyAssetRecordDAO::insertBiospecimens,
              StudyAssetRecordDAO::updateBiospecimens,
              StudyAssetRecordDAO::retireBiospecimensExcept));

  /**
   * Syncs every asset type against the properties a registration write just stored, then stores
   * each list again with its ids. A type with no property has all of its live rows retired, since a
   * registration write replaces every property.
   */
  public void syncRegistration(Handle handle, Integer studyId, List<StudyProperty> props) {
    StudyAssetRecordDAO dao = handle.attach(StudyAssetRecordDAO.class);
    StudyDAO studyDAO = handle.attach(StudyDAO.class);
    for (AssetType<?> type : TYPES) {
      Optional<JsonElement> stored =
          props.stream()
              .filter(p -> type.key().equals(p.getKey()) && p.getValue() != null)
              .findFirst()
              .map(p -> JsonParser.parseString(p.getValue().toString()));
      if (stored.isPresent() && !stored.get().isJsonArray()) {
        logWarn(
            "Study %d: %s is not a list, so its rows were left as they are"
                .formatted(studyId, type.key()));
        continue;
      }
      JsonArray entries = stored.map(JsonElement::getAsJsonArray).orElseGet(JsonArray::new);
      sync(dao, studyId, type, entries);
      if (stored.isPresent()) {
        studyDAO.updateStudyProperty(
            studyId, type.key(), PropertyType.Json.toString(), entries.toString());
      }
    }
  }

  private <R> void sync(
      StudyAssetRecordDAO dao, Integer studyId, AssetType<R> type, JsonArray entries) {
    LiveRows live = new LiveRows(type.findLive().apply(dao, studyId));
    if (live.ids.isEmpty() && entries.isEmpty()) {
      return;
    }
    Set<String> usedLegacyKeys = new HashSet<>(type.findUsedLegacyKeys().apply(dao, studyId));
    boolean biospecimens = type.key().equals(StudyAssets.BIOSPECIMENS);
    Map<Integer, String> rowSampleIds =
        biospecimens ? dao.findLiveBiospecimenSampleIds(studyId) : Map.of();
    Map<String, Long> idCounts =
        entries.asList().stream()
            .filter(JsonElement::isJsonObject)
            .map(JsonElement::getAsJsonObject)
            .map(entry -> idText(entry.get(type.idKey())))
            .filter(Objects::nonNull)
            .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

    Writes<R> writes = new Writes<>();
    for (int i = 0; i < entries.size(); i++) {
      if (!entries.get(i).isJsonObject()) {
        continue;
      }
      JsonObject entry = entries.get(i).getAsJsonObject();
      JsonElement id = entry.get(type.idKey());
      Integer match = live.claim(id);
      if (biospecimens) {
        settleSampleId(entry, rowId(id), match, rowSampleIds.get(match), i);
      }
      Fields fields = new Fields(entry);
      if (match != null) {
        writes.update(entry, match, type.toRecord().build(match, studyId, null, fields));
      } else {
        String legacyKey = legacyKey(id, idCounts, usedLegacyKeys);
        writes.insert(entry, type.toRecord().build(null, studyId, legacyKey, fields));
      }
      if (!fields.wrongType.isEmpty()) {
        logWarn(
            "Study %d: %s entry %d stored null for %s, which had the wrong type"
                .formatted(studyId, type.key(), i, fields.wrongType));
      }
    }
    writes.apply(dao, studyId, type);
  }

  /**
   * A new row keeps its client id as a legacy key only when no other row or entry shares it. An id
   * holding a NUL character or an unpaired surrogate, which Postgres text cannot store, keeps none.
   */
  private static String legacyKey(
      JsonElement id, Map<String, Long> idCounts, Set<String> usedLegacyKeys) {
    String text = idText(id);
    return rowId(id) == null
            && text != null
            && idCounts.get(text) == 1
            && !usedLegacyKeys.contains(text)
            && text.codePoints()
                .noneMatch(c -> c == 0 || Character.getType(c) == Character.SURROGATE)
            && text.codePointCount(0, text.length()) <= MAX_LEGACY_KEY_LENGTH
        ? text
        : null;
  }

  /** Decision 12 of study-asset-records-plan.md: a biospecimen's own id lives in sampleId. */
  private static void settleSampleId(
      JsonObject entry, Integer rowId, Integer match, String rowSampleId, int position) {
    JsonElement sample = entry.get(SAMPLE_ID);
    boolean absent = sample == null || sample.isJsonNull();
    if (!absent && !isBlankString(sample)) {
      if (sample.isJsonPrimitive() && sample.getAsJsonPrimitive().isNumber()) {
        entry.addProperty(SAMPLE_ID, sample.getAsString());
      }
      return;
    }
    String original = copyableId(entry.get(BIOSPECIMEN_ID));
    if (original != null) {
      entry.addProperty(SAMPLE_ID, original);
    } else if (rowId != null && match == null) {
      throw new BadRequestException(
          "Biospecimen at index %d: biospecimenId is server-assigned; send the sample id in sampleId"
              .formatted(position));
    } else if (absent && rowSampleId != null) {
      entry.addProperty(SAMPLE_ID, rowSampleId);
    }
  }

  /** A non-blank string, or a whole number as plain digits, that is not in row-id form. */
  private static String copyableId(JsonElement id) {
    if (id == null || !id.isJsonPrimitive() || rowId(id) != null) {
      return null;
    }
    JsonPrimitive value = id.getAsJsonPrimitive();
    if (value.isString()) {
      return nonBlank(value.getAsString());
    }
    if (value.isNumber()) {
      BigDecimal number = value.getAsBigDecimal();
      return number.stripTrailingZeros().scale() <= 0
          ? number.toBigIntegerExact().toString()
          : null;
    }
    return null;
  }

  /**
   * The row id an asset id names: a JSON number with a whole value from 1 to 2147483647, or a
   * string of plain digits with no leading zero in that range. Matches the SQL function {@code
   * asset_id_in_row_form}.
   */
  static Integer rowId(JsonElement id) {
    if (id == null || !id.isJsonPrimitive()) {
      return null;
    }
    JsonPrimitive value = id.getAsJsonPrimitive();
    if (value.isString()) {
      String text = value.getAsString();
      return ROW_ID.matcher(text).matches() && Long.parseLong(text) <= Integer.MAX_VALUE
          ? Integer.valueOf(text)
          : null;
    }
    if (value.isNumber()) {
      BigDecimal number = value.getAsBigDecimal();
      return number.signum() > 0
              && number.compareTo(MAX_ROW_ID) <= 0
              && number.stripTrailingZeros().scale() <= 0
          ? number.intValueExact()
          : null;
    }
    return null;
  }

  /** An id's text for matching and keeping as a legacy key: a non-blank string or a number. */
  private static String idText(JsonElement id) {
    if (id == null || !id.isJsonPrimitive()) {
      return null;
    }
    JsonPrimitive value = id.getAsJsonPrimitive();
    return value.isString() || value.isNumber() ? nonBlank(value.getAsString()) : null;
  }

  private static boolean isBlankString(JsonElement value) {
    return value.isJsonPrimitive()
        && value.getAsJsonPrimitive().isString()
        && value.getAsString().isBlank();
  }

  private static String nonBlank(String value) {
    return StringUtils.isBlank(value) ? null : value;
  }

  /** The study row lock keeps a matched row live, so any other count is a bug. */
  static void requireAllLive(int[] updateCounts, Integer studyId, String key) {
    if (Arrays.stream(updateCounts).anyMatch(count -> count != 1)) {
      throw new IllegalStateException(
          "Study %d: a matched %s row was not live".formatted(studyId, key));
    }
  }

  /** A type's live rows, each of which one entry per write can claim. */
  private static final class LiveRows {

    private final Set<Integer> ids = new HashSet<>();
    private final Map<String, Integer> byLegacyKey = new HashMap<>();
    private final Set<Integer> claimed = new HashSet<>();

    LiveRows(List<StudyAssetKey> rows) {
      for (StudyAssetKey row : rows) {
        ids.add(row.id());
        if (row.legacyKey() != null) {
          byLegacyKey.put(row.legacyKey(), row.id());
        }
      }
    }

    /** The unclaimed live row an id names, by row id or by legacy key, now claimed. */
    Integer claim(JsonElement id) {
      Integer rowId = rowId(id);
      // A legacy key is never in row-id form, so a row-form id can only name a row id.
      if (rowId != null) {
        return ids.contains(rowId) && claimed.add(rowId) ? rowId : null;
      }
      Integer match = byLegacyKey.get(idText(id));
      return match != null && claimed.add(match) ? match : null;
    }
  }

  /** The row writes one sync makes, and the entries that get each row's id. */
  private static final class Writes<R> {

    private final List<JsonObject> matched = new ArrayList<>();
    private final List<Integer> matchedIds = new ArrayList<>();
    private final List<R> updates = new ArrayList<>();
    private final List<JsonObject> added = new ArrayList<>();
    private final List<R> inserts = new ArrayList<>();

    void update(JsonObject entry, Integer id, R row) {
      matched.add(entry);
      matchedIds.add(id);
      updates.add(row);
    }

    void insert(JsonObject entry, R row) {
      added.add(entry);
      inserts.add(row);
    }

    void apply(StudyAssetRecordDAO dao, Integer studyId, AssetType<R> type) {
      if (!updates.isEmpty()) {
        requireAllLive(type.update().apply(dao, updates), studyId, type.key());
      }
      List<Integer> newIds = inserts.isEmpty() ? List.of() : type.insert().apply(dao, inserts);
      List<Integer> keepIds = new ArrayList<>(matchedIds);
      keepIds.addAll(newIds);
      type.retire().apply(dao, studyId, keepIds);
      for (int i = 0; i < matched.size(); i++) {
        matched.get(i).addProperty(type.idKey(), matchedIds.get(i));
      }
      for (int i = 0; i < added.size(); i++) {
        added.get(i).addProperty(type.idKey(), newIds.get(i));
      }
    }
  }

  /** Reads an entry's fields for their columns. A value of the wrong type reads as null. */
  private static final class Fields {

    private final JsonObject entry;
    private final String prefix;
    private final List<String> wrongType;

    Fields(JsonObject entry) {
      this(entry, "", new ArrayList<>());
    }

    private Fields(JsonObject entry, String prefix, List<String> wrongType) {
      this.entry = entry;
      this.prefix = prefix;
      this.wrongType = wrongType;
    }

    String text(String key) {
      JsonElement value = present(key);
      if (value == null) {
        return null;
      }
      return value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()
          ? NulCharacters.stripFrom(value.getAsString())
          : wrong(key);
    }

    @SuppressWarnings("java:S1168") // An absent list stores NULL, not an empty array.
    List<String> texts(String key) {
      JsonElement value = present(key);
      if (value == null) {
        return null;
      }
      if (!value.isJsonArray()
          || !value.getAsJsonArray().asList().stream()
              .allMatch(e -> e.isJsonPrimitive() && e.getAsJsonPrimitive().isString())) {
        return wrong(key);
      }
      return value.getAsJsonArray().asList().stream()
          .map(e -> NulCharacters.stripFrom(e.getAsString()))
          .toList();
    }

    String object(String key) {
      JsonElement value = present(key);
      if (value == null) {
        return null;
      }
      return value.isJsonObject() ? NulCharacters.stripFromJsonText(value.toString()) : wrong(key);
    }

    BigDecimal number(String key) {
      JsonElement value = present(key);
      if (value == null) {
        return null;
      }
      return value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber()
          ? value.getAsBigDecimal()
          : wrong(key);
    }

    /** The fields of a nested object; one of the wrong type reads as an empty object. */
    Fields nested(String key) {
      JsonElement value = present(key);
      if (value != null && !value.isJsonObject()) {
        wrong(key);
      }
      JsonObject object =
          value != null && value.isJsonObject() ? value.getAsJsonObject() : new JsonObject();
      return new Fields(object, prefix + key + ".", wrongType);
    }

    private JsonElement present(String key) {
      JsonElement value = entry.get(key);
      return value == null || value.isJsonNull() ? null : value;
    }

    private <T> T wrong(String key) {
      wrongType.add(prefix + key);
      return null;
    }
  }

  @FunctionalInterface
  private interface RecordBuilder<R> {
    R build(Integer id, Integer studyId, String legacyKey, Fields fields);
  }

  @FunctionalInterface
  private interface Retire {
    int apply(StudyAssetRecordDAO dao, Integer studyId, List<Integer> keepIds);
  }

  private record AssetType<R>(
      String key,
      String idKey,
      RecordBuilder<R> toRecord,
      BiFunction<StudyAssetRecordDAO, Integer, List<StudyAssetKey>> findLive,
      BiFunction<StudyAssetRecordDAO, Integer, List<String>> findUsedLegacyKeys,
      BiFunction<StudyAssetRecordDAO, List<R>, List<Integer>> insert,
      BiFunction<StudyAssetRecordDAO, List<R>, int[]> update,
      Retire retire) {}
}
