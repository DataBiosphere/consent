package org.broadinstitute.consent.http.mcp;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares a single input parameter for an MCP tool.
 *
 * <p>Used within {@link McpTool#params()} to describe parameters that are either MCP-specific or
 * whose descriptions should override the defaults that {@link McpToolScanner} would otherwise
 * derive from the method's JAX-RS annotations ({@code @QueryParam}, {@code @PathParam}).
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({}) // only usable as a member of @McpTool
public @interface McpToolParam {

  /** JSON Schema property name. Must match the JAX-RS param name if overriding one. */
  String name();

  /** JSON Schema primitive type: "string", "integer", "boolean", "number". Default: "string". */
  String type() default "string";

  /** Human-readable description exposed to the MCP client. */
  String description() default "";

  /** Whether the MCP client must supply this parameter. Default: false (optional). */
  boolean required() default false;
}
