package org.broadinstitute.consent.http.service.users;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.consent.http.db.DACUserDAO;
import org.broadinstitute.consent.http.db.DataSetAssociationDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.db.ResearcherPropertyDAO;
import org.broadinstitute.consent.http.db.UserRoleDAO;
import org.broadinstitute.consent.http.db.VoteDAO;
import org.broadinstitute.consent.http.db.mongo.MongoConsentDB;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.DACUser;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.service.users.handler.UserHandlerAPI;

import javax.ws.rs.NotAuthorizedException;
import java.util.List;


public class DatabaseUserAPI extends DatabaseDACUserAPI implements UserAPI {

    public DatabaseUserAPI(DACUserDAO userDAO, UserRoleDAO roleDAO, ElectionDAO electionDAO, VoteDAO voteDAO, DataSetAssociationDAO dataSetAssociationDAO, UserHandlerAPI userHandlerAPI, ResearcherPropertyDAO  researcherPropertyDAO) {
        super(userDAO, roleDAO, electionDAO, voteDAO, dataSetAssociationDAO, userHandlerAPI, researcherPropertyDAO);
    }

    @Override
    public DACUser createUser(DACUser user, String userEmail) {
        validateEmail(user.getEmail(), userEmail);
        user.setEmail(userEmail);
        validateRoles(user.getRoles());
        return createDACUser(user);
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

}
