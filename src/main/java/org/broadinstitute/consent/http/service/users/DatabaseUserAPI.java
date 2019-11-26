package org.broadinstitute.consent.http.service.users;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.consent.http.db.DACUserDAO;
import org.broadinstitute.consent.http.db.DataSetAssociationDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.db.ResearcherPropertyDAO;
import org.broadinstitute.consent.http.db.UserRoleDAO;
import org.broadinstitute.consent.http.db.VoteDAO;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.DACUser;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.service.users.handler.UserHandlerAPI;

import javax.ws.rs.BadRequestException;
import java.util.List;


public class DatabaseUserAPI extends DatabaseDACUserAPI implements UserAPI {

    public DatabaseUserAPI(DACUserDAO userDAO, UserRoleDAO roleDAO, ElectionDAO electionDAO, VoteDAO voteDAO, DataSetAssociationDAO dataSetAssociationDAO, UserHandlerAPI userHandlerAPI, ResearcherPropertyDAO researcherPropertyDAO) {
        super(userDAO, roleDAO, electionDAO, voteDAO, dataSetAssociationDAO, userHandlerAPI, researcherPropertyDAO);
    }

    @Override
    public DACUser createUser(DACUser user) {
        validateEmail(user.getEmail());
        validateRoles(user.getRoles());
        return createDACUser(user);
    }

    @Override
    public DACUser findUserByEmail(String email) {
        DACUser dacUser = dacUserDAO.findDACUserByEmail(email);
        if (dacUser == null) {
            return null;
        }
        dacUser.setRoles(userRoleDAO.findRolesByUserId(dacUser.getDacUserId()));
        return dacUser;
    }

    private void validateEmail(String emailToValidate) {
        if (StringUtils.isEmpty(emailToValidate)) {
            throw new BadRequestException("Email address cannot be empty");
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
        } else {
            throw new IllegalArgumentException("Roles are required.");
        }
    }

}
