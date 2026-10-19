package org.broadinstitute.consent.http.configurations;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Optional {@code liquibase} block in the application config. A config can omit the block, or omit
 * {@code runOnStartup} in it. In both cases {@code runOnStartup} is {@code false} and the
 * application does not run the Liquibase changesets at startup.
 *
 * <p>In deployed environments, an init-container runs the changesets before the pods start, so
 * deployed configs omit this block. Local development databases are often restored from an older
 * dump and need the newer changesets, so set {@code runOnStartup: true} in a local config to run
 * them when the application starts.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class LiquibaseConfiguration {

  private boolean runOnStartup = false;

  public boolean isRunOnStartup() {
    return runOnStartup;
  }

  public void setRunOnStartup(boolean runOnStartup) {
    this.runOnStartup = runOnStartup;
  }
}
