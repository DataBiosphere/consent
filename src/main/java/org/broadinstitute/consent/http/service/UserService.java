package org.broadinstitute.consent.http.service;

import static org.broadinstitute.consent.http.enumeration.UserFields.ERA_EXPIRATION_DATE;
import static org.broadinstitute.consent.http.enumeration.UserFields.ERA_STATUS;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.consent.http.db.DaaDAO;
import org.broadinstitute.consent.http.db.InstitutionDAO;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.db.UserPropertyDAO;
import org.broadinstitute.consent.http.db.UserRoleDAO;
import org.broadinstitute.consent.http.enumeration.UserFields;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.exceptions.LibraryCardRequiredException;
import org.broadinstitute.consent.http.models.DataAccessAgreement;
import org.broadinstitute.consent.http.models.DuosUser;
import org.broadinstitute.consent.http.models.Institution;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserProperty;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.models.UserUpdateFields;
import org.broadinstitute.consent.http.resources.Resource;
import org.broadinstitute.consent.http.service.dao.UserServiceDAO;
import org.broadinstitute.consent.http.service.feature.InstitutionAndLibraryCardEnforcement;
import org.broadinstitute.consent.http.util.ConsentLogger;
import org.broadinstitute.consent.http.util.gson.GsonUtil;

public class UserService implements ConsentLogger {

  public static final String LIBRARY_CARD_FIELD = "libraryCard";
  public static final String USER_PROPERTIES_FIELD = "properties";
  public static final String USER_STATUS_INFO_FIELD = "userStatusInfo";

  private final UserPropertyDAO userPropertyDAO;
  private final UserDAO userDAO;
  private final UserRoleDAO userRoleDAO;
  private final InstitutionDAO institutionDAO;
  private final UserServiceDAO userServiceDAO;
  private final DaaDAO daaDAO;
  private final InstitutionService institutionService;
  private final InstitutionAndLibraryCardEnforcement institutionAndLibraryCardEnforcement;

  @Inject
  public UserService(
      UserDAO userDAO,
      UserPropertyDAO userPropertyDAO,
      UserRoleDAO userRoleDAO,
      InstitutionDAO institutionDAO,
      UserServiceDAO userServiceDAO,
      DaaDAO daaDAO,
      InstitutionService institutionService,
      InstitutionAndLibraryCardEnforcement institutionAndLibraryCardEnforcement) {
    this.userDAO = userDAO;
    this.userPropertyDAO = userPropertyDAO;
    this.userRoleDAO = userRoleDAO;
    this.institutionDAO = institutionDAO;
    this.userServiceDAO = userServiceDAO;
    this.daaDAO = daaDAO;
    this.institutionService = institutionService;
    this.institutionAndLibraryCardEnforcement = institutionAndLibraryCardEnforcement;
  }

