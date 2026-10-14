package org.broadinstitute.consent.http.models.dto;

import org.apache.commons.validator.routines.UrlValidator;

public record DatasetMailDTO(
    Integer datasetId, String name, String identifier, String dataLocationUrl) {

  private static final UrlValidator WEB_URL_VALIDATOR =
      new UrlValidator(new String[] {"http", "https"});

  public DatasetMailDTO {
    dataLocationUrl = dataLocationUrl == null ? null : dataLocationUrl.strip();
  }

  public boolean hasWebDataLocationUrl() {
    return dataLocationUrl != null && WEB_URL_VALIDATOR.isValid(dataLocationUrl);
  }
}
