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
import java.util.Set;
import java.util.concurrent.ExecutionException;
import org.apache.commons.io.IOUtils;
import org.broadinstitute.consent.http.models.dataset_registration_v1.DatasetRegistrationSchemaV1;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JsonSchemaUtil {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());
  private final LoadingCache<String, String> cache;
  private final String datasetRegistrationSchemaV1 = "/dataset-registration-schema_v1.json";
  private JsonSchemaFactory factory;


  public JsonSchemaUtil() {
    factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V201909);
    CacheLoader<String, String> loader = new CacheLoader<>() {
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
      logger.error("Unable to load the data submitter schema: " + ee.getMessage());
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
   * @return List of human-readable validation errors, or an empty list if valid.
   */
  public Set<ValidationMessage> validateSchema_v1(String datasetRegistrationInstance) {
    try {
      JsonSchema schema = getDatasetRegistrationSchema();
      JsonNode datasetRegistrationJson = new ObjectMapper().readTree(datasetRegistrationInstance);

      return schema.validate(datasetRegistrationJson);
    } catch (ExecutionException ee) {
      logger.error("Unable to load the data submitter schema: " + ee.getMessage());
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
      logger.error("Unable to load the data submitter schema: " + ee.getMessage());
      return null;
    }
  }
}