  /**
   * Update a select group of user fields for a user id.
   *
   * @param userUpdateFields A UserUpdateFields object for all update information
   * @param userId The User's ID
   * @return The updated User
   */
  public User updateUserFieldsById(UserUpdateFields userUpdateFields, Integer userId) {
    if (Objects.nonNull(userUpdateFields)) {

      // Update Primary User Fields
      if (Objects.nonNull(userUpdateFields.getDisplayName())) {
        userDAO.updateDisplayName(userId, userUpdateFields.getDisplayName());
      }
      if (Objects.nonNull(userUpdateFields.getEmailPreference())) {
        userDAO.updateEmailPreference(userId, userUpdateFields.getEmailPreference());
      }
      if (Objects.nonNull(userUpdateFields.getEraCommonsId())) {
        userDAO.updateEraCommonsId(userId, userUpdateFields.getEraCommonsId());
      }

      // Update User Properties
      List<UserProperty> userProps = userUpdateFields.buildUserProperties(userId);
      if (!userProps.isEmpty()) {
        userPropertyDAO.deletePropertiesByUserAndKey(userProps);
        userPropertyDAO.insertAll(userProps);
      }

      // Handle Roles
      // TODO: Confirm if we need to prevent removing the chairperson role through this.  We have
      // other business logic in the application that checks to see if there's at least one other
      // chairperson on a DAC.
      if (Objects.nonNull(userUpdateFields.getUserRoleIds())) {
        List<Integer> currentRoleIds =
            userRoleDAO.findRolesByUserId(userId).stream().map(UserRole::getRoleId).toList();
        List<Integer> roleIdsToAdd = userUpdateFields.getRoleIdsToAdd(currentRoleIds);
        List<Integer> roleIdsToRemove = userUpdateFields.getRoleIdsToRemove(currentRoleIds);
        // Add the new role ids to the user
        if (!roleIdsToAdd.isEmpty()) {
          List<UserRole> newRoles =
              roleIdsToAdd.stream()
                  .map(
                      id ->
                          new UserRole(
                              id,
                              Objects.requireNonNull(UserRoles.getUserRoleFromId(id))
                                  .getRoleName()))
                  .toList();
          userRoleDAO.insertUserRoles(newRoles, userId);
        }
        // Remove the old role ids from the user
        if (!roleIdsToRemove.isEmpty()) {
          userRoleDAO.removeUserRoles(userId, roleIdsToRemove);
        }
      }
    }
    return findUserById(userId);
  }

  public void insertRoleAndInstitutionForUser(UserRole role, User user) {
    var userId = user.getUserId();
    try {
      if (user.getInstitutionId() == null) {
        Institution institution = institutionService.findInstitutionForEmail(user.getEmail());
        if (institution == null) {
          throw new BadRequestException(
              "No institution found for user: %s".formatted(user.getEmail()));
        }
        userServiceDAO.insertRoleAndInstitutionTxn(role, institution.getId(), userId);
      } else {
        userRoleDAO.insertSingleUserRole(role.getRoleId(), userId);
      }
    } catch (Exception e) {
      logException("Error when updating user: %s, role: %s".formatted(userId, role), e);
      throw e;
    }
  }

  public User createUser(User user) {
    // Default role is researcher.
    if (CollectionUtils.isEmpty(user.getRoles())) {
      user.setResearcherRole();
    }
    validateRequiredFields(user);
    User existingUser = userDAO.findUserByEmail(user.getEmail());
    if (Objects.nonNull(existingUser)) {
      throw new BadRequestException("User exists with this email address: " + user.getEmail());
    }
    Institution institution = institutionService.findInstitutionForEmail(user.getEmail());
    if (institution != null) {
      user.setInstitutionId(institution.getId());
    }
    return userServiceDAO.createUser(user);
  }

  public User findUserById(Integer id) throws NotFoundException {
    User user = userDAO.findUserById(id);
    if (user == null) {
      throw new NotFoundException("Unable to find user with id: " + id);
    }
    return user;
  }

  public User findUserByEmail(String email) throws NotFoundException {
    User user = userDAO.findUserByEmail(email);
    if (user == null) {
      throw new NotFoundException("Unable to find user with email: " + email);
    }
    return user;
  }

  /**
   * Find users as a specific role, e.g., Admins can see all users, other roles can only see a
   * subset of users.
   *
   * @param user The user making the request
   * @param roleName The role the user is making the request as
   * @return List of Users for specified role name
   */
  public List<User> getUsersAsRole(User user, String roleName) {
    switch (roleName) {
      // SigningOfficial console is technically pulling LCs, it's just bringing associated users
      // along for the ride
      // However LCs can be created for users not yet registered in the system
      // As such a more specialized query is needed to produce the proper listing
      case Resource.SIGNINGOFFICIAL:
        Integer institutionId = user.getInstitutionId();
        if (Objects.nonNull(user.getInstitutionId())) {
          return userDAO.getUsersFromInstitutionWithCards(institutionId);
        } else {
          throw new NotFoundException(
              "Signing Official (user: "
                  + user.getDisplayName()
                  + ") is not associated with an Institution.");
        }
      case Resource.ADMIN:
        return userDAO.findUsersWithLCsAndInstitution();
      default:
        // do nothing
    }
    return Collections.emptyList();
  }

