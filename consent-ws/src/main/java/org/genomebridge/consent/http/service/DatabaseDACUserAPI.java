package org.genomebridge.consent.http.service;

import org.apache.commons.lang3.StringUtils;
import org.genomebridge.consent.http.models.DACUser;
import org.genomebridge.consent.http.db.DACUserDAO;
import org.genomebridge.consent.http.db.DACUserRoleDAO;
import org.genomebridge.consent.http.enumeration.DACUserRoles;
import org.genomebridge.consent.http.models.DACUserRole;
import org.skife.jdbi.v2.exceptions.UnableToExecuteStatementException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.ws.rs.NotFoundException;
import java.util.*;


/**
 * Implementation class for VoteAPI on top of ElectionDAO database support.
 */
public class DatabaseDACUserAPI extends AbstractDACUserAPI {

    private DACUserDAO dacUserDAO;
    private DACUserRoleDAO roleDAO;

    /**
     * Initialize the singleton API instance using the provided DAO. This method
     * should only be called once during application initialization (from the
     * run() method). If called a second time it will throw an
     * IllegalStateException. Note that this method is not synchronized, as it
     * is not intended to be called more than once.
     *
     * @param userDao The Data Access Object instance that the API should use to
     *            read/write data.
     */
    public static void initInstance(DACUserDAO userDao, DACUserRoleDAO roleDAO) {
        DACUserAPIHolder.setInstance(new DatabaseDACUserAPI(userDao, roleDAO));
    }

    /**
     * The constructor is private to force use of the factory methods and
     * enforce the singleton pattern.
     *
     * @param userDAO The Data Access Object used to read/write data.
     */
    private DatabaseDACUserAPI(DACUserDAO userDAO, DACUserRoleDAO roleDAO) {
        this.dacUserDAO = userDAO;
        this.roleDAO = roleDAO;

    }

    @Override
    public DACUser createDACUser(DACUser dacUser) throws IllegalArgumentException {
        validateRequieredFields(dacUser);
        Integer dacUserID;
        try {
            dacUserID = dacUserDAO.insertDACUser(dacUser.getEmail(), dacUser.getDisplayName(), new Date());
        }catch (UnableToExecuteStatementException e){
            throw new IllegalArgumentException("Email shoud be unique.");
        }
        if(dacUser.getRoles() != null) {
            insertUserRoles(dacUser, dacUserID);
        }
        DACUser user = dacUserDAO.findDACUserById(dacUserID);
        user.setRoles(roleDAO.findRolesByUserId(user.getDacUserId()));
        return user;

    }

    @Override
    public DACUser describeDACUserByEmail(String email) throws IllegalArgumentException {
        DACUser dacUser = dacUserDAO.findDACUserByEmail(email);
        if (dacUser == null) {
            throw new NotFoundException("Could not find dacUser for specified email : " + email);
        }
        dacUser.setRoles(roleDAO.findRolesByUserId(dacUser.getDacUserId()));
        return dacUser;
    }

    @Override
    public DACUser updateDACUserByEmail(DACUser rec) throws IllegalArgumentException, NotFoundException {
        validateExistentUser(rec.getEmail());
        validateRequieredFields(rec);
        dacUserDAO.updateDACUser(rec.getEmail(), rec.getDisplayName());
        DACUser dacUser = describeDACUserByEmail(rec.getEmail());
        dacUser.setRoles(roleDAO.findRolesByUserId(dacUser.getDacUserId()));
        return dacUser;
    }

    @Override
    public void deleteDACUser(String email) throws IllegalArgumentException, NotFoundException {
        validateExistentUser(email);
        dacUserDAO.deleteDACUserByEmail(email);
    }

    @Override
    public void updateExistentChairPersonToAlumni(Integer dacUserID) {
        Integer existentRoleId = roleDAO.findRoleIdByName(DACUserRoles.CHAIRPERSON.getValue());
        Integer chairPersonId = dacUserDAO.findDACUserIdByRole(existentRoleId, dacUserID);
        if(chairPersonId != null){
            Integer newRoleId = roleDAO.findRoleIdByName(DACUserRoles.ALUMNI.getValue());
            roleDAO.updateUserRoles(newRoleId, chairPersonId, existentRoleId);
        }
    }

    @Override
    public Collection<DACUser> describeUsers() {
        List<DACUser> dacUsers = dacUserDAO.findUsers();
        return getUsersWithRoles(dacUsers);
    }

    private Collection<DACUser> getUsersWithRoles(List<DACUser> dacUsers) {
        Map<Integer, DACUser> users = new HashMap<>();
        if(dacUsers != null){
            for(DACUser user : dacUsers){
                if(users.containsKey(user.getDacUserId())){
                    DACUser existentUser = users.get(user.getDacUserId());
                    List<DACUserRole> existentRoles = existentUser.getRoles();
                    existentRoles.addAll(user.getRoles());
                }else{
                    users.put(user.getDacUserId(),user);
                }
            }
        }
        return users.values();
    }

    private void validateExistentUser(String email) {
        if (dacUserDAO.findDACUserByEmail(email) == null) {
            throw new NotFoundException("The user for the specified E-Mail address does not exist");
        }
    }

    private void validateRequieredFields(DACUser newDac) {
        if (StringUtils.isEmpty(newDac.getDisplayName())) {
            throw new IllegalArgumentException("Display Name can't be null. The user needs a name to display.");
        }
        if (StringUtils.isEmpty(newDac.getEmail())) {
            throw new IllegalArgumentException("The user needs a valid email to be able to login.");
        }
    }

    private void insertUserRoles(DACUser dacUser, Integer dacUserId) {
        List<DACUserRole> roles = dacUser.getRoles();
        List<String> rolesName = new ArrayList<>();
        for(DACUserRole role : roles) {
            rolesName.add(role.getName());
        }
        List<Integer> rolesId = roleDAO.findRolesIdByName(rolesName);
        roleDAO.insertUserRoles(rolesId, dacUserId);
    }


}



