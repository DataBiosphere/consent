package org.broadinstitute.consent.http.models.sam;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * This represents the Sam response to GET /api/config/v1/resourceTypes
 */
public class ResourceType {

  private List<ActionPattern> actionPatterns;
  private String name;
  private String ownerRoleName;
  private Boolean reuseIds;
  private List<ResourceTypeRole> roles;

  public ResourceType setActionPatterns(List<ActionPattern> actionPatterns) {
    this.actionPatterns = actionPatterns;
    return this;
  }

  public ResourceType setName(String name) {
    this.name = name;
    return this;
  }

  public ResourceType setOwnerRoleName(String ownerRoleName) {
    this.ownerRoleName = ownerRoleName;
    return this;
  }

  public ResourceType setReuseIds(Boolean reuseIds) {
    this.reuseIds = reuseIds;
    return this;
  }

  public ResourceType setRoles(List<ResourceTypeRole> roles) {
    this.roles = roles;
    return this;
  }

  @Override
  public String toString() {
    return new Gson().toJson(this);
  }
}
