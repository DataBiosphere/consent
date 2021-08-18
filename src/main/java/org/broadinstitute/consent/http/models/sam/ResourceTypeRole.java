package org.broadinstitute.consent.http.models.sam;

import java.util.List;
import java.util.Map;

public class ResourceTypeRole {

  private List<String> actions;
  private Map<String, List<String>> descendantRoles;
  private List<String> includedRoles;
  private String roleName;
}
