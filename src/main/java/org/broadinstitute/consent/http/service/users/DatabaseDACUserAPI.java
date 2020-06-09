package org.broadinstitute.consent.http.service.users;


import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.consent.http.db.DACUserDAO;
import org.broadinstitute.consent.http.db.UserRoleDAO;
import org.broadinstitute.consent.http.enumeration.RoleStatus;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.service.UserService;
import org.broadinstitute.consent.http.service.users.handler.UserRolesHandler;
import org.jdbi.v3.core.statement.UnableToExecuteStatementException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.NotFoundException;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @deprecated Use UserService
 * Implementation class for DACUserAPI on top of DACUserDAO database support.
 */
@Deprecated
public class DatabaseDACUserAPI extends AbstractDACUserAPI {

    protected final DACUserDAO dacUserDAO;
    protected final UserRoleDAO userRoleDAO;
    private final UserRolesHandler rolesHandler;
    private final UserService userService;

    public static void initInstance(DACUserDAO userDao, UserRoleDAO userRoleDAO, UserRolesHandler rolesHandler, UserService userService) {
        DACUserAPIHolder.setInstance(new DatabaseDACUserAPI(userDao, userRoleDAO, rolesHandler, userService));
    }

    protected Logger logger() {
        return LoggerFactory.getLogger(this.getClass());
    }

    /**
     * The constructor is private to force use of the factory methods and
     * enforce the singleton pattern.
     *
     * @param userDAO The Data Access Object used to read/write data.
     */
    DatabaseDACUserAPI(DACUserDAO userDAO, UserRoleDAO userRoleDAO, UserRolesHandler rolesHandler, UserService userService) {
        this.dacUserDAO = userDAO;
        this.userRoleDAO = userRoleDAO;
        this.rolesHandler = rolesHandler;
        this.userService = userService;
    }

    @Override
    public User createDACUser(User dacUser) throws IllegalArgumentException {
        validateRequiredFields(dacUser);
        Integer dacUserID;
        try {
            dacUserID = dacUserDAO.insertDACUser(dacUser.getEmail(), dacUser.getDisplayName(), new Date());
        } catch (UnableToExecuteStatementException e) {
            throw new IllegalArgumentException("Email should be unique.", e);
        }
        if (dacUser.getRoles() != null) {
            insertUserRoles(dacUser, dacUserID);
        }
        User user = dacUserDAO.findDACUserById(dacUserID);
        user.setRoles(userRoleDAO.findRolesByUserId(user.getDacUserId()));
        return user;

    }

    @Override
    public List<User> describeAdminUsersThatWantToReceiveMails() {
        return dacUserDAO.describeUsersByRoleAndEmailPreference(UserRoles.ADMIN.getRoleName(), true);
    }

    @Override
    public User updateUserStatus(String status, Integer userId) {
        Integer statusId = RoleStatus.getValueByStatus(status);
        validateExistentUserById(userId);
        if (statusId == null) {
            throw new IllegalArgumentException(status + " is not a valid status.");
        }
        dacUserDAO.updateUserStatus(statusId, userId);
        return userService.findUserById(userId);
    }

    @Override
    public User updateUserRationale(String rationale, Integer userId) {
        validateExistentUserById(userId);
        if (rationale == null) {
            throw new IllegalArgumentException("Rationale is required.");
        }
        dacUserDAO.updateUserRationale(rationale, userId);
        return userService.findUserById(userId);
    }

    @Override
    public User updateDACUserById(Map<String, User> dac, Integer id) throws IllegalArgumentException, NotFoundException {
        User updatedUser = dac.get(UserRolesHandler.UPDATED_USER_KEY);
        // validate user exists
        validateExistentUserById(id);
        // validate required fields are not null or empty
        validateRequiredFields(updatedUser);
        rolesHandler.updateRoles(updatedUser);
        try {
            dacUserDAO.updateDACUser(updatedUser.getEmail(), updatedUser.getDisplayName(), id, updatedUser.getAdditionalEmail());
        } catch (UnableToExecuteStatementException e) {
            throw new IllegalArgumentException("Email shoud be unique.");
        }
        User user = userService.findUserByEmail(updatedUser.getEmail());
        user.setRoles(userRoleDAO.findRolesByUserId(user.getDacUserId()));
        return user;
    }

    @Override
    public void updateEmailPreference(boolean preference, Integer userId) {
        dacUserDAO.updateEmailPreference(preference, userId);
    }

    private void validateExistentUserById(Integer id) {
        if (dacUserDAO.findDACUserById(id) == null) {
            throw new NotFoundException("The user for the specified id does not exist");
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
            if (r.getRoleId() == null) {
                r.setRoleId(userRoleDAO.findRoleIdByName(r.getName()));
            }
        });
        userRoleDAO.insertUserRoles(roles, dacUserId);
    }

}
