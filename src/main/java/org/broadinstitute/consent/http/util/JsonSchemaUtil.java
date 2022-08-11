package org.broadinstitute.consent.http.util;

import org.apache.commons.io.IOUtils;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.Charset;

public class JsonSchemaUtil {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  /**
   * Compares an instance of a data submitter object to the data submitter schema
   *
   * @param dataSubmitterInstance The string instance of a data submitter object
   * @return True if the instance validates, false otherwise
   */
  public boolean isValidDataSubmitterObject_v1(String dataSubmitterInstance) {
    try {
      String schemaString =
          IOUtils.resourceToString("/data-submitter-schema_v1.json", Charset.defaultCharset());
      JSONObject jsonSchema = new JSONObject(schemaString);
      JSONObject jsonSubject = new JSONObject(dataSubmitterInstance);
      Schema schema = SchemaLoader.load(jsonSchema);
      schema.validate(jsonSubject);
      return true;
    } catch (IOException ioe) {
      logger.error("Unable to load the data submitter schema: " + ioe.getMessage());
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
}