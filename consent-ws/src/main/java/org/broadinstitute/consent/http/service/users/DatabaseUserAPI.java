package org.broadinstitute.consent.http.service.users;

import com.mongodb.BasicDBObject;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.map.HashedMap;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.consent.http.db.*;
import org.broadinstitute.consent.http.db.mongo.MongoConsentDB;
import org.broadinstitute.consent.http.enumeration.Actions;
import org.broadinstitute.consent.http.enumeration.DACUserRoles;
import org.broadinstitute.consent.http.enumeration.ElectionStatus;
import org.broadinstitute.consent.http.models.DACUser;
import org.broadinstitute.consent.http.models.DACUserRole;
import org.broadinstitute.consent.http.models.dto.PatchOperation;
import org.broadinstitute.consent.http.service.users.handler.UserHandlerAPI;
import org.broadinstitute.consent.http.service.users.handler.UserRoleHandlerException;
import org.broadinstitute.consent.http.util.DarConstants;
import org.bson.Document;
import javax.ws.rs.NotAuthorizedException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class DatabaseUserAPI extends DatabaseDACUserAPI implements UserAPI {

    private MongoConsentDB mongo;

    private final String DISPLAY_NAME = "displayName";

    private final String ROLES = "roles";

    public DatabaseUserAPI(DACUserDAO userDAO, DACUserRoleDAO roleDAO, ElectionDAO electionDAO, VoteDAO voteDAO, DataSetAssociationDAO dataSetAssociationDAO, UserHandlerAPI userHandlerAPI, MongoConsentDB mongo,  ResearcherPropertyDAO  researcherPropertyDAO) {
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
        List<DACUserRole> dacUserRoles = getRoles(roles);
        if(action.getOp().contains(Actions.REPLACE.getValue())){
            user.setRoles(dacUserRoles);
        }
        else if(action.getOp().contains(Actions.ADD.getValue())){
            user.getRoles().addAll(dacUserRoles);
        }
        else if(action.getOp().contains(Actions.REMOVE.getValue())){
            dacUserRoles.stream().forEach(role -> user.getRoles().remove(role));
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

    private void validateRoles(List<DACUserRole> roles) {
        if (CollectionUtils.isNotEmpty(roles)) {
            roles.forEach(role -> {
                if (!(role.getName().equalsIgnoreCase(DACUserRoles.DATAOWNER.getValue())
                        || role.getName().equalsIgnoreCase(DACUserRoles.RESEARCHER.getValue()))) {
                    throw new IllegalArgumentException("Invalid role: " + role.getName() + ". Valid roles are: " + DACUserRoles.DATAOWNER.getValue() + " and " + DACUserRoles.RESEARCHER.getValue());
                }
            });
        }else{
            throw new IllegalArgumentException("Roles are required.");
        }
    }

    private void validateAndUpdateRoles(List<DACUserRole> existentRoles, List<DACUserRole> newRoles, DACUser user) throws UserRoleHandlerException {
        Map<Integer, Integer>  rolesToRemove = new HashedMap();
        Map<Integer, Integer>  rolesToAdd = new HashedMap();
        updateDataOwnerRole(existentRoles, newRoles, user, rolesToRemove, rolesToAdd);
        updateResearcherRole(existentRoles, newRoles, user, rolesToRemove, rolesToAdd);
        rolesToRemove.forEach((userId,roleId)->{
           roleDAO.removeSingleUserRole(userId, roleId);
        });
        rolesToAdd.forEach((userId,roleId) ->{
            roleDAO.insertSingleUserRole(roleId,userId, false);
        });
    }
    private void updateDataOwnerRole(List<DACUserRole> existentRoles, List<DACUserRole> newRoles, DACUser user, Map<Integer, Integer>  rolesToRemove, Map<Integer, Integer>  rolesToAdd) throws UserRoleHandlerException {
        boolean isDO = containsRole(existentRoles, DACUserRoles.DATAOWNER.getValue());
        boolean isNewDO = containsRole(newRoles, DACUserRoles.DATAOWNER.getValue());
        //remove data owner
        if (isDO && !isNewDO) {
            if (hasDataSetAssociation(user)) {
                throw new UserRoleHandlerException("Role can not be removed. The specified user is the only data owner associated to a data set that needs approval.");
            }
            else if (hasOpenElections(user)) {
                throw new UserRoleHandlerException("Role can not be removed. There are open dataset elections for this user.");
            }
            else{
                rolesToRemove.put(user.getDacUserId(), roleIdMap.get(DACUserRoles.DATAOWNER.getValue()));
            }
        }
        //add data owner
        if (!isDO && isNewDO) {
            rolesToAdd.put(user.getDacUserId(), roleIdMap.get(DACUserRoles.DATAOWNER.getValue()));

        }
    }

    private void updateResearcherRole(List<DACUserRole> existentRoles, List<DACUserRole> newRoles, DACUser user,Map<Integer, Integer>  rolesToRemove, Map<Integer, Integer>  rolesToAdd) throws UserRoleHandlerException {
        boolean isResearcher = containsRole(existentRoles, DACUserRoles.RESEARCHER.getValue());
        boolean isNewResearcher = containsRole(newRoles, DACUserRoles.RESEARCHER.getValue());
        //remove researcher
        if (isResearcher && !isNewResearcher) {
            BasicDBObject query = new BasicDBObject(DarConstants.STATUS, new BasicDBObject("$ne",ElectionStatus.CANCELED.getValue()));
            query.append("userId", user.getDacUserId());
            Document dar = mongo.getDataAccessRequestCollection().find(query).first();
            if (dar != null) {
                throw new UserRoleHandlerException("Role can not be removed. The specified user has open DAR.");
            }else{
                rolesToRemove.put(user.getDacUserId(), roleIdMap.get(DACUserRoles.RESEARCHER.getValue()));
            }
        }
        //add researcher
        if (!isResearcher && isNewResearcher) {
            if (containsRole(existentRoles, DACUserRoles.CHAIRPERSON.getValue()) || containsRole(existentRoles, DACUserRoles.MEMBER.getValue())) {
                throw new UserRoleHandlerException("Role can not be added. Researcher  is incompatible with  Member and Chairperson role.");
            } else {
                rolesToRemove.put(user.getDacUserId(), roleIdMap.get(DACUserRoles.RESEARCHER.getValue()));
            }
        }
    }

    private boolean containsRole(List<DACUserRole> roles, String roleName) {
        boolean result = false;
        if (CollectionUtils.isNotEmpty(roles)) {
            result = roles.parallelStream().anyMatch(role -> role.getName().equalsIgnoreCase(roleName));
        }
        return result;
    }

    private List<DACUserRole> getRoles(String[] roles){
        List<DACUserRole> dacUserRoles = new ArrayList<>();
        if(roles != null && roles.length > 0){
            for(int i=0; i < roles.length; i++){
                DACUserRole role = new DACUserRole();
                role.setName(roles[i].trim());
                role.setRoleId(roleIdMap.get(roles[i].toUpperCase()));
                dacUserRoles.add(role);
            }
        }
        return dacUserRoles;
    }

}
