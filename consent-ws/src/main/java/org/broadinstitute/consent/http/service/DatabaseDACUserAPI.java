package org.broadinstitute.consent.http.service;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.consent.http.db.DACUserDAO;
import org.broadinstitute.consent.http.db.DACUserRoleDAO;
import org.broadinstitute.consent.http.enumeration.DACUserRoles;
import org.broadinstitute.consent.http.models.DACUser;
import org.broadinstitute.consent.http.models.DACUserRole;
import org.skife.jdbi.v2.exceptions.UnableToExecuteStatementException;

import javax.ws.rs.NotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;


/**
 * Implementation class for VoteAPI on top of ElectionDAO database support.
 */
public class DatabaseDACUserAPI extends AbstractDACUserAPI {

    private final DACUserDAO dacUserDAO;
    private final DACUserRoleDAO roleDAO;

    /**
     * Initialize the singleton API instance using the provided DAO. This method
     * should only be called once during application initialization (from the
     * run() method). If called a second time it will throw an
     * IllegalStateException. Note that this method is not synchronized, as it
     * is not intended to be called more than once.
     *
     * @param userDao The Data Access Object instance that the API should use to
     *            read/write data.
     * @param roleDAO
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
    public DACUser describeChairpersonUser() throws NotFoundException {
        DACUser dacUser = dacUserDAO.findChairpersonUser();
        return dacUser;
    }

    @Override
    public Collection<DACUser> describeAdminUsers() throws NotFoundException {
        return dacUserDAO.describeAdminUsers();
    }

    @Override
    public DACUser describeDACUserById(Integer id) throws IllegalArgumentException {
        DACUser dacUser = dacUserDAO.findDACUserById(id);
        if (dacUser == null) {
            throw new NotFoundException("Could not find dacUser for specified id : " + id);
        }
        dacUser.setRoles(roleDAO.findRolesByUserId(dacUser.getDacUserId()));
        return dacUser;
    }

    @Override
    public DACUser updateDACUserById(DACUser rec,Integer id) throws IllegalArgumentException, NotFoundException {
        validateExistentUserById(id);
        validateRequieredFields(rec);
        if(rec.getRoles() != null && rec.getRoles().size() > 0){
            validateRoles(rec.getRoles(), id);
            updateRoles(rec, id);
        }
        try{
            dacUserDAO.updateDACUser(rec.getEmail(), rec.getDisplayName(), id);
        }catch(UnableToExecuteStatementException e){
            throw new IllegalArgumentException("Email shoud be unique.");
        }
        DACUser dacUser = describeDACUserByEmail(rec.getEmail());
        dacUser.setRoles(roleDAO.findRolesByUserId(dacUser.getDacUserId()));
        return dacUser;
    }

    private void updateRoles(DACUser rec, Integer id) {
        roleDAO.removeRolesByUser(id);
        insertUserRoles(rec, id);
    }

    private void validateExistentUserById(Integer id) {
        if(dacUserDAO.findDACUserById(id) == null){
            throw new NotFoundException("The user for the specified id does not exist");
        }
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
        return dacUserDAO.findUsers();
    }

    @Override
    public Collection<String> describeUsersEmails(List<Integer> dacUserIds){
        return dacUserDAO.describeUsersEmails(dacUserIds);
    }

    private void validateExistentUser(String email) {
        if(dacUserDAO.findDACUserByEmail(email) == null){
            throw new NotFoundException("The user for the specified E-Mail address does not exist");
        }
    }

    private void validateRequieredFields(DACUser newDac) {
        if(StringUtils.isEmpty(newDac.getDisplayName())){
            throw new IllegalArgumentException("Display Name can't be null. The user needs a name to display.");
        }
        if(StringUtils.isEmpty(newDac.getEmail())){
            throw new IllegalArgumentException("The user needs a valid email to be able to login.");
        }
    }

    private void validateRoles(List<DACUserRole> newRoles, Integer userId){
        List<DACUserRole> existentRoles = roleDAO.findRolesByUserId(userId);
        if(existentRoles.stream().anyMatch(role -> role.getName().equalsIgnoreCase(DACUserRoles.ADMIN.getValue()))
                && !newRoles.stream().anyMatch(role -> role.getName().equalsIgnoreCase(DACUserRoles.ADMIN.getValue()))){
            if(dacUserDAO.verifyAdminUsers() < 2){
                throw new IllegalArgumentException("At least one user with Admin roles should exist.");
            }
       }
        if(newRoles.stream().anyMatch(role -> role.getName().equalsIgnoreCase(DACUserRoles.CHAIRPERSON.getValue()))){
             updateExistentChairPersonToAlumni(userId);
        }
    }

    private void insertUserRoles(DACUser dacUser, Integer dacUserId){
        List<DACUserRole> roles = dacUser.getRoles();
        List<String> rolesName = new ArrayList<>();
        for(DACUserRole role : roles){
            rolesName.add(role.getName());
        }
        List<Integer> rolesId = roleDAO.findRolesIdByName(rolesName);
        roleDAO.insertUserRoles(rolesId, dacUserId);
    }

}



