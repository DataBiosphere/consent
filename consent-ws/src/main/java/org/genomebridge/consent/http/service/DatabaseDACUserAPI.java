package org.genomebridge.consent.http.service;

import com.sun.jersey.api.NotFoundException;
import org.genomebridge.consent.http.db.DACUserDAO;
import org.genomebridge.consent.http.models.DACUser;
import org.skife.jdbi.v2.DBI;

/**
 * Implementation class for VoteAPI on top of ElectionDAO database support.
 */
public class DatabaseDACUserAPI extends AbstractDACUserAPI {

    private DACUserDAO dacUserDAO;
    private DBI jdbi;

    /**
     * Initialize the singleton API instance using the provided DAO. This method
     * should only be called once during application initialization (from the
     * run() method). If called a second time it will throw an
     * IllegalStateException. Note that this method is not synchronized, as it
     * is not intended to be called more than once.
     *
     * @param dao The Data Access Object instance that the API should use to
     *            read/write data.
     */
    public static void initInstance(DBI jdbi,DACUserDAO dao) {
        DACUserAPIHolder.setInstance(new DatabaseDACUserAPI(jdbi,dao));
    }

    /**
     * The constructor is private to force use of the factory methods and
     * enforce the singleton pattern.
     *
     * @param dao The Data Access Object used to read/write data.
     */
    private DatabaseDACUserAPI(DBI jdbi,DACUserDAO dao) {
        this.jdbi = jdbi;
        this.dacUserDAO = dao;
    }

    @Override
    public DACUser createDACUser(DACUser dacUser) throws IllegalArgumentException {
        validateRequieredFields(dacUser);
        Integer dacUserID = dacUserDAO.insertDACUser(dacUser.getEmail(),dacUser.getDisplayName(),dacUser.getMemberStatus());
        return dacUserDAO.findDACUserById(dacUserID);
    }

    @Override
    public DACUser describeDACUserByEmail(String email) throws IllegalArgumentException {
        DACUser dacUser = dacUserDAO.findDACUserByEmail(email);
        if (dacUser == null) {
            throw new NotFoundException("Could not find dacUser for specified email : " + email);
        }
        return dacUser;
    }

    @Override
    public DACUser updateDACUserByEmail(DACUser rec) throws IllegalArgumentException, NotFoundException {
        validateExistentUser(rec.getEmail());
        validateRequieredFields(rec);
        dacUserDAO.updateDACUser(rec.getEmail(), rec.getDisplayName(), rec.getMemberStatus());
        return describeDACUserByEmail(rec.getEmail());
    }

    @Override
    public void deleteDACUser(String email) throws IllegalArgumentException, NotFoundException {
        validateExistentUser(email);
        dacUserDAO.deleteDACUserByEmail(email);
    }

    private void validateExistentUser(String email) {
        if (dacUserDAO.findDACUserByEmail(email) == null) {
            throw new NotFoundException("The user for the specified E-Mail address does not exist");
        }
    }

    private void validateRequieredFields(DACUser newDac) {
        if (newDac.getDisplayName() == null) {
            throw new IllegalArgumentException("Display Name can't be null. The user needs a name to display.");
        }
        if (newDac.getEmail() == null) {
            throw new IllegalArgumentException("The user needs a valid email to be able to login.");
        }
    }
}



