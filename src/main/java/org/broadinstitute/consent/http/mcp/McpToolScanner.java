package org.broadinstitute.consent.http.mcp;

import io.dropwizard.auth.Auth;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.server.McpStatelessServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.broadinstitute.consent.http.authentication.AuthorizationHelper;
import org.broadinstitute.consent.http.models.DuosUser;
import org.broadinstitute.consent.http.service.UserService;
import org.broadinstitute.consent.http.util.ConsentLogger;

/**
 * Scans JAX-RS resource instances for {@link McpTool}-annotated methods and produces {@link
 * McpStatelessServerFeatures.SyncToolSpecification} objects that can be registered directly with
 * {@link io.modelcontextprotocol.server.McpStatelessSyncServer}.
 *
 * <h2>Input schema construction</h2>
 *
 * For each annotated method the scanner inspects the method's parameters:
 *
 * <ul>
 *   <li>{@code @Auth} parameters are mapped to the resolved caller — they are not exposed in the
 *       MCP input schema.
 *   <li>{@code @PathParam} parameters are added as <em>required</em> string/integer properties.
 *   <li>{@code @QueryParam} parameters are added as <em>optional</em> properties.
 * </ul>
 *
 * Any {@link McpToolParam} entries in {@link McpTool#params()} whose name matches one of the above
 * JAX-RS params will <em>override</em> the auto-generated description and required flag. Entries
 * with names that do not match any JAX-RS param are added as additional properties.
 *
 * <h2>Handler auto-generation</h2>
 *
 * The generated handler resolves a {@link DuosUser} from the transport context, builds the method's
 * argument array by matching each parameter annotation to the MCP arguments map or the resolved
 * user, invokes the resource method via reflection, and wraps the result with {@link
 * McpToolResults#of}.
 */
public final class McpToolScanner implements ConsentLogger {

  // nosemgrep
  private static final String DESCRIPTION = "description";
  private final AuthorizationHelper authorizationHelper;
  private final UserService userService;

  public McpToolScanner(AuthorizationHelper authorizationHelper, UserService userService) {
    this.authorizationHelper = authorizationHelper;
    this.userService = userService;
  }

  /**
   * Scans each resource instance for {@link McpTool}-annotated methods and returns a tool
   * specification for each one found.
   *
   * @param resources JAX-RS resource instances (typically obtained from Guice)
   * @return list of tool specs, one per annotated method, in encounter order
   */
  public List<McpStatelessServerFeatures.SyncToolSpecification> scan(Object... resources) {
    List<McpStatelessServerFeatures.SyncToolSpecification> specs = new ArrayList<>();
    for (Object resource : resources) {
      for (Method method : resource.getClass().getMethods()) {
        McpTool annotation = method.getAnnotation(McpTool.class);
        if (annotation != null) {
          specs.add(buildSpec(resource, method, annotation));
          logInfo(
              "Registered MCP tool: "
                  + annotation.name()
                  + " → "
                  + resource.getClass().getSimpleName()
                  + "."
                  + method.getName());
        }
      }
    }
    return specs;
  }

  // ── Spec construction ────────────────────────────────────────────────────────────────────────

  private McpStatelessServerFeatures.SyncToolSpecification buildSpec(
      Object resource, Method method, McpTool annotation) {

    // Index McpToolParam overrides/additions by name for quick lookup.
    Map<String, McpToolParam> paramOverrides = new LinkedHashMap<>();
    for (McpToolParam p : annotation.params()) {
      paramOverrides.put(p.name(), p);
    }

    // Build the input schema by walking the method's parameter list.
    Map<String, Object> properties = new LinkedHashMap<>();
    List<String> required = new ArrayList<>();

    for (Parameter param : method.getParameters()) {
      PathParam pp = param.getAnnotation(PathParam.class);
      QueryParam qp = param.getAnnotation(QueryParam.class);

      if (pp != null) {
        String name = pp.value();
        String type = javaTypeToJsonType(param.getParameterizedType());
        String desc =
            descriptionFor(name, paramOverrides, "Path parameter identifying the " + name);
        boolean req = !paramOverrides.containsKey(name) || paramOverrides.get(name).required();
        properties.put(name, Map.of("type", type, DESCRIPTION, desc));
        if (req) {
          required.add(name);
        }
        paramOverrides.remove(name); // consumed
      } else if (qp != null) {
        String name = qp.value();
        String type = javaTypeToJsonType(param.getParameterizedType());
        String desc = descriptionFor(name, paramOverrides, "Filter by " + name);
        boolean req = paramOverrides.containsKey(name) && paramOverrides.get(name).required();
        properties.put(name, Map.of("type", type, DESCRIPTION, desc));
        if (req) {
          required.add(name);
        }
        paramOverrides.remove(name); // consumed
      }
      // @Auth params are not exposed in the schema.
    }

    // Any remaining McpToolParam entries are MCP-only additions not present on the method.
    for (McpToolParam extra : paramOverrides.values()) {
      properties.put(extra.name(), Map.of("type", extra.type(), DESCRIPTION, extra.description()));
      if (extra.required()) {
        required.add(extra.name());
      }
    }

    McpSchema.JsonSchema inputSchema =
        new McpSchema.JsonSchema(
            "object",
            properties.isEmpty() ? null : properties,
            required.isEmpty() ? null : required,
            /* additionalProperties= */ null,
            /* defs= */ null,
            /* definitions= */ null);

    Map<String, Object> outputSchema = Map.of("type", annotation.outputType());

    McpSchema.Tool tool =
        McpSchema.Tool.builder()
            .name(annotation.name())
            .description(annotation.description())
            .inputSchema(inputSchema)
            .outputSchema(outputSchema)
            .build();

    return new McpStatelessServerFeatures.SyncToolSpecification(
        tool, (context, request) -> invoke(resource, method, annotation.name(), context, request));
  }

