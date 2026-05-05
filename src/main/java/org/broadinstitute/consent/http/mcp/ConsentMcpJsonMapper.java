package org.broadinstitute.consent.http.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.TypeRef;
import java.io.IOException;

/**
 * Network-free {@link McpJsonMapper} implementation backed by Jackson's {@link ObjectMapper}.
 *
 * <p>The SDK ships {@code mcp-json-jackson2} as the default Jackson adapter, but that module
 * registers a {@code JacksonJsonSchemaValidatorSupplier} via ServiceLoader which depends on {@code
 * com.networknt:json-schema-validator 1.5.7}. This project pins 3.0.2, whose API is incompatible
 * (the {@code SpecVersion$VersionFlag} inner class was removed), causing a {@link
 * NoClassDefFoundError} at startup.
 *
 * <p>This implementation provides only the JSON serialisation/deserialisation that the MCP server
 * transport and tool dispatching actually need — no schema validation. {@code mcp-json-jackson2} is
 * excluded from the Maven dependency tree entirely.
 */
public class ConsentMcpJsonMapper implements McpJsonMapper {

  private final ObjectMapper mapper;

  public ConsentMcpJsonMapper(ObjectMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  public <T> T readValue(String content, Class<T> type) throws IOException {
    return mapper.readValue(content, type);
  }

  @Override
  public <T> T readValue(byte[] content, Class<T> type) throws IOException {
    return mapper.readValue(content, type);
  }

  @Override
  public <T> T readValue(String content, TypeRef<T> type) throws IOException {
    return mapper.readValue(content, mapper.getTypeFactory().constructType(type.getType()));
  }

  @Override
  public <T> T readValue(byte[] content, TypeRef<T> type) throws IOException {
    return mapper.readValue(content, mapper.getTypeFactory().constructType(type.getType()));
  }

  @Override
  public <T> T convertValue(Object fromValue, Class<T> type) {
    return mapper.convertValue(fromValue, type);
  }

  @Override
  public <T> T convertValue(Object fromValue, TypeRef<T> type) {
    return mapper.convertValue(fromValue, mapper.getTypeFactory().constructType(type.getType()));
  }

  @Override
  public String writeValueAsString(Object value) throws IOException {
    return mapper.writeValueAsString(value);
  }

  @Override
  public byte[] writeValueAsBytes(Object value) throws IOException {
    return mapper.writeValueAsBytes(value);
  }
}
