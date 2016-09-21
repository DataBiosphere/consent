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
import org.broadinstitute.consent.http.db.DACUserDAO;
import org.broadinstitute.consent.http.db.DACUserRoleDAO;
import org.broadinstitute.consent.http.db.DataSetAssociationDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.db.VoteDAO;
import org.broadinstitute.consent.http.enumeration.DACUserRoles;
import org.broadinstitute.consent.http.enumeration.ElectionStatus;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.enumeration.VoteType;
import org.broadinstitute.consent.http.models.DACUser;
import org.broadinstitute.consent.http.models.DACUserRole;
import org.broadinstitute.consent.http.models.DatasetAssociation;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.Role;
import org.broadinstitute.consent.http.models.Vote;
import org.broadinstitute.consent.http.service.DataAccessRequestAPI;
import org.broadinstitute.consent.http.service.EmailNotifierAPI;

public class DACUserRolesHandler extends AbstractUserRolesHandler {

    private final DACUserDAO dacUserDAO;
    private final ElectionDAO electionDAO;
    private final VoteDAO voteDAO;
    private final DACUserRoleDAO userRoleDAO;
    private final DataSetAssociationDAO datasetAssociationDAO;
    private final String MEMBER = DACUserRoles.MEMBER.getValue();
    private final String ADMIN = DACUserRoles.ADMIN.getValue();
    private final String CHAIRPERSON = DACUserRoles.CHAIRPERSON.getValue();
    private final String RESEARCHER = DACUserRoles.RESEARCHER.getValue();
    private final String DATA_OWNER = DACUserRoles.DATAOWNER.getValue();
    private final String ALUMNI = DACUserRoles.ALUMNI.getValue();
    private final Map<String, Integer> roleIdMap;
    private final EmailNotifierAPI emailNotifierAPI;
    private final DataAccessRequestAPI dataAccessRequestAPI;


    public DACUserRolesHandler(DACUserDAO userDao, DACUserRoleDAO roleDAO, ElectionDAO electionDAO, VoteDAO voteDAO, DataSetAssociationDAO datasetAssociationDAO, EmailNotifierAPI emailNotifierAPI, DataAccessRequestAPI dataAccessRequestAPI) {
        this.dacUserDAO = userDao;
        this.electionDAO = electionDAO;
        this.userRoleDAO = roleDAO;
        this.voteDAO = voteDAO;
        this.datasetAssociationDAO = datasetAssociationDAO;
        this.roleIdMap = createRoleMap(userRoleDAO.findRoles());
        this.emailNotifierAPI = emailNotifierAPI;
        this.dataAccessRequestAPI = dataAccessRequestAPI;
    }

