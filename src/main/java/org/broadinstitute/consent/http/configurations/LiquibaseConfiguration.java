package org.broadinstitute.consent.http.configurations;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * In deployed environments, an init-container runs the Liquibase changesets before the pods start,
 * so the application does not run them at startup. Local development databases are often restored
 * from an older dump and need the newer changesets, so set {@code runOnStartup: true} in a local
 * config to run them when the application starts.
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
