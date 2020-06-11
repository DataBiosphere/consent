package org.broadinstitute.consent.http.service;

import com.google.inject.Inject;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
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
        User createdUser = userDAO.findUserById(dacUserID);
        createdUser.setRoles(userRoleDAO.findRolesByUserId(user.getDacUserId()));
        return createdUser;
    }

    public User findUserById(Integer id) throws NotFoundException {
        User user = userDAO.findUserById(id);
        if (user == null) {
            throw new NotFoundException("Unable to find user with id: " + id);
        }
        user.setRoles(userRoleDAO.findRolesByUserId(user.getDacUserId()));
        return user;
    }

    public User findUserByEmail(String email) throws NotFoundException {
        User user = userDAO.findUserByEmail(email);
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
        researcherPropertyDAO.deleteAllPropertiesByUser(user.getDacUserId());
        userDAO.deleteUserByEmail(email);
    }

    public List<ResearcherProperty> findAllUserProperties(Integer userId) {
        return researcherPropertyDAO.findResearcherPropertiesByUser(userId);
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

    private void insertUserRoles(List<UserRole> roles, Integer dacUserId) {
        roles.forEach(r -> {
            if (r.getRoleId() == null) {
                r.setRoleId(userRoleDAO.findRoleIdByName(r.getName()));
            }
        });
        userRoleDAO.insertUserRoles(roles, dacUserId);
    }

}