  // ── Handler invocation ───────────────────────────────────────────────────────────────────────

  private McpSchema.CallToolResult invoke(
      Object resource,
      Method method,
      String toolName,
      McpTransportContext context,
      McpSchema.CallToolRequest request) {
    try {
      DuosUser duosUser = McpAuthHelper.resolveDuosUser(context, authorizationHelper, userService);
      Map<String, Object> args = request.arguments() != null ? request.arguments() : Map.of();
      Object[] methodArgs = buildArgs(method, duosUser, args);
      Object result = method.invoke(resource, methodArgs);

      if (result instanceof Response response) {
        int status = response.getStatus();
        if (status >= 400) {
          // Resource method returned an error response (e.g. 401, 403, 404, 500).
          // Extract a readable message from the entity if possible; fall back to the status reason.
          Object entity = response.getEntity();
          String message = entity != null ? String.valueOf(entity) : "HTTP " + status;
          return McpToolResults.error(message);
        }
        Object entity = response.getEntity();
        if (entity == null) {
          return McpToolResults.ofText("(no content)");
        }
        return McpToolResults.of(entity);
      }
      return McpToolResults.of(result);

    } catch (jakarta.ws.rs.NotAuthorizedException | jakarta.ws.rs.NotFoundException e) {
      return McpToolResults.error(e.getMessage());
    } catch (java.lang.reflect.InvocationTargetException e) {
      Throwable cause = e.getCause();
      if (cause instanceof jakarta.ws.rs.NotAuthorizedException
          || cause instanceof jakarta.ws.rs.NotFoundException) {
        return McpToolResults.error(cause.getMessage());
      }
      logException(cause != null ? (Exception) cause : e);
      return McpToolResults.error(
          "Unexpected error in "
              + toolName
              + ": "
              + (cause != null ? cause.getMessage() : e.getMessage()));
    } catch (Exception e) {
      logException(e);
      return McpToolResults.error("Unexpected error in " + toolName + ": " + e.getMessage());
    }
  }

  /** Maps method parameters to the corresponding values from the resolved user or MCP args. */
  private static Object[] buildArgs(Method method, DuosUser duosUser, Map<String, Object> mcpArgs) {
    Parameter[] params = method.getParameters();
    Object[] args = new Object[params.length];
    for (int i = 0; i < params.length; i++) {
      Parameter param = params[i];
      if (param.isAnnotationPresent(Auth.class)) {
        args[i] = duosUser;
      } else if (param.isAnnotationPresent(PathParam.class)) {
        String name = param.getAnnotation(PathParam.class).value();
        args[i] = coerce(mcpArgs.get(name), param.getParameterizedType());
      } else if (param.isAnnotationPresent(QueryParam.class)) {
        String name = param.getAnnotation(QueryParam.class).value();
        Object raw = mcpArgs.get(name);
        args[i] = raw != null ? coerce(raw, param.getParameterizedType()) : null;
      } else {
        // Body params, FormDataMultiPart, etc. — not supported; pass null.
        args[i] = null;
      }
    }
    return args;
  }

  // ── Type helpers ─────────────────────────────────────────────────────────────────────────────

  /**
   * Maps a Java generic type to its JSON Schema primitive type string. Handles common JAX-RS param
   * types and basic generics (e.g. {@code List<Integer>}).
   */
  static String javaTypeToJsonType(Type type) {
    if (type instanceof ParameterizedType pt) {
      Type raw = pt.getRawType();
      if (raw == List.class || raw == java.util.Collection.class) {
        return "array";
      }
    }
    if (type == Integer.class || type == int.class || type == Long.class || type == long.class) {
      return "integer";
    }
    if (type == Boolean.class || type == boolean.class) {
      return "boolean";
    }
    if (type == Double.class
        || type == double.class
        || type == Float.class
        || type == float.class) {
      return "number";
    }
    return "string";
  }

  /**
   * Coerces a raw MCP argument value (typically String or Number from JSON deserialisation) to the
   * target Java type expected by the method parameter.
   */
  static Object coerce(Object value, Type targetType) {
    if (value == null) {
      return null;
    }
    if (targetType == String.class) {
      return String.valueOf(value);
    }
    if (targetType == Integer.class || targetType == int.class) {
      return value instanceof Number n ? n.intValue() : Integer.parseInt(String.valueOf(value));
    }
    if (targetType == Long.class || targetType == long.class) {
      return value instanceof Number n ? n.longValue() : Long.parseLong(String.valueOf(value));
    }
    if (targetType == Boolean.class || targetType == boolean.class) {
      return value instanceof Boolean b ? b : Boolean.parseBoolean(String.valueOf(value));
    }
    if (targetType instanceof ParameterizedType pt && pt.getRawType() == List.class) {
      // MCP args deserialise JSON arrays as List<Object>; cast elements to the list's type arg.
      Type elementType = pt.getActualTypeArguments()[0];
      if (value instanceof List<?> list) {
        return list.stream().map(item -> coerce(item, elementType)).toList();
      }
      // Single value wrapped in a list.
      return List.of(coerce(value, elementType));
    }
    return value;
  }

  private static String descriptionFor(
      String name, Map<String, McpToolParam> overrides, String defaultDesc) {
    McpToolParam override = overrides.get(name);
    return (override != null && !override.description().isBlank())
        ? override.description()
        : defaultDesc;
  }
}
