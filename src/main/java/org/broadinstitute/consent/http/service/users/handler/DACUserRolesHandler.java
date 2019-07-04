package org.broadinstitute.consent.http.service.users.handler;

import freemarker.template.TemplateException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.mail.MessagingException;
import org.apache.commons.collections.CollectionUtils;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.db.UserRoleDAO;
import org.broadinstitute.consent.http.db.DataSetAssociationDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.db.VoteDAO;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.enumeration.ElectionStatus;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.enumeration.VoteType;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.models.DatasetAssociation;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.Role;
import org.broadinstitute.consent.http.models.Vote;
import org.broadinstitute.consent.http.service.DataAccessRequestAPI;
import org.broadinstitute.consent.http.service.EmailNotifierAPI;

public class DACUserRolesHandler extends AbstractUserRolesHandler {

    private final UserDAO userDAO;
    private final ElectionDAO electionDAO;
    private final VoteDAO voteDAO;
    private final UserRoleDAO userRoleDAO;
    private final DataSetAssociationDAO datasetAssociationDAO;
    private final String MEMBER = UserRoles.MEMBER.getValue();
    private final String ADMIN = UserRoles.ADMIN.getValue();
    private final String CHAIRPERSON = UserRoles.CHAIRPERSON.getValue();
    private final String RESEARCHER = UserRoles.RESEARCHER.getValue();
    private final String DATA_OWNER = UserRoles.DATAOWNER.getValue();
    private final String ALUMNI = UserRoles.ALUMNI.getValue();
    private final Map<String, Integer> roleIdMap;
    private final EmailNotifierAPI emailNotifierAPI;
    private final DataAccessRequestAPI dataAccessRequestAPI;


    public DACUserRolesHandler(UserDAO userDao, UserRoleDAO roleDAO, ElectionDAO electionDAO, VoteDAO voteDAO, DataSetAssociationDAO datasetAssociationDAO, EmailNotifierAPI emailNotifierAPI, DataAccessRequestAPI dataAccessRequestAPI) {
        this.userDAO = userDao;
        this.electionDAO = electionDAO;
        this.userRoleDAO = roleDAO;
        this.voteDAO = voteDAO;
        this.datasetAssociationDAO = datasetAssociationDAO;
        this.roleIdMap = createRoleMap(userRoleDAO.findRoles());
        this.emailNotifierAPI = emailNotifierAPI;
        this.dataAccessRequestAPI = dataAccessRequestAPI;
    }

    public static void initInstance(UserDAO userDao, UserRoleDAO roleDAO, ElectionDAO electionDAO, VoteDAO voteDAO, DataSetAssociationDAO datasetAssociationDAO, EmailNotifierAPI emailNotifierAPI, DataAccessRequestAPI dataAccessRequestAPI) {
        UserHandlerAPIHolder.setInstance(new DACUserRolesHandler(userDao, roleDAO, electionDAO, voteDAO, datasetAssociationDAO, emailNotifierAPI, dataAccessRequestAPI));
    }

    private Map<String, Integer> createRoleMap(List<Role> roles) {
        Map<String, Integer> rolesMap = new HashMap();
        roles.stream().forEach((r) -> rolesMap.put(r.getName().toUpperCase(), r.getRoleId()));
        return rolesMap;
    }

