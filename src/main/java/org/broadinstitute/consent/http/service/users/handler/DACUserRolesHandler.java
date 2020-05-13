package org.broadinstitute.consent.http.service.users.handler;

import freemarker.template.TemplateException;
import org.apache.commons.collections.CollectionUtils;
import org.broadinstitute.consent.http.db.DACUserDAO;
import org.broadinstitute.consent.http.db.DataSetAssociationDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.db.UserRoleDAO;
import org.broadinstitute.consent.http.db.VoteDAO;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.enumeration.VoteType;
import org.broadinstitute.consent.http.models.DACUser;
import org.broadinstitute.consent.http.models.DatasetAssociation;
import org.broadinstitute.consent.http.models.Role;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.models.Vote;
import org.broadinstitute.consent.http.service.DataAccessRequestAPI;
import org.broadinstitute.consent.http.service.EmailNotifierService;

import javax.mail.MessagingException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DACUserRolesHandler extends AbstractUserRolesHandler {

    public static final String UPDATED_USER_KEY = "updatedUser";
    public static final String ALTERNATIVE_OWNER_KEY = "alternativeDataOwnerUser";

    private final DACUserDAO dacUserDAO;
    private final ElectionDAO electionDAO;
    private final VoteDAO voteDAO;
    private final UserRoleDAO userRoleDAO;
    private final DataSetAssociationDAO datasetAssociationDAO;
    private final String MEMBER = UserRoles.MEMBER.getRoleName();
    private final String DATA_OWNER = UserRoles.DATAOWNER.getRoleName();
    private final Map<String, Integer> roleIdMap;
    private final EmailNotifierService emailNotifierService;
    private final DataAccessRequestAPI dataAccessRequestAPI;

    private final UserRole admin = new UserRole(UserRoles.ADMIN.getRoleId(), UserRoles.ADMIN.getRoleName());
    private final UserRole alumni = new UserRole(UserRoles.ALUMNI.getRoleId(), UserRoles.ALUMNI.getRoleName());
    private final UserRole researcher = new UserRole(UserRoles.RESEARCHER.getRoleId(), UserRoles.RESEARCHER.getRoleName());
    private final UserRole dataOwner = new UserRole(UserRoles.DATAOWNER.getRoleId(), UserRoles.DATAOWNER.getRoleName());

    public DACUserRolesHandler(DACUserDAO userDao, UserRoleDAO roleDAO, ElectionDAO electionDAO, VoteDAO voteDAO, DataSetAssociationDAO datasetAssociationDAO, EmailNotifierService emailNotifierService, DataAccessRequestAPI dataAccessRequestAPI) {
        this.dacUserDAO = userDao;
        this.electionDAO = electionDAO;
        this.userRoleDAO = roleDAO;
        this.voteDAO = voteDAO;
        this.datasetAssociationDAO = datasetAssociationDAO;
        this.roleIdMap = createRoleMap(userRoleDAO.findRoles());
        this.emailNotifierService = emailNotifierService;
        this.dataAccessRequestAPI = dataAccessRequestAPI;
    }

    public static void initInstance(DACUserDAO userDao, UserRoleDAO roleDAO, ElectionDAO electionDAO, VoteDAO voteDAO, DataSetAssociationDAO datasetAssociationDAO, EmailNotifierService emailNotifierService, DataAccessRequestAPI dataAccessRequestAPI) {
        UserHandlerAPIHolder.setInstance(new DACUserRolesHandler(userDao, roleDAO, electionDAO, voteDAO, datasetAssociationDAO, emailNotifierService, dataAccessRequestAPI));
    }

    private Map<String, Integer> createRoleMap(List<Role> roles) {
        Map<String, Integer> rolesMap = new HashMap<>();
        roles.forEach((r) -> rolesMap.put(r.getName().toUpperCase(), r.getRoleId()));
        return rolesMap;
    }

    /**
     * Receives a map of users and decides what to do with them. If we won't
     * delegate, there'll only be one parameter, 'updatedUser' If we delegate
     * data ownership, there will a map entry for 'alternativeDataOwnerUser'.
     *
     * @param usersMap Map with the users we need to update. Entry: User
     *                 Identification(explained above) -> DacUser
     */
    @Override
    public void updateRoles(Map<String, DACUser> usersMap) throws MessagingException, IOException, TemplateException {
        DACUser updatedUser;
        DACUser doUserToDelegate = null;

        try {
            userRoleDAO.begin();
            voteDAO.begin();
            boolean delegateOwner = usersMap.containsKey(ALTERNATIVE_OWNER_KEY);
            updatedUser = usersMap.get(UPDATED_USER_KEY);
            // roles as should be ..
            List<UserRole> updatedRoles = updatedUser.getRoles();

            // roles as currently are ...
            List<UserRole> originalRoles = userRoleDAO.findRolesByUserId(updatedUser.getDacUserId());

            // roles required to remove ...
            List<UserRole> rolesToRemove = substractAllRoles(originalRoles, updatedRoles);

            // roles to add ..
            List<UserRole> rolesToAdd = substractAllRoles(updatedRoles, originalRoles);

            // If there aren't any open elections and we didn't delegate to any owner then we don't have to validate anything.
            if (electionDAO.verifyOpenElections() == 0 && !delegateOwner) {
                changeRolesWithoutDelegation(updatedUser, rolesToRemove, rolesToAdd);
            }
            if (delegateOwner) {
                doUserToDelegate = dacUserDAO.findDACUserByEmail(usersMap.get("alternativeDataOwnerUser").getEmail());
                doUserToDelegate.setRoles(userRoleDAO.findRolesByUserId(doUserToDelegate.getDacUserId()));
            }
            // removing deleted roles
            for (UserRole role : rolesToRemove) {
                switch (UserRoles.valueOf(role.getName().toUpperCase())) {

                    case CHAIRPERSON:
                        // Chairpersons are only removed via Dac administration
                        break;
                    case MEMBER:
                        // Members are only removed via Dac administration
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
                        removeDataOwner(updatedUser, delegateOwner, doUserToDelegate);
                        break;
                }
            }
            // adding new roles
            for (UserRole role : rolesToAdd) {
                switch (UserRoles.valueOf(role.getName().toUpperCase())) {
                    case CHAIRPERSON:
                        // Chairpersons are only added via Dac administration
                        break;
                    case MEMBER:
                        // Members are only added via Dac administration
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
            userRoleDAO.commit();
            voteDAO.commit();
        } catch (Exception e) {
            userRoleDAO.rollback();
            voteDAO.rollback();
            throw e;
        }
    }

    /* Removes data owners role, assigns votes (update) if needed, then removes
     * and assigns the role.
     */
    private void removeDataOwner(DACUser updatedUser, boolean delegate, DACUser doUserToDelegate) throws MessagingException, IOException, TemplateException {
        userRoleDAO.removeSingleUserRole(updatedUser.getDacUserId(), dataOwner.getRoleId());
        List<Integer> openElectionIdsForThisUser = electionDAO.findDataSetOpenElectionIds(updatedUser.getDacUserId());
        if (delegate) {
            assignNewRole(doUserToDelegate, dataOwner);
            verifyAndDelegateElections(updatedUser, doUserToDelegate, openElectionIdsForThisUser, VoteType.DATA_OWNER.getValue());
            updateDataSetsOwnership(updatedUser, doUserToDelegate);
        } else {
            verifyAndDelegateElections(updatedUser, doUserToDelegate, openElectionIdsForThisUser, VoteType.DATA_OWNER.getValue());
            deleteDatasetsOwnership(updatedUser);
        }
    }

    private void verifyAndDelegateElections(DACUser updatedUser, DACUser doUserToDelegate, List<Integer> openElectionIdsForThisUser, String voteType) throws MessagingException, IOException, TemplateException {
        List<Integer> electionsIdToDelegateVotes = new ArrayList<>();
        List<Integer> electionsIdToRemoveVotes = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(openElectionIdsForThisUser)) {
            openElectionIdsForThisUser.forEach(electionId -> {
                List<Vote> vote = voteDAO.findVotesByTypeAndElectionIds(new ArrayList<>(Collections.singletonList(electionId)), voteType);
                if (vote.size() == 1) {
                    electionsIdToDelegateVotes.add(electionId);
                } else {
                    electionsIdToRemoveVotes.add(electionId);
                }
            });
            removeVotes(updatedUser, electionsIdToRemoveVotes);
            delegateVotes(updatedUser, doUserToDelegate, electionsIdToDelegateVotes);
            emailNotifierService.sendUserDelegateResponsibilitiesMessage(
                    doUserToDelegate,
                    updatedUser.getDacUserId(),
                    voteType.equalsIgnoreCase(VoteType.DATA_OWNER.getValue())? DATA_OWNER: MEMBER,
                    voteDAO.findVotesByElectionIdsAndUser(electionsIdToDelegateVotes, doUserToDelegate.getDacUserId()));
        }
    }

    private void removeVotes(DACUser updatedUser, List<Integer> electionsIdToRemoveVotes) {
        if (CollectionUtils.isNotEmpty(electionsIdToRemoveVotes)) {
            voteDAO.removeVotesByElectionIdAndUser(electionsIdToRemoveVotes, updatedUser.getDacUserId());
        }
    }

    private void delegateVotes(DACUser updatedUser, DACUser doUserToDelegate, List<Integer> electionsIdToDelegateVotes) {
        if (CollectionUtils.isNotEmpty(electionsIdToDelegateVotes)) {
            voteDAO.delegateDataSetOpenElectionsVotes(updatedUser.getDacUserId(), electionsIdToDelegateVotes, doUserToDelegate.getDacUserId());
        }
    }

    private void updateDataSetsOwnership(DACUser updatedUser, DACUser userToDelegate) {
        List<DatasetAssociation> associations = datasetAssociationDAO.findAllDatasetAssociationsByOwnerId(updatedUser.getDacUserId());
        List<DatasetAssociation> newAssociations = new ArrayList<>();
        associations.forEach((as) -> {
            if (!as.getDacuserId().equals(userToDelegate.getDacUserId())) {
                newAssociations.add(new DatasetAssociation(as.getDatasetId(), userToDelegate.getDacUserId()));
            }
        });
        deleteDatasetsOwnership(updatedUser);
        datasetAssociationDAO.insertDatasetUserAssociation(newAssociations);
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
        List<String> referenceIds = dataAccessRequestAPI.describeDataAccessIdsForOwner(updatedUser.getDacUserId());
        if(!CollectionUtils.isEmpty(referenceIds)){
            electionDAO.bulkCancelOpenElectionByReferenceIdAndType(ElectionType.DATA_ACCESS.getValue(), referenceIds);
            electionDAO.bulkCancelOpenElectionByReferenceIdAndType(ElectionType.RP.getValue(), referenceIds);
        }
        for(String referenceId: referenceIds){
            dataAccessRequestAPI.cancelDataAccessRequest(referenceId);
        }
        userRoleDAO.removeSingleUserRole(updatedUser.getDacUserId(), researcher.getRoleId());
    }

    /**
     * Assigns userToAssignRole the role sent as a parameter, if he does not
     * have it yet.
     * @param userToAssignRole User whose roles will be updated.
     * @param role New role to add to @userToAssignRole .
     */
    private void assignNewRole(DACUser userToAssignRole, UserRole role) {
        List<UserRole> roles = userRoleDAO.findRolesByUserId(userToAssignRole.getDacUserId());
        if (!containsRole(roles, role.getName())) {
            List<UserRole> newRoles = new ArrayList<>();
            newRoles.add(role);
            userRoleDAO.insertUserRoles(newRoles, userToAssignRole.getDacUserId());
        }
    }

    private List<UserRole> generateRoleIdList(List<UserRole> roleList) {
        roleList.forEach((r) -> r.setRoleId(roleIdMap.get(r.getName().toUpperCase())));
        return roleList;
    }

    private void deleteDatasetsOwnership(DACUser updatedUser) {
        datasetAssociationDAO.deleteDatasetRelationshipsForUser(updatedUser.getDacUserId());
    }


    private void changeRolesWithoutDelegation(DACUser updatedUser, List<UserRole> removedRoles, List<UserRole> newRoles) {
        if (CollectionUtils.isNotEmpty(removedRoles)) {
            userRoleDAO.removeUserRoles(updatedUser.getDacUserId(),
                    removedRoles.stream().map(UserRole::getRoleId).collect(Collectors.toList()));
        }
        if (CollectionUtils.isNotEmpty(newRoles)) {
            userRoleDAO.insertUserRoles(generateRoleIdList(newRoles), updatedUser.getDacUserId());
        }
    }

    private List<UserRole> substractAllRoles(List<UserRole> roles, List<UserRole> toSubstractRoles) {
        List<String> toSubstractRolesNames = toSubstractRoles.stream().map(role -> role.getName().toUpperCase()).collect(Collectors.toList());
        return roles.stream().filter(rol -> !toSubstractRolesNames.contains(rol.getName().toUpperCase())).collect(Collectors.toList());
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