  public List<SimplifiedUser> getUsersByDaaId(Integer daaId) {
    if (Objects.isNull(daaId)) {
      throw new IllegalArgumentException();
    }
    DataAccessAgreement daa = daaDAO.findById(daaId);
    if (Objects.isNull(daa)) {
      throw new NotFoundException();
    }
    List<User> users = userDAO.getUsersWithCardsByDaaId(daaId);
    return users.stream().map(SimplifiedUser::new).toList();
  }

  public List<UserProperty> findAllUserProperties(Integer userId) {
    return userPropertyDAO.findUserPropertiesByUserIdAndPropertyKeys(
        userId, UserFields.getValues());
  }

  public List<SimplifiedUser> findSOsByInstitutionId(Integer institutionId) {
    if (Objects.isNull(institutionId)) {
      return Collections.emptyList();
    }

    List<User> users = userDAO.getSOsByInstitution(institutionId);
    return users.stream().map(SimplifiedUser::new).toList();
  }

  public List<User> findUsersByInstitutionId(Integer institutionId) {
    if (Objects.isNull(institutionId)) {
      throw new IllegalArgumentException();
    }
    Institution institution = institutionDAO.findInstitutionById(institutionId);
    if (Objects.isNull(institution)) {
      throw new NotFoundException();
    }
    return userDAO.findUsersByInstitution(institutionId);
  }

  public void deleteUserRole(User authUser, Integer userId, Integer roleId) {
    userRoleDAO.removeSingleUserRole(userId, roleId);
    logInfo(
        "User %s deleted roleId: %s from User ID: %s"
            .formatted(authUser.getDisplayName(), roleId, userId));
  }

  /**
   * Convenience method to return a response-friendly json object of the user.
   *
   * @param duosUser The DuosUser. Used to determine if we should return auth user properties
   * @param userId The User. This is the user we want to return properties for
   * @return JsonObject.
   */
  public JsonObject findUserWithPropertiesByIdAsJsonObject(DuosUser duosUser, Integer userId) {
    Gson gson = GsonUtil.getInstance();
    User user = findUserById(userId);
    List<UserProperty> props = findAllUserProperties(user.getUserId());
    JsonObject userJson = gson.toJsonTree(user).getAsJsonObject();
    JsonArray propsJson = gson.toJsonTree(props).getAsJsonArray();
    userJson.add(USER_PROPERTIES_FIELD, propsJson);
    if (user.getLibraryCard() != null) {
      JsonObject libraryCardJson = gson.toJsonTree(user.getLibraryCard()).getAsJsonObject();
      userJson.add(LIBRARY_CARD_FIELD, libraryCardJson);
    }
    if (duosUser.getEmail().equalsIgnoreCase(user.getEmail())
        && Objects.nonNull(duosUser.getUserStatusInfo())) {
      JsonObject userStatusInfoJson =
          gson.toJsonTree(duosUser.getUserStatusInfo()).getAsJsonObject();
      userJson.add(USER_STATUS_INFO_FIELD, userStatusInfoJson);
    }
    return userJson;
  }

  private void validateRequiredFields(User user) {
    if (StringUtils.isEmpty(user.getDisplayName())) {
      throw new BadRequestException("Display Name cannot be empty");
    }
    if (StringUtils.isEmpty(user.getEmail())) {
      throw new BadRequestException("Email address cannot be empty");
    }
    Set<String> validRoleNameList =
        Stream.of(
                UserRoles.ADMIN,
                UserRoles.ALUMNI,
                UserRoles.DATASUBMITTER,
                UserRoles.ITDIRECTOR,
                UserRoles.RESEARCHER,
                UserRoles.SERVICE_ACCOUNT,
                UserRoles.SIGNINGOFFICIAL)
            .map(UserRoles::getRoleName)
            .collect(Collectors.toSet());
    user.getRoles()
        .forEach(
            role -> {
              if (!validRoleNameList.contains(role.getName())) {
                String validRoleNames = String.join(", ", validRoleNameList);
                throw new BadRequestException(
                    "Invalid role: " + role.getName() + ". Valid roles are: " + validRoleNames);
              }
            });
  }

