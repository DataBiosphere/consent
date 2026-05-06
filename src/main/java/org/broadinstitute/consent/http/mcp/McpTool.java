package org.broadinstitute.consent.http.mcp;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a JAX-RS resource method as an MCP tool.
 *
 * <p>{@link McpToolScanner} discovers all methods annotated with {@code @McpTool} in a set of
 * resource instances and builds a {@link
 * io.modelcontextprotocol.server.McpStatelessServerFeatures.SyncToolSpecification} for each one.
 *
 * <h2>Input schema</h2>
 *
 * The scanner auto-detects {@code @PathParam} and {@code @QueryParam} parameters from the method
 * signature, mapping their Java types to JSON Schema types. Descriptions for these are taken from
 * {@link #params()} if an entry with a matching {@link McpToolParam#name()} is present; otherwise a
 * generic description is generated. Params listed in {@link #params()} with names that do not match
 * any JAX-RS annotation are added as additional tool inputs (useful for MCP-only filtering that is
 * also expressed as a {@code @QueryParam} on the method).
 *
 * <h2>Handler auto-generation</h2>
 *
 * The scanner generates a handler that:
 *
 * <ol>
 *   <li>Resolves the calling {@link org.broadinstitute.consent.http.models.DuosUser} from the MCP
 *       transport context via {@link McpAuthHelper}.
 *   <li>Builds the method's argument array by matching {@code @Auth}, {@code @PathParam}, and
 *       {@code @QueryParam} parameters to the resolved user and the tool call's arguments map.
 *   <li>Invokes the method via reflection.
 *   <li>Extracts the entity from the returned {@link jakarta.ws.rs.core.Response} (or wraps a
 *       non-Response return value directly) and returns it as a {@link McpToolResults#of} result.
 * </ol>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface McpTool {

  /** MCP tool name exposed to clients (e.g. {@code "dataset_search"}). Must be unique. */
  String name();

  /** Human-readable description used by MCP clients to decide when to invoke this tool. */
  String description();

  /**
   * JSON Schema type for the tool's output, used to populate {@code outputSchema} on the tool spec.
   * Common values: {@code "object"} (default) or {@code "array"}.
   */
  String outputType() default "object";

  /**
   * Parameter metadata. Entries whose {@link McpToolParam#name()} matches a {@code @PathParam} or
   * {@code @QueryParam} on the method override the auto-generated description and required flag for
   * that parameter. Entries with unmatched names are added as additional input properties.
   */
  McpToolParam[] params() default {};
}
