package org.broadinstitute.consent.http.service;

import org.broadinstitute.consent.http.db.DACUserDAO;
import org.broadinstitute.consent.http.db.UserRoleDAO;
import org.broadinstitute.consent.http.models.DACUser;

import javax.ws.rs.NotFoundException;
import java.util.Collection;

public class UserService {

    private DACUserDAO userDAO;
    private UserRoleDAO roleDAO;

    public UserService(DACUserDAO userDAO, UserRoleDAO roleDAO) {
        this.userDAO = userDAO;
        this.roleDAO = roleDAO;
    }

    public DACUser findUserById(Integer id) throws NotFoundException {
        DACUser dacUser = userDAO.findDACUserById(id);
        if (dacUser == null) {
            throw new NotFoundException("Unable to find user with id: " + id);
        }
        dacUser.setRoles(roleDAO.findRolesByUserId(dacUser.getDacUserId()));
        return dacUser;
    }

    public DACUser findUserByEmail(String email) throws NotFoundException {
        DACUser dacUser = userDAO.findDACUserByEmail(email);
        if (dacUser == null) {
            throw new NotFoundException("Unable to find user with email: " + email);
        }
        dacUser.setRoles(roleDAO.findRolesByUserId(dacUser.getDacUserId()));
        return dacUser;
    }

    public Collection<DACUser> describeUsers() {
        return userDAO.findUsers();
    }

}
