package org.broadinstitute.consent.http.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.beans.Transient;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import net.gcardone.junidecode.Junidecode;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.broadinstitute.consent.http.enumeration.UserRoles;

public class User {

  public static final String QUERY_FIELDS_WITH_U_PREFIX =
      " u.user_id as u_user_id, " +
          " u.email as u_email, " +
          " u.display_name as u_display_name, " +
          " u.create_date as u_create_date, " +
          " u.email_preference as u_email_preference, " +
          " u.institution_id as u_institution_id," +
          " u.era_commons_id as u_era_commons_id ";

  @JsonProperty
  private Integer userId;

  @JsonProperty
  private String email;

  @JsonProperty
  private String displayName;

  @JsonProperty
  private Date createDate;

  @JsonProperty
  private List<UserRole> roles;

  @JsonProperty
  private List<UserProperty> properties;

  @JsonProperty
  private Boolean emailPreference;

  @JsonProperty
  private Integer institutionId;

  @JsonProperty
  private String eraCommonsId;

  private Institution institution;

  private List<LibraryCard> libraryCards;

  public User() {
  }

  public Integer getUserId() {
    return userId;
  }

  public void setUserId(Integer userId) {
    this.userId = userId;
  }

  public User(Integer userId, String email, String displayName, Date createDate) {
    this.userId = userId;
    this.email = email;
    this.displayName = displayName;
    this.createDate = createDate;
  }

  public User(Integer userId, String email, String displayName, Date createDate,
      List<UserRole> roles) {
    this.userId = userId;
    this.email = email;
    this.displayName = displayName;
    this.createDate = createDate;
    this.roles = roles;
  }

  /**
   * Convenience method for backwards compatibility support for older clients. This method is
   * intended to reconstruct a User object from a supplied JSON string for primary fields and roles.
   * Other associated objects are not intended to be parsed using this method since it comes from
   * user-supplied data and may conflict with what the system knows about those associations.
   *
   * @param json A json string that may or may not be correctly structured as a DACUser
   */
  public User(String json) {
    Gson gson = new Gson();
    JsonObject userJsonObject = gson.fromJson(json, JsonObject.class);
    // There are no cases where we want to pull the create date/update date from user-provided data.
    // Nor do we need to retrieve the full institution object from user-provided data.
    JsonObject filteredUserJsonObject = filterFields(
        userJsonObject,
        Arrays.asList("createDate", "institution", "libraryCards"));
    User u = gson.fromJson(filteredUserJsonObject.toString(), User.class);
    setUserId(u);
    setEmail(u);
    setDisplayName(u);
    setEmailPreference(u);
    setRoles(u);
    setInstitutionId(u);
  }

  /**
   * Private method to filter out fields that we do not want to parse from json objects.
   *
   * @param obj    The json object
   * @param fields The fields to remove
   * @return Filtered Clone of the object.
   */
  private JsonObject filterFields(JsonObject obj, List<String> fields) {
    JsonObject copy = obj.deepCopy();
    fields.forEach(f -> {
      if (copy.has(f)) {
        copy.remove(f);
      }
    });
    return copy;
  }

  private void setUserId(User u) {
    if (Objects.nonNull(u.getUserId())) {
      this.setUserId(u.getUserId());
    }
  }

  private void setEmail(User u) {
    if (!StringUtils.isEmpty(u.getEmail()) && u.getEmail() != null) {
      this.setEmail(Junidecode.unidecode(u.getEmail()));
    }
  }

  private void setDisplayName(User u) {
    if (!StringUtils.isEmpty(u.getDisplayName())) {
      this.setDisplayName(u.getDisplayName());
    }
  }

  private void setEmailPreference(User u) {
    if (Objects.nonNull(u.getEmailPreference())) {
      this.setEmailPreference(u.getEmailPreference());
    }
  }

  private void setRoles(User u) {
    if (CollectionUtils.isNotEmpty(u.getRoles())) {
      this.setRoles(u.getRoles());
    }
  }

