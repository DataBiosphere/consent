package org.broadinstitute.consent.http.mcp;

import io.dropwizard.lifecycle.Managed;
import io.modelcontextprotocol.server.McpStatelessSyncServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dropwizard {@link Managed} wrapper for {@link McpStatelessSyncServer}.
 *
 * <p>Dropwizard calls {@link #start()} after all resources are registered and the HTTP server is
 * running, and calls {@link #stop()} during graceful shutdown before the JVM exits.
 *
 * <p>{@code McpStatelessSyncServer} has no explicit {@code start()} method — it begins serving as
 * soon as the transport servlet receives connections. {@code closeGracefully()} handles shutdown.
 */
public class ConsentMcpManaged implements Managed {

  private static final Logger LOGGER = LoggerFactory.getLogger(ConsentMcpManaged.class);

  private final McpStatelessSyncServer server;

  public ConsentMcpManaged(McpStatelessSyncServer server) {
    this.server = server;
  }

  @Override
  public void start() {
    // McpStatelessSyncServer starts automatically when the transport servlet receives connections.
    LOGGER.info("Consent MCP server ready");
  }

  @Override
  public void stop() {
    LOGGER.info("Stopping Consent MCP server");
    try {
      server.closeGracefully();
      LOGGER.info("Consent MCP server stopped");
    } catch (Exception e) {
      LOGGER.warn("MCP server did not shut down cleanly", e);
    }
  }
}
