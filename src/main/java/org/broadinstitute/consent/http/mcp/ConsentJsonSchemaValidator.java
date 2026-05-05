package org.broadinstitute.consent.http.mcp;

import io.modelcontextprotocol.json.schema.JsonSchemaValidator;
import java.util.Map;

/**
 * No-op {@link JsonSchemaValidator} that accepts all tool-call inputs without validation.
 *
 * <p>The SDK requires a {@link JsonSchemaValidator} via ServiceLoader. The default implementation
 * ({@code DefaultJsonSchemaValidator} in {@code mcp-json-jackson2}) uses {@code
 * com.networknt:json-schema-validator 1.5.7}, which conflicts with the project's 3.0.2 pin. We
 * register this pass-through instead.
 *
 * <p>MCP tool schemas in this service are simple Maps used for documentation only; runtime
 * validation of tool arguments is not required.
 */
public class ConsentJsonSchemaValidator implements JsonSchemaValidator {

  @Override
  public ValidationResponse validate(Map<String, Object> schema, Object structuredContent) {
    // Pass through — tool-argument validation is not required for DUOS MCP tools.
    return ValidationResponse.asValid(String.valueOf(structuredContent));
  }
}