  private void setInstitutionId(User u) {
    if (Objects.nonNull(u.getInstitutionId())) {
      this.setInstitutionId(u.getInstitutionId());
    }
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public List<UserRole> getRoles() {
    return roles;
  }

  public void setRoles(List<UserRole> roles) {
    this.roles = roles;
  }

  public void setAdminRole() {
    this.roles = Collections.singletonList(UserRoles.Admin());
  }

  public void setChairpersonRole() {
    this.roles = Collections.singletonList(UserRoles.Chairperson());
  }

  public void setChairpersonRoleWithDAC(int dacId) {
    UserRole chairpersonRole = UserRoles.Chairperson();
    chairpersonRole.setDacId(dacId);
    this.roles = Collections.singletonList(chairpersonRole);
  }

  public void setITDirectorRole() {
    this.roles = Collections.singletonList(UserRoles.ITDirector());
  }

  public void setMemberRole() {
    this.roles = Collections.singletonList(UserRoles.Member());
  }

  public void setResearcherRole() {
    this.roles = Collections.singletonList(UserRoles.Researcher());
  }

  public void setSigningOfficialRole() {
    this.roles = Collections.singletonList(UserRoles.SigningOfficial());
  }

  public List<UserProperty> getProperties() {
    return properties;
  }

  public void setProperties(List<UserProperty> properties) {
    this.properties = properties;
  }

  public Date getCreateDate() {
    return createDate;
  }

  public void setCreateDate(Date createDate) {
    this.createDate = createDate;
  }

  public Boolean getEmailPreference() {
    return emailPreference;
  }

  public void setEmailPreference(Boolean emailPreference) {
    this.emailPreference = emailPreference;
  }

  public Integer getInstitutionId() {
    return institutionId;
  }

  public void setInstitutionId(Integer institutionId) {
    this.institutionId = institutionId;
  }

  public String getEraCommonsId() {
    return eraCommonsId;
  }

  public void setEraCommonsId(String eraCommonsId) {
    this.eraCommonsId = eraCommonsId;
  }

  public void setInstitution(Institution institution) {
    this.institution = institution;
  }

  public void setInstitution(User user) {
    if (Objects.nonNull(user.getInstitution())) {
      this.institution = user.institution;
    }
  }

  public Institution getInstitution() {
    return institution;
  }

  public void setLibraryCards(List<LibraryCard> cards) {
    this.libraryCards = cards;
  }

  public List<LibraryCard> getLibraryCards() {
    return this.libraryCards;
  }

  public void addRole(UserRole userRole) {
    if (Objects.isNull(this.getRoles())) {
      this.setRoles(new ArrayList<>());
    }

    if (!this.getRoles().contains(userRole)) {
      this.getRoles().add(userRole);
    }
  }

  public void addProperty(UserProperty userProp) {
    if (Objects.isNull(this.getProperties())) {
      this.setProperties(new ArrayList<>());
    }
    if (!this.getProperties().contains(userProp)) {
      this.getProperties().add(userProp);
    }
  }

  public void addLibraryCard(LibraryCard card) {
    if (Objects.isNull(this.getLibraryCards())) {
      this.setLibraryCards(new ArrayList<>());
    }
    if (!this.getLibraryCards().contains(card)) {
      this.getLibraryCards().add(card);
    }
  }

  @Override
  public int hashCode() {
    return this.getUserId();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }

    User other = (User) obj;
    return new EqualsBuilder().append(getUserId(), other.getUserId()).isEquals();
  }

  @Override
  public String toString() {
    return new Gson().toJson(this);
  }

  public boolean hasUserRole(UserRoles role) {
    if (Objects.isNull(this.getRoles())) {
      return false;
    } else {
      return this.getRoles().stream().anyMatch((r) -> r.getRoleId().equals(role.getRoleId()));
    }
  }

  @Transient
  public List<Integer> getUserRoleIdsFromUser() {
    if (Objects.isNull(this.getRoles())) {
      return List.of();
    }
    return this.getRoles()
        .stream()
        .map(UserRole::getRoleId)
        .collect(Collectors.toList());
  }

  @Transient
  public boolean doesUserHaveAnyRoleInSet(EnumSet<UserRoles> userRoles) {
    List<Integer> queriedRoleIds = userRoles.stream().map(UserRoles::getRoleId)
        .collect(Collectors.toList());
    return getUserRoleIdsFromUser().stream().anyMatch(queriedRoleIds::contains);
  }

  @Transient
  public Boolean checkIfUserHasRole(String roleName, Integer dacId) {
    UserRoles role = UserRoles.getUserRoleFromName(roleName);
    List<UserRole> roles = getRoles();
    List<UserRole> targetRoles = roles.stream()
        .filter((r) -> {
          return r.getName().equals(role.getRoleName())
              && r.getRoleId().equals(role.getRoleId())
              && Objects.equals(r.getDacId(), dacId);
        })
        .collect(Collectors.toList());
    return !targetRoles.isEmpty();

  }

}
