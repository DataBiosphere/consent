package org.broadinstitute.consent.http.service.users.handler;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;
import org.broadinstitute.consent.http.db.DACUserDAO;
import org.broadinstitute.consent.http.db.DACUserRoleDAO;
import org.broadinstitute.consent.http.db.DataSetAssociationDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.db.VoteDAO;
import org.broadinstitute.consent.http.db.mongo.MongoConsentDB;
import org.broadinstitute.consent.http.enumeration.DACUserRoles;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.models.DACUser;
import org.broadinstitute.consent.http.models.DACUserRole;
import org.broadinstitute.consent.http.models.DatasetAssociation;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.Role;
import org.broadinstitute.consent.http.models.Vote;

public class DACUserRolesHandler extends AbstractUserRolesHandler {

    private final DACUserDAO dacUserDAO;
    private final ElectionDAO electionDAO;
    private final VoteDAO voteDAO;
    private final DACUserRoleDAO userRoleDAO;
    private final DataSetAssociationDAO datasetAssociationDAO;
    private final MongoConsentDB mongo;

    private final String MEMBER = DACUserRoles.MEMBER.getValue();
    private final String ADMIN = DACUserRoles.ADMIN.getValue();
    private final String CHAIRPERSON = DACUserRoles.CHAIRPERSON.getValue();
    private final String RESEARCHER = DACUserRoles.RESEARCHER.getValue();
    private final String DATA_OWNER = DACUserRoles.DATA_OWNER.getValue();
    private final String ALUMNI = DACUserRoles.ALUMNI.getValue();

    // DacMembers(3) AND (1) Chairperson
    private final Integer MINIMUM_DAC_USERS = 3;

    private final Map<String, Integer> roleIdMap;

    public static void initInstance(MongoConsentDB mongo, DACUserDAO userDao, DACUserRoleDAO roleDAO, ElectionDAO electionDAO, VoteDAO voteDAO, DataSetAssociationDAO datasetAssociationDAO) {
        UserHandlerAPIHolder.setInstance(new DACUserRolesHandler(mongo, userDao, roleDAO, electionDAO, voteDAO, datasetAssociationDAO));
    }

    public DACUserRolesHandler(MongoConsentDB mongo, DACUserDAO userDao, DACUserRoleDAO roleDAO, ElectionDAO electionDAO, VoteDAO voteDAO, DataSetAssociationDAO datasetAssociationDAO) {
        this.mongo = mongo;
        this.dacUserDAO = userDao;
        this.electionDAO = electionDAO;
        this.userRoleDAO = roleDAO;
        this.voteDAO = voteDAO;
        this.datasetAssociationDAO = datasetAssociationDAO;
        this.roleIdMap = createRoleMap(userRoleDAO.findRoles());
    }

