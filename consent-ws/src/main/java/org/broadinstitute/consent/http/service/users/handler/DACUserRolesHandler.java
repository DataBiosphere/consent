package org.broadinstitute.consent.http.service.users.handler;

import com.google.common.collect.Lists;
import com.mongodb.BasicDBObject;
import com.mongodb.Block;
import com.mongodb.client.FindIterable;
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
import org.broadinstitute.consent.http.enumeration.ElectionStatus;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.models.DACUser;
import org.broadinstitute.consent.http.models.DACUserRole;
import org.broadinstitute.consent.http.models.DatasetAssociation;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.Role;
import org.broadinstitute.consent.http.models.Vote;
import org.broadinstitute.consent.http.util.DarConstants;
import org.bson.Document;

public class DACUserRolesHandler extends AbstractUserRolesHandler {

    private DACUserDAO dacUserDAO;
    private ElectionDAO electionDAO;
    private VoteDAO voteDAO;
    private DACUserRoleDAO userRoleDAO;
    private DataSetAssociationDAO datasetAssociationDAO;
    private MongoConsentDB mongo;


    private final String MEMBER = DACUserRoles.MEMBER.getValue();
    private final String ADMIN = DACUserRoles.ADMIN.getValue();
    private final String CHAIRPERSON = DACUserRoles.CHAIRPERSON.getValue();
    private final String RESEARCHER = DACUserRoles.RESEARCHER.getValue();
    private final String DATA_OWNER = DACUserRoles.DATA_OWNER.getValue();
    private final String ALUMNI = DACUserRoles.ALUMNI.getValue();

    // DacMembers(3) AND (1) Chairperson
    private final Integer MINIMUM_DAC_USERS = 3;

