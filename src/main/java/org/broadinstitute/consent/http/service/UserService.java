package org.broadinstitute.consent.http.service;

import com.google.inject.Inject;
import org.broadinstitute.consent.http.db.DACUserDAO;
import org.broadinstitute.consent.http.db.ResearcherPropertyDAO;
import org.broadinstitute.consent.http.db.UserRoleDAO;
import org.broadinstitute.consent.http.db.VoteDAO;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.ResearcherProperty;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.models.Vote;

import javax.ws.rs.NotFoundException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class UserService {

    private final DACUserDAO userDAO;
    private final ResearcherPropertyDAO researcherPropertyDAO;
    private final UserRoleDAO roleDAO;
    private final VoteDAO voteDAO;

    @Inject
    public UserService(DACUserDAO userDAO, ResearcherPropertyDAO researcherPropertyDAO, UserRoleDAO roleDAO, VoteDAO voteDAO) {
        this.userDAO = userDAO;
        this.researcherPropertyDAO = researcherPropertyDAO;
        this.roleDAO = roleDAO;
        this.voteDAO = voteDAO;
    }

    public User findUserById(Integer id) throws NotFoundException {
        User user = userDAO.findDACUserById(id);
        if (user == null) {
            throw new NotFoundException("Unable to find user with id: " + id);
        }
        user.setRoles(roleDAO.findRolesByUserId(user.getDacUserId()));
        return user;
    }

    public User findUserByEmail(String email) throws NotFoundException {
        User user = userDAO.findDACUserByEmail(email);
        if (user == null) {
            throw new NotFoundException("Unable to find user with email: " + email);
        }
        user.setRoles(roleDAO.findRolesByUserId(user.getDacUserId()));
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
        List<Integer> roleIds = roleDAO.
                findRolesByUserId(user.getDacUserId()).
                stream().
                map(UserRole::getRoleId).
                collect(Collectors.toList());
        if (!roleIds.isEmpty()) {
            roleDAO.removeUserRoles(user.getDacUserId(), roleIds);
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

}
