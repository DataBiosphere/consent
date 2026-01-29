package org.broadinstitute.consent.http.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SchemaValidatorsConfig;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import jakarta.ws.rs.BadRequestException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import org.apache.commons.io.IOUtils;
import org.broadinstitute.consent.http.models.dataset_registration_v1.DatasetRegistrationSchemaV1;
import org.jspecify.annotations.NonNull;

public class JsonSchemaUtil implements ConsentLogger {

  private final LoadingCache<String, String> cache;
  private final JsonSchemaFactory factory;
  private static final String CONSENT_GROUPS = "consentGroups";
  private static final String FUNDING = "funding";
  private static final String INTELLECTUAL_PROPERTIES = "intellectualProperties";
  private static final String PROPERTIES = "properties";
  private static final String ASSETS = "assets";
  private static final String LABEL = "label";

  public JsonSchemaUtil() {
    factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V201909);
    CacheLoader<String, String> loader =
        new CacheLoader<>() {
          @Override
          public String load(String key) throws Exception {
            return IOUtils.resourceToString(key, Charset.defaultCharset());
          }
        };
    this.cache = CacheBuilder.newBuilder().build(loader);
  }

  public String getDatasetRegistrationSchemaV1() {
    try {
      String datasetRegistrationSchemaV1 = "/dataset-registration-schema_v1.json";
      return cache.get(datasetRegistrationSchemaV1);
    } catch (ExecutionException ee) {
      logException("Unable to load the data submitter schema: %s".formatted(ee.getMessage()), ee);
      return null;
    }
  }

  /**
   * Loads a Schema populated from the current dataset registration schema
   *
   * @return Schema The Schema
   * @throws ExecutionException Error reading from cache
   */
  private JsonSchema getDatasetRegistrationSchema() throws ExecutionException {
    String schemaString = getDatasetRegistrationSchemaV1();
    SchemaValidatorsConfig config = new SchemaValidatorsConfig();
    config.setHandleNullableField(false);
    config.setTypeLoose(false);
    config.setFormatAssertionsEnabled(true);
    return factory.getSchema(schemaString, config);
  }

  /**
   * Compares an instance of a dataset registration object to the dataset registration schema
   *
   * @param datasetRegistrationInstance The string instance of a dataset registration object
   * @return Set of human-readable validation errors, or an empty list if valid.
   */
  public Set<ValidationMessage> validateSchema_v1(String datasetRegistrationInstance) {
    try {
      JsonSchema schema = getDatasetRegistrationSchema();
      JsonNode datasetRegistrationJson = new ObjectMapper().readTree(datasetRegistrationInstance);

      return schema.validate(datasetRegistrationJson);
    } catch (ExecutionException ee) {
      logException("Unable to load the data submitter schema: %s".formatted(ee.getMessage()), ee);
      return Set.of();
    } catch (Exception e) {
      throw new BadRequestException("Invalid schema");
    }
  }

  public DatasetRegistrationSchemaV1 deserializeDatasetRegistration(
      String datasetRegistrationInstance) {
    try {
      Set<ValidationMessage> errors = this.validateSchema_v1(datasetRegistrationInstance);
      if (!errors.isEmpty()) {
        return null;
      }

      Gson gson = new Gson();
      return gson.fromJson(datasetRegistrationInstance, DatasetRegistrationSchemaV1.class);
    } catch (Exception ee) {
      logException("Unable to load the data submitter schema: %s".formatted(ee.getMessage()), ee);
      return null;
    }
  }

  /**
   * Utility to load field label and asset type mappings from the schema. Returns a map: fieldName
   * -> [label, assetType]
   */
  public static Map<String, String[]> getFieldLabelAndAssetTypeMap() {
    Map<String, String[]> map = new HashMap<>();
    try (InputStream is =
            JsonSchemaUtil.class.getResourceAsStream("/dataset-registration-schema_v1.json");
        InputStreamReader reader =
            new InputStreamReader(Objects.requireNonNull(is), StandardCharsets.UTF_8)) {
      JsonObject schema = JsonParser.parseReader(reader).getAsJsonObject();
      JsonObject properties = schema.getAsJsonObject(PROPERTIES);

      // Handle asset fields
      if (properties.has(ASSETS)) {
        JsonObject assets = properties.getAsJsonObject(ASSETS);
        if (assets.has(PROPERTIES)) {
          JsonObject assetsProps = assets.getAsJsonObject(PROPERTIES);
          for (String assetKey : assetsProps.keySet()) {
            String assetLabel = getAssetLabel(assetKey);
            map.put(assetKey, new String[] {assetLabel, assetLabel});
            JsonObject assetDef = assetsProps.getAsJsonObject(assetKey);
            if (assetDef != null && assetDef.has("items")) {
              JsonObject itemDef = assetDef.getAsJsonObject("items");
              if (itemDef.has("$ref")) {
                String ref = itemDef.get("$ref").getAsString();
                String defName = ref.substring(ref.lastIndexOf("/") + 1);
                JsonObject defs = schema.getAsJsonObject("$defs");
                if (defs != null && defs.has(defName)) {
                  JsonObject def = defs.getAsJsonObject(defName);
                  if (def.has(PROPERTIES)) {
                    JsonObject subProps = def.getAsJsonObject(PROPERTIES);
                    for (String subKey : subProps.keySet()) {
                      JsonObject subProp = subProps.getAsJsonObject(subKey);
                      String subLabel =
                          subProp != null && subProp.has(LABEL)
                              ? subProp.get(LABEL).getAsString()
                              : subKey;
                      map.put(assetKey + "." + subKey, new String[] {subLabel, assetKey});
                    }
                  }
                }
              }
            }
          }
        }
      }

      // Handle all other fields
      for (String index : properties.keySet()) {
        if (index.equals(ASSETS)) continue;
        JsonObject prop = properties.getAsJsonObject(index);
        String label = prop.has(LABEL) ? prop.get(LABEL).getAsString() : index;
        String assetType = index.equals(CONSENT_GROUPS) ? "Datasets" : "Study";
        map.put(index, new String[] {label, assetType});
      }
    } catch (Exception e) {
      // fallback use field names
    }
    return map;
  }

  /**
   * Utility to enhance error formatting for schema validation errors. Groups by asset type and uses
   * user-facing labels.
   */
  public static String formatGroupedValidationErrors(Set<ValidationMessage> errors) {
    Map<String, String[]> fieldMap = getFieldLabelAndAssetTypeMap();
    Map<String, List<String>> grouped = groupValidationMessages(errors, fieldMap);

    StringBuilder sb = new StringBuilder("Please correct the following fields:\n");
    for (var entry : grouped.entrySet()) {
      sb.append(entry.getKey()).append(":\n");
      for (String msg : entry.getValue()) {
        sb.append("  - ").append(msg).append("\n");
      }
    }
    return sb.toString().trim();
  }

  /** Helper to group validation messages by asset type */
  private static Map<String, List<String>> groupValidationMessages(
      Set<ValidationMessage> errors, Map<String, String[]> fieldMap) {
    Map<String, List<String>> grouped = new LinkedHashMap<>();
    for (ValidationMessage error : errors) {
      String msgText = error.getMessage();
      String field = extractFieldFromMessage(msgText);

      String[] labelAndType = getLabelAndAssetType(field, fieldMap);
      String label = labelAndType[0];
      String assetType = capitalize(labelAndType[1]);

      String cleanedMsg = getCleanedMsg(msgText, field, label);
      grouped.computeIfAbsent(assetType, _ -> new java.util.ArrayList<>()).add(cleanedMsg);
    }
    return grouped;
  }

  /** Helper to get the label and asset type for a given field */
  private static String[] getLabelAndAssetType(String field, Map<String, String[]> fieldMap) {
    java.util.regex.Matcher m =
        java.util.regex.Pattern.compile("^(\\w+)\\[(\\d+)](?:\\.(\\w+))?$").matcher(field);
    if (m.matches()) {
      String assetKey = m.group(1);
      String[] assetLabelAndType = fieldMap.get(assetKey);
      String assetType =
          (assetLabelAndType != null && assetLabelAndType[1] != null)
              ? assetLabelAndType[1]
              : assetKey;
      String subKey = m.group(3);
      String label = subKey != null ? subKey : assetKey;
      return new String[] {label, assetType};
    } else {
      String[] labelAndType = fieldMap.get(field);
      if (labelAndType == null && field.contains(".")) {
        String assetKey = field.split("\\.")[0];
        labelAndType = fieldMap.get(assetKey);
      }
      if (labelAndType == null) {
        labelAndType = new String[] {field, "Other"};
      }
      return labelAndType;
    }
  }

  /** Helper to get the user-facing asset label */
  private static String getAssetLabel(String assetLabel) {
    if (CONSENT_GROUPS.equals(assetLabel)) return "Datasets";
    if (FUNDING.equals(assetLabel)) return "Funding";
    if (INTELLECTUAL_PROPERTIES.equals(assetLabel)) return "Intellectual Property";
    // Capitalize and pluralize
    String label = assetLabel.substring(0, 1).toUpperCase() + assetLabel.substring(1);
    // If not already plural, add 's'
    if (!label.endsWith("s")) label += "s";
    return label;
  }

  /** Helper to extract the field from a validation message */
  private static String extractFieldFromMessage(String msgText) {
    var assetMatcher =
        java.util.regex.Pattern.compile(
                "^\\$\\.assets\\.(\\w+)\\[(\\d+)]: required property '([^']+)' not found")
            .matcher(msgText);
    if (assetMatcher.find()) {
      return assetMatcher.group(1) + "[" + assetMatcher.group(2) + "]." + assetMatcher.group(3);
    }
    if (msgText.contains("required property")) {
      int idx = msgText.indexOf("'");
      int end = msgText.indexOf("'", idx + 1);
      if (idx >= 0 && end > idx) {
        return msgText.substring(idx + 1, end);
      }
    }
    if (msgText.startsWith("$.") || msgText.startsWith("$")) {
      int start = msgText.startsWith("$.") ? 2 : 1;
      int colon = msgText.indexOf(":");
      if (colon > start) {
        String field = msgText.substring(start, colon);
        if (field.startsWith("assets.")) {
          field = field.substring("assets.".length());
        }
        return field;
      }
    }
    return "";
  }

  /** Helper that capitalizes the first letter of a string */
  private static String capitalize(String s) {
    if (s == null || s.isEmpty()) return s;
    return s.substring(0, 1).toUpperCase() + s.substring(1);
  }

  /**
   * Helper that cleans up a validation message by removing raw field references and adding labels
   */
  private static @NonNull String getCleanedMsg(String msgText, String field, String label) {
    String cleanedMsg = msgText.replaceFirst("^\\$\\.?", "");

    // Remove the raw field (e.g., assets.models[0].name) from the start of the message
    cleanedMsg = cleanedMsg.replaceFirst("^assets\\.[^:]+: ?", "");
    cleanedMsg = cleanedMsg.replaceFirst("^" + field + ": ?", "");

    // If the message does not already start with the label, prepend it for known fields
    if (label != null && !label.isEmpty() && !cleanedMsg.startsWith(label)) {
      cleanedMsg = label + ": " + cleanedMsg;
    }

    // Replace all occurrences of the raw field with the label
    assert label != null;
    cleanedMsg = cleanedMsg.replace(field, label);

    // Replace 'required property' field name with label
    if (msgText.contains("required property")) {
      cleanedMsg = cleanedMsg.replace("'" + field + "'", "'" + label + "'");
    }
    return cleanedMsg;
  }
}