    private Map<String, Integer> createRoleMap(List<Role> roles) {
        Map<String, Integer> rolesMap = new HashMap();
        roles.stream().forEach((r) -> {
            rolesMap.put(r.getName().toUpperCase(), r.getRoleId());
        });
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
     * Identification(explained above) -> DacUser
     *
     * @throws
     * org.broadinstitute.consent.http.service.users.handler.UserRoleHandlerException
     */
    @Override
    public void updateRoles(Map<String, DACUser> usersMap) throws UserRoleHandlerException {
        DACUser updatedUser = null;
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

            // If there aren't any open elections and we didn't delegate to any member then we don't have to validate anything.
            if (electionDAO.verifyOpenElections() == 0 && !delegateMember && !delegateOwner) {
                changeRolesWithoutDelegation(updatedUser, rolesToRemove, updatedRoles, delegateMember);
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

                switch (DACUserRoles.valueOf(role.getName())) {

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

                    case DATA_OWNER:
                        removeDataOwner(updatedUser, delegateOwner, doUserToDelegate);
                        break;
                }
            }

            // adding new roles 
            for (DACUserRole role : rolesToAdd) {

                switch (DACUserRoles.valueOf(role.getName())) {

                    case CHAIRPERSON:
                        addChairPerson(updatedUser);
                        break;

                    case MEMBER:
                        assignNewRole(updatedUser, new DACUserRole(roleIdMap.get(MEMBER), MEMBER, true));
                        break;

                    case ALUMNI:
                        if (containsAnyRole(updatedRoles, new String[]{MEMBER, CHAIRPERSON})) {
                            throw new UserRoleHandlerException("User to delegate: " + userToDelegate.getDisplayName() + " has a role: "
                                    + " [Member] or [Chairperson] that is incompatible with the role you want to assign.");
                        } else {
                            assignNewRole(updatedUser, new DACUserRole(roleIdMap.get(ALUMNI), ALUMNI, true));
                        }
                        break;

                    case ADMIN:
                        assignNewRole(updatedUser, new DACUserRole(roleIdMap.get(ADMIN), ADMIN, true));
                        break;

                    case RESEARCHER:
                        if (containsAnyRole(updatedRoles, new String[]{MEMBER, CHAIRPERSON})) {
                            throw new UserRoleHandlerException("User to delegate: " + userToDelegate.getDisplayName() + " has a role: "
                                    + " [Member] or [Chairperson] that is incompatible with the role you want to assign.");
                        } else {
                            assignNewRole(updatedUser, new DACUserRole(roleIdMap.get(RESEARCHER), RESEARCHER, true));
                        }
                        break;

                    case DATA_OWNER:
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

    //--------------------- refactored -----------------------------------------------------------
    /* Removes data owners role, assigns votes (update) if needed, then removes
     * and assigns the role.
     *
     * @param updatedUser
     * @param userToDelegate
     * @param delegate
     * @throws UserRoleHandlerException
     */
    /**
     * TODO: Remove delegate variable. Main method will know if delegation is
     * needed. I think this method should be split in several: one to remove the
     * data owner and another to assign a data owner and his votes, if needed.
     */
    private void removeDataOwner(DACUser updatedUser, boolean delegate, DACUser doUserToDelegate) throws UserRoleHandlerException {
        removeRole(updatedUser.getDacUserId(), DATA_OWNER);

        if (delegate) {
            assignNewRole(doUserToDelegate, new DACUserRole(roleIdMap.get(DATA_OWNER), DATA_OWNER, true));
            voteDAO.delegateDatasetOpenElectionsVotes(updatedUser.getDacUserId(), doUserToDelegate.getDacUserId());
            updateDatasetsOwnership(updatedUser, doUserToDelegate);
        } else {
            deleteDatasetsOwnership(updatedUser);
        }
    }

//    /**
//     * Finds ALL the votes related to a member, pending or done, on open
//     * Elections.
//     *
//     * @param member The member the votes are related to.
//     *
//     * @return The list of votes the user has, pending or not.
//     */
//    private List<Vote> findMemberVotes(DACUser member) {
//        return voteDAO.findVotesOnOpenElections(member.getDacUserId());
//    }
    /**
     * Helper method, receives a List of vote IDs and deletes them. If empty, no
     * action is performed
     *
     * @param votesId List of Ids of the votes we need to delete.
     */
    private void removeVotes(List<Integer> voteIds) {
        if (CollectionUtils.isNotEmpty(voteIds)) {
            voteDAO.removeVotesById(voteIds);
        }
    }

    /**
     *
     *
     * private void removeResearcher(DACUser updatedUser) throws
     * UserRoleHandlerException { try { FindIterable<Document> accessList =
     * mongo.getDataAccessRequestCollection().find(new
     * BasicDBObject(DarConstants.USER_ID, updatedUser.getDacUserId()));
     * List<String> accessRequestIds = getRequestIds(accessList); if
     * (!CollectionUtils.isEmpty(accessRequestIds)) {
     * electionDAO.updateElectionStatusByReferenceAndStatus(accessRequestIds,
     * ElectionStatus.OPEN.getValue(), ElectionStatus.CANCELED.getValue()); } }
     * catch (Exception e) { throw e; } removeRole(updatedUser.getDacUserId(),
     * RESEARCHER); }
     *
     * private List getRequestIds(FindIterable<Document> access) { List<String>
     * accessIds = new ArrayList<>(); if (access != null) {
     * access.forEach((Block<Document>) document -> {
     * accessIds.add(document.get(DarConstants.ID).toString()); }); } return
     * accessIds; }
     *
     * /**
     * TODO: Verify if this method is needed for the Data Owners. I think not,
     * since we will be inserting the votes for the delegated users.
     */
    private void updateDatasetsOwnership(DACUser updatedUser, DACUser userToDelegate) {
        List<DatasetAssociation> associations = datasetAssociationDAO.findAllDatasetAssociationsByOwnerId(updatedUser.getDacUserId());
        for (DatasetAssociation as : associations) {
            as.setDacuserId(userToDelegate.getDacUserId());
        }
        deleteDatasetsOwnership(updatedUser);
        datasetAssociationDAO.insertDatasetUserAssociation(associations);
    }

    /**
     * transfer pending votes for a chairperson to another one.
     *
     */
    /**
     * TODO: Verify if this method is needed for the Chairperson. I think not,
     * since we will be inserting the votes for the delegated users.
     */
    private void delegateChairPersonVotes(Integer fromDacUserId, Integer toDacUSerId) {
        voteDAO.delegateChairPersonOpenElectionsVotes(fromDacUserId, toDacUSerId);
    }

    private void addChairPerson(DACUser newChairperson) throws UserRoleHandlerException {
        DACUser currentChairPerson = dacUserDAO.findChairpersonUser();
        removeRole(currentChairPerson.getDacUserId(), CHAIRPERSON);
        addRole(newChairperson.getDacUserId(), new DACUserRole(roleIdMap.get(CHAIRPERSON), CHAIRPERSON, true));
        delegateChairPersonVotes(currentChairPerson.getDacUserId(), newChairperson.getDacUserId());
    }
//
//    //--------------------- end refactored -------------------------------------------------------
//    /**
//     * When there is open elections, you should call this method.
//     *
//     * @param oldMember Member that leaves the DacMember role.
//     * @param newMember New member to take over oldMember.
//     */
//    private void removeChairperson(DACUser oldMember, DACUser newMember) {
//        transferVotesForDacMember(oldMember, newMember);
//        assignNewRole(newMember, new DACUserRole(roleIdMap.get(CHAIRPERSON), CHAIRPERSON, true));
//    }

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
     * Removes alumni role from updatedUser.
     *
     * @param updatedUser The user to update
     */
    private void removeResearcher(DACUser updatedUser) {
        removeRole(updatedUser.getDacUserId(), ALUMNI);
    }

    /**
     * Assigns userToAssignRole the role sent as a parameter, if he does not
     * have it yet.
     *
     * @param userToAssignRole Member that leaves the DacMember role.
     * @param role New member to take over oldMember.
     */
    private void assignNewRole(DACUser userToAssignRole, DACUserRole role) {
        if (!containsRole(userToAssignRole.getRoles(), role.getName())) {
            List<DACUserRole> newRoles = new ArrayList<>();
            newRoles.add(role);
            userRoleDAO.insertUserRoles(newRoles, userToAssignRole.getDacUserId());
        }
    }

    /**
     * Assign the corresponding role IDs to the list sent as a parameter
     *
     * @param roleList List of DacUserRoles
     */
    private List<DACUserRole> generateRoleIdList(List<DACUserRole> roleList) {
        roleList.stream().forEach((r) -> {
            r.setRoleId(roleIdMap.get(r.getName().toUpperCase()));
        });
        return roleList;
    }

    /**
     * Helper method that removes the ownership of the datasets from the
     * database.
     *
     * @param updatedUser User that is being removed and is no longer owner of
     * the datasets.
     */
    private void deleteDatasetsOwnership(DACUser updatedUser) {
        datasetAssociationDAO.deleteDatasetRelationshipsForUser(updatedUser.getDacUserId());
    }

    /**
     * --------------------------------------------------------------------------------------------------------------------------------------------------------
     */
    /**
     * * DANGER LINE!!!!!
     * -------------------------------------------------------------------------------------------------------------------------------------
     */
    /**
     * --------------------------------------------------------------------------------------------------------------------------------------------------------
     */
    //TODO: All the code below this line is NOT refactored!
    /**
     * Fairly simple, when there aren't open elections we don't need to check
     * anything. Simply update the roles for the updated users. If the number of
     * members/chairperson is below the minimum it will be checked on the
     * election's creation.
     *
     * @param updatedUser DacUser to update
     * @param removedRoles Roles to add (new)
     * @param newRoles Roles to remove(delete)
     * @param delegateChairperson TODO: Refactor and remove this variable
     *
     */
    private void changeRolesWithoutDelegation(DACUser updatedUser, List<DACUserRole> removedRoles, List<DACUserRole> newRoles, boolean delegateChairperson) throws UserRoleHandlerException {

        if (CollectionUtils.isNotEmpty(removedRoles)) {
            userRoleDAO.removeUserRoles(updatedUser.getDacUserId(),
                    removedRoles.stream().map(dacUserRole -> dacUserRole.getRoleId()).collect(Collectors.toList()));
        }

        if (!CollectionUtils.isEmpty(newRoles)) {
            if (containsAnyRole(newRoles, new String[]{CHAIRPERSON}) && !Objects.isNull(dacUserDAO.findChairpersonUser())) {
                changeChairPerson(updatedUser, delegateChairperson, null);
                newRoles = newRoles.stream().filter(dacUserRole -> !dacUserRole.getName().toUpperCase().equals(CHAIRPERSON)).collect(Collectors.toList());
            }
            if (!CollectionUtils.isEmpty(newRoles)) {
                userRoleDAO.insertUserRoles(generateRoleIdList(newRoles), updatedUser.getDacUserId());
            }
        }
    }

    /**
     * Finds all the open elections where this chairperson is participating.
     *
     * @return List of the elections this user participates into.
     */
    /**
     * TODO: I think we can create another query, something similar to the query
     * of the DacMember, and get all the elections in a single query.
     */
    private List<Election> collectElectionsForChairperson() {
        Set<Election> electionList = new HashSet<>();
        electionList.addAll(electionDAO.getOpenAndFinalElections(ElectionType.DATA_ACCESS.getValue()));
        electionList.addAll(electionDAO.getOpenAndFinalElections(ElectionType.RP.getValue()));
        electionList.addAll(electionDAO.getOpenElections(ElectionType.TRANSLATE_DUL.getValue()));
        return Lists.newArrayList(electionList);
    }

    /**
     * Collects all the votes for the chairperson. The main difference between
     * this and the member collect is that we want to assign the made votes to
     * the new Chairperson, so even the made ones will be updated.
     *
     * TODO: We talked about refactoring this to an DELETE/INSERT approach, so I
     * think an update no longer applies. This method it might be helpful
     * though.
     *
     * @param electionList The list of the elections this user is participating
     * into.
     * @param oldDacMember The member that will leave the chairperson role.
     * @return
     */
    private List<Vote> collectVotesForChairperson(List<Election> electionList, DACUser oldDacMember) {
        List<Vote> votes = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(electionList)) {
            votes.addAll(voteDAO.findVotesByElectionsIdsAndUserIdIncludeMade(electionList.stream().map(Election::getElectionId).collect(Collectors.toList()), oldDacMember.getDacUserId()));
        }
        return votes;
    }

    /**
     * TODO: I think this must be removed.
     *
     * @param votes
     * @param dacUserId
     */
    private void updateDacUserIdForVotes(List<Vote> votes, Integer dacUserId) {
        if (!CollectionUtils.isEmpty(votes)) {
            voteDAO.updateUserIdForVotes(votes.stream().map(Vote::getVoteId).collect(Collectors.toList()), dacUserId);
        }
    }

    //--------------------------------------------------------
    //
    private void changeChairPerson(DACUser oldChairPerson, boolean delegateChairperson, DACUser newChairPerson) throws UserRoleHandlerException {
        removeRole(oldChairPerson.getDacUserId(), CHAIRPERSON);

        if (delegateChairperson) {

            List<Vote> votesToInsert = removeChairPersonVotes(oldChairPerson);

            // pending votes other than DAC vote type. We need to insert this always 
            List<Vote> cpVotes = votesToInsert.stream()
                    .filter(vote -> !(vote.getType().equalsIgnoreCase("DAC")))
                    .collect(Collectors.toList());

            // pending votes, DAC type. We need to verify if already exists or not
            List<Vote> dacVotes = votesToInsert.stream()
                    .filter(vote -> vote.getType().equalsIgnoreCase("DAC"))
                    .collect(Collectors.toList());

            List<Vote> existingDACVotes = voteDAO.findVotesByElectionIdAndTypeAnduser(dacVotes.stream()
                    .map(v -> v.getElectionId()).collect(Collectors.toList()), "DAC", newChairPerson.getDacUserId());

            List<Vote> dacVotesToInclude = dacVotes.stream()
                    .filter(v ->verify(existingDACVotes, v))
                    .collect(Collectors.toList());

            insertNewChairPersonVotes(newChairPerson, cpVotes);
            insertNewChairPersonVotes(newChairPerson, dacVotesToInclude);
            assignNewRole(newChairPerson, new DACUserRole(roleIdMap.get(CHAIRPERSON), CHAIRPERSON, true));
        }
    }

    private boolean verify(List<Vote> existing, Vote vote) {
        return ! existing.stream().anyMatch(v -> Objects.equals(v.getElectionId(), vote.getElectionId()));
    }
    
    private void changeDacMember(DACUser oldMember, boolean delegateMember, DACUser newMember) {
        removeRole(oldMember.getDacUserId(), MEMBER);

        if (delegateMember) {
            List<Vote> votesToInsert = removeMemberVotes(oldMember);
            insertNewMemberVotes(newMember, votesToInsert);
            assignNewRole(newMember, new DACUserRole(roleIdMap.get(MEMBER), MEMBER, true));
        }
    }

    //--------------------------------------------------------------------------------------
    //
    private List<Vote> removeChairPersonVotes(DACUser oldMember) {
        /**
         * We want to keep the ones made, so we won't delete them. We'll throw
         * them away from the list.
         */

        // get all votes on pending elections 
        List<Vote> votesOnPendingElections = voteDAO.findVotesOnOpenElections(oldMember.getDacUserId());

        // remove null votes on pending elections
        removeVotes(votesOnPendingElections.stream().map(vote -> vote.getVoteId()).collect(Collectors.toList()));

        // returns ALL votes on pending elections, null AND not null
        return votesOnPendingElections;
    }

    private List<Vote> removeMemberVotes(DACUser oldMember) {
        /**
         * We want to keep the ones made, so we won't delete them. We'll throw
         * them away from the list.
         */

        // get all votes on pending elections 
        List<Vote> votesOnPendingElections = voteDAO.findVotesOnOpenElections(oldMember.getDacUserId());

        // get pending votes n pending elections
        List<Vote> pendingVotes = votesOnPendingElections.stream().filter(vote -> Objects.isNull(vote.getVote())).collect(Collectors.toList());

        // remove null votes on pending elections
        removeVotes(pendingVotes.stream().map(vote -> vote.getVoteId()).collect(Collectors.toList()));

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
        userRoleDAO.insertSingleUserRole(role, dacUserId);
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

    private void insertNewMemberVotes(DACUser newMember, List<Vote> votesToInsert) {
        if (CollectionUtils.isNotEmpty(votesToInsert)) {
            votesToInsert.stream().forEach((v) -> {
                v.initVote(newMember.getDacUserId(), new Date(), null, null, false, false);
            });
            voteDAO.batchVotesInsert(votesToInsert);
        }
    }

    private void insertNewChairPersonVotes(DACUser newMember, List<Vote> votesToInsert) {

        // get votes to recreate on pending elections
        List<Vote> votes = votesToInsert.stream()
                .filter(vote -> !vote.getType().equalsIgnoreCase("DAC"))
                .collect(Collectors.toList());

        if (CollectionUtils.isNotEmpty(votesToInsert)) {
            votesToInsert.stream().forEach((v) -> {
                v.initVote(newMember.getDacUserId(), new Date(), null, null, false, false);
            });
            voteDAO.batchVotesInsert(votesToInsert);
        }
    }

}
