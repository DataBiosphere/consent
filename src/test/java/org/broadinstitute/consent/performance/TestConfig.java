package org.broadinstitute.consent.performance;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class TestConfig {

  final Config config = ConfigFactory.load("performance");

  public Config loadConfig() {
    return config;
  }

  public String getBaseUrl() {
    return config.getString("ontology.baseUrl");
  }

  public int getPause() {
    return config.getInt("ontology.pause");
  }
}
