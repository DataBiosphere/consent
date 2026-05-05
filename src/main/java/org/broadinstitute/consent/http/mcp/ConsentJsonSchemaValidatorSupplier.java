package org.broadinstitute.consent.http.mcp;

import io.modelcontextprotocol.json.schema.JsonSchemaValidator;
import io.modelcontextprotocol.json.schema.JsonSchemaValidatorSupplier;

/**
 * ServiceLoader registration for {@link JsonSchemaValidator}.
 *
 * <p>Registered in {@code
 * META-INF/services/io.modelcontextprotocol.json.schema.JsonSchemaValidatorSupplier}.
 */
public class ConsentJsonSchemaValidatorSupplier implements JsonSchemaValidatorSupplier {

  @Override
  public JsonSchemaValidator get() {
    return new ConsentJsonSchemaValidator();
  }
}
