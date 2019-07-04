package org.broadinstitute.consent.http.service.users;

import com.google.inject.Inject;
import com.mongodb.BasicDBObject;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.map.HashedMap;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.db.DataSetAssociationDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.db.ResearcherPropertyDAO;
import org.broadinstitute.consent.http.db.UserRoleDAO;
import org.broadinstitute.consent.http.db.VoteDAO;
import org.broadinstitute.consent.http.db.mongo.MongoConsentDB;
import org.broadinstitute.consent.http.enumeration.Actions;
import org.broadinstitute.consent.http.enumeration.ElectionStatus;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.enumeration.VoteType;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.Role;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.models.dto.PatchOperation;
import org.broadinstitute.consent.http.service.users.handler.UserRoleHandlerException;
import org.broadinstitute.consent.http.util.DarConstants;
import org.bson.Document;
import org.skife.jdbi.v2.exceptions.UnableToExecuteStatementException;

import javax.ws.rs.NotAuthorizedException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@SuppressWarnings("Duplicates")
// TODO: Once all user functions are migrated here, remove other duplicate code.
public class UserService {

    private UserDAO userDAO;
    private UserRoleDAO userRoleDAO;
    private ElectionDAO electionDAO;
    private VoteDAO voteDAO;
    private DataSetAssociationDAO dataSetAssociationDAO;
    private ResearcherPropertyDAO researcherPropertyDAO;
    private MongoConsentDB mongoDB;

    private final Map<String, Integer> roleIdMap;
    private final String DISPLAY_NAME = "displayName";
    private final String ROLES = "roles";

    @Inject
    public UserService(UserDAO userDAO, UserRoleDAO userRoleDAO, ElectionDAO electionDAO,
                       VoteDAO voteDAO, DataSetAssociationDAO dataSetAssociationDAO,
                       ResearcherPropertyDAO researcherPropertyDAO, MongoConsentDB mongoDB) {
        this.userDAO = userDAO;
        this.userRoleDAO = userRoleDAO;
        this.electionDAO = electionDAO;
        this.voteDAO = voteDAO;
        this.dataSetAssociationDAO = dataSetAssociationDAO;
        this.researcherPropertyDAO = researcherPropertyDAO;
        this.mongoDB = mongoDB;
        this.roleIdMap = createRoleMap(userRoleDAO.findRoles());
    }

    public User createUser(User user, String userEmail) {
        validateEmail(user.getEmail(), userEmail);
        user.setEmail(userEmail);
        validateRoles(user.getRoles());
        return createUser(user);
    }

    public User createUser(User user) throws IllegalArgumentException {
        validateRequiredFields(user);
        Integer dacUserID;
        try {
            dacUserID = userDAO.insertUser(user.getEmail(), user.getDisplayName(), new Date());
        } catch (UnableToExecuteStatementException e) {
            throw new IllegalArgumentException("Email should be unique.", e);
        }
        if (user.getRoles() != null) {
            insertUserRoles(user, dacUserID);
        }
        User savedUser = userDAO.findUserById(dacUserID);
        savedUser.setRoles(userRoleDAO.findRolesByUserId(savedUser.getUserId()));
        return savedUser;
    }

    public User updateUser(User user, String userEmail) throws IllegalArgumentException, UserRoleHandlerException {
        validateEmail(user.getEmail(), userEmail);
        user.setEmail(userEmail);
        validateRoles(user.getRoles());
        User existentUser = userDAO.findUserByEmail(user.getEmail());
        validateAndUpdateRoles(existentUser.getRoles(), user.getRoles(), existentUser);
        userDAO.updateUser(user.getDisplayName(), existentUser.getUserId());
        return userDAO.findUserById(existentUser.getUserId());
    }

    public User updatePartialUser(List<PatchOperation> patchOperations, String name) throws UserRoleHandlerException {
        User user = userDAO.findUserByEmail(name);
        patchOperations.stream().forEach(action -> {
            if (action.getPath().equalsIgnoreCase(DISPLAY_NAME)) {
                setDisplayName(user, action);
            } else if (action.getPath().equalsIgnoreCase(ROLES)) {
                setRoles(user, action);
            } else {
                throw new IllegalArgumentException("Invalid path.");
            }
        });
        if (user.getRoles().size() == 0) throw new IllegalArgumentException("Roles are required");
        return updateUser(user, name);
    }


