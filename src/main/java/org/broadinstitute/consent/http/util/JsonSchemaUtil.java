package org.broadinstitute.consent.http.util;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.gson.Gson;
import org.apache.commons.io.IOUtils;
import org.broadinstitute.consent.http.models.dataset_registration_v1.DatasetRegistrationSchemaV1;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.util.concurrent.ExecutionException;

public class JsonSchemaUtil {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());
  private final LoadingCache<String, String> cache;
  private final String datasetRegistrationSchemaV1 = "/dataset-registration-schema_v1.json";

  public JsonSchemaUtil() {
    CacheLoader<String, String> loader = new CacheLoader<>() {
      @Override
      public String load(String key) throws Exception {
        return IOUtils.resourceToString(key, Charset.defaultCharset());
      }
    };
    this.cache = CacheBuilder.newBuilder().build(loader);
  }

  /**
   * Loads a Schema populated from the current dataset registration schema
   * @return Schema The Schema
   * @throws ExecutionException Error reading from cache
   */
  private Schema getDatasetRegistrationSchema() throws ExecutionException {
      String schemaString = cache.get("/dataset-registration-schema_v1.json");
      JSONObject jsonSchema = new JSONObject(schemaString);
      return SchemaLoader
        .builder()
        .schemaJson(jsonSchema)
        .addFormatValidator(new JsonSchemaDateValidator())
        .build()
        .load()
        .build();
  }

  /**
   * Compares an instance of a dataset registration object to the dataset registration schema
   *
   * @param datasetRegistrationInstance The string instance of a dataset registration object
   * @return True if the instance validates, false otherwise
   */
  public boolean isValidSchema_v1(String datasetRegistrationInstance) {
    try {
      JSONObject jsonSubject = new JSONObject(datasetRegistrationInstance);
      Schema schema = getDatasetRegistrationSchema();
      schema.validate(jsonSubject);
      return true;
    } catch (ExecutionException ee) {
      logger.error("Unable to load the data submitter schema: " + ee.getMessage());
      return false;
    } catch (ValidationException ve) {
      if (logger.isDebugEnabled()) {
        logger.debug("Provided instance does not validate: " + ve.getMessage());
      }
      if (logger.isTraceEnabled()) {
        for (String m : ve.getAllMessages()) {
          logger.trace("Validation error: " + m);
        }
      }
      return false;
    }
  }

  public DatasetRegistrationSchemaV1 deserializeDatasetRegistration(String datasetRegistrationInstance) {
    try {
      JSONObject jsonSubject = new JSONObject(datasetRegistrationInstance);
      Schema schema = getDatasetRegistrationSchema();
      schema.validate(jsonSubject);
      Gson gson = new Gson();
      return gson.fromJson(datasetRegistrationInstance, DatasetRegistrationSchemaV1.class);
    } catch (ExecutionException ee) {
      logger.error("Unable to load the data submitter schema: " + ee.getMessage());
      return null;
    }
  }
}
