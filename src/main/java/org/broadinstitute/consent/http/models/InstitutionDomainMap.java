package org.broadinstitute.consent.http.models;

import java.util.List;
import java.util.Map;

public class InstitutionDomainMap {

  private Map<String, List<String>> institutionDomainMap;

  public Map<String, List<String>> getInstitutionDomainMap() {
    return institutionDomainMap;
  }

  public void setInstitutionDomainMap(Map<String, List<String>> institutionDomainMap) {
    this.institutionDomainMap = institutionDomainMap;
  }

  public List<String> getDomainsForInstitution(String institution) {
    return institutionDomainMap.get(institution);
  }
}
