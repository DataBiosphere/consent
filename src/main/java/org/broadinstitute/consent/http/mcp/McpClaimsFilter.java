package org.broadinstitute.consent.http.mcp;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MultivaluedHashMap;
import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import org.broadinstitute.consent.http.filters.ClaimsCache;

/**
 * Jakarta Servlet Filter registered on /mcp/* that mirrors what RequestHeaderCacheFilter does for
 * Jersey requests. Apache mod_oauth2 (AuthType oauth2 on the /mcp Location block) validates the
 * Bearer token and sets OAUTH2_CLAIM_* headers on every inbound request. This filter reads those
 * headers and loads them into ClaimsCache, keyed by the raw Bearer token. McpAuthHelper then reads
 * from the same cache when resolving a caller inside a tool handler.
 *
 * <p>The Bearer token itself is propagated to tool handlers via the transport provider's
 * contextExtractor (configured in ConsentModule), which stores it in McpTransportContext. This
 * approach works correctly even when the SDK dispatches tool handlers on Reactor scheduler threads
 * that are different from the original HTTP request thread.
 */
public class McpClaimsFilter implements Filter {

  @Override
  public void init(FilterConfig filterConfig) {}

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    if (request instanceof HttpServletRequest httpReq) {
      String authHeader = httpReq.getHeader(HttpHeaders.AUTHORIZATION);
      if (authHeader != null && authHeader.startsWith("Bearer ")) {
        String bearer = authHeader.substring("Bearer ".length());
        // Build a MultivaluedMap so we can reuse ClaimsCache.loadCache, which filters for
        // headers whose names start with "OAUTH2_CLAIM" (set by Apache mod_oauth2).
        MultivaluedHashMap<String, String> headers = new MultivaluedHashMap<>();
        Enumeration<String> names = httpReq.getHeaderNames();
        if (names != null) {
          while (names.hasMoreElements()) {
            String name = names.nextElement();
            headers.addAll(name, Collections.list(httpReq.getHeaders(name)));
          }
        }
        ClaimsCache.getInstance().loadCache(bearer, headers);
      }
    }
    chain.doFilter(request, response);
  }

  @Override
  public void destroy() {}
}