    public static void initInstance(DACUserDAO userDao, DACUserRoleDAO roleDAO, ElectionDAO electionDAO, VoteDAO voteDAO, DataSetAssociationDAO datasetAssociationDAO, EmailNotifierAPI emailNotifierAPI, DataAccessRequestAPI dataAccessRequestAPI) {
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
    public void updateRoles(Map<String, DACUser> usersMap) throws UserRoleHandlerException, MessagingException, IOException, TemplateException {
        DACUser updatedUser;
        DACUser userToDelegate = null;
        DACUser doUserToDelegate = null;

        try {
            userRoleDAO.begin();
            voteDAO.begin();
            boolean delegateMember = usersMap.containsKey("userToDelegate");
            boolean delegateOwner = usersMap.containsKey("alternativeDataOwnerUser");
            updatedUser = usersMap.get("updatedUser");
            // roles as should be ..
            List<DACUserRole> updatedRoles = updatedUser.getRoles();

            // roles as currently are ...
            List<DACUserRole> originalRoles = userRoleDAO.findRolesByUserId(updatedUser.getDacUserId());

            // roles required to remove ...
            List<DACUserRole> rolesToRemove = substractAllRoles(originalRoles, updatedRoles);

            // roles to add ..
            List<DACUserRole> rolesToAdd = substractAllRoles(updatedRoles, originalRoles);


            updateAdminEmailPreference(originalRoles,updatedRoles,updatedUser.getDacUserId());

            // If there aren't any open elections and we didn't delegate to any member then we don't have to validate anything.
            if (electionDAO.verifyOpenElections() == 0 && !delegateMember && !delegateOwner) {
                changeRolesWithoutDelegation(updatedUser, rolesToRemove, rolesToAdd, delegateMember);
            }
            if (delegateMember) {
                userToDelegate = dacUserDAO.findDACUserByEmail(usersMap.get("userToDelegate").getEmail());
                userToDelegate.setRoles(userRoleDAO.findRolesByUserId(userToDelegate.getDacUserId()));
            }
            if (delegateOwner) {
                doUserToDelegate = dacUserDAO.findDACUserByEmail(usersMap.get("alternativeDataOwnerUser").getEmail());
                doUserToDelegate.setRoles(userRoleDAO.findRolesByUserId(doUserToDelegate.getDacUserId()));
            }
            // removing deleted roles
            for (DACUserRole role : rolesToRemove) {
                switch (DACUserRoles.valueOf(role.getName().toUpperCase())) {

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
            for (DACUserRole role : rolesToAdd) {
                switch (DACUserRoles.valueOf(role.getName().toUpperCase())) {
                    case CHAIRPERSON:
                        addChairPerson(updatedUser);
                        break;
                    case MEMBER:
                        assignNewRole(updatedUser, new DACUserRole(roleIdMap.get(MEMBER), MEMBER, true));
                        break;
                    case ALUMNI:
                        if (containsAnyRole(updatedRoles, new String[]{MEMBER, CHAIRPERSON})) {
                            throw new UserRoleHandlerException("User to delegate: " + (userToDelegate != null ? userToDelegate.getDisplayName() : null) + " has a role: "
                                    + " [Member] or [Chairperson] that is incompatible with the role you want to assign.");
                        } else {
                            assignNewRole(updatedUser, new DACUserRole(roleIdMap.get(ALUMNI), ALUMNI, true));
                        }
                        break;
                    case ADMIN:
                        assignNewRole(updatedUser, new DACUserRole(roleIdMap.get(ADMIN), ADMIN, role.getEmailPreference() == null ? true : role.getEmailPreference()));
                        break;
                    case RESEARCHER:
                        if (containsAnyRole(updatedRoles, new String[]{MEMBER, CHAIRPERSON})) {
                            assert userToDelegate != null;
                            throw new UserRoleHandlerException("User to delegate: " + (userToDelegate != null ? userToDelegate.getDisplayName() : null) + " has a role: "
                                    + " [Member] or [Chairperson] that is incompatible with the role you want to assign.");
                        } else {
                            assignNewRole(updatedUser, new DACUserRole(roleIdMap.get(RESEARCHER), RESEARCHER, true));
                        }
                        break;
                    case DATAOWNER:
                        assignNewRole(updatedUser, new DACUserRole(roleIdMap.get(DATA_OWNER), DATA_OWNER, true));
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

    private void updateAdminEmailPreference(List<DACUserRole> originalRoles, List<DACUserRole> updatedRoles, Integer dacUserId) throws UserRoleHandlerException {
        try{
            boolean isAdminRole = originalRoles.stream().anyMatch(r -> r.getName().equalsIgnoreCase(ADMIN));
            List<DACUserRole> newAdminRole =  updatedRoles.stream().filter(r -> r.getName().equalsIgnoreCase(ADMIN)).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(newAdminRole)) {
                return;
            }
            DACUserRole updatedAdminRole = newAdminRole.get(0);
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
    private void removeDataOwner(DACUser updatedUser, boolean delegate, DACUser doUserToDelegate) throws UserRoleHandlerException, MessagingException, IOException, TemplateException {
        removeRole(updatedUser.getDacUserId(), DATA_OWNER);
        List<Integer> openElectionIdsForThisUser = electionDAO.findDataSetOpenElectionIds(updatedUser.getDacUserId());
        if (delegate) {
            assignNewRole(doUserToDelegate, new DACUserRole(roleIdMap.get(DATA_OWNER), DATA_OWNER, true));
            verifyAndDelegateElections(updatedUser, doUserToDelegate, openElectionIdsForThisUser, 1, VoteType.DATA_OWNER.getValue());
            updateDataSetsOwnership(updatedUser, doUserToDelegate);
        } else {
            verifyAndDelegateElections(updatedUser, doUserToDelegate, openElectionIdsForThisUser, 1, VoteType.DATA_OWNER.getValue());
            deleteDatasetsOwnership(updatedUser);
        }
    }

    private void verifyAndDelegateElections(DACUser updatedUser, DACUser doUserToDelegate, List<Integer> openElectionIdsForThisUser, Integer cantVotes, String voteType) throws MessagingException, IOException, TemplateException {
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
            emailNotifierAPI.sendUserDelegateResponsibilitiesMessage(doUserToDelegate, updatedUser.getDacUserId(), voteType.equalsIgnoreCase(VoteType.DATA_OWNER.getValue())? DATA_OWNER: MEMBER, voteDAO.findVotesByElectionIdsAndUser(electionsIdToDelegateVotes, doUserToDelegate.getDacUserId()));
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

    private void removeVotes(List<Integer> voteIds) {
        if (CollectionUtils.isNotEmpty(voteIds)) {
            voteDAO.removeVotesById(voteIds);
        }
    }

    private void updateDataSetsOwnership(DACUser updatedUser, DACUser userToDelegate) {
        List<DatasetAssociation> associations = datasetAssociationDAO.findAllDatasetAssociationsByOwnerId(updatedUser.getDacUserId());
        List<DatasetAssociation> newAssociations = new ArrayList<>();
        associations.stream().forEach((as) -> {
            if (!as.getDacuserId().equals(userToDelegate.getDacUserId())) {
                newAssociations.add(new DatasetAssociation(as.getDatasetId(), userToDelegate.getDacUserId()));
            }
        });
        deleteDatasetsOwnership(updatedUser);
        datasetAssociationDAO.insertDatasetUserAssociation(newAssociations);
    }

    private void delegateChairPersonVotes(Integer fromDacUserId, Integer toDacUSerId) {
        voteDAO.delegateChairPersonOpenElectionsVotes(fromDacUserId, toDacUSerId);
    }

    private void addChairPerson(DACUser newChairperson) throws UserRoleHandlerException {
        DACUser currentChairPerson = dacUserDAO.findChairpersonUser();
        if(currentChairPerson != null){
            removeRole(currentChairPerson.getDacUserId(), CHAIRPERSON);
            addRole(currentChairPerson.getDacUserId(), new DACUserRole(roleIdMap.get(ALUMNI), ALUMNI, true));
            addRole(newChairperson.getDacUserId(), new DACUserRole(roleIdMap.get(CHAIRPERSON), CHAIRPERSON, true));
            delegateChairPersonVotes(currentChairPerson.getDacUserId(), newChairperson.getDacUserId());
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
        removeRole(updatedUser.getDacUserId(), ADMIN);
    }

    /**
     * Removes alumni role from updatedUser.
     *
     * @param updatedUser The user to update
     */
    private void removeAlumni(DACUser updatedUser) {
        removeRole(updatedUser.getDacUserId(), ALUMNI);
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
        removeRole(updatedUser.getDacUserId(), RESEARCHER);
    }

    /**
     * Assigns userToAssignRole the role sent as a parameter, if he does not
     * have it yet.
     * @param userToAssignRole User whose roles will be updated.
     * @param role New role to add to @userToAssignRole .
     */
    private void assignNewRole(DACUser userToAssignRole, DACUserRole role) {
        List<DACUserRole> roles = userRoleDAO.findRolesByUserId(userToAssignRole.getDacUserId());
        if (!containsRole(roles, role.getName())) {
            List<DACUserRole> newRoles = new ArrayList<>();
            newRoles.add(role);
            userRoleDAO.insertUserRoles(newRoles, userToAssignRole.getDacUserId());
        }
    }

    private List<DACUserRole> generateRoleIdList(List<DACUserRole> roleList) {
        roleList.stream().forEach((r) -> r.setRoleId(roleIdMap.get(r.getName().toUpperCase())));
        return roleList;
    }

    private void deleteDatasetsOwnership(DACUser updatedUser) {
        datasetAssociationDAO.deleteDatasetRelationshipsForUser(updatedUser.getDacUserId());
    }


    private void changeRolesWithoutDelegation(DACUser updatedUser, List<DACUserRole> removedRoles, List<DACUserRole> newRoles, boolean delegateChairperson) throws UserRoleHandlerException, MessagingException, IOException, TemplateException {
        if (CollectionUtils.isNotEmpty(removedRoles)) {
            userRoleDAO.removeUserRoles(updatedUser.getDacUserId(),
                    removedRoles.stream().map(dacUserRole -> dacUserRole.getRoleId()).collect(Collectors.toList()));
        }
        if (CollectionUtils.isNotEmpty(newRoles)) {
            if (containsAnyRole(newRoles, new String[]{CHAIRPERSON}) && !Objects.isNull(dacUserDAO.findChairpersonUser())) {
                changeChairPerson(updatedUser, delegateChairperson, null);
                newRoles = newRoles.stream().filter(dacUserRole -> !dacUserRole.getName().toUpperCase().equals(CHAIRPERSON)).collect(Collectors.toList());
            }
            if (CollectionUtils.isNotEmpty(newRoles)) {
                userRoleDAO.insertUserRoles(generateRoleIdList(newRoles), updatedUser.getDacUserId());
            }
        }
    }

    private void changeChairPerson(DACUser oldChairPerson, boolean delegateChairperson, DACUser newChairPerson) throws UserRoleHandlerException, MessagingException, IOException, TemplateException {
        removeRole(oldChairPerson.getDacUserId(), CHAIRPERSON);
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
                        .map(Vote::getElectionId).collect(Collectors.toList()), VoteType.DAC.getValue(), newChairPerson.getDacUserId());

                List<Vote> dacVotesToInclude = dacVotes.stream()
                        .filter(v -> verify(existingDACVotes, v))
                        .collect(Collectors.toList());
                insertNewChairPersonVotes(newChairPerson, cpVotes);
                insertNewChairPersonVotes(newChairPerson, dacVotesToInclude);
                emailNotifierAPI.sendUserDelegateResponsibilitiesMessage(newChairPerson, oldChairPerson.getDacUserId(), CHAIRPERSON, dacVotes);
            }
            if (containsAnyRole(newChairPerson.getRoles(), new String[]{ALUMNI})) {
                removeAlumni(newChairPerson);
            }
            if (containsAnyRole(newChairPerson.getRoles(), new String[]{MEMBER})) {
                removeRole(newChairPerson.getDacUserId(), MEMBER);
            }
            assignNewRole(newChairPerson, new DACUserRole(roleIdMap.get(CHAIRPERSON), CHAIRPERSON, true));

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

    private void changeDacMember(DACUser oldMember, boolean delegateMember, DACUser newMember) throws MessagingException, IOException, TemplateException {
        removeRole(oldMember.getDacUserId(), MEMBER);
        List<Integer> openDULElectionIdsForThisUser = electionDAO.findOpenElectionIdByTypeAndUser(oldMember.getDacUserId(), ElectionType.TRANSLATE_DUL.getValue());
        List<Election> accessRpElectionIds = electionDAO.findAccessRpOpenElectionIds(oldMember.getDacUserId());
        verifyAndUpdateAccessElection(oldMember, newMember, accessRpElectionIds);
        if (delegateMember) {
            verifyAndDelegateElections(oldMember, newMember, openDULElectionIdsForThisUser, 4, VoteType.DAC.getValue());
            assignNewRole(newMember, new DACUserRole(roleIdMap.get(MEMBER), MEMBER, true));
            if (containsAnyRole(newMember.getRoles(), new String[]{ALUMNI})) {
                removeAlumni(newMember);
            }
            if (containsAnyRole(newMember.getRoles(), new String[]{CHAIRPERSON})) {
                removeRole(newMember.getDacUserId(), CHAIRPERSON);
            }
        } else {
            removeVotes(oldMember, openDULElectionIdsForThisUser);
        }
    }


    /**
     * If a person does not vote on access or rp elections, both votes have to be delegated
     */
    private void verifyAndUpdateAccessElection(DACUser oldMember, DACUser newMember, List<Election> accessRpElectionIds) {
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


    private List<Vote> removeChairPersonVotes(DACUser oldMember) {
        // get all votes on pending elections
        List<Vote> votesOnPendingElections = voteDAO.findVotesOnOpenElections(oldMember.getDacUserId());
        // remove null votes on pending elections
        removeVotes(votesOnPendingElections.stream().map(vote -> vote.getVoteId()).collect(Collectors.toList()));
        // returns ALL votes on pending elections, null AND not null
        return votesOnPendingElections;
    }

    private List<DACUserRole> substractAllRoles(List<DACUserRole> roles, List<DACUserRole> toSubstractRoles) {
        List<String> toSubstractRolesNames = toSubstractRoles.stream().map(role -> role.getName().toUpperCase()).collect(Collectors.toList());
        return roles.stream().filter(rol -> !toSubstractRolesNames.contains(rol.getName().toUpperCase())).collect(Collectors.toList());
    }

    private void removeRole(Integer dacUserId, String role) {
        userRoleDAO.removeSingleUserRole(dacUserId, roleIdMap.get(role));
    }

    private void addRole(Integer dacUserId, DACUserRole role) {
        userRoleDAO.insertSingleUserRole(role.getRoleId(), dacUserId, role.getEmailPreference());
    }

    public boolean containsRole(Collection<DACUserRole> roles, String role) {
        return roles.stream().anyMatch(r -> r.getName().equalsIgnoreCase(role));
    }

    public boolean containsAnyRole(Collection<DACUserRole> roles, String[] rolesToMatch) {
        for (String role : rolesToMatch) {
            if (containsRole(roles, role)) {
                return true;
            }
        }
        return false;
    }


    private void insertNewChairPersonVotes(DACUser newMember, List<Vote> votesToInsert) {
        if (CollectionUtils.isNotEmpty(votesToInsert)) {
            votesToInsert.stream().forEach((v) -> v.initVote(newMember.getDacUserId(), null, null, null, false, false, null));
            voteDAO.batchVotesInsert(votesToInsert);
        }
    }

}
