package org.broadinstitute.consent.http.mcp;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.modelcontextprotocol.server.McpStatelessServerFeatures;
import java.util.List;
import org.broadinstitute.consent.http.authentication.AuthorizationHelper;
import org.broadinstitute.consent.http.resources.DatasetResource;
import org.broadinstitute.consent.http.service.UserService;
import org.broadinstitute.consent.http.util.ConsentLogger;

/**
 * Assembles all MCP tool specifications for the Consent MCP server by delegating to {@link
 * McpToolScanner}.
 *
 * <p>To expose a resource method as an MCP tool, annotate it with {@link McpTool} (and optionally
 * {@link McpToolParam} for parameter metadata), then add the resource instance to the {@link
 * McpToolScanner#scan} call in {@link #allTools()}.
 *
 * <p>{@link McpToolScanner} auto-generates the tool spec (name, description, input/output schema)
 * from the annotation and the method's JAX-RS parameter annotations ({@code @PathParam},
 * {@code @QueryParam}). The handler resolves the caller as a {@link
 * org.broadinstitute.consent.http.models.DuosUser}, invokes the resource method via reflection, and
 * wraps the result with {@link McpToolResults#of}.
 */
@Singleton
public class ConsentMcpToolProvider implements ConsentLogger {

  private final McpToolScanner scanner;
  private final DatasetResource datasetResource;

  @Inject
  public ConsentMcpToolProvider(
      AuthorizationHelper authorizationHelper,
      UserService userService,
      DatasetResource datasetResource) {
    this.scanner = new McpToolScanner(authorizationHelper, userService);
    this.datasetResource = datasetResource;
  }

  /**
   * Returns all registered MCP tool specifications.
   *
   * <p>To add new tools: annotate the target resource method with {@link McpTool}, then add its
   * resource instance to the {@link McpToolScanner#scan} argument list below.
   */
  public List<McpStatelessServerFeatures.SyncToolSpecification> allTools() {
    return scanner.scan(datasetResource);
  }
}
