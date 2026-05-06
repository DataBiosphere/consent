package org.broadinstitute.consent.http.mcp;

import io.modelcontextprotocol.common.McpTransportContext;
import org.broadinstitute.consent.http.authentication.AuthorizationHelper;
import org.broadinstitute.consent.http.models.AuthUser;
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
 * <p>Tool handlers receive the {@link McpTransportContext} directly as the first parameter of the
 * {@code BiFunction} handler, and call {@link #resolveUser(McpTransportContext,
 * AuthorizationHelper, UserService)} to obtain a fully populated {@link User} for the caller
 * including their roles.
 */
public final class McpAuthHelper {

  private McpAuthHelper() {}

  /**
   * Resolve the calling user for the current MCP tool invocation.
   *
   * <p>Reads the Bearer token from the supplied {@link McpTransportContext} (stored by the
   * transport's contextExtractor in ConsentModule), looks it up in ClaimsCache via {@link
   * AuthorizationHelper#resolveAuthUser(String)}, then fetches the fully populated {@link User}
   * (with roles) from the database via {@link UserService#findUserByEmail(String)}.
   *
   * @param context the transport context propagated to this tool handler
   * @param authorizationHelper to resolve AuthUser from the cached claims
   * @param userService to load the full User record including roles
   * @return the fully populated User for the caller
   * @throws jakarta.ws.rs.NotAuthorizedException if the token is missing or not in the cache
   * @throws jakarta.ws.rs.NotFoundException if the email from the token is not registered in DUOS
   */
  public static User resolveUser(
      McpTransportContext context,
      AuthorizationHelper authorizationHelper,
      UserService userService) {
    Object bearerObj = context.get("bearer");
    String bearer = bearerObj != null ? String.valueOf(bearerObj) : null;
    AuthUser authUser = authorizationHelper.resolveAuthUser(bearer);
    return userService.findUserByEmail(authUser.getEmail());
  }
}
