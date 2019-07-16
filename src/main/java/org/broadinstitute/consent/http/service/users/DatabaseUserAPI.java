package org.broadinstitute.consent.http.service.users;

import com.mongodb.BasicDBObject;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.consent.http.db.DACUserDAO;
import org.broadinstitute.consent.http.db.DataSetAssociationDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.db.ResearcherPropertyDAO;
import org.broadinstitute.consent.http.db.UserRoleDAO;
import org.broadinstitute.consent.http.db.VoteDAO;
import org.broadinstitute.consent.http.db.mongo.MongoConsentDB;
import org.broadinstitute.consent.http.enumeration.Actions;
import org.broadinstitute.consent.http.enumeration.ElectionStatus;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.DACUser;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.models.dto.PatchOperation;
import org.broadinstitute.consent.http.service.users.handler.UserHandlerAPI;
import org.broadinstitute.consent.http.service.users.handler.UserRoleHandlerException;
import org.broadinstitute.consent.http.util.DarConstants;
import org.bson.Document;

import javax.ws.rs.NotAuthorizedException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class DatabaseUserAPI extends DatabaseDACUserAPI implements UserAPI {

    private MongoConsentDB mongo;

    private final String DISPLAY_NAME = "displayName";

    private final String ROLES = "roles";

    public DatabaseUserAPI(DACUserDAO userDAO, UserRoleDAO roleDAO, ElectionDAO electionDAO, VoteDAO voteDAO, DataSetAssociationDAO dataSetAssociationDAO, UserHandlerAPI userHandlerAPI, MongoConsentDB mongo, ResearcherPropertyDAO  researcherPropertyDAO) {
        super(userDAO, roleDAO, electionDAO, voteDAO, dataSetAssociationDAO, userHandlerAPI, researcherPropertyDAO);
        this.mongo = mongo;
    }

    @Override
    public DACUser createUser(DACUser user, String userEmail) {
        validateEmail(user.getEmail(), userEmail);
        user.setEmail(userEmail);
        validateRoles(user.getRoles());
        return createDACUser(user);
    }

    @Override
    public DACUser updateUser(DACUser user, String userEmail) throws IllegalArgumentException, UserRoleHandlerException {
        validateEmail(user.getEmail(), userEmail);
        user.setEmail(userEmail);
        validateRoles(user.getRoles());
        DACUser existentUser = describeDACUserByEmail(user.getEmail());
        validateAndUpdateRoles(existentUser.getRoles(), user.getRoles(), existentUser);
        dacUserDAO.updateDACUser(user.getDisplayName(), existentUser.getDacUserId());
        return describeDACUserById(existentUser.getDacUserId());
    }

    @Override
    public DACUser updatePartialUser(List<PatchOperation> patchOperations, String name) throws UserRoleHandlerException {
        DACUser user = describeDACUserByEmail(name);
        patchOperations.stream().forEach(action -> {
            if(action.getPath().equalsIgnoreCase(DISPLAY_NAME)){
                setDisplayName(user, action);
            }
            else if(action.getPath().equalsIgnoreCase(ROLES)){
                setRoles(user, action);
            }
            else{
                throw new IllegalArgumentException("Invalid path.");
            }
        });
        if(user.getRoles().size() == 0) throw new IllegalArgumentException("Roles are required");
        return updateUser(user, name);
    }

    private void setRoles(DACUser user, PatchOperation action) {
        String roles [] = action.getValue().split(",");
        List<UserRole> userRoles = getRoles(roles);
        if(action.getOp().contains(Actions.REPLACE.getValue())){
            user.setRoles(userRoles);
        }
        else if(action.getOp().contains(Actions.ADD.getValue())){
            user.getRoles().addAll(userRoles);
        }
        else if(action.getOp().contains(Actions.REMOVE.getValue())){
            userRoles.stream().forEach(role -> user.getRoles().remove(role));
        }
        else{
            throw new IllegalArgumentException("Operation is not allowed. Allowed operations are: replace, remove and add");
        }
    }

    private void setDisplayName(DACUser user, PatchOperation action) {
        if(action.getOp().equalsIgnoreCase(Actions.REPLACE.getValue())){
            user.setDisplayName(action.getValue());
        }else{
            throw new IllegalArgumentException(action.getOp() + " operation is not supported for this field. The only supported operation for this field is replace.");
        }
    }

    private void validateEmail(String emailToValidate, String email) {
        if (StringUtils.isNotEmpty(emailToValidate) && !emailToValidate.equalsIgnoreCase(email)) {
            throw new NotAuthorizedException("You don't have permission to update the specified user.", "message");
        }
    }

    private void validateRoles(List<UserRole> roles) {
        if (CollectionUtils.isNotEmpty(roles)) {
            roles.forEach(role -> {
                if (!(role.getName().equalsIgnoreCase(UserRoles.DATAOWNER.getRoleName())
                        || role.getName().equalsIgnoreCase(UserRoles.RESEARCHER.getRoleName()))) {
                    throw new IllegalArgumentException("Invalid role: " + role.getName() + ". Valid roles are: " + UserRoles.DATAOWNER.getRoleName() + " and " + UserRoles.RESEARCHER.getRoleName());
                }
            });
        }else{
            throw new IllegalArgumentException("Roles are required.");
        }
    }

    private void validateAndUpdateRoles(List<UserRole> existentRoles, List<UserRole> newRoles, DACUser user) throws UserRoleHandlerException {
        Map<Integer, Integer> rolesToRemove = new HashMap<>();
        Map<Integer, Integer> rolesToAdd = new HashMap<>();
        updateDataOwnerRole(existentRoles, newRoles, user, rolesToRemove, rolesToAdd);
        updateResearcherRole(existentRoles, newRoles, user, rolesToRemove, rolesToAdd);
        rolesToRemove.forEach(userRoleDAO::removeSingleUserRole);
        rolesToAdd.forEach((userId, roleId) -> {
            userRoleDAO.insertSingleUserRole(roleId, userId);
        });
    }

    private void updateDataOwnerRole(List<UserRole> existentRoles, List<UserRole> newRoles, DACUser user, Map<Integer, Integer>  rolesToRemove, Map<Integer, Integer>  rolesToAdd) throws UserRoleHandlerException {
        boolean isDO = containsRole(existentRoles, UserRoles.DATAOWNER.getRoleName());
        boolean isNewDO = containsRole(newRoles, UserRoles.DATAOWNER.getRoleName());
        //remove data owner
        if (isDO && !isNewDO) {
            if (hasDataSetAssociation(user)) {
                throw new UserRoleHandlerException("Role can not be removed. The specified user is the only data owner associated to a data set that needs approval.");
            }
            else if (hasOpenElections(user)) {
                throw new UserRoleHandlerException("Role can not be removed. There are open dataset elections for this user.");
            }
            else{
                rolesToRemove.put(user.getDacUserId(), roleIdMap.get(UserRoles.DATAOWNER.getRoleName()));
            }
        }
        //add data owner
        if (!isDO && isNewDO) {
            rolesToAdd.put(user.getDacUserId(), roleIdMap.get(UserRoles.DATAOWNER.getRoleName()));

        }
    }

    private void updateResearcherRole(List<UserRole> existentRoles, List<UserRole> newRoles, DACUser user, Map<Integer, Integer>  rolesToRemove, Map<Integer, Integer>  rolesToAdd) throws UserRoleHandlerException {
        boolean isResearcher = containsRole(existentRoles, UserRoles.RESEARCHER.getRoleName());
        boolean isNewResearcher = containsRole(newRoles, UserRoles.RESEARCHER.getRoleName());
        //remove researcher
        if (isResearcher && !isNewResearcher) {
            BasicDBObject query = new BasicDBObject(DarConstants.STATUS, new BasicDBObject("$ne",ElectionStatus.CANCELED.getValue()));
            query.append("userId", user.getDacUserId());
            Document dar = mongo.getDataAccessRequestCollection().find(query).first();
            if (dar != null) {
                throw new UserRoleHandlerException("Role can not be removed. The specified user has open DAR.");
            }else{
                rolesToRemove.put(user.getDacUserId(), roleIdMap.get(UserRoles.RESEARCHER.getRoleName()));
            }
        }
        //add researcher
        if (!isResearcher && isNewResearcher) {
            if (containsRole(existentRoles, UserRoles.CHAIRPERSON.getRoleName()) || containsRole(existentRoles, UserRoles.MEMBER.getRoleName())) {
                throw new UserRoleHandlerException("Role can not be added. Researcher  is incompatible with  Member and Chairperson role.");
            } else {
                rolesToRemove.put(user.getDacUserId(), roleIdMap.get(UserRoles.RESEARCHER.getRoleName()));
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

    private List<UserRole> getRoles(String[] roles){
        List<UserRole> userRoles = new ArrayList<>();
        if(roles != null && roles.length > 0){
            for(int i=0; i < roles.length; i++){
                UserRole role = new UserRole();
                role.setName(roles[i].trim());
                role.setRoleId(roleIdMap.get(roles[i].toUpperCase()));
                userRoles.add(role);
            }
        }
        return userRoles;
    }

}
