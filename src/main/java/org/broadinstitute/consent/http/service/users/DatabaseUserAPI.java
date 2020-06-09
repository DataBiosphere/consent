package org.broadinstitute.consent.http.service.users;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.consent.http.db.DACUserDAO;
import org.broadinstitute.consent.http.db.UserRoleDAO;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.service.UserService;
import org.broadinstitute.consent.http.service.users.handler.UserRolesHandler;

import javax.ws.rs.BadRequestException;
import java.util.List;

/**
 * @deprecated Use UserService
 */
@Deprecated
public class DatabaseUserAPI extends DatabaseDACUserAPI implements UserAPI {

    public DatabaseUserAPI(DACUserDAO userDAO, UserRoleDAO roleDAO, UserRolesHandler userHandlerAPI, UserService userService) {
        super(userDAO, roleDAO, userHandlerAPI, userService);
    }

    @Override
    public User createUser(User user) {
        validateEmail(user.getEmail());
        validateRoles(user.getRoles());
        return createDACUser(user);
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
