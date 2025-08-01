package org.broadinstitute.consent.http.models;

import java.util.Map;
import java.util.Set;

public class InstitutionDomainMap {

  private Map<String, Set<String>> institutionDomainMap;

  public Map<String, Set<String>> getInstitutionDomainMap() {
    return institutionDomainMap;
  }

  public void setInstitutionDomainMap(Map<String, Set<String>> institutionDomainMap) {
    this.institutionDomainMap = institutionDomainMap;
  }

  public Set<String> getDomainsForInstitution(String institution) {
    return institutionDomainMap.get(institution);
  }
}