    private Map<String, Integer> roleIdMap;

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
        for (Role r : roles) {
            rolesMap.put(r.getName().toUpperCase(), r.getRoleId());
        }
        return rolesMap;
    }

    /**
     * Delete the member votes, returns a list with the pending votes.
     * @param oldMember The member the votes are related to.
     *
     * @return The list of votes the user has still pending to vote.
     *
     */
    private List<Vote> removeMemberVotes(DACUser oldMember){
        /** We want to keep the ones made, so we won't delete them. We'll throw them away from the list.*/
        List<Vote> votesNotMadeForUser = findMemberVotesForUser(oldMember).stream().filter(vote -> Objects.isNull(vote.getVote())).collect(Collectors.toList());
        removeVotes(votesNotMadeForUser.stream().map(vote -> vote.getVoteId()).collect(Collectors.toList()));
        return votesNotMadeForUser;
    }


    /**
     * Finds ALL the votes related to a member, pending or done.
     * @param member The member the votes are related to.
     *
     * @return The list of votes the user has, pending or not.
     */
    private List<Vote> findMemberVotesForUser(DACUser member){
        List<Vote> votes = new ArrayList<>();
        List<Election> electionList =  electionDAO.getOpenElectionsForMember();
        if(CollectionUtils.isNotEmpty(electionList)){
            votes.addAll(voteDAO.findVotesByElectionsIdsAndUserId(electionList.stream().map(Election::getElectionId).collect(Collectors.toList()), member.getDacUserId()));
        }
        return votes;
    }

    /**
     * Helper method, receives a List of vote IDs and deletes them. If empty, no action is performed
     *
     * @param votesId List of Ids of the votes we need to delete.
     */
    private void removeVotes(List<Integer> votesId) {
        if(!CollectionUtils.isEmpty(votesId)){
            voteDAO.removeVotesById(votesId);
        }
    }

    /**
     * Receives the List of votes to update and insert. All previous values will be set to
     * null/false, except the vote type, which we want to keep.
     *
     * @param newMember Member to assign the votes to.
     * @param votesToInsert List of the votes previously owned by the old DacMember. This way
     *                      we won't have to guess the type (DAC, CHAIRPERSON, FINAL, AGREEMENT, etc.)
     */
    private void insertNewMemberVotes(DACUser newMember, List<Vote> votesToInsert){
        for(Vote v: votesToInsert){
            v.setDacUserId(newMember.getDacUserId());
            v.setCreateDate(new Date());
            v.setUpdateDate(null);
            v.setRationale(null);
            v.setReminderSent(false);
            v.setHasConcerns(false);
        }
        voteDAO.batchVotesInsert(votesToInsert);
    }

    /**
     * If newMember is null, it assumes oldMember does not need delegation of any kind, and all pending
     * votes will be deleted.
     *
     * If newMember is not null, it will insert votes in every election the oldMember had
     * pending votes.
     *
     * @param oldMember Member to assign the votes to.
     * @param newMember New member to take over oldMember.
     */
    private void transferVotesForDacMember(DACUser oldMember, DACUser newMember){
        List<Vote> votes = removeMemberVotes(oldMember);
        insertNewMemberVotes(newMember, votes);
    }

    /**
     * Makes the necessary transitions. If newMember is null, there's nothing
     * to delegate and all pending votes related to oldMember will be deleted.
     *
     * Otherwise, the pending votes will be assigned to newMember. We will
     * assign the member role here. We won't assign/remove any roles here
     * to oldMember, assuming those will be updated by the main process with
     * all the other role modifications he might have.
     *
     * @param oldMember Member that leaves the DacMember role.
     * @param newMember New member to take over oldMember.
     */
    private void removeDacMember(DACUser oldMember, DACUser newMember){
        if(Objects.isNull(newMember)){
            removeMemberVotes(oldMember);
        } else {
            transferVotesForDacMember(oldMember, newMember);
            assignNewRole(newMember, new DACUserRole(roleIdMap.get(MEMBER), MEMBER, true));
        }
    }

    /**
     * When there is open elections, you should call this method.
     *
     * @param oldMember Member that leaves the DacMember role.
     * @param newMember New member to take over oldMember.
     */
    private void removeChairperson(DACUser oldMember, DACUser newMember){
            transferVotesForDacMember(oldMember, newMember);
            assignNewRole(newMember, new DACUserRole(roleIdMap.get(CHAIRPERSON), CHAIRPERSON, true));
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
        removeRole(updatedUser, ADMIN);
    }

    /**
     * Assigns userToAssignRole the role sent as a parameter, if he
     * does not have it yet.
     *
     * @param userToAssignRole Member that leaves the DacMember role.
     * @param role New member to take over oldMember.
     */
    private void assignNewRole(DACUser userToAssignRole, DACUserRole role) {
        if(!containsRole(userToAssignRole.getRoles(), role.getName())){
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
        for(DACUserRole r: roleList){
            r.setRoleId(roleIdMap.get(r.getName().toUpperCase()));
        }
        return roleList;
    }

    /**
     *  Helper method that removes the ownership of the datasets from the database.
     * @param updatedUser User that is being removed and is no longer owner of the datasets.
     */
    private void deleteDatasetsOwnership(DACUser updatedUser) {
        datasetAssociationDAO.deleteDatasetRelationshipsForUser(updatedUser.getDacUserId());
    }

    /**
     * This might be helpful to remove roles. Opposite to what it happens with insertion,
     * if we try to delete something that it doesn't exist there are no errors. No need
     * to double check.
     *
     * @param user The user from which the role is being removed.
     * @param role The role we want to remove.
     */
    private void removeRole(DACUser user, String role) {
        userRoleDAO.removeSingleUserRole(user.getDacUserId(), roleIdMap.get(role));
    }

    /**--------------------------------------------------------------------------------------------------------------------------------------------------------*/
    /*** DANGER LINE!!!!! -------------------------------------------------------------------------------------------------------------------------------------*/
    /**--------------------------------------------------------------------------------------------------------------------------------------------------------*/
    //TODO: All the code below this line is NOT refactored!
    /**
     * Receives a map of users and decides what to do with them.
     * If we won't delegate, there'll only be one parameter, 'updatedUser'
     * If we delegate member or chair, there will be also an 'userToDelegate'
     * If we delegate data ownership, there will a third member in the map, 'alternativeDataOwnerUser'.
     * @param usersMap Map with the members we need to update. Entry: User Identification(explained above) -> DacUser
     *
     * */
    /** TODO: This was the MAIN method. */
    public void updateRoles(Map<String, DACUser> usersMap) throws UserRoleHandlerException {
        try {
            userRoleDAO.begin();
            voteDAO.begin();
            DACUser updatedUser = usersMap.get("updatedUser");
            DACUser userToDelegate = null;
            boolean delegateMember = usersMap.containsKey("userToDelegate") ? true : false;
            boolean delegateOwner = usersMap.containsKey("alternativeDataOwnerUser") ? true : false;

            /* We need the previous roles to check which ones are the ones missing (removed) and new ones(added)*/
            List<DACUserRole> updatedRoles = updatedUser.getRoles();
            List<DACUserRole> existentRoles = userRoleDAO.findRolesByUserId(updatedUser.getDacUserId());
            List<DACUserRole> removedRoles = substractAllRoles(existentRoles, updatedRoles);
            List<DACUserRole> newRoles = substractAllRoles(updatedRoles, existentRoles);
            // All the users that come from the ui have no roles set. This is a must!
            updatedUser.setRoles(existentRoles);


            // Delegation of DataOwners is not conditioned by the other elections or other roles.
            if(containsRole(removedRoles, DATA_OWNER)){
                DACUser doUserToDelegate = null;
                if(delegateOwner){
                    doUserToDelegate = usersMap.get("alternativeDataOwnerUser");
                    doUserToDelegate.setDacUserId(dacUserDAO.findDACUserByEmail(doUserToDelegate.getEmail()).getDacUserId());
                    doUserToDelegate.setRoles(userRoleDAO.findRolesByUserId(doUserToDelegate.getDacUserId()));
                }
                removeDataOwner(updatedUser, doUserToDelegate, delegateOwner);
            }

            // If there aren't any open elections and we didn't delegate to any member then we don't have to validate anything.
            if(electionDAO.verifyOpenElections() == 0 && (!delegateMember || !delegateOwner)){
                changeRolesWithoutDelegation(updatedUser, removedRoles, newRoles, delegateMember);
            }else {
                if (delegateMember) {
                    userToDelegate = usersMap.get("userToDelegate");
                    userToDelegate.setDacUserId(dacUserDAO.findDACUserByEmail(userToDelegate.getEmail()).getDacUserId());
                    userToDelegate.setRoles(userRoleDAO.findRolesByUserId(userToDelegate.getDacUserId()));
                }
                for (DACUserRole role : removedRoles) {

                    // CHAIRPERSON REMOVED
                    if (role.getName().toUpperCase().equals(CHAIRPERSON.toUpperCase())) {
                        removeChairperson(updatedUser, delegateMember, !containsRole(newRoles, MEMBER), userToDelegate);
                    }

                    // MEMBER REMOVED
                    if (role.getName().toUpperCase().equals(MEMBER.toUpperCase())) {
                        removeDacMember(updatedUser, userToDelegate);
                    }
                    // ALUMNI REMOVED
                    if (role.getName().toUpperCase().equals(ALUMNI.toUpperCase())) {
                        userRoleDAO.removeSingleUserRole(updatedUser.getDacUserId(), roleIdMap.get(ALUMNI));
                    }

                    // ADMIN REMOVED
                    if (role.getName().toUpperCase().equals(ADMIN.toUpperCase())) {
                        removeAdmin(updatedUser);
                    }

                    // RESEARCHER REMOVED
                    if (role.getName().toUpperCase().equals(RESEARCHER.toUpperCase())) {
                        removeResearcher(updatedUser);
                    }
                }

                for (DACUserRole role : newRoles) {
                    // ADD CHAIRPERSON
                    if (role.getName().toUpperCase().equals(CHAIRPERSON.toUpperCase())) {
                        changeChairperson(updatedUser, true);
                    }
                    // ADD MEMBER
                    if (role.getName().toUpperCase().equals(MEMBER.toUpperCase())) {
                        removeRole(updatedUser, ALUMNI);
                        removeRole(updatedUser, MEMBER);
                        assignNewRole(updatedUser, new DACUserRole(roleIdMap.get(MEMBER), MEMBER, true));
                    }
                    // ADD ADMIN
                    if (role.getName().toUpperCase().equals(ADMIN.toUpperCase())) {
                        assignNewRole(updatedUser, new DACUserRole(roleIdMap.get(ADMIN), ADMIN, true));
                    }
                    // ADD ALUMNI
                    if ((role.getName().toUpperCase().equals(ALUMNI.toUpperCase())) && (!containsAnyRole(removedRoles, new String[]{MEMBER, CHAIRPERSON}))) {
                        if (containsAnyRole(newRoles, new String[]{MEMBER, CHAIRPERSON})) {
                            throw new UserRoleHandlerException("User to delegate: " + userToDelegate.getDisplayName() + " has a role: " +
                                    " [Member] or [Chairperson] that is incompatible with the role you want to assign.");
                        } else {
                            assignNewRole(updatedUser, new DACUserRole(roleIdMap.get(ALUMNI), ALUMNI, true));
                        }

                    }
                    // ADD DATA_OWNER
                    if (role.getName().toUpperCase().equals(DATA_OWNER.toUpperCase())) {
                        assignNewRole(updatedUser, new DACUserRole(roleIdMap.get(DATA_OWNER), DATA_OWNER, true));
                    }

                    // ADD RESEARCHER
                    if (role.getName().toUpperCase().equals(RESEARCHER.toUpperCase())) {
                        if (containsAnyRole(newRoles, new String[]{MEMBER, CHAIRPERSON})) {
                            throw new UserRoleHandlerException("User to delegate: " + userToDelegate.getDisplayName() + " has a role: " +
                                    " [Member] or [Chairperson] that is incompatible with the role you want to assign.");
                        } else {
                            assignNewRole(updatedUser, new DACUserRole(roleIdMap.get(RESEARCHER), RESEARCHER, true));
                        }
                    }
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

    /** Fairly simple, when there aren't open elections we don't need to check anything. Simply update the roles for the updated users.
     *  If the number of members/chairperson is below the minimum it will be checked on the election's creation.
     *
     * @param updatedUser DacUser to update
     * @param removedRoles Roles to add (new)
     * @param newRoles Roles to remove(delete)
     * @param delegateChairperson TODO: Refactor and remove this variable
     * */
    private void changeRolesWithoutDelegation(DACUser updatedUser, List<DACUserRole> removedRoles, List<DACUserRole> newRoles, boolean delegateChairperson) {
        if(!CollectionUtils.isEmpty(removedRoles)){
            userRoleDAO.removeUserRoles(updatedUser.getDacUserId(), removedRoles.stream().map(dacUserRole -> dacUserRole.getRoleId()).collect(Collectors.toList()));
        }
        if(!CollectionUtils.isEmpty(newRoles)){
            if(containsAnyRole(newRoles, new String[]{CHAIRPERSON}) && !Objects.isNull(dacUserDAO.findChairpersonUser())){
                changeChairperson(updatedUser, delegateChairperson);
                newRoles = newRoles.stream().filter(dacUserRole -> !dacUserRole.getName().toUpperCase().equals(CHAIRPERSON)).collect(Collectors.toList());
            }
            if(!CollectionUtils.isEmpty(newRoles)){
                userRoleDAO.insertUserRoles(generateRoleIdList(newRoles), updatedUser.getDacUserId());
            }
        }
    }

    /**
     * TODO : This won't happen this way (we won't switch chairpersons. Create a method to remove a chairperson
     *       get all his votes and then, with them, insert them again for the new chairperson.
     *
     */
    private void changeChairperson(DACUser updatedUser, boolean delegateChairperson) {
        DACUser currentChairperson = dacUserDAO.findChairpersonUser();
        currentChairperson.setRoles(userRoleDAO.findRolesByUserId(currentChairperson.getDacUserId()));
        delegateChairpersonResponsabilities(currentChairperson, updatedUser, delegateChairperson);
        assignNewRole(updatedUser, new DACUserRole(roleIdMap.get(CHAIRPERSON), CHAIRPERSON, true));
        removeRole(currentChairperson, CHAIRPERSON);
        assignNewRole(currentChairperson, new DACUserRole(roleIdMap.get(ALUMNI), ALUMNI, true));
        removeRole(updatedUser, MEMBER);
        removeRole(updatedUser, ALUMNI);
    }


    /**
     *  Removes data owners role, assigns votes (update) if needed, then removes and assigns the role.
     * @param updatedUser
     * @param userToDelegate
     * @param delegate
     * @throws UserRoleHandlerException
     */
    /**
     * TODO: Remove delegate variable. Main method will know if delegation is needed.
     *  I think this method should be split in several: one to remove the data owner and
     *  another to assign a data owner and his votes, if needed.
     */
    private void removeDataOwner(DACUser updatedUser, DACUser userToDelegate, boolean delegate) throws UserRoleHandlerException {
        if (delegate) {
            List<Integer> doElectionsIds = electionDAO.getOpenElections(ElectionType.DATA_SET.getValue()).stream().map(Election::getElectionId).collect(Collectors.toList());
            if(!CollectionUtils.isEmpty(doElectionsIds)){
                List<Vote> votes = voteDAO.findDataOwnerVotesByElectionsIdsAndUserId(doElectionsIds, updatedUser.getDacUserId());
                updateDacUserIdForVotes(votes, userToDelegate.getDacUserId());
            }
            assignNewRole(userToDelegate, new DACUserRole(roleIdMap.get(DATA_OWNER), DATA_OWNER, true));
            updateDatasetsOwnership(updatedUser, userToDelegate);
        } else {
            deleteDatasetsOwnership(updatedUser);
        }
        removeRole(updatedUser, DATA_OWNER);
    }

    /**
     * TODO: Remove delegate variable. Main method will know if delegation is needed.
     *  I think this method should be split in several: one to remove the chairperson and
     *  another to assign a data owner and his votes, if needed. Kind of the same as the
     *  members/dataowners.
     */
    private void removeChairperson(DACUser updatedUser, boolean delegateChairperson, boolean changeToAlumni, DACUser userToDelegate) throws UserRoleHandlerException {
        if (!delegateChairperson)
            throw new UserRoleHandlerException("Must provide the user to whom delegate the pending cases of the previous chairperson.");
        if (containsAnyRole(userToDelegate.getRoles(), new String[]{RESEARCHER})) {
            throw new UserRoleHandlerException("User to delegate: " + userToDelegate.getDisplayName() + " has a role: [Researcher]" +
                    " that is incompatible with the role you want to assign.");
        }
        delegateChairpersonResponsabilities(updatedUser, userToDelegate, delegateChairperson);
        assignNewRole(userToDelegate, new DACUserRole(roleIdMap.get(CHAIRPERSON), CHAIRPERSON, true));
        removeRole(userToDelegate, MEMBER);
        removeRole(userToDelegate, ALUMNI);
        removeRole(updatedUser, CHAIRPERSON);
        if(changeToAlumni){
            assignNewRole(updatedUser, new DACUserRole(roleIdMap.get(ALUMNI), ALUMNI, true));
        }
    }



    private void removeResearcher(DACUser updatedUser) throws UserRoleHandlerException {
        try{
            FindIterable<Document> accessList =  mongo.getDataAccessRequestCollection().find(new BasicDBObject(DarConstants.USER_ID, updatedUser.getDacUserId()));
            List<String> accessRequestIds = getRequestIds(accessList);
            if(!CollectionUtils.isEmpty(accessRequestIds)){
                electionDAO.updateElectionStatusByReferenceAndStatus(accessRequestIds, ElectionStatus.OPEN.getValue(),ElectionStatus.CANCELED.getValue());
            }
        } catch(Exception e){
            throw e;
        }
        removeRole(updatedUser, RESEARCHER);
    }

    private List getRequestIds(FindIterable<Document> access) {
        List<String> accessIds = new ArrayList<>();
        if (access != null) {
            access.forEach((Block<Document>) document -> {
                accessIds.add(document.get(DarConstants.ID).toString());
            });
        }
        return accessIds;
    }

    /**
     * TODO: Verify if this method is needed for the Data Owners. I think not, since
     * we will be inserting the votes for the delegated users.
     */
    private void updateDatasetsOwnership(DACUser updatedUser, DACUser userToDelegate) {
        List<DatasetAssociation> associations = datasetAssociationDAO.findAllDatasetAssociationsByOwnerId(updatedUser.getDacUserId());
        for(DatasetAssociation as: associations){
            as.setDacuserId(userToDelegate.getDacUserId());
        }
        deleteDatasetsOwnership(updatedUser);
        datasetAssociationDAO.insertDatasetUserAssociation(associations);
    }

    /**
     * Checks if an user already contains the role. Handy because if we
     * try to insert a role to an user that already has that role, we'll
     * end up with a 500. Ups.
     *
     */
    public boolean containsRole(Collection<DACUserRole> roles, String rol){
        return roles.stream().anyMatch(r -> r.getName().equalsIgnoreCase(rol));
    }

    /**
     * Returns true if the user has any of the roles sent as a parameter.
     *
     */
    public boolean containsAnyRole(Collection<DACUserRole> roles, String [] invalidRoles){
        for(String rol : invalidRoles){
            if(containsRole(roles,rol)){ return true;}
        }
        return false;
    }

    /**
     * Applies the difference to get the roles the user didn't have.
     *
     */
    private List<DACUserRole> substractAllRoles(List<DACUserRole> roles,List<DACUserRole> toSubstractRoles){
        List<String> toSubstractRolesNames = toSubstractRoles.stream().map(role -> role.getName().toUpperCase()).collect(Collectors.toList());
        return   roles.stream().filter(rol -> !toSubstractRolesNames.contains(rol.getName().toUpperCase())).collect(Collectors.toList());
    }

    /**
     *  Updates the votes of the previous chairperson and assigns them to the new one.
     * @param oldDacMember
     * @param newDacMember
     * @param delegate TODO: Remove, no longer needed, this will be responsibility of the main method.
     */
    /**
     * TODO: Verify if this method is needed for the Chairperson. I think not, since
     * we will be inserting the votes for the delegated users.
     */
    private void delegateChairpersonResponsabilities(DACUser oldDacMember, DACUser newDacMember, boolean delegate){
        List<Election> elections = collectElectionsForChairperson();
        List <Vote> votes = collectVotesForChairperson(elections, oldDacMember);
        if(CollectionUtils.isNotEmpty(votes)) {
            updateDacUserIdForVotes(votes, newDacMember.getDacUserId());
        }
        if(delegate){
            electionDAO.updateElectionStatus(elections.stream().map(Election::getElectionId).collect(Collectors.toList()), ElectionStatus.OPEN.getValue());
        }
    }

    /**
     *  Finds all the open elections where this chairperson is participating.
     * @return List of the elections this user participates into.
     */
    /**
     * TODO: I think we can create another query, something similar to the query of the DacMember, and get all
     * the elections in a single query.
     */
    private List<Election> collectElectionsForChairperson() {
        Set<Election> electionList = new HashSet<>();
        electionList.addAll(electionDAO.getOpenAndFinalElections(ElectionType.DATA_ACCESS.getValue()));
        electionList.addAll(electionDAO.getOpenAndFinalElections(ElectionType.RP.getValue()));
        electionList.addAll(electionDAO.getOpenElections(ElectionType.TRANSLATE_DUL.getValue()));
        return Lists.newArrayList(electionList);
    }

    /**
     * Collects all the votes for the chairperson. The main difference between this and the member collect
     * is that we want to assign the made votes to the new Chairperson, so even the made ones will be updated.
     *
     * TODO: We talked about refactoring this to an DELETE/INSERT approach, so I think an update no longer applies.
     * This method it might be helpful though.
     *
     * @param electionList The list of the elections this user is participating into.
     * @param oldDacMember The member that will leave the chairperson role.
     * @return
     */
    private List<Vote> collectVotesForChairperson(List<Election> electionList, DACUser oldDacMember){
        List<Vote> votes = new ArrayList<>();
        if(CollectionUtils.isNotEmpty(electionList)){
            votes.addAll(voteDAO.findVotesByElectionsIdsAndUserIdIncludeMade(electionList.stream().map(Election::getElectionId).collect(Collectors.toList()), oldDacMember.getDacUserId()));
        }
        return votes;
    }

    /**
     * TODO: I think this must be removed.
     * @param votes
     * @param dacUserId
     */
    private void updateDacUserIdForVotes(List<Vote> votes, Integer dacUserId) {
        if(!CollectionUtils.isEmpty(votes)){
            voteDAO.updateUserIdForVotes(votes.stream().map(Vote::getVoteId).collect(Collectors.toList()), dacUserId);
        }
    }



}