package org.broadinstitute.consent.http.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.consent.http.db.InstitutionDAO;
import org.broadinstitute.consent.http.db.LibraryCardDAO;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.db.UserPropertyDAO;
import org.broadinstitute.consent.http.db.UserRoleDAO;
import org.broadinstitute.consent.http.db.VoteDAO;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.Institution;
import org.broadinstitute.consent.http.models.LibraryCard;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserProperty;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.models.UserUpdateFields;
import org.broadinstitute.consent.http.models.Vote;
import org.broadinstitute.consent.http.resources.Resource;
import org.broadinstitute.consent.http.service.users.handler.UserRolesHandler;
import org.jdbi.v3.core.statement.UnableToExecuteStatementException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class UserService {

    public final static String LIBRARY_CARDS_FIELD = "libraryCards";
    public final static String RESEARCHER_PROPERTIES_FIELD = "researcherProperties";
    public final static String USER_STATUS_INFO_FIELD = "userStatusInfo";

    private final UserPropertyDAO userPropertyDAO;
    private final UserDAO userDAO;
    private final UserRoleDAO userRoleDAO;
    private final VoteDAO voteDAO;
    private final InstitutionDAO institutionDAO;
    private final LibraryCardDAO libraryCardDAO;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Inject
    public UserService(UserDAO userDAO, UserPropertyDAO userPropertyDAO, UserRoleDAO userRoleDAO, VoteDAO voteDAO, InstitutionDAO institutionDAO, LibraryCardDAO libraryCardDAO) {
        this.userDAO = userDAO;
        this.userPropertyDAO = userPropertyDAO;
        this.userRoleDAO = userRoleDAO;
        this.voteDAO = voteDAO;
        this.institutionDAO = institutionDAO;
        this.libraryCardDAO = libraryCardDAO;
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
            if (Objects.nonNull(userUpdateFields.getInstitutionId())) {
                userDAO.updateInstitutionId(userId, userUpdateFields.getInstitutionId());
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

            // Handle Roles; must be admin to update roles.
            if (Objects.nonNull(userUpdateFields.getUserRoleIds())) {
                List<Integer> currentRoleIds = userRoleDAO.findRolesByUserId(userId).stream().map(UserRole::getRoleId).collect(Collectors.toList());
                List<Integer> roleIdsToAdd = userUpdateFields.getRoleIdsToAdd(currentRoleIds);
                List<Integer> roleIdsToRemove = userUpdateFields.getRoleIdsToRemove(currentRoleIds);
                // Add the new role ids to the user
                if (!roleIdsToAdd.isEmpty()) {
                    List<UserRole> newRoles = roleIdsToAdd.stream()
                        .map(id -> new UserRole(id, Objects.requireNonNull(UserRoles.getUserRoleFromId(id)).getRoleName()))
                        .collect(Collectors.toList());
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

    public static class SimplifiedUser {
        public Integer userId;
        @Deprecated
        public Integer dacUserId;
        public String displayName;
        public String email;

        public SimplifiedUser(User user) {
            this.userId = user.getUserId();
            this.dacUserId = user.getUserId();
            this.displayName = user.getDisplayName();
            this.email = user.getEmail();
        }

        public SimplifiedUser() {
        }

        public void setUserId(Integer userId) {
            this.userId = userId;
        }

        @Deprecated
        public void setDacUserId(Integer userId) {
            this.dacUserId = userId;
        }

        public void setDisplayName(String name) {
            this.displayName = name;
        }

        public void setEmail(String email) {
            this.email = email;
        }
    }

    public User createUser(User user) {
        // Default role is researcher.
        if (Objects.isNull(user.getRoles()) || CollectionUtils.isEmpty(user.getRoles())) {
            UserRole researcher = new UserRole(UserRoles.RESEARCHER.getRoleId(), UserRoles.RESEARCHER.getRoleName());
            user.setRoles(Collections.singletonList(researcher));
        }
        validateRequiredFields(user);
        User existingUser = userDAO.findUserByEmail(user.getEmail());
        if (Objects.nonNull(existingUser)) {
            throw new BadRequestException("User exists with this email address: " + user.getEmail());
        }
        Integer userId = userDAO.insertUser(user.getEmail(), user.getDisplayName(), new Date());
        insertUserRoles(user.getRoles(), userId);
        addExistingLibraryCards(user);
        return userDAO.findUserById(userId);
    }

    public User findUserById(Integer id) throws NotFoundException {
        User user = userDAO.findUserById(id);
        if (user == null) {
            throw new NotFoundException("Unable to find user with id: " + id);
        }
        List<LibraryCard> cards = libraryCardDAO.findLibraryCardsByUserId(user.getUserId());
        if (Objects.nonNull(cards) && !cards.isEmpty()) {
            user.setLibraryCards(cards);
        }
        return user;
    }

    public User findUserByEmail(String email) throws NotFoundException {
        User user = userDAO.findUserByEmail(email);
        if (user == null) {
            throw new NotFoundException("Unable to find user with email: " + email);
        }
        List<LibraryCard> cards = libraryCardDAO.findLibraryCardsByUserId(user.getUserId());
        if (Objects.nonNull(cards) && !cards.isEmpty()) {
            user.setLibraryCards(cards);
        }
        return user;
    }

    /**
     * Find users as a specific role, e.g., Admins can see all users, other roles
     * can only see a subset of users.
     *
     * @param user The user making the request
     * @param roleName The role the user is making the request as
     * @return List of Users for specified role name
     */
    public List<User> getUsersAsRole(User user, String roleName) {
        switch(roleName) {
            // SigningOfficial console is technically pulling LCs, it's just bringing associated users along for the ride
            // However LCs can be created for users not yet registered in the system
            // As such a more specialized query is needed to produce the proper listing
            case Resource.SIGNINGOFFICIAL :
                Integer institutionId = user.getInstitutionId();
                if (Objects.nonNull(user.getInstitutionId())) {
                    List<User> institutionUsers = userDAO.getUsersFromInstitutionWithCards(institutionId);
                    List<User> unregisteredUsers = userDAO.getCardsForUnregisteredUsers(institutionId);
                    return Stream.of(
                        institutionUsers,
                        unregisteredUsers
                    ).flatMap(Collection::stream)
                    .collect(Collectors.toList());
                } else {
                    throw new NotFoundException("Signing Official (user: " + user.getDisplayName() + ") is not associated with an Institution.");
                }
            case Resource.ADMIN :
                return new ArrayList<>(userDAO.findUsers());
        }
        return Collections.emptyList();
    }

    public void deleteUserByEmail(String email) {
        User user = userDAO.findUserByEmail(email);
        if (user == null) {
            throw new NotFoundException("The user for the specified E-Mail address does not exist");
        }
        List<Integer> roleIds = userRoleDAO.
                findRolesByUserId(user.getUserId()).
                stream().
                map(UserRole::getRoleId).
                collect(Collectors.toList());
        if (!roleIds.isEmpty()) {
            userRoleDAO.removeUserRoles(user.getUserId(), roleIds);
        }
        List<Vote> votes = voteDAO.findVotesByUserId(user.getUserId());
        if (!votes.isEmpty()) {
            List<Integer> voteIds = votes.stream().map(Vote::getVoteId).collect(Collectors.toList());
            voteDAO.removeVotesByIds(voteIds);
        }
        userPropertyDAO.deleteAllPropertiesByUser(user.getUserId());
        userDAO.deleteUserById(user.getUserId());
    }

    public List<UserProperty> findAllUserProperties(Integer userId) {
        return userPropertyDAO.findResearcherPropertiesByUser(userId);
    }

    public List<User> describeAdminUsersThatWantToReceiveMails() {
        return userDAO.describeUsersByRoleAndEmailPreference(UserRoles.ADMIN.getRoleName(), true);
    }


    public User updateDACUserById(Map<String, User> dac, Integer id) throws IllegalArgumentException, NotFoundException {
        User updatedUser = dac.get(UserRolesHandler.UPDATED_USER_KEY);
        // validate user exists
        User existingUser = userDAO.findUserById(id);
        if (Objects.isNull(existingUser)) {
            throw new NotFoundException("The user for the specified id does not exist");
        }
        // validate required fields are not null or empty
        if (StringUtils.isEmpty(updatedUser.getDisplayName())) {
            updatedUser.setDisplayName(existingUser.getDisplayName());
        }

        if (Objects.isNull(updatedUser.getInstitutionId())) {
            updatedUser.setInstitutionId(existingUser.getInstitutionId());
        }

        if (Objects.nonNull(updatedUser.getInstitutionId()) && !checkForValidInstitution(updatedUser.getInstitutionId())) {
            throw new BadRequestException("Institution with the given id does not exist");
        }

        try {
            userDAO.updateUser(updatedUser.getDisplayName(), id, updatedUser.getInstitutionId());
        } catch (UnableToExecuteStatementException e) {
            throw new IllegalArgumentException("Email shoud be unique.");
        }
        return userDAO.findUserById(id);
    }

    public void updateEmailPreference(boolean preference, Integer userId) {
        userDAO.updateEmailPreference(userId, preference);
    }

    public List<SimplifiedUser> findSOsByInstitutionId(Integer institutionId) {
        if (Objects.isNull(institutionId)) {
            return Collections.emptyList();
        }

        List<User> users = userDAO.getSOsByInstitution(institutionId);
        return users.stream().map(SimplifiedUser::new).collect(Collectors.toList());
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
        logger.info("User " + authUser.getDisplayName() + " deleted roleId: " + roleId + " from User ID: " + userId);
    }

    public List<User> findUsersWithNoInstitution() {
        return userDAO.getUsersWithNoInstitution();
    }

    /**
     * Convenience method to return a response-friendly json object of the user.
     *
     * @param authUser The AuthUser. Used to determine if we should return auth user properties
     * @param userId The User. This is the user we want to return properties for
     * @return JsonObject.
     */
    public JsonObject findUserWithPropertiesByIdAsJsonObject(AuthUser authUser, Integer userId) {
        Gson gson = new Gson();
        User user = findUserById(userId);
        List<UserProperty> props = findAllUserProperties(user.getUserId());
        List<LibraryCard> entries = Objects.nonNull(user.getLibraryCards()) ? user.getLibraryCards() : List.of();
        JsonObject userJson = gson.toJsonTree(user).getAsJsonObject();
        JsonArray propsJson = gson.toJsonTree(props).getAsJsonArray();
        JsonArray entriesJson = gson.toJsonTree(entries).getAsJsonArray();
        userJson.add(RESEARCHER_PROPERTIES_FIELD, propsJson);
        userJson.add(LIBRARY_CARDS_FIELD, entriesJson);
        if (authUser.getEmail().equalsIgnoreCase(user.getEmail()) && Objects.nonNull(authUser.getUserStatusInfo())) {
            JsonObject userStatusInfoJson = gson.toJsonTree(authUser.getUserStatusInfo()).getAsJsonObject();
            userJson.add(USER_STATUS_INFO_FIELD, userStatusInfoJson);
        }
        return userJson;
    }

    private void validateRequiredFields(User user) {
        if (Objects.isNull(user.getDisplayName()) || StringUtils.isEmpty(user.getDisplayName())) {
            throw new BadRequestException("Display Name cannot be empty");
        }
        if (Objects.isNull(user.getEmail()) || StringUtils.isEmpty(user.getEmail())) {
            throw new BadRequestException("Email address cannot be empty");
        }
        user.getRoles().forEach(role -> {
            List<UserRoles> validRoles = Stream.of(UserRoles.DATAOWNER, UserRoles.RESEARCHER, UserRoles.ALUMNI, UserRoles.ADMIN).collect(Collectors.toList());
            List<String> validRoleNameList = validRoles.stream().map(UserRoles::getRoleName).collect(Collectors.toList());
            if (!validRoleNameList.contains(role.getName())) {
                String validRoleNames = String.join(", ", validRoleNameList);
                throw new BadRequestException("Invalid role: " + role.getName() + ". Valid roles are: " + validRoleNames);
            }
        });
    }

    public void insertUserRoles(List<UserRole> roles, Integer userId) {
        roles.forEach(r -> {
            if (r.getRoleId() == null) {
                r.setRoleId(userRoleDAO.findRoleIdByName(r.getName()));
            }
        });
        userRoleDAO.insertUserRoles(roles, userId);
    }

    private void addExistingLibraryCards(User user) {
        List<LibraryCard> libraryCards = libraryCardDAO.findAllLibraryCardsByUserEmail(user.getEmail());

        if (Objects.isNull(libraryCards) || libraryCards.isEmpty()) {
            return;
        }

        libraryCards
                .forEach(lc -> {
                    lc.setUserId(user.getUserId());

                    if (!Objects.isNull(lc.getInstitutionId())) {
                        user.setInstitutionId(lc.getInstitutionId());
                    }

                    libraryCardDAO.updateLibraryCardById(
                            lc.getId(),
                            lc.getUserId(),
                            lc.getInstitutionId(),
                            lc.getEraCommonsId(),
                            lc.getUserName(),
                            lc.getUserEmail(),
                            user.getUserId(),
                            new Date());
                });

        userDAO.updateUser(user.getDisplayName(), user.getUserId(), user.getInstitutionId());
    }

    private Boolean checkForValidInstitution(Integer institutionId) {
        Integer existingId = institutionDAO.checkForExistingInstitution(institutionId);
        return Objects.nonNull(existingId) && existingId > 0;
    }

}
