package org.broadinstitute.consent.http.util;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.gson.Gson;
import com.networknt.schema.Error;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SchemaRegistryConfig;
import com.networknt.schema.dialect.Dialects;
import jakarta.ws.rs.BadRequestException;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import org.apache.commons.io.IOUtils;
import org.broadinstitute.consent.http.models.dataset_registration_v1.DatasetRegistrationSchemaV1;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

public class JsonSchemaUtil implements ConsentLogger {

  private final LoadingCache<String, String> cache;
  private final String datasetRegistrationSchemaV1 = "/dataset-registration-schema_v1.json";
  private final SchemaRegistry factory;

  public JsonSchemaUtil() {
    SchemaRegistryConfig config =
        SchemaRegistryConfig.builder().formatAssertionsEnabled(true).typeLoose(false).build();
    factory =
        SchemaRegistry.withDialect(Dialects.getDraft7())
            .builder()
            .schemaRegistryConfig(config)
            .build();
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
  private Schema getDatasetRegistrationSchema() throws ExecutionException {
    String schemaString = getDatasetRegistrationSchemaV1();

    return factory.getSchema(schemaString);
  }

  /**
   * Compares an instance of a dataset registration object to the dataset registration schema
   *
   * @param datasetRegistrationInstance The string instance of a dataset registration object
   * @return List of human-readable validation errors, or an empty list if valid.
   */
  public Set<Error> validateSchema_v1(String datasetRegistrationInstance) {
    try {
      Schema schema = getDatasetRegistrationSchema();
      JsonNode datasetRegistrationJson = new ObjectMapper().readTree(datasetRegistrationInstance);

      return new HashSet<>(schema.validate(datasetRegistrationJson));
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
      Set<Error> errors = this.validateSchema_v1(datasetRegistrationInstance);
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
}
