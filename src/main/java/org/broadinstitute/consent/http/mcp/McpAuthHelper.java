package org.broadinstitute.consent.http.mcp;

import io.modelcontextprotocol.common.McpTransportContext;
import org.broadinstitute.consent.http.authentication.AuthorizationHelper;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.DuosUser;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.service.UserService;

/**
 * Authentication helper for MCP tool handlers.
 *
 * <p>MCP tool calls arrive as HTTP POST requests to /mcp. McpClaimsFilter intercepts each request,
 * populates ClaimsCache from the OAUTH2_CLAIM_* headers set by Apache mod_oauth2. The transport's
 * contextExtractor (configured in ConsentModule) captures the raw Bearer token from the request and
 * stores it in {@link McpTransportContext} under the key {@code "bearer"}.
 *
 * <p>Tool handlers (and the auto-generated handlers in {@link McpToolScanner}) call
 * {@link #resolveDuosUser} to obtain a {@link DuosUser} that can be passed directly to resource
 * methods annotated with {@link McpTool}.
 */
public final class McpAuthHelper {

  private McpAuthHelper() {}

  /**
   * Resolve the calling user as a {@link DuosUser} for use with JAX-RS resource method invocation.
   *
   * <p>Reads the Bearer token from the supplied {@link McpTransportContext}, looks it up in
   * ClaimsCache via {@link AuthorizationHelper#resolveAuthUser(String)}, fetches the fully
   * populated {@link User} (with roles), and wraps both in a {@link DuosUser} — the type that
   * resource methods receive via {@code @Auth DuosUser}.
   *
   * @param context the transport context propagated to this tool handler
   * @param authorizationHelper to resolve AuthUser from the cached claims
   * @param userService to load the full User record including roles
   * @return a DuosUser combining the AuthUser and the fully populated User
   * @throws jakarta.ws.rs.NotAuthorizedException if the token is missing or not in the cache
   * @throws jakarta.ws.rs.NotFoundException if the email from the token is not registered in DUOS
   */
  public static DuosUser resolveDuosUser(
      McpTransportContext context,
      AuthorizationHelper authorizationHelper,
      UserService userService) {
    Object bearerObj = context.get("bearer");
    String bearer = bearerObj != null ? String.valueOf(bearerObj) : null;
    AuthUser authUser = authorizationHelper.resolveAuthUser(bearer);
    User user = userService.findUserByEmail(authUser.getEmail());
    return new DuosUser(authUser, user);
  }
}