    // Helper Methods
    // TODO: All of these are copied from DatabaseUserAPI and DatabaseDACUserApi
    // Merge and consolidate as needed.

    private void validateEmail(String emailToValidate, String email) {
        if (StringUtils.isNotEmpty(emailToValidate) && !emailToValidate.equalsIgnoreCase(email)) {
            throw new NotAuthorizedException("You don't have permission to update the specified user.", "message");
        }
    }

    private void validateRoles(List<UserRole> roles) {
        if (CollectionUtils.isNotEmpty(roles)) {
            roles.forEach(role -> {
                if (!(role.getName().equalsIgnoreCase(UserRoles.DATAOWNER.getValue())
                        || role.getName().equalsIgnoreCase(UserRoles.RESEARCHER.getValue()))) {
                    throw new IllegalArgumentException("Invalid role: " + role.getName() + ". Valid roles are: " + UserRoles.DATAOWNER.getValue() + " and " + UserRoles.RESEARCHER.getValue());
                }
            });
        } else {
            throw new IllegalArgumentException("Roles are required.");
        }
    }

    private void validateRequiredFields(User newDac) {
        if (StringUtils.isEmpty(newDac.getDisplayName())) {
            throw new IllegalArgumentException("Display Name can't be null. The user needs a name to display.");
        }
        if (StringUtils.isEmpty(newDac.getEmail())) {
            throw new IllegalArgumentException("The user needs a valid email to be able to login.");
        }
    }

    private void insertUserRoles(User user, Integer dacUserId) {
        List<UserRole> roles = user.getRoles();
        roles.forEach(r -> {
            r.setRoleId(userRoleDAO.findRoleIdByName(r.getName()));
            if (Objects.isNull(r.getEmailPreference())) {
                r.setEmailPreference(true);
            }
        });
        userRoleDAO.insertUserRoles(roles, dacUserId);
    }

    private void validateAndUpdateRoles(List<UserRole> existentRoles, List<UserRole> newRoles, User user) throws UserRoleHandlerException {
        Map<Integer, Integer> rolesToRemove = new HashedMap();
        Map<Integer, Integer> rolesToAdd = new HashedMap();
        updateDataOwnerRole(existentRoles, newRoles, user, rolesToRemove, rolesToAdd);
        updateResearcherRole(existentRoles, newRoles, user, rolesToRemove, rolesToAdd);
        rolesToRemove.forEach((userId, roleId) -> {
            userRoleDAO.removeSingleUserRole(userId, roleId);
        });
        rolesToAdd.forEach((userId, roleId) -> {
            userRoleDAO.insertSingleUserRole(roleId, userId, false);
        });
    }

    private void updateDataOwnerRole(List<UserRole> existentRoles, List<UserRole> newRoles, User user, Map<Integer, Integer> rolesToRemove, Map<Integer, Integer> rolesToAdd) throws UserRoleHandlerException {
        boolean isDO = containsRole(existentRoles, UserRoles.DATAOWNER.getValue());
        boolean isNewDO = containsRole(newRoles, UserRoles.DATAOWNER.getValue());
        //remove data owner
        if (isDO && !isNewDO) {
            if (hasDataSetAssociation(user)) {
                throw new UserRoleHandlerException("Role can not be removed. The specified user is the only data owner associated to a data set that needs approval.");
            } else if (hasOpenElections(user)) {
                throw new UserRoleHandlerException("Role can not be removed. There are open dataset elections for this user.");
            } else {
                rolesToRemove.put(user.getUserId(), roleIdMap.get(UserRoles.DATAOWNER.getValue()));
            }
        }
        //add data owner
        if (!isDO && isNewDO) {
            rolesToAdd.put(user.getUserId(), roleIdMap.get(UserRoles.DATAOWNER.getValue()));

        }
    }

