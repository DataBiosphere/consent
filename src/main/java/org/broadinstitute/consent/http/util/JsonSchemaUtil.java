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
      JsonObject properties = schema.getAsJsonObject("properties");

      // Handle asset fields
      if (properties.has("assets")) {
        JsonObject assets = properties.getAsJsonObject("assets");
        if (assets.has("properties")) {
          JsonObject assetsProps = assets.getAsJsonObject("properties");
          for (String assetKey : assetsProps.keySet()) {
            String assetLabel = assetKey.substring(0, 1).toUpperCase() + assetKey.substring(1);
            // Add the top-level asset key mapping ONCE, before subfields
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
                  if (def.has("properties")) {
                    JsonObject subProps = def.getAsJsonObject("properties");
                    for (String subKey : subProps.keySet()) {
                      map.put(assetKey + "." + subKey, new String[] {subKey, assetKey});
                    }
                  }
                }
              }
            }
          }
        }
      }

      // Handle all other fields
      for (String key : properties.keySet()) {
        if (key.equals("assets")) continue; // already handled
        JsonObject prop = properties.getAsJsonObject(key);
        String label = prop.has("label") ? prop.get("label").getAsString() : key;
        String assetType = "Study";
        if (key.equals("consentGroups")) assetType = "Dataset";
        map.put(key, new String[] {label, assetType});
      }
    } catch (Exception e) {
      // fallback use field names
    }
    return map;
  }

  /**
   * Enhanced error formatting for schema validation errors. Groups by asset type and uses
   * user-facing labels.
   */
  public static String formatGroupedValidationErrors(Set<ValidationMessage> errors) {
    Map<String, String[]> fieldMap = getFieldLabelAndAssetTypeMap();
    Map<String, List<String>> grouped = new LinkedHashMap<>();
    for (ValidationMessage error : errors) {
      String field = "";
      String msgText = error.getMessage();

      // Extract field for asset arrays
      java.util.regex.Matcher assetMatcher =
          java.util.regex.Pattern.compile(
                          "^\\$\\.assets\\.(\\w+)\\[(\\d+)]: required property '([^']+)' not found")
              .matcher(msgText);
      if (assetMatcher.find()) {
        String assetKey = assetMatcher.group(1);
        String idx = assetMatcher.group(2);
        String subKey = assetMatcher.group(3);
        field = assetKey + "[" + idx + "]." + subKey;
      } else if (msgText.contains("required property")) {
        int idx = msgText.indexOf("'");
        if (idx >= 0) {
          int end = msgText.indexOf("'", idx + 1);
          if (end > idx) field = msgText.substring(idx + 1, end);
        }
      } else if (msgText.startsWith("$.") || msgText.startsWith("$")) {
        int start = msgText.startsWith("$.") ? 2 : 1;
        int colon = msgText.indexOf(":");
        if (colon > start) {
          field = msgText.substring(start, colon);
          if (field.startsWith("assets.")) {
            field = field.substring("assets.".length());
          }
        }
      }

      String label;
      String assetType;

      // Format label for asset arrays
      java.util.regex.Matcher m =
          java.util.regex.Pattern.compile("^(\\w+)\\[(\\d+)](?:\\.(\\w+))?$").matcher(field);
      if (m.matches()) {
        String assetKey = m.group(1);
        int idx = Integer.parseInt(m.group(2)) + 1;
        String subKey = m.group(3);
        String[] assetLabelAndType = fieldMap.get(assetKey);
        String assetLabel;
        if ("funding".equals(assetKey)) {
          assetLabel = "Funding";
        } else if ("intellectualProperties".equals(assetKey)) {
          assetLabel = "Intellectual Property";
        } else {
          assetLabel = assetLabelAndType != null ? assetLabelAndType[0] : assetKey;
          // Default: remove trailing 's' for other asset types
          if (assetLabel.endsWith("s")) {
            assetLabel = assetLabel.substring(0, assetLabel.length() - 1);
          }
        }
        label = assetLabel + " " + idx;
        if (subKey != null) {
          String[] subLabelAndType = fieldMap.get(assetKey + "." + subKey);
          String subLabel = subLabelAndType != null ? subLabelAndType[0] : subKey;
          label += ": " + subLabel;
        }
        assetType = assetLabelAndType != null ? assetLabelAndType[1] : assetKey;
      } else {
        String[] labelAndType = fieldMap.get(field);
        if (labelAndType == null && field.contains(".")) {
          String assetKey = field.split("\\.")[0];
          labelAndType = fieldMap.get(assetKey);
        }
        if (labelAndType == null) {
          labelAndType = new String[] {field, "Other"};
        }
        label = labelAndType[0];
        assetType = labelAndType[1];
      }

      // Clean up the error message for user display
      String cleanedMsg = getCleanedMsg(msgText, field, label);

      grouped.computeIfAbsent(assetType, _ -> new java.util.ArrayList<>()).add(cleanedMsg);
    }

    StringBuilder sb = new StringBuilder("Please correct the following fields:\n");
    for (var entry : grouped.entrySet()) {
      sb.append(entry.getKey()).append(":\n");
      for (String msg : entry.getValue()) {
        sb.append("  - ").append(msg).append("\n");
      }
    }
    return sb.toString().trim();
  }

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
