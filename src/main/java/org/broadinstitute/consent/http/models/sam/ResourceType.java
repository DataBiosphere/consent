package org.broadinstitute.consent.http.models.sam;

import java.util.List;

public class ResourceType {

  private List<ActionPattern> actionPatterns;
  private String name;
  private String ownerRoleName;
  private Boolean reuseIds;
  private List<ResourceTypeRole> roles;
}
