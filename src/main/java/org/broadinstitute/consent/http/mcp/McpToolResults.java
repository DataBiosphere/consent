package org.broadinstitute.consent.http.mcp;

import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import java.util.List;
import org.broadinstitute.consent.http.util.gson.GsonUtil;

/**
 * Utility class for converting domain objects into {@link McpServerFeatures} CallToolResult values.
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
    return new McpSchema.CallToolResult(
        List.of(new McpSchema.TextContent(json)), /* isError= */ false);
  }

  /**
   * Wrap a plain text message as a successful tool result.
   *
   * @param text the text to return
   * @return a non-error CallToolResult with a single text content item
   */
  public static McpSchema.CallToolResult ofText(String text) {
    return new McpSchema.CallToolResult(
        List.of(new McpSchema.TextContent(text)), /* isError= */ false);
  }

  /**
   * Build an error result. The MCP client will receive isError=true and the message as text.
   *
   * @param message a human-readable error description
   * @return an error CallToolResult
   */
  public static McpSchema.CallToolResult error(String message) {
    return new McpSchema.CallToolResult(
        List.of(new McpSchema.TextContent(message)), /* isError= */ true);
  }
}
