package org.broadinstitute.consent.http.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.gson.Gson;
import com.google.inject.Inject;
import com.networknt.schema.Error;
import com.networknt.schema.InputFormat;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import jakarta.ws.rs.BadRequestException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import org.apache.commons.io.IOUtils;
import org.broadinstitute.consent.http.models.dataset_registration_v1.DatasetRegistrationSchemaV1;
import org.jspecify.annotations.NonNull;

public class JsonSchemaUtil implements ConsentLogger {

  private final LoadingCache<String, String> cache;
  private final SchemaRegistry schemaRegistry;
  Map<String, String> fieldLabels = new HashMap<>();
  Map<String, String> fieldDescriptions = new HashMap<>();
  private static final Map<String, String> FIELD_NAME_OVERRIDES =
      Map.of("consentGroups", "Datasets");
  private static final String SCHEMA_LOAD_ERROR = "Unable to load the data submitter schema: %s";

  @Inject
  public JsonSchemaUtil() {
    // Create SchemaRegistry with JSON Schema Draft 2019-09
    schemaRegistry =
        new SchemaRegistry.Builder()
            .defaultDialectId(SpecificationVersion.DRAFT_2019_09.getDialectId())
            .build();
    CacheLoader<String, String> loader =
        new CacheLoader<>() {
          @Override
          public @NonNull String load(@NonNull String key) throws Exception {
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
      logException(SCHEMA_LOAD_ERROR.formatted(ee.getMessage()), ee);
      return null;
    }
  }

  /**
   * Loads a Schema populated from the current dataset registration schema
   *
   * @return Schema The Schema
   * @throws ExecutionException Error reading from cache
   */
  Schema getDatasetRegistrationSchema() throws ExecutionException {
    String schemaString = getDatasetRegistrationSchemaV1();

    // Extract labels and descriptions for error messages
    extractLabelsAndDescriptions(schemaString);

    return schemaRegistry.getSchema(schemaString);
  }

  /**
   * Compares an instance of a dataset registration object to the dataset registration schema
   *
   * @param datasetRegistrationInstance The string instance of a dataset registration object
   * @return Set of human-readable validation errors, or an empty list if valid.
   */
  public Set<Error> validateSchemaV1(String datasetRegistrationInstance) {
    try {
      Schema schema = getDatasetRegistrationSchema();
      // Validate the input JSON string against the schema
      List<Error> errors = schema.validate(datasetRegistrationInstance, InputFormat.JSON);

      return new HashSet<>(errors);
    } catch (ExecutionException ee) {
      logException(SCHEMA_LOAD_ERROR.formatted(ee.getMessage()), ee);
      return Set.of();
    } catch (Exception _) {
      throw new BadRequestException("Invalid schema");
    }
  }

  /**
   * Compares an instance of a dataset registration object to the dataset registration schema
   *
   * @param json The string instance of a dataset registration object
   * @return Set of human-readable validation error messages, or an empty list if valid.
   */
  public Set<String> validateSchemaMessagesV1(String json) {
    return validateSchemaV1(json).stream()
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
      Set<Error> errors = this.validateSchemaV1(datasetRegistrationInstance);
      if (!errors.isEmpty()) {
        return null;
      }

      Gson gson = new Gson();
      return gson.fromJson(datasetRegistrationInstance, DatasetRegistrationSchemaV1.class);
    } catch (Exception ee) {
      logException(SCHEMA_LOAD_ERROR.formatted(ee.getMessage()), ee);
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
          .properties()
          .forEach(
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
    } catch (Exception _) {
      fieldLabels = Map.of();
      fieldDescriptions = Map.of();
    }
  }

  /**
   * Formats a validation message into a human-readable string
   *
   * @param error The validation error
   * @return The formatted message
   */
  String formatMessage(Error error) {
    String field = fieldFromMessage(error);
    String name = displayName(field);

    return switch (error.getKeyword()) {
      case "required" -> name + " is required";
      case "minLength" -> name + " must not be empty";
      case "minItems" -> name + " must have at least one item";
      case "enum" -> name + " must be one of the allowed options";
      case "type" -> name + " has an invalid value";
      default -> error.getMessage(); // fallback to default message
    };
  }

  /**
   * Derives the field name from the schema location in a validation message
   *
   * @param error The validation error
   * @return The field name, or null if not found
   */
  private String fieldFromMessage(Error error) {
    // For REQUIRED errors, try to get the property name from details
    if ("required".equals(error.getKeyword())) {
      String property = error.getProperty();
      if (property != null) {
        return property;
      }
      // Fallback to arguments
      Object[] args = error.getArguments();
      if (args != null && args.length > 0) {
        return args[0].toString();
      }
    }

    // For all others, derive from instance location first (arguments may be numeric schema
    // constraints, e.g. minItems argument is the minimum count, not the field name)
    String instanceLoc = error.getInstanceLocation().toString();
    if (instanceLoc != null && !instanceLoc.isEmpty()) {
      int lastSlash = instanceLoc.lastIndexOf('/');
      if (lastSlash >= 0 && lastSlash < instanceLoc.length() - 1) {
        return instanceLoc.substring(lastSlash + 1);
      }
    }

    // Fallback to arguments only when instance location yields nothing useful
    Object[] args = error.getArguments();
    if (args != null && args.length > 0) {
      return args[0].toString();
    }

    return null;
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
