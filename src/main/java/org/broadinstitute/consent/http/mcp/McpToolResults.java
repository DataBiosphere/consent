package org.broadinstitute.consent.http.mcp;

import io.modelcontextprotocol.spec.McpSchema;
import org.broadinstitute.consent.http.util.gson.GsonUtil;

/**
 * Utility class for building {@link McpSchema.CallToolResult} values from domain objects.
 *
 * <p>All serialisation uses {@link GsonUtil#buildGson()} to stay consistent with the REST layer's
 * JSON output format (date handling, enum names, etc.).
 */
public final class McpToolResults {

  private McpToolResults() {}

  /**
   * Wrap any serialisable object as a successful tool result containing a single JSON text node.
   *
   * @param value any object serialisable by GsonUtil
   * @return a non-error CallToolResult whose single content item is the JSON string
   */
  public static McpSchema.CallToolResult of(Object value) {
    String json = GsonUtil.buildGson().toJson(value);
    return McpSchema.CallToolResult.builder().addTextContent(json).isError(false).build();
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
