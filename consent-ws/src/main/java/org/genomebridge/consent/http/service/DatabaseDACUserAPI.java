package org.genomebridge.consent.http.service;

import com.sun.jersey.api.NotFoundException;
import org.apache.commons.lang.StringUtils;
import org.genomebridge.consent.http.db.DACUserDAO;
import org.genomebridge.consent.http.db.ElectionDAO;
import org.genomebridge.consent.http.db.VoteDAO;
import org.genomebridge.consent.http.models.DACUser;
import org.genomebridge.consent.http.models.Vote;
import org.skife.jdbi.v2.DBI;

import java.util.Date;
import java.util.List;

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
    public Integer createDACUser(DACUser dacUser) throws IllegalArgumentException {
        Integer dacUserID = dacUserDAO.insertDACUser(dacUser.getEmail(),dacUser.getDisplayName(),dacUser.getMemberStatus());
        return dacUserID;
    }

    @Override
    public DACUser describeDACUserByEmail(String email) throws IllegalArgumentException {
        DACUser dacUser = dacUserDAO.findDACUserByEmail(email);
        if (dacUser == null) {
            throw new NotFoundException("Could not find dacUser for specified email : " + email);
        }
        return dacUser;
    }
}



