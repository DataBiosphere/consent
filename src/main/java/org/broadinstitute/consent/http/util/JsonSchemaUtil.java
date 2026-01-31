package org.broadinstitute.consent.http.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.gson.Gson;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SchemaValidatorsConfig;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import jakarta.ws.rs.BadRequestException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import org.apache.commons.io.IOUtils;
import org.broadinstitute.consent.http.models.dataset_registration_v1.DatasetRegistrationSchemaV1;

public class JsonSchemaUtil implements ConsentLogger {

  private final LoadingCache<String, String> cache;
  private final JsonSchemaFactory factory;
  Map<String, String> fieldLabels = new HashMap<>();
  Map<String, String> fieldDescriptions = new HashMap<>();
  private static final Map<String, String> FIELD_NAME_OVERRIDES =
      Map.of("consentGroups", "Datasets");

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

  /**
   * Loads the dataset registration schema v1 as a string
   *
   * @return The schema as a string
   */
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
  JsonSchema getDatasetRegistrationSchema() throws ExecutionException {
    String schemaString = getDatasetRegistrationSchemaV1();

    // Extract labels and descriptions for error messages
    extractLabelsAndDescriptions(schemaString);

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

  /**
   * Compares an instance of a dataset registration object to the dataset registration schema
   *
   * @param json The string instance of a dataset registration object
   * @return Set of human-readable validation error messages, or an empty list if valid.
   */
  public Set<String> validateSchemaMessages_v1(String json) {
    return validateSchema_v1(json).stream()
        .map(this::formatMessage)
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  /**
   * Deserializes a dataset registration instance into a DatasetRegistrationSchemaV1 object
   *
   * @param datasetRegistrationInstance The string instance of a dataset registration object
   * @return The deserialized DatasetRegistrationSchemaV1 object, or null if invalid
   */
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
   * Extracts field labels and descriptions from the schema for use in error messages and tooltips
   *
   * @param schemaString The schema as a string
   */
  private void extractLabelsAndDescriptions(String schemaString) {
    try {
      ObjectMapper mapper = new ObjectMapper();
      JsonNode schemaNode = mapper.readTree(schemaString);
      JsonNode properties = schemaNode.path("properties");

      properties
          .fields()
          .forEachRemaining(
              entry -> {
                String field = entry.getKey();

                JsonNode labelNode = entry.getValue().path("label");
                if (labelNode.isTextual()) {
                  fieldLabels.put(field, labelNode.asText());
                }

                JsonNode descNode = entry.getValue().path("description");
                if (descNode.isTextual()) {
                  fieldDescriptions.put(field, descNode.asText());
                }
              });
    } catch (Exception e) {
      fieldLabels = Map.of();
      fieldDescriptions = Map.of();
    }
  }

  /**
   * Formats a validation message into a human-readable string
   *
   * @param vm The validation message
   * @return The formatted message
   */
  String formatMessage(ValidationMessage vm) {
    String field = fieldFromMessage(vm);
    String name = displayName(field);

    return switch (vm.getType()) {
      case "required" -> name + " is required";
      case "minLength" -> name + " must not be empty";
      case "minItems" -> name + " must have at least one item";
      case "enum" -> name + " must be one of the allowed options";
      case "type" -> name + " has an invalid value";
      default -> vm.getMessage(); // fallback to default message
    };
  }

  /**
   * Derives the field name from the schema location in a validation message
   *
   * @param vm The validation message
   * @return The field name, or null if not found
   */
  private String fieldFromMessage(ValidationMessage vm) {
    // REQUIRED → field name comes from arguments
    if ("required".equals(vm.getType())) {
      Object[] args = vm.getArguments();
      return (args != null && args.length > 0) ? args[0].toString() : null;
    }

    // ALL OTHERS → derive from schema location
    String loc = vm.getSchemaLocation().toString();
    if (loc == null) return null;

    String marker = "#/properties/";
    int idx = loc.indexOf(marker);
    if (idx < 0) return null;

    String rest = loc.substring(idx + marker.length());
    int slash = rest.indexOf('/');

    return slash > 0 ? rest.substring(0, slash) : rest;
  }

  /**
   * Gets the display name for a field, using label or description if available
   *
   * @param field The field name
   * @return The display name
   */
  String displayName(String field) {
    if (field == null) return "Field";

    if (FIELD_NAME_OVERRIDES.containsKey(field)) {
      return FIELD_NAME_OVERRIDES.get(field);
    }

    if (fieldLabels.containsKey(field)) {
      return fieldLabels.get(field);
    }

    if (fieldDescriptions.containsKey(field)) {
      return fieldDescriptions.get(field);
    }

    return field;
  }
}
