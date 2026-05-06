package org.broadinstitute.consent.http.mcp;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.spec.McpSchema;
import org.broadinstitute.consent.http.util.gson.GsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for building {@link McpSchema.CallToolResult} values from domain objects.
 *
 * <p>All serialisation uses {@link GsonUtil#buildGson()} to stay consistent with the REST layer's
 * JSON output format (date handling, enum names, etc.). The resulting JSON string is then parsed
 * back into a generic Java structure (Map / List) and placed in {@code structuredContent} so that
 * MCP clients receive a native JSON value rather than a double-encoded string.
 */
public final class McpToolResults {

  private static final Logger LOGGER = LoggerFactory.getLogger(McpToolResults.class);
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final TypeReference<Object> OBJECT_TYPE = new TypeReference<>() {};

  private McpToolResults() {}

  /**
   * Wrap any serialisable object as a successful tool result with structured (native JSON) content.
   *
   * <p>The object is first serialised to JSON via {@link GsonUtil#buildGson()} (preserving the REST
   * layer's format), then parsed back to a generic Java structure that is placed in {@code
   * structuredContent}. This avoids the double-encoded-string problem that arises when JSON is
   * embedded as plain text.
   *
   * @param value any object serialisable by GsonUtil
   * @return a non-error CallToolResult with structured content
   */
  public static McpSchema.CallToolResult of(Object value) {
    String json = GsonUtil.buildGson().toJson(value);
    try {
      Object parsed = OBJECT_MAPPER.readValue(json, OBJECT_TYPE);
      return McpSchema.CallToolResult.builder().structuredContent(parsed).isError(false).build();
    } catch (Exception e) {
      LOGGER.warn("Failed to parse tool result as structured content; falling back to text", e);
      return McpSchema.CallToolResult.builder().addTextContent(json).isError(false).build();
    }
  }

  /**
   * Wrap a plain text message as a successful tool result.
   *
   * @param text the text to return
   * @return a non-error CallToolResult with a single text content item
   */
  public static McpSchema.CallToolResult ofText(String text) {
    return McpSchema.CallToolResult.builder().addTextContent(text).isError(false).build();
  }

  /**
   * Build an error result. The MCP client will receive isError=true and the message as text.
   *
   * @param message a human-readable error description
   * @return an error CallToolResult
   */
  public static McpSchema.CallToolResult error(String message) {
    return McpSchema.CallToolResult.builder().addTextContent(message).isError(true).build();
  }
}
