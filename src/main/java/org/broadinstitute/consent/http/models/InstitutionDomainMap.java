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

  /**
   * Returns the institution name for a given email address.
   * @param email the email address to check
   * @return the institution name if found, otherwise null
   */
  public String getInstitutionForEmail(String email) {
    String trimmedEmail = email.trim();
    String domain = trimmedEmail.substring(trimmedEmail.indexOf('@') + 1);
    return institutionDomainMap.entrySet().stream()
        .filter(entry -> entry.getValue().stream()
            .anyMatch(d -> d.equalsIgnoreCase(domain)))
        .map(Map.Entry::getKey)
        .findFirst()
        .orElse(null);
  }
}
