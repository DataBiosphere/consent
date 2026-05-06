package org.broadinstitute.consent.http.mcp;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.server.McpStatelessServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import java.util.List;
import java.util.Map;
import org.broadinstitute.consent.http.authentication.AuthorizationHelper;
import org.broadinstitute.consent.http.models.DatasetStudySummary;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.service.DatasetService;
import org.broadinstitute.consent.http.service.UserService;
import org.broadinstitute.consent.http.util.ConsentLogger;

/**
 * Assembles all MCP tool specifications for the Consent MCP server.
 *
 * <p>Each tool handler follows the same pattern:
 *
 * <ol>
 *   <li>Call {@link McpAuthHelper#resolveUser} to authenticate the caller and load their roles.
 *   <li>Delegate to the appropriate service method (same as the corresponding REST resource).
 *   <li>Return the result via {@link McpToolResults}.
 * </ol>
 *
 * <p>Error handling: unexpected exceptions are caught, logged, and returned as error results so
 * that the MCP client receives a structured response rather than a raw 500. Authorization and
 * not-found exceptions propagate their messages to the caller.
 */
@Singleton
public class ConsentMcpToolProvider implements ConsentLogger {

  // Typed input schema for dataset_search.
  // McpSchema.JsonSchema(type, properties, required, additionalProperties, defs, definitions).
  // properties values are Map<String,Object> (each property is itself a plain map of JSON-Schema
  // keywords). query is optional; omitting it returns all datasets visible to the caller.
  private static final McpSchema.JsonSchema DATASET_SEARCH_INPUT_SCHEMA =
      new McpSchema.JsonSchema(
          "object",
          Map.of(
              "query",
              Map.of(
                  "type",
                  "string",
                  "description",
                  "Case-insensitive text matched against dataset name and study name."
                      + " Omit to return all datasets visible to the caller.")),
          /* required= */ null,
          /* additionalProperties= */ null,
          /* defs= */ null,
          /* definitions= */ null);

  private final DatasetService datasetService;
  private final AuthorizationHelper authorizationHelper;
  private final UserService userService;

  @Inject
  public ConsentMcpToolProvider(
      DatasetService datasetService,
      AuthorizationHelper authorizationHelper,
      UserService userService) {
    this.datasetService = datasetService;
    this.authorizationHelper = authorizationHelper;
    this.userService = userService;
  }

  /** Returns all registered tool specifications. Add new tools here as phases are completed. */
  public List<McpStatelessServerFeatures.SyncToolSpecification> allTools() {
    return List.of(datasetSearchToolSpec());
  }

  // ── dataset_search ──────────────────────────────────────────────────────────────────────────

  private McpStatelessServerFeatures.SyncToolSpecification datasetSearchToolSpec() {
    // McpSchema.Tool record in SDK 0.14.1: (name, title, description, inputSchema, outputSchema,
    // annotations, meta).  Use the builder so outputSchema stays null — passing a raw Map to the
    // 5th constructor arg would populate outputSchema, which causes the SDK to require structured
    // content in the result.
    McpSchema.Tool tool =
        McpSchema.Tool.builder()
            .name("dataset_search")
            .description(
                "Search DUOS datasets and studies visible to the caller."
                    + " Returns dataset id, name, identifier, study name, and public visibility."
                    + " Provide a query string to filter by name; omit it to list all accessible datasets.")
            .inputSchema(DATASET_SEARCH_INPUT_SCHEMA)
            .build();
    return new McpStatelessServerFeatures.SyncToolSpecification(tool, this::handleDatasetSearch);
  }

  /**
   * Tool handler for {@code dataset_search}.
   *
   * <p>Resolves the calling user, fetches all dataset/study summaries they are permitted to see
   * (via {@link DatasetService#findAllDatasetStudySummaries}), then optionally filters the results
   * by a case-insensitive substring match on dataset name or study name.
   */
  private McpSchema.CallToolResult handleDatasetSearch(
      McpTransportContext context, McpSchema.CallToolRequest request) {
    try {
      User caller = McpAuthHelper.resolveUser(context, authorizationHelper, userService);

      List<DatasetStudySummary> summaries = datasetService.findAllDatasetStudySummaries(caller);

      Map<String, Object> args = request.arguments() != null ? request.arguments() : Map.of();
      String query = args.containsKey("query") ? String.valueOf(args.get("query")).strip() : "";
      if (!query.isBlank()) {
        String q = query.toLowerCase();
        summaries =
            summaries.stream()
                .filter(
                    d ->
                        (d.dataset_name() != null && d.dataset_name().toLowerCase().contains(q))
                            || (d.study_name() != null && d.study_name().toLowerCase().contains(q)))
                .toList();
      }

      return McpToolResults.of(summaries);
    } catch (jakarta.ws.rs.NotAuthorizedException | jakarta.ws.rs.NotFoundException e) {
      return McpToolResults.error(e.getMessage());
    } catch (Exception e) {
      logException(e);
      return McpToolResults.error("Unexpected error during dataset_search: " + e.getMessage());
    }
  }
}
