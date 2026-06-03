package org.broadinstitute.consent.http.configurations;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class NihConfiguration {

  private List<String> denyEmailPatterns = List.of();

  public List<String> getDenyEmailPatterns() {
    return denyEmailPatterns;
  }

  public void setDenyEmailPatterns(List<String> denyEmailPatterns) {
    this.denyEmailPatterns = denyEmailPatterns;
  }
}
