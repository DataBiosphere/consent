package org.broadinstitute.consent.http.service.users;


import freemarker.template.TemplateException;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.consent.http.db.DACUserDAO;
import org.broadinstitute.consent.http.db.ResearcherPropertyDAO;
import org.broadinstitute.consent.http.db.UserRoleDAO;
import org.broadinstitute.consent.http.enumeration.RoleStatus;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.DACUser;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.service.users.handler.UserHandlerAPI;
import org.broadinstitute.consent.http.service.users.handler.UserRoleHandlerException;
import org.skife.jdbi.v2.exceptions.UnableToExecuteStatementException;

import javax.mail.MessagingException;
import javax.ws.rs.NotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implementation class for DACUserAPI on top of DACUserDAO database support.
 */
public class DatabaseDACUserAPI extends AbstractDACUserAPI {

    protected final DACUserDAO dacUserDAO;
    protected final UserRoleDAO userRoleDAO;
    private final UserHandlerAPI rolesHandler;
    private final ResearcherPropertyDAO researcherPropertyDAO;

    public static void initInstance(DACUserDAO userDao, UserRoleDAO userRoleDAO, UserHandlerAPI userHandlerAPI, ResearcherPropertyDAO researcherPropertyDAO) {
        DACUserAPIHolder.setInstance(new DatabaseDACUserAPI(userDao, userRoleDAO, userHandlerAPI, researcherPropertyDAO));
    }

    protected org.apache.log4j.Logger logger() {
        return org.apache.log4j.Logger.getLogger("DatabaseDACUserAPI");
    }

    /**
     * The constructor is private to force use of the factory methods and
     * enforce the singleton pattern.
     *
     * @param userDAO The Data Access Object used to read/write data.
     */
    DatabaseDACUserAPI(DACUserDAO userDAO, UserRoleDAO userRoleDAO, UserHandlerAPI userHandlerAPI, ResearcherPropertyDAO researcherPropertyDAO) {
        this.dacUserDAO = userDAO;
        this.userRoleDAO = userRoleDAO;
        this.rolesHandler = userHandlerAPI;
        this.researcherPropertyDAO = researcherPropertyDAO;
    }

    @Override
    public DACUser createDACUser(DACUser dacUser) throws IllegalArgumentException {
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
        DACUser user = dacUserDAO.findDACUserById(dacUserID);
        user.setRoles(userRoleDAO.findRolesByUserId(user.getDacUserId()));
        return user;

    }

    @Override
    public DACUser describeDACUserByEmail(String email) throws NotFoundException {
        DACUser dacUser = dacUserDAO.findDACUserByEmail(email);
        if (dacUser == null) {
            throw new NotFoundException("Could not find dacUser for specified email : " + email);
        }
        dacUser.setRoles(userRoleDAO.findRolesByUserId(dacUser.getDacUserId()));
        return dacUser;
    }

    @Override
    public List<DACUser> describeAdminUsersThatWantToReceiveMails() {
        return dacUserDAO.describeUsersByRoleAndEmailPreference(UserRoles.ADMIN.getRoleName(), true);
    }

    @Override
    public DACUser describeDACUserById(Integer id) throws IllegalArgumentException {
        DACUser dacUser = dacUserDAO.findDACUserById(id);
        if (dacUser == null) {
            throw new NotFoundException("Could not find dacUser for specified id : " + id);
        }
        dacUser.setRoles(userRoleDAO.findRolesByUserId(dacUser.getDacUserId()));
        return dacUser;
    }

    @Override
    public DACUser updateUserStatus(String status, Integer userId) {
        Integer statusId = RoleStatus.getValueByStatus(status);
        validateExistentUserById(userId);
        if (statusId == null) {
            throw new IllegalArgumentException(status + " is not a valid status.");
        }
        dacUserDAO.updateUserStatus(statusId, userId);
        return describeDACUserById(userId);
    }

    @Override
    public DACUser updateUserRationale(String rationale, Integer userId) {
        validateExistentUserById(userId);
        if (rationale == null) {
            throw new IllegalArgumentException("Rationale is required.");
        }
        dacUserDAO.updateUserRationale(rationale, userId);
        return describeDACUserById(userId);
    }

    @Override
    public DACUser updateDACUserById(Map<String, DACUser> dac, Integer id) throws IllegalArgumentException, NotFoundException, UserRoleHandlerException, MessagingException, IOException, TemplateException {
        DACUser updatedUser = dac.get("updatedUser");
        // validate user exists
        validateExistentUserById(id);
        // validate required fields are not null or empty
        validateRequiredFields(updatedUser);
        rolesHandler.updateRoles(dac);
        try {
            dacUserDAO.updateDACUser(updatedUser.getEmail(), updatedUser.getDisplayName(), id, updatedUser.getAdditionalEmail());
        } catch (UnableToExecuteStatementException e) {
            throw new IllegalArgumentException("Email shoud be unique.");
        }
        DACUser dacUser = describeDACUserByEmail(updatedUser.getEmail());
        dacUser.setRoles(userRoleDAO.findRolesByUserId(dacUser.getDacUserId()));
        return dacUser;
    }

    @Override
    public DACUser updateDACUserById(DACUser dac, Integer id) throws IllegalArgumentException, NotFoundException {
        validateExistentUserById(id);
        validateRequiredFields(dac);
        try {
            dacUserDAO.updateDACUser(dac.getEmail(), dac.getDisplayName(), id, dac.getAdditionalEmail());
        } catch (UnableToExecuteStatementException e) {
            throw new IllegalArgumentException("Email should be unique.");
        }
        return describeDACUserByEmail(dac.getEmail());
    }

    @Override
    public void deleteDACUser(String email) throws IllegalArgumentException, NotFoundException {
        DACUser user = dacUserDAO.findDACUserByEmail(email);
        if (user == null) {
            throw new NotFoundException("The user for the specified E-Mail address does not exist");
        }
        List<Integer> roleIds = userRoleDAO.
                findRolesByUserId(user.getDacUserId()).
                stream().
                map(UserRole::getRoleId).
                collect(Collectors.toList());
        if (!roleIds.isEmpty()) {
            userRoleDAO.removeUserRoles(user.getDacUserId(), roleIds);
        }
        dacUserDAO.deleteDACUserByEmail(email);
    }

    @Override
    public Collection<DACUser> describeUsers() {
        Collection<DACUser> users = dacUserDAO.findUsers();
        users.forEach(user -> {
            // TODO: This nested dao call isn't scalable. See DUOS-404
            String isProfileCompleted = researcherPropertyDAO.isProfileCompleted(user.getDacUserId());
            user.setProfileCompleted(isProfileCompleted == null ? false : Boolean.valueOf(isProfileCompleted));
        });
        return users;
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

    private void validateRequiredFields(DACUser newDac) {
        if (StringUtils.isEmpty(newDac.getDisplayName())) {
            throw new IllegalArgumentException("Display Name can't be null. The user needs a name to display.");
        }
        if (StringUtils.isEmpty(newDac.getEmail())) {
            throw new IllegalArgumentException("The user needs a valid email to be able to login.");
        }
    }

    private void insertUserRoles(DACUser dacUser, Integer dacUserId) {
        List<UserRole> roles = dacUser.getRoles();
        roles.forEach(r -> {
            if (r.getRoleId() == null) {
                r.setRoleId(userRoleDAO.findRoleIdByName(r.getName()));
            }
        });
        userRoleDAO.insertUserRoles(roles, dacUserId);
    }

}
