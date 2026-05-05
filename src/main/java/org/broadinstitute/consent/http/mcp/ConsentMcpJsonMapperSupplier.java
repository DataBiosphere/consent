package org.broadinstitute.consent.http.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.McpJsonMapperSupplier;

/**
 * ServiceLoader registration for {@link McpJsonMapper}.
 *
 * <p>The SDK discovers a default {@link McpJsonMapper} via {@code
 * ServiceLoader<McpJsonMapperSupplier>} when {@link McpJsonMapper#getDefault()} is called
 * internally. Because we excluded {@code mcp-json-jackson2} (to avoid its transitive dependency on
 * {@code com.networknt:json-schema-validator 1.5.7} which conflicts with the project's 3.0.2 pin),
 * we register this supplier instead so the SDK can find a mapper at runtime.
 *
 * <p>Registered in {@code META-INF/services/io.modelcontextprotocol.json.McpJsonMapperSupplier}.
 */
public class ConsentMcpJsonMapperSupplier implements McpJsonMapperSupplier {

  @Override
  public McpJsonMapper get() {
    return new ConsentMcpJsonMapper(new ObjectMapper());
  }
}
