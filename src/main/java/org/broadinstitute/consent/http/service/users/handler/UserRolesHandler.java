package org.broadinstitute.consent.http.service.users.handler;

import com.google.inject.Inject;
import org.apache.commons.collections.CollectionUtils;
import org.broadinstitute.consent.http.db.DACUserDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.db.UserRoleDAO;
import org.broadinstitute.consent.http.db.VoteDAO;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.enumeration.VoteType;
import org.broadinstitute.consent.http.models.DACUser;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.models.Vote;
import org.broadinstitute.consent.http.service.DataAccessRequestService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class UserRolesHandler {

    public static final String UPDATED_USER_KEY = "updatedUser";

    private final DACUserDAO dacUserDAO;
    private final DataAccessRequestService dataAccessRequestService;
    private final ElectionDAO electionDAO;
    private final UserRoleDAO userRoleDAO;
    private final VoteDAO voteDAO;

    private final UserRole admin = new UserRole(UserRoles.ADMIN.getRoleId(), UserRoles.ADMIN.getRoleName());
    private final UserRole alumni = new UserRole(UserRoles.ALUMNI.getRoleId(), UserRoles.ALUMNI.getRoleName());
    private final UserRole researcher = new UserRole(UserRoles.RESEARCHER.getRoleId(), UserRoles.RESEARCHER.getRoleName());
    private final UserRole dataOwner = new UserRole(UserRoles.DATAOWNER.getRoleId(), UserRoles.DATAOWNER.getRoleName());

    @Inject
    public UserRolesHandler(DACUserDAO userDao, DataAccessRequestService dataAccessRequestService, ElectionDAO electionDAO, UserRoleDAO roleDAO, VoteDAO voteDAO) {
        this.dacUserDAO = userDao;
        this.dataAccessRequestService = dataAccessRequestService;
        this.electionDAO = electionDAO;
        this.userRoleDAO = roleDAO;
        this.voteDAO = voteDAO;
    }

    /**
     * Update a user's roles based on what they currently have vs what is provided here. Note that we cannot
     * update Chair or Member roles - that has to happen in conjunction with DAC votes (see DacService) so we
     * don't perform any operations on those roles. We also don't want to error out here and allow for the case
     * where a Chair or Member wants to add a new role, remove an old role, etc., without impacting their current
     * chair/member status.
     *
     * @param updatedUser User we need to update.
     */
    public void updateRoles(DACUser updatedUser) {
        // Roles as should be ..
        List<UserRole> updatedRoles = updatedUser.getRoles();
        List<Integer> updatedRoleIds = updatedRoles.stream().map(UserRole::getRoleId).collect(Collectors.toList());

        // Roles as currently are ...
        List<UserRole> originalRoles = userRoleDAO.findRolesByUserId(updatedUser.getDacUserId());
        List<Integer> originalRoleIds = originalRoles.stream().map(UserRole::getRoleId).collect(Collectors.toList());

        // Roles to remove: Any original role that does not exist in the new role list
        List<UserRole> rolesToRemove = originalRoles.stream().
                filter(r -> !updatedRoleIds.contains(r.getRoleId())).
                collect(Collectors.toList());


        // Roles to add: Any new role that does not exist in the original role list
        List<UserRole> rolesToAdd = updatedRoles.stream().
                filter(r -> !originalRoleIds.contains(r.getRoleId())).
                collect(Collectors.toList());

        // Remove deleted roles
        for (UserRole role : rolesToRemove) {
            switch (UserRoles.valueOf(role.getName().toUpperCase())) {
                case CHAIRPERSON:
                    // No-op, we cannot update chairperson roles here.
                    break;
                case MEMBER:
                    // No-op, we cannot update member roles here.
                    break;
                case ALUMNI:
                    removeAlumni(updatedUser);
                    break;
                case ADMIN:
                    removeAdmin(updatedUser);
                    break;
                case RESEARCHER:
                    removeResearcher(updatedUser);
                    break;
                case DATAOWNER:
                    removeDataOwner(updatedUser);
                    break;
            }
        }

        // Add new roles
        for (UserRole role : rolesToAdd) {
            switch (UserRoles.valueOf(role.getName().toUpperCase())) {
                case CHAIRPERSON:
                    // No-op, we cannot update chairperson roles here.
                    break;
                case MEMBER:
                    // No-op, we cannot update member roles here.
                    break;
                case ALUMNI:
                    assignNewRole(updatedUser, alumni);
                    break;
                case ADMIN:
                    assignNewRole(updatedUser, admin);
                    break;
                case RESEARCHER:
                    assignNewRole(updatedUser, researcher);
                    break;
                case DATAOWNER:
                    assignNewRole(updatedUser, dataOwner);
                    break;
            }
        }
    }

    /* Removes data owners role, assigns votes (update) if needed, then removes
     * and assigns the role.
     */
    private void removeDataOwner(DACUser updatedUser) {
        userRoleDAO.removeSingleUserRole(updatedUser.getDacUserId(), dataOwner.getRoleId());
        List<Integer> openElectionIdsForThisUser = electionDAO.findDataSetOpenElectionIds(updatedUser.getDacUserId());
        removeDataOwnerVotes(updatedUser, openElectionIdsForThisUser);
    }

    private void removeDataOwnerVotes(DACUser updatedUser, List<Integer> openElectionIdsForThisUser) {
        List<Integer> dataOwnerVoteIds = voteDAO.findVotesByUserId(updatedUser.getDacUserId()).stream().
                filter(v -> v.getType().equalsIgnoreCase(VoteType.DATA_OWNER.getValue())).
                filter(v -> openElectionIdsForThisUser.contains(v.getElectionId())).
                map(Vote::getVoteId).
                collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(dataOwnerVoteIds)) {
            voteDAO.removeVotesByIds(dataOwnerVoteIds);
        }
    }

    /**
     * Removes admin role from updatedUser.
     *
     * @param updatedUser The user to update
     */
    private void removeAdmin(DACUser updatedUser) {
        if (dacUserDAO.verifyAdminUsers() < 2) {
            throw new IllegalArgumentException("At least one user with Admin roles should exist.");
        }
        userRoleDAO.removeSingleUserRole(updatedUser.getDacUserId(), admin.getRoleId());
    }

    /**
     * Removes alumni role from updatedUser.
     *
     * @param updatedUser The user to update
     */
    private void removeAlumni(DACUser updatedUser) {
        userRoleDAO.removeSingleUserRole(updatedUser.getDacUserId(), alumni.getRoleId());
    }

    /**
     * Removes researcher role from updatedUser.
     *
     * @param updatedUser The user to update
     */
    private void removeResearcher(DACUser updatedUser) {
        // Find list of related dars
        List<String> referenceIds = dataAccessRequestService.findAllDataAccessRequests().stream().
                filter(d -> Objects.nonNull(d.getData())).
                filter(d -> Objects.nonNull(d.getData().getUserId())).
                filter(d -> d.getData().getUserId().equals(updatedUser.getDacUserId())).
                map(DataAccessRequest::getReferenceId).
                collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(referenceIds)) {
            electionDAO.bulkCancelOpenElectionByReferenceIdAndType(ElectionType.DATA_ACCESS.getValue(), referenceIds);
            electionDAO.bulkCancelOpenElectionByReferenceIdAndType(ElectionType.RP.getValue(), referenceIds);
        }
        for (String referenceId : referenceIds) {
            dataAccessRequestService.cancelDataAccessRequest(referenceId);
        }
        userRoleDAO.removeSingleUserRole(updatedUser.getDacUserId(), researcher.getRoleId());
    }

    /**
     * Assigns userToAssignRole the role sent as a parameter, if he does not
     * have it yet.
     *
     * @param userToAssignRole User whose roles will be updated.
     * @param role             New role to add to @userToAssignRole .
     */
    private void assignNewRole(DACUser userToAssignRole, UserRole role) {
        List<UserRole> roles = userRoleDAO.findRolesByUserId(userToAssignRole.getDacUserId());
        if (!containsRole(roles, role.getName())) {
            List<UserRole> newRoles = new ArrayList<>();
            newRoles.add(role);
            userRoleDAO.insertUserRoles(newRoles, userToAssignRole.getDacUserId());
        }
    }

    public boolean containsRole(Collection<UserRole> roles, String role) {
        return roles.stream().anyMatch(r -> r.getName().equalsIgnoreCase(role));
    }

    public boolean containsAnyRole(Collection<UserRole> roles, String[] rolesToMatch) {
        for (String role : rolesToMatch) {
            if (containsRole(roles, role)) {
                return true;
            }
        }
        return false;
    }

}