    /**
     * Receives a map of users and decides what to do with them. If we won't
     * delegate, there'll only be one parameter, 'updatedUser' If we delegate
     * member or chair, there will be also an 'userToDelegate' If we delegate
     * data ownership, there will a third member in the map,
     * 'alternativeDataOwnerUser'.
     *
     * @param usersMap Map with the members we need to update. Entry: User
     *                 Identification(explained above) -> DacUser
     */
    @Override
    public void updateRoles(Map<String, User> usersMap) throws UserRoleHandlerException, MessagingException, IOException, TemplateException {
        User updatedUser;
        User userToDelegate = null;
        User doUserToDelegate = null;

        try {
            userRoleDAO.begin();
            voteDAO.begin();
            boolean delegateMember = usersMap.containsKey("userToDelegate");
            boolean delegateOwner = usersMap.containsKey("alternativeDataOwnerUser");
            updatedUser = usersMap.get("updatedUser");
            // roles as should be ..
            List<UserRole> updatedRoles = updatedUser.getRoles();

            // roles as currently are ...
            List<UserRole> originalRoles = userRoleDAO.findRolesByUserId(updatedUser.getUserId());

            // roles required to remove ...
            List<UserRole> rolesToRemove = substractAllRoles(originalRoles, updatedRoles);

            // roles to add ..
            List<UserRole> rolesToAdd = substractAllRoles(updatedRoles, originalRoles);


            updateAdminEmailPreference(originalRoles,updatedRoles,updatedUser.getUserId());

            // If there aren't any open elections and we didn't delegate to any member then we don't have to validate anything.
            if (electionDAO.verifyOpenElections() == 0 && !delegateMember && !delegateOwner) {
                changeRolesWithoutDelegation(updatedUser, rolesToRemove, rolesToAdd, delegateMember);
            }
            if (delegateMember) {
                userToDelegate = userDAO.findDACUserByEmail(usersMap.get("userToDelegate").getEmail());
                userToDelegate.setRoles(userRoleDAO.findRolesByUserId(userToDelegate.getUserId()));
            }
            if (delegateOwner) {
                doUserToDelegate = userDAO.findDACUserByEmail(usersMap.get("alternativeDataOwnerUser").getEmail());
                doUserToDelegate.setRoles(userRoleDAO.findRolesByUserId(doUserToDelegate.getUserId()));
            }
            // removing deleted roles
            for (UserRole role : rolesToRemove) {
                switch (UserRoles.valueOf(role.getName().toUpperCase())) {

                    case CHAIRPERSON:
                        changeChairPerson(updatedUser, delegateMember, userToDelegate);
                        break;
                    case MEMBER:
                        changeDacMember(updatedUser, delegateMember, userToDelegate);
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
                        addChairPerson(updatedUser);
                        break;
                    case MEMBER:
                        assignNewRole(updatedUser, new UserRole(roleIdMap.get(MEMBER), MEMBER, true));
                        break;
                    case ALUMNI:
                        if (containsAnyRole(updatedRoles, new String[]{MEMBER, CHAIRPERSON})) {
                            throw new UserRoleHandlerException("User to delegate: " + (userToDelegate != null ? userToDelegate.getDisplayName() : null) + " has a role: "
                                    + " [Member] or [Chairperson] that is incompatible with the role you want to assign.");
                        } else {
                            assignNewRole(updatedUser, new UserRole(roleIdMap.get(ALUMNI), ALUMNI, true));
                        }
                        break;
                    case ADMIN:
                        assignNewRole(updatedUser, new UserRole(roleIdMap.get(ADMIN), ADMIN, role.getEmailPreference() == null ? true : role.getEmailPreference()));
                        break;
                    case RESEARCHER:
                        if (containsAnyRole(updatedRoles, new String[]{MEMBER, CHAIRPERSON})) {
                            assert userToDelegate != null;
                            throw new UserRoleHandlerException("User to delegate: " + (userToDelegate != null ? userToDelegate.getDisplayName() : null) + " has a role: "
                                    + " [Member] or [Chairperson] that is incompatible with the role you want to assign.");
                        } else {
                            assignNewRole(updatedUser, new UserRole(roleIdMap.get(RESEARCHER), RESEARCHER, true));
                        }
                        break;
                    case DATAOWNER:
                        assignNewRole(updatedUser, new UserRole(roleIdMap.get(DATA_OWNER), DATA_OWNER, true));
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

    private void updateAdminEmailPreference(List<UserRole> originalRoles, List<UserRole> updatedRoles, Integer dacUserId) throws UserRoleHandlerException {
        try{
            boolean isAdminRole = originalRoles.stream().anyMatch(r -> r.getName().equalsIgnoreCase(ADMIN));
            List<UserRole> newAdminRole =  updatedRoles.stream().filter(r -> r.getName().equalsIgnoreCase(ADMIN)).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(newAdminRole)) {
                return;
            }
            UserRole updatedAdminRole = newAdminRole.get(0);
            if(isAdminRole){
                userRoleDAO.updateEmailPreferenceUserRole(updatedAdminRole, dacUserId);
            }
        } catch (Exception e){
            throw new UserRoleHandlerException("Problem occurred while updating the user email preference.");
        }
    }


    /* Removes data owners role, assigns votes (update) if needed, then removes
     * and assigns the role.
     */
    private void removeDataOwner(User updatedUser, boolean delegate, User doUserToDelegate) throws UserRoleHandlerException, MessagingException, IOException, TemplateException {
        removeRole(updatedUser.getUserId(), DATA_OWNER);
        List<Integer> openElectionIdsForThisUser = electionDAO.findDataSetOpenElectionIds(updatedUser.getUserId());
        if (delegate) {
            assignNewRole(doUserToDelegate, new UserRole(roleIdMap.get(DATA_OWNER), DATA_OWNER, true));
            verifyAndDelegateElections(updatedUser, doUserToDelegate, openElectionIdsForThisUser, 1, VoteType.DATA_OWNER.getValue());
            updateDataSetsOwnership(updatedUser, doUserToDelegate);
        } else {
            verifyAndDelegateElections(updatedUser, doUserToDelegate, openElectionIdsForThisUser, 1, VoteType.DATA_OWNER.getValue());
            deleteDatasetsOwnership(updatedUser);
        }
    }

    private void verifyAndDelegateElections(User updatedUser, User doUserToDelegate, List<Integer> openElectionIdsForThisUser, Integer cantVotes, String voteType) throws MessagingException, IOException, TemplateException {
        List<Integer> electionsIdToDelegateVotes = new ArrayList<>();
        List<Integer> electionsIdToRemoveVotes = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(openElectionIdsForThisUser)) {
            openElectionIdsForThisUser.stream().forEach(electionId -> {
                List<Vote> vote = voteDAO.findVotesByTypeAndElectionIds(new ArrayList<>(Arrays.asList(electionId)), voteType);
                if (vote.size() == cantVotes) {
                    electionsIdToDelegateVotes.add(electionId);
                } else {
                    electionsIdToRemoveVotes.add(electionId);
                }
            });
            removeVotes(updatedUser, electionsIdToRemoveVotes);
            delegateVotes(updatedUser, doUserToDelegate, electionsIdToDelegateVotes);
            emailNotifierAPI.sendUserDelegateResponsibilitiesMessage(doUserToDelegate, updatedUser.getUserId(), voteType.equalsIgnoreCase(VoteType.DATA_OWNER.getValue())? DATA_OWNER: MEMBER, voteDAO.findVotesByElectionIdsAndUser(electionsIdToDelegateVotes, doUserToDelegate.getUserId()));
        }
    }

    private void removeVotes(User updatedUser, List<Integer> electionsIdToRemoveVotes) {
        if (CollectionUtils.isNotEmpty(electionsIdToRemoveVotes)) {
            voteDAO.removeVotesByElectionIdAndUser(electionsIdToRemoveVotes, updatedUser.getUserId());
        }
    }

    private void delegateVotes(User updatedUser, User doUserToDelegate, List<Integer> electionsIdToDelegateVotes) {
        if (CollectionUtils.isNotEmpty(electionsIdToDelegateVotes)) {
            voteDAO.delegateDataSetOpenElectionsVotes(updatedUser.getUserId(), electionsIdToDelegateVotes, doUserToDelegate.getUserId());
        }
    }

    private void removeVotes(List<Integer> voteIds) {
        if (CollectionUtils.isNotEmpty(voteIds)) {
            voteDAO.removeVotesById(voteIds);
        }
    }

    private void updateDataSetsOwnership(User updatedUser, User userToDelegate) {
        List<DatasetAssociation> associations = datasetAssociationDAO.findAllDatasetAssociationsByOwnerId(updatedUser.getUserId());
        List<DatasetAssociation> newAssociations = new ArrayList<>();
        associations.stream().forEach((as) -> {
            if (!as.getDacuserId().equals(userToDelegate.getUserId())) {
                newAssociations.add(new DatasetAssociation(as.getDatasetId(), userToDelegate.getUserId()));
            }
        });
        deleteDatasetsOwnership(updatedUser);
        datasetAssociationDAO.insertDatasetUserAssociation(newAssociations);
    }

    private void delegateChairPersonVotes(Integer fromDacUserId, Integer toDacUSerId) {
        voteDAO.delegateChairPersonOpenElectionsVotes(fromDacUserId, toDacUSerId);
    }

    private void addChairPerson(User newChairperson) throws UserRoleHandlerException {
        User currentChairPerson = userDAO.findChairpersonUser();
        if(currentChairPerson != null){
            removeRole(currentChairPerson.getUserId(), CHAIRPERSON);
            addRole(currentChairPerson.getUserId(), new UserRole(roleIdMap.get(ALUMNI), ALUMNI, true));
            addRole(newChairperson.getUserId(), new UserRole(roleIdMap.get(CHAIRPERSON), CHAIRPERSON, true));
            delegateChairPersonVotes(currentChairPerson.getUserId(), newChairperson.getUserId());
        }
    }

    /**
     * Removes admin role from updatedUser.
     *
     * @param updatedUser The user to update
     */
    private void removeAdmin(User updatedUser) {
        if (userDAO.verifyAdminUsers() < 2) {
            throw new IllegalArgumentException("At least one user with Admin roles should exist.");
        }
        removeRole(updatedUser.getUserId(), ADMIN);
    }

    /**
     * Removes alumni role from updatedUser.
     *
     * @param updatedUser The user to update
     */
    private void removeAlumni(User updatedUser) {
        removeRole(updatedUser.getUserId(), ALUMNI);
    }

    /**
     * Removes researcher role from updatedUser.
     *
     * @param updatedUser The user to update
     */
    private void removeResearcher(User updatedUser) {
        // Find list of related dars
        List<String> referenceIds = dataAccessRequestAPI.describeDataAccessIdsForOwner(updatedUser.getUserId());
        if(!CollectionUtils.isEmpty(referenceIds)){
            electionDAO.bulkCancelOpenElectionByReferenceIdAndType(ElectionType.DATA_ACCESS.getValue(), referenceIds);
            electionDAO.bulkCancelOpenElectionByReferenceIdAndType(ElectionType.RP.getValue(), referenceIds);
        }
        for(String referenceId: referenceIds){
            dataAccessRequestAPI.cancelDataAccessRequest(referenceId);
        }
        removeRole(updatedUser.getUserId(), RESEARCHER);
    }

    /**
     * Assigns userToAssignRole the role sent as a parameter, if he does not
     * have it yet.
     * @param userToAssignRole User whose roles will be updated.
     * @param role New role to add to @userToAssignRole .
     */
    private void assignNewRole(User userToAssignRole, UserRole role) {
        List<UserRole> roles = userRoleDAO.findRolesByUserId(userToAssignRole.getUserId());
        if (!containsRole(roles, role.getName())) {
            List<UserRole> newRoles = new ArrayList<>();
            newRoles.add(role);
            userRoleDAO.insertUserRoles(newRoles, userToAssignRole.getUserId());
        }
    }

    private List<UserRole> generateRoleIdList(List<UserRole> roleList) {
        roleList.stream().forEach((r) -> r.setRoleId(roleIdMap.get(r.getName().toUpperCase())));
        return roleList;
    }

    private void deleteDatasetsOwnership(User updatedUser) {
        datasetAssociationDAO.deleteDatasetRelationshipsForUser(updatedUser.getUserId());
    }


    private void changeRolesWithoutDelegation(User updatedUser, List<UserRole> removedRoles, List<UserRole> newRoles, boolean delegateChairperson) throws UserRoleHandlerException, MessagingException, IOException, TemplateException {
        if (CollectionUtils.isNotEmpty(removedRoles)) {
            userRoleDAO.removeUserRoles(updatedUser.getUserId(),
                    removedRoles.stream().map(dacUserRole -> dacUserRole.getRoleId()).collect(Collectors.toList()));
        }
        if (CollectionUtils.isNotEmpty(newRoles)) {
            if (containsAnyRole(newRoles, new String[]{CHAIRPERSON}) && !Objects.isNull(userDAO.findChairpersonUser())) {
                changeChairPerson(updatedUser, delegateChairperson, null);
                newRoles = newRoles.stream().filter(dacUserRole -> !dacUserRole.getName().toUpperCase().equals(CHAIRPERSON)).collect(Collectors.toList());
            }
            if (CollectionUtils.isNotEmpty(newRoles)) {
                userRoleDAO.insertUserRoles(generateRoleIdList(newRoles), updatedUser.getUserId());
            }
        }
    }

    private void changeChairPerson(User oldChairPerson, boolean delegateChairperson, User newChairPerson) throws UserRoleHandlerException, MessagingException, IOException, TemplateException {
        removeRole(oldChairPerson.getUserId(), CHAIRPERSON);
        if (delegateChairperson) {
            List<Vote> votesToInsert = removeChairPersonVotes(oldChairPerson);
            // pending votes other than DAC vote type. We need to insert this always
            if(CollectionUtils.isNotEmpty(votesToInsert)) {
                List<Vote> cpVotes = votesToInsert.stream()
                        .filter(vote -> !(vote.getType().equalsIgnoreCase(VoteType.DAC.getValue())))
                        .collect(Collectors.toList());
                // pending votes, DAC type. We need to verify if already exists or not
                List<Vote> dacVotes = votesToInsert.stream()
                        .filter(vote -> vote.getType().equalsIgnoreCase(VoteType.DAC.getValue()))
                        .collect(Collectors.toList());
                List<Vote> existingDACVotes = voteDAO.findVotesByElectionIdAndTypeAndUser(dacVotes.stream()
                        .map(Vote::getElectionId).collect(Collectors.toList()), VoteType.DAC.getValue(), newChairPerson.getUserId());

                List<Vote> dacVotesToInclude = dacVotes.stream()
                        .filter(v -> verify(existingDACVotes, v))
                        .collect(Collectors.toList());
                insertNewChairPersonVotes(newChairPerson, cpVotes);
                insertNewChairPersonVotes(newChairPerson, dacVotesToInclude);
                emailNotifierAPI.sendUserDelegateResponsibilitiesMessage(newChairPerson, oldChairPerson.getUserId(), CHAIRPERSON, dacVotes);
            }
            if (containsAnyRole(newChairPerson.getRoles(), new String[]{ALUMNI})) {
                removeAlumni(newChairPerson);
            }
            if (containsAnyRole(newChairPerson.getRoles(), new String[]{MEMBER})) {
                removeRole(newChairPerson.getUserId(), MEMBER);
            }
            assignNewRole(newChairPerson, new UserRole(roleIdMap.get(CHAIRPERSON), CHAIRPERSON, true));

            // update DATA_ACCESS elections status from FINAL to OPEN.
            List<Integer> toUpdateFinalStatusElections = electionDAO.findElectionsIdByTypeAndStatus(
                    ElectionType.DATA_ACCESS.getValue(),
                    ElectionStatus.FINAL.getValue());
            if(CollectionUtils.isNotEmpty(toUpdateFinalStatusElections))
                electionDAO.updateElectionStatus(toUpdateFinalStatusElections,ElectionStatus.OPEN.getValue());
        }
    }

    private boolean verify(List<Vote> existing, Vote vote) {
        return !existing.stream().anyMatch(v -> Objects.equals(v.getElectionId(), vote.getElectionId()));
    }

    private void changeDacMember(User oldMember, boolean delegateMember, User newMember) throws MessagingException, IOException, TemplateException {
        removeRole(oldMember.getUserId(), MEMBER);
        List<Integer> openDULElectionIdsForThisUser = electionDAO.findOpenElectionIdByTypeAndUser(oldMember.getUserId(), ElectionType.TRANSLATE_DUL.getValue());
        List<Election> accessRpElectionIds = electionDAO.findAccessRpOpenElectionIds(oldMember.getUserId());
        verifyAndUpdateAccessElection(oldMember, newMember, accessRpElectionIds);
        if (delegateMember) {
            verifyAndDelegateElections(oldMember, newMember, openDULElectionIdsForThisUser, 4, VoteType.DAC.getValue());
            assignNewRole(newMember, new UserRole(roleIdMap.get(MEMBER), MEMBER, true));
            if (containsAnyRole(newMember.getRoles(), new String[]{ALUMNI})) {
                removeAlumni(newMember);
            }
            if (containsAnyRole(newMember.getRoles(), new String[]{CHAIRPERSON})) {
                removeRole(newMember.getUserId(), CHAIRPERSON);
            }
        } else {
            removeVotes(oldMember, openDULElectionIdsForThisUser);
        }
    }


    /**
     * If a person does not vote on access or rp elections, both votes have to be delegated
     */
    private void verifyAndUpdateAccessElection(User oldMember, User newMember, List<Election> accessRpElectionIds) {
        List<Integer> electionsToDelegate = new ArrayList<>();
        List<Integer> electionsToRemove = new ArrayList<>();
        accessRpElectionIds.stream().forEach(election -> {
            if (election.getElectionType().equals(ElectionType.DATA_ACCESS.getValue())) {
                Integer rpElectionId = electionDAO.findRPElectionByElectionAccessId(election.getElectionId());
                if (rpElectionId != null) {
                    loadDelegateAndRemoveVoteElection(electionsToDelegate, electionsToRemove, election, rpElectionId);
                }
                loadDelegateAndRemoveVoteElection(electionsToDelegate, electionsToRemove, election, election.getElectionId());
            } else {
                Integer accessElectionId = electionDAO.findAccessElectionByElectionRPId(election.getElectionId());
                if (accessElectionId != null) {
                    loadDelegateAndRemoveVoteElection(electionsToDelegate, electionsToRemove, election, accessElectionId);
                }
            }
        });
        removeVotes(oldMember, electionsToRemove);
        if(newMember != null){
            delegateVotes(oldMember, newMember, electionsToDelegate);
        }
    }

    private void loadDelegateAndRemoveVoteElection(List<Integer> electionsToDelegate, List<Integer> electionsToRemove, Election election, Integer electionId) {
        List<Vote> votes = voteDAO.findDACVotesByElectionId(electionId);
        if(votes.size() == 4){
            electionsToDelegate.add(electionId);
            electionsToDelegate.add(election.getElectionId());
        }else {
            electionsToRemove.add(electionId);
            electionsToRemove.add(election.getElectionId());
        }
    }


    private List<Vote> removeChairPersonVotes(User oldMember) {
        // get all votes on pending elections
        List<Vote> votesOnPendingElections = voteDAO.findVotesOnOpenElections(oldMember.getUserId());
        // remove null votes on pending elections
        removeVotes(votesOnPendingElections.stream().map(vote -> vote.getVoteId()).collect(Collectors.toList()));
        // returns ALL votes on pending elections, null AND not null
        return votesOnPendingElections;
    }

    private List<UserRole> substractAllRoles(List<UserRole> roles, List<UserRole> toSubstractRoles) {
        List<String> toSubstractRolesNames = toSubstractRoles.stream().map(role -> role.getName().toUpperCase()).collect(Collectors.toList());
        return roles.stream().filter(rol -> !toSubstractRolesNames.contains(rol.getName().toUpperCase())).collect(Collectors.toList());
    }

    private void removeRole(Integer dacUserId, String role) {
        userRoleDAO.removeSingleUserRole(dacUserId, roleIdMap.get(role));
    }

    private void addRole(Integer dacUserId, UserRole role) {
        userRoleDAO.insertSingleUserRole(role.getRoleId(), dacUserId, role.getEmailPreference());
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


    private void insertNewChairPersonVotes(User newMember, List<Vote> votesToInsert) {
        if (CollectionUtils.isNotEmpty(votesToInsert)) {
            votesToInsert.stream().forEach((v) -> v.initVote(newMember.getUserId(), null, null, null, false, false, null));
            voteDAO.batchVotesInsert(votesToInsert);
        }
    }

}
