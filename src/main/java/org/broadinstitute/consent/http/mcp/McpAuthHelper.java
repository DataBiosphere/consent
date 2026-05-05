package org.broadinstitute.consent.http.mcp;

import io.modelcontextprotocol.server.McpSyncServerExchange;
import org.broadinstitute.consent.http.authentication.AuthorizationHelper;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.service.UserService;

/**
 * Authentication helper for MCP tool handlers.
 *
 * <p>MCP tool calls arrive as HTTP POST requests to /mcp/messages. McpClaimsFilter intercepts each
 * such request, populates ClaimsCache from the OAUTH2_CLAIM_* headers set by Apache mod_oauth2. The
 * transport provider's contextExtractor (configured in ConsentModule) captures the raw Bearer token
 * from the request and stores it in McpTransportContext under the key {@code "bearer"}.
 *
 * <p>Tool handlers call {@link #resolveUser(McpSyncServerExchange, AuthorizationHelper,
 * UserService)} to obtain a fully populated {@link User} for the caller, including their roles. The
 * bearer token is read from the exchange's transport context, which is propagated through the
 * Reactor subscription context and therefore works correctly even when the SDK dispatches the tool
 * handler on a different thread from the original HTTP request thread.
 */
public final class McpAuthHelper {

  private McpAuthHelper() {}

  /**
   * Resolve the calling user for the current MCP tool invocation.
   *
   * <p>Reads the Bearer token from the exchange's {@link
   * io.modelcontextprotocol.common.McpTransportContext} (stored by the transport's contextExtractor
   * in ConsentModule), looks it up in ClaimsCache via {@link
   * AuthorizationHelper#resolveAuthUser(String)}, then fetches the fully populated {@link User}
   * (with roles) from the database via {@link UserService#findUserByEmail(String)}.
   *
   * @param exchange the current MCP exchange, which carries the transport context
   * @param authorizationHelper to resolve AuthUser from the cached claims
   * @param userService to load the full User record including roles
   * @return the fully populated User for the caller
   * @throws jakarta.ws.rs.NotAuthorizedException if the token is missing or not in the cache
   * @throws jakarta.ws.rs.NotFoundException if the email from the token is not registered in DUOS
   */
  public static User resolveUser(
      McpSyncServerExchange exchange,
      AuthorizationHelper authorizationHelper,
      UserService userService) {
    Object bearerObj = exchange.transportContext().get("bearer");
    String bearer = bearerObj != null ? String.valueOf(bearerObj) : null;
    AuthUser authUser = authorizationHelper.resolveAuthUser(bearer);
    return userService.findUserByEmail(authUser.getEmail());
  }
}
