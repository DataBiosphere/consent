package org.broadinstitute.consent.http.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.google.gson.Gson;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class InstitutionDomainMapTest {

  @Test
  public void testInstitutionDomainMapGetAndSet() {
    InstitutionDomainMap map = new InstitutionDomainMap();

    // should be null by default
    assertNull(map.getInstitutionDomainMap());

    // set a test map
    Map<String, List<String>> testMap = Map.of(
        "Broad Institute", List.of("broadinstitute.org", "broad.mit.edu"),
        "Harvard", List.of("harvard.edu", "hms.harvard.edu")
    );
    map.setInstitutionDomainMap(testMap);

    assertEquals(testMap, map.getInstitutionDomainMap());
    assertEquals(2, map.getInstitutionDomainMap().size());
    assertEquals(2, map.getInstitutionDomainMap().get("Broad Institute").size());
  }

  @Test
  public void testInstitutionDomainMapSerDeser() {
    Gson gson = new Gson();

    // json string representation
    String domainMapJson = """
        {
          "institutionDomainMap": {
            "Broad Institute": ["broadinstitute.org", "broad.mit.edu"],
            "Harvard": ["harvard.edu", "hms.harvard.edu"]
          }
        }
        """;
    InstitutionDomainMap map1 = gson.fromJson(domainMapJson,
        InstitutionDomainMap.class);

    // object representation
    InstitutionDomainMap map2 = new InstitutionDomainMap();
    Map<String, List<String>> domainMap = Map.of(
        "Broad Institute", List.of("broadinstitute.org", "broad.mit.edu"),
        "Harvard", List.of("harvard.edu", "hms.harvard.edu")
    );
    map2.setInstitutionDomainMap(domainMap);

    String json = gson.toJson(map2);
    InstitutionDomainMap map3 = gson.fromJson(json, InstitutionDomainMap.class);

    assertEquals(map1.getInstitutionDomainMap(), map2.getInstitutionDomainMap());
    assertEquals(map2.getInstitutionDomainMap(), map3.getInstitutionDomainMap());
    assertEquals(domainMap, map3.getInstitutionDomainMap());
  }

  @Test
  public void testGetDomainsForInstitution() {
    InstitutionDomainMap map = new InstitutionDomainMap();
    Map<String, List<String>> testMap = Map.of(
        "Broad Institute", List.of("broadinstitute.org", "broad.mit.edu"),
        "Harvard", List.of("harvard.edu", "hms.harvard.edu")
    );

    map.setInstitutionDomainMap(testMap);

    List<String> domains = map.getDomainsForInstitution("Broad Institute");
    assertEquals(2, domains.size());
    assertEquals("broadinstitute.org", domains.get(0));
    assertEquals("broad.mit.edu", domains.get(1));

    List<String> emptyDomains = map.getDomainsForInstitution("Non-Existent Institution");
    assertNull(emptyDomains);
  }
}
