package org.broadinstitute.consent.http.service;

import com.google.inject.Inject;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.consent.http.db.ResearcherPropertyDAO;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.db.UserRoleDAO;
import org.broadinstitute.consent.http.db.VoteDAO;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.ResearcherProperty;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.models.Vote;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class UserService {

    private final ResearcherPropertyDAO researcherPropertyDAO;
    private final UserDAO userDAO;
    private final UserRoleDAO userRoleDAO;
    private final VoteDAO voteDAO;

    @Inject
    public UserService(UserDAO userDAO, ResearcherPropertyDAO researcherPropertyDAO, UserRoleDAO userRoleDAO, VoteDAO voteDAO) {
        this.userDAO = userDAO;
        this.researcherPropertyDAO = researcherPropertyDAO;
        this.userRoleDAO = userRoleDAO;
        this.voteDAO = voteDAO;
    }

    public User createUser(User user) {
        validateDisplayName(user.getDisplayName());
        validateEmail(user.getEmail());
        validateRoles(user.getRoles());
        Integer dacUserID;
        User existingUser = userDAO.findDACUserByEmail(user.getEmail());
        if (Objects.nonNull(existingUser)) {
            throw new BadRequestException("User exists with this email address: " + user.getEmail());
        }
        dacUserID = userDAO.insertDACUser(user.getEmail(), user.getDisplayName(), new Date());
        if (user.getRoles() != null) {
            insertUserRoles(user, dacUserID);
        }
        User foundUser = userDAO.findDACUserById(dacUserID);
        foundUser.setRoles(userRoleDAO.findRolesByUserId(user.getDacUserId()));
        return foundUser;
    }

    public User findUserById(Integer id) throws NotFoundException {
        User user = userDAO.findDACUserById(id);
        if (user == null) {
            throw new NotFoundException("Unable to find user with id: " + id);
        }
        user.setRoles(userRoleDAO.findRolesByUserId(user.getDacUserId()));
        return user;
    }

    public User findUserByEmail(String email) throws NotFoundException {
        User user = userDAO.findDACUserByEmail(email);
        if (user == null) {
            throw new NotFoundException("Unable to find user with email: " + email);
        }
        user.setRoles(userRoleDAO.findRolesByUserId(user.getDacUserId()));
        return user;
    }

    public Collection<User> describeUsers() {
        return userDAO.findUsers();
    }

    public void deleteUserByEmail(String email) {
        User user = userDAO.findDACUserByEmail(email);
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
        researcherPropertyDAO.deleteAllPropertiesByUser(user.getDacUserId());
        userDAO.deleteDACUserByEmail(email);
    }

    public List<ResearcherProperty> findAllUserProperties(Integer userId) {
        return researcherPropertyDAO.findResearcherPropertiesByUser(userId);
    }

    private void validateDisplayName(String displayName) {
        if (StringUtils.isEmpty(displayName)) {
            throw new BadRequestException("Display Name can't be null. The user needs a name to display.");
        }
    }

    private void validateEmail(String emailToValidate) {
        if (StringUtils.isEmpty(emailToValidate)) {
            throw new BadRequestException("Email address cannot be empty");
        }
    }

    private void validateRoles(List<UserRole> roles) {
        if (CollectionUtils.isNotEmpty(roles)) {
            roles.forEach(role -> {
                if (!(role.getName().equalsIgnoreCase(UserRoles.DATAOWNER.getRoleName())
                        || role.getName().equalsIgnoreCase(UserRoles.RESEARCHER.getRoleName()))) {
                    throw new BadRequestException("Invalid role: " + role.getName() + ". Valid roles are: " + UserRoles.DATAOWNER.getRoleName() + " and " + UserRoles.RESEARCHER.getRoleName());
                }
            });
        } else {
            throw new BadRequestException("Roles are required.");
        }
    }

    private void insertUserRoles(User user, Integer dacUserId) {
        List<UserRole> roles = user.getRoles();
        roles.forEach(r -> {
            if (r.getRoleId() == null) {
                r.setRoleId(userRoleDAO.findRoleIdByName(r.getName()));
            }
        });
        userRoleDAO.insertUserRoles(roles, dacUserId);
    }

}