  public List<User> findUsersInJsonArray(String json, String arrayKey) {
    List<JsonElement> jsonElementList;
    try {
      JsonObject jsonObject = new Gson().fromJson(json, JsonObject.class);
      jsonElementList = jsonObject.getAsJsonArray(arrayKey).asList();
    } catch (Exception e) {
      throw new BadRequestException("Invalid JSON or missing array with key: " + arrayKey);
    }
    return jsonElementList.stream().distinct().map(e -> findUserById(e.getAsInt())).toList();
  }

  public void validateActiveERACredentials(User user) {
    if (user.getLibraryCard() == null) {
      throw new LibraryCardRequiredException();
    }
    boolean hasEraCommonsId = user.getEraCommonsId() != null;
    if (!hasEraCommonsId) {
      throw new BadRequestException("User does not have an Era Commons ID");
    }
    List<UserProperty> userProperties = findAllUserProperties(user.getUserId());
    List<UserProperty> eraStatusProps =
        userProperties.stream()
            .filter(
                userProperty ->
                    userProperty.getPropertyKey().equalsIgnoreCase(ERA_STATUS.getValue()))
            .toList();
    List<UserProperty> eraExpirationProps =
        userProperties.stream()
            .filter(
                userProperty ->
                    userProperty.getPropertyKey().equalsIgnoreCase(ERA_EXPIRATION_DATE.getValue()))
            .toList();
    if (eraStatusProps.size() == 1 && eraExpirationProps.size() == 1) {
      if (!eraStatusProps.get(0).getPropertyValue().equalsIgnoreCase("true")) {
        throw new BadRequestException("User does not have an Era Commons ID that is authorized.");
      }
      if (Instant.ofEpochMilli(Long.parseLong(eraExpirationProps.get(0).getPropertyValue()))
          .isBefore(Instant.now())) {
        throw new BadRequestException("User has an expired Era Commons ID.");
      }
    } else {
      throw new BadRequestException(
          "Invalid ERA configuration for this user.  Only one ERA Commons ID is allowed.");
    }
  }

  /**
   * Compliance method that implements a set of rules in order to ensure Library Card and
   * Institution matching rules are adhered to when authorizing users of the system.
   *
   * @param email of the user being evaluated
   * @return user with the Institution and Library Card rules applied or null if the requestor isn't
   *     a DUOS user.
   */
  public User enforceInstitutionAndLibraryCardRules(String email) {
    return institutionAndLibraryCardEnforcement.enforceInstitutionAndLibraryCardRules(email);
  }

  public static class SimplifiedUser {

    private Integer userId;
    private String displayName;
    private String email;
    private Integer institutionId;

    public SimplifiedUser(User user) {
      this.userId = user.getUserId();
      this.displayName = user.getDisplayName();
      this.email = user.getEmail();
      this.institutionId = user.getInstitutionId();
    }

    public SimplifiedUser() {}

    public void setUserId(Integer userId) {
      this.userId = userId;
    }

    public void setDisplayName(String name) {
      this.displayName = name;
    }

    public void setEmail(String email) {
      this.email = email;
    }

    public void setInstitutionId(Integer institutionId) {
      this.institutionId = institutionId;
    }

    public String getDisplayName() {
      return displayName;
    }

    public String getEmail() {
      return email;
    }

    public Integer getInstitutionId() {
      return institutionId;
    }

    public Integer getUserId() {
      return userId;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      SimplifiedUser that = (SimplifiedUser) o;
      return Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
      return Objects.hash(userId);
    }
  }
}
