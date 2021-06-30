package org.broadinstitute.consent.http.service;

import com.google.inject.Inject;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.consent.http.db.InstitutionDAO;
import org.broadinstitute.consent.http.db.LibraryCardDAO;
import org.broadinstitute.consent.http.db.UserPropertyDAO;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.db.UserRoleDAO;
import org.broadinstitute.consent.http.db.VoteDAO;
import org.broadinstitute.consent.http.enumeration.RoleStatus;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.LibraryCard;
import org.broadinstitute.consent.http.models.UserProperty;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.models.Vote;
import org.broadinstitute.consent.http.service.users.handler.UserRolesHandler;
import org.jdbi.v3.core.statement.UnableToExecuteStatementException;

public class UserService {

    private final UserPropertyDAO userPropertyDAO;
    private final UserDAO userDAO;
    private final UserRoleDAO userRoleDAO;
    private final VoteDAO voteDAO;
    private final InstitutionDAO institutionDAO;
    private final LibraryCardDAO libraryCardDAO;

    @Inject
    public UserService(UserDAO userDAO, UserPropertyDAO userPropertyDAO, UserRoleDAO userRoleDAO, VoteDAO voteDAO, InstitutionDAO institutionDAO, LibraryCardDAO libraryCardDAO) {
        this.userDAO = userDAO;
        this.userPropertyDAO = userPropertyDAO;
        this.userRoleDAO = userRoleDAO;
        this.voteDAO = voteDAO;
        this.institutionDAO = institutionDAO;
        this.libraryCardDAO = libraryCardDAO;
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
        Integer dacUserID = userDAO.insertUser(user.getEmail(), user.getDisplayName(), new Date());
        insertUserRoles(user.getRoles(), dacUserID);
        addExistingLibraryCards(user);
        return userDAO.findUserById(dacUserID);
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

    public Collection<User> describeUsers() {
        return userDAO.findUsers();
    }

    public void deleteUserByEmail(String email) {
        User user = userDAO.findUserByEmail(email);
        if (user == null) {
            throw new NotFoundException("The user for the specified E-Mail address does not exist");
        }
        List<Integer> roleIds = userRoleDAO.
                findRolesByUserId(user.getDacUserId()).
                stream().
                map(UserRole::getRoleId).
                collect(Collectors.toList());
        if (!roleIds.isEmpty()) {
            userRoleDAO.removeUserRoles(user.getDacUserId(), roleIds);
        }
        List<Vote> votes = voteDAO.findVotesByUserId(user.getDacUserId());
        if (!votes.isEmpty()) {
            List<Integer> voteIds = votes.stream().map(Vote::getVoteId).collect(Collectors.toList());
            voteDAO.removeVotesByIds(voteIds);
        }
        userPropertyDAO.deleteAllPropertiesByUser(user.getDacUserId());
        userDAO.deleteUserByEmail(email);
    }

    public List<UserProperty> findAllUserProperties(Integer userId) {
        return userPropertyDAO.findResearcherPropertiesByUser(userId);
    }

    public List<User> describeAdminUsersThatWantToReceiveMails() {
        return userDAO.describeUsersByRoleAndEmailPreference(UserRoles.ADMIN.getRoleName(), true);
    }

    public User updateUserStatus(String status, Integer userId) {
        Integer statusId = RoleStatus.getValueByStatus(status);
        validateExistentUserById(userId);
        if (statusId == null) {
            throw new IllegalArgumentException(status + " is not a valid status.");
        }
        userDAO.updateUserStatus(statusId, userId);
        return userDAO.findUserById(userId);
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
            userDAO.updateUser(updatedUser.getDisplayName(), id, updatedUser.getAdditionalEmail(), updatedUser.getInstitutionId());
        } catch (UnableToExecuteStatementException e) {
            throw new IllegalArgumentException("Email shoud be unique.");
        }
        return userDAO.findUserById(id);
    }

    public void updateEmailPreference(boolean preference, Integer userId) {
        userDAO.updateEmailPreference(preference, userId);
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

    private void validateExistentUserById(Integer id) {
        if (userDAO.findUserById(id) == null) {
            throw new NotFoundException("The user for the specified id does not exist");
        }
    }

    public void insertUserRoles(List<UserRole> roles, Integer dacUserId) {
        roles.forEach(r -> {
            if (r.getRoleId() == null) {
                r.setRoleId(userRoleDAO.findRoleIdByName(r.getName()));
            }
        });
        userRoleDAO.insertUserRoles(roles, dacUserId);
    }

    private void addExistingLibraryCards(User user) {
        List<LibraryCard> libraryCards = libraryCardDAO.findAllLibraryCardsByUserEmail(user.getEmail());

        if (Objects.isNull(libraryCards) || libraryCards.isEmpty()) {
            return;
        }

        libraryCards
                .stream()
                .forEach(lc -> {
                    lc.setUserId(user.getDacUserId());

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
                            user.getDacUserId(),
                            new Date());
                });

        userDAO.updateUser(user.getDisplayName(), user.getDacUserId(), user.getAdditionalEmail(), user.getInstitutionId());
    }

    private Boolean checkForValidInstitution(Integer institutionId) {
        Integer existingId = institutionDAO.checkForExistingInstitution(institutionId);
        return Objects.nonNull(existingId) && existingId > 0;
    }

}
