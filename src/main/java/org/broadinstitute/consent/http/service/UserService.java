package org.broadinstitute.consent.http.service;

import org.broadinstitute.consent.http.db.DACUserDAO;
import org.broadinstitute.consent.http.db.UserRoleDAO;
import org.broadinstitute.consent.http.models.DACUser;

public class UserService {

    private DACUserDAO userDAO;
    private UserRoleDAO roleDAO;

    public UserService(DACUserDAO userDAO, UserRoleDAO roleDAO) {
        this.userDAO = userDAO;
        this.roleDAO = roleDAO;
    }

    DACUser findUserById(Integer id) {
        DACUser dacUser = userDAO.findDACUserById(id);
        if (dacUser == null) {
            return null;
        }
        dacUser.setRoles(roleDAO.findRolesByUserId(dacUser.getDacUserId()));
        return dacUser;
    }

    DACUser findUserByEmail(String email) {
        DACUser dacUser = userDAO.findDACUserByEmail(email);
        if (dacUser == null) {
            return null;
        }
        dacUser.setRoles(roleDAO.findRolesByUserId(dacUser.getDacUserId()));
        return dacUser;
    }

}