    private void updateResearcherRole(List<UserRole> existentRoles, List<UserRole> newRoles, User user, Map<Integer, Integer> rolesToRemove, Map<Integer, Integer> rolesToAdd) throws UserRoleHandlerException {
        boolean isResearcher = containsRole(existentRoles, UserRoles.RESEARCHER.getValue());
        boolean isNewResearcher = containsRole(newRoles, UserRoles.RESEARCHER.getValue());
        //remove researcher
        if (isResearcher && !isNewResearcher) {
            BasicDBObject query = new BasicDBObject(DarConstants.STATUS, new BasicDBObject("$ne", ElectionStatus.CANCELED.getValue()));
            query.append("userId", user.getUserId());
            Document dar = mongoDB.getDataAccessRequestCollection().find(query).first();
            if (dar != null) {
                throw new UserRoleHandlerException("Role can not be removed. The specified user has open DAR.");
            } else {
                rolesToRemove.put(user.getUserId(), roleIdMap.get(UserRoles.RESEARCHER.getValue()));
            }
        }
        //add researcher
        if (!isResearcher && isNewResearcher) {
            if (containsRole(existentRoles, UserRoles.CHAIRPERSON.getValue()) || containsRole(existentRoles, UserRoles.MEMBER.getValue())) {
                throw new UserRoleHandlerException("Role can not be added. Researcher  is incompatible with  Member and Chairperson role.");
            } else {
                rolesToRemove.put(user.getUserId(), roleIdMap.get(UserRoles.RESEARCHER.getValue()));
            }
        }
    }

    private boolean containsRole(List<UserRole> roles, String roleName) {
        boolean result = false;
        if (CollectionUtils.isNotEmpty(roles)) {
            result = roles.parallelStream().anyMatch(role -> role.getName().equalsIgnoreCase(roleName));
        }
        return result;
    }

    private boolean hasDataSetAssociation(User updatedUser) {
        List<Integer> associatedDataSetId = dataSetAssociationDAO.getDataSetsIdOfDataOwnerNeedsApproval(updatedUser.getUserId());
        // verify if it's the only data owner associeted to a data set
        if (CollectionUtils.isNotEmpty(associatedDataSetId)) {
            List<Integer> dataOwnersPerDataSet = dataSetAssociationDAO.getCountOfDataOwnersPerDataSet(associatedDataSetId);
            if (dataOwnersPerDataSet.stream().anyMatch(dataOwners -> (dataOwners == 1))) {
                return true;
            }
        }
        return false;
    }

    private boolean hasOpenElections(User updatedUser) {
        List<Integer> openElectionIdsForThisUser = electionDAO.findDataSetOpenElectionIds(updatedUser.getUserId());
        if (CollectionUtils.isNotEmpty(openElectionIdsForThisUser)) {
            List<Integer> voteCount = voteDAO.findVoteCountForElections(openElectionIdsForThisUser, VoteType.DATA_OWNER.getValue());
            if (voteCount.stream().anyMatch((votes) -> ((votes) == 1))) {
                return true;
            }
        }
        return false;
    }

    /**
     * This method generates a map with the current role Ids, loaded from the
     * database on class initialization. Entry: Role name (UPPERCASE) -> Role ID
     */
    private Map<String, Integer> createRoleMap(List<Role> roles) {
        Map<String, Integer> rolesMap = new HashMap();
        roles.stream().forEach((r) -> rolesMap.put(r.getName().toUpperCase(), r.getRoleId()));
        return rolesMap;
    }

    private void setDisplayName(User user, PatchOperation action) {
        if (action.getOp().equalsIgnoreCase(Actions.REPLACE.getValue())) {
            user.setDisplayName(action.getValue());
        } else {
            throw new IllegalArgumentException(action.getOp() + " operation is not supported for this field. The only supported operation for this field is replace.");
        }
    }

    private void setRoles(User user, PatchOperation action) {
        String roles[] = action.getValue().split(",");
        List<UserRole> userRoles = getRoles(roles);
        if (action.getOp().contains(Actions.REPLACE.getValue())) {
            user.setRoles(userRoles);
        } else if (action.getOp().contains(Actions.ADD.getValue())) {
            user.getRoles().addAll(userRoles);
        } else if (action.getOp().contains(Actions.REMOVE.getValue())) {
            userRoles.stream().forEach(role -> user.getRoles().remove(role));
        } else {
            throw new IllegalArgumentException("Operation is not allowed. Allowed operations are: replace, remove and add");
        }
    }

    private List<UserRole> getRoles(String[] roles) {
        List<UserRole> userRoles = new ArrayList<>();
        if (roles != null && roles.length > 0) {
            for (int i = 0; i < roles.length; i++) {
                UserRole role = new UserRole();
                role.setName(roles[i].trim());
                role.setRoleId(roleIdMap.get(roles[i].toUpperCase()));
                userRoles.add(role);
            }
        }
        return userRoles;
    }

}
