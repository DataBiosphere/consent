package org.broadinstitute.consent.http.service;
import org.broadinstitute.consent.http.db.ApprovalExpirationTimeDAO;
import org.broadinstitute.consent.http.db.DACUserDAO;
import org.broadinstitute.consent.http.models.ApprovalExpirationTime;
import org.broadinstitute.consent.http.util.DarConstants;

import javax.ws.rs.NotFoundException;
import java.util.Date;


/**
 * Implementation class for ConsentAPI on top of ConsentDAO database support.
 */
public class DatabaseApprovalExpirationTimeAPI extends AbstractApprovalExpirationTimeAPI {

    private ApprovalExpirationTimeDAO approvalExpirationTimeDAO;
    private DACUserDAO dacUserDAO;


    /**
     * The constructor is private to force use of the factory methods and enforce the singleton pattern.
     *
     * @param dao The Data Access Object used to read/write data.
     */
    protected DatabaseApprovalExpirationTimeAPI(ApprovalExpirationTimeDAO dao, DACUserDAO dacUserDAO) {
        this.approvalExpirationTimeDAO = dao;
        this.dacUserDAO = dacUserDAO;
    }

    /**
     * Initialize the singleton API instance using the provided DAO.  This method should only be called once
     * during application initialization (from the run() method).  If called a second time it will throw an
     * IllegalStateException.
     * Note that this method is not synchronized, as it is not intended to be called more than once.
     *
     * @param dao The Data Access Object instance that the API should use to read/write data.
     */

    public static void initInstance(ApprovalExpirationTimeDAO dao, DACUserDAO dacUserDAO) {
        ApprovalExpirationTimeAPIHolder.setInstance(new DatabaseApprovalExpirationTimeAPI(dao, dacUserDAO));
    }


    @Override
    public ApprovalExpirationTime create(ApprovalExpirationTime approvalExpirationTime) {
        if(approvalExpirationTimeDAO.findApprovalExpirationTime() != null){
            throw new IllegalArgumentException("It's already set the approval expiration time");
        }
        validateRequiredFields(approvalExpirationTime);
        Integer id = approvalExpirationTimeDAO.insertApprovalExpirationTime(new Date(), approvalExpirationTime.getAmountOfDays(), approvalExpirationTime.getUserId());
        return findApprovalExpirationTimeById(id);
    }



    @Override
    public ApprovalExpirationTime update(ApprovalExpirationTime approvalExpirationTime, Integer id) throws NotFoundException {
        validateRequiredFields(approvalExpirationTime);
        approvalExpirationTimeDAO.updateApprovalExpirationTime(id, approvalExpirationTime.getAmountOfDays(), new Date(), approvalExpirationTime.getUserId());
        return findApprovalExpirationTimeById(id);
    }

    @Override
    public ApprovalExpirationTime findApprovalExpirationTime() {
        ApprovalExpirationTime approvalExpirationTime =  approvalExpirationTimeDAO.findApprovalExpirationTime();
        if(approvalExpirationTime == null){
            approvalExpirationTime = new ApprovalExpirationTime();
            approvalExpirationTime.setAmountOfDays(DarConstants.DEFAULT_AMOUNT_OF_DAYS);
            approvalExpirationTime.setDisplayName(DarConstants.DUOS_DEFAULT);
        }
        return approvalExpirationTime;
    }

    @Override
    public ApprovalExpirationTime findApprovalExpirationTimeById(Integer id) throws NotFoundException {
        ApprovalExpirationTime approvalExpirationTime = approvalExpirationTimeDAO.findApprovalExpirationTimeById(id);
        if(approvalExpirationTime == null){
            throw new NotFoundException("Approval expiration time for the specified id does not exist");
        }
        return approvalExpirationTime;
    }

    @Override
    public void deleteApprovalExpirationTime(Integer id) {
        approvalExpirationTimeDAO.deleteApprovalExpirationTime(id);
    }

    private void validateRequiredFields(ApprovalExpirationTime approvalExpirationTime) {
        if(approvalExpirationTime.getAmountOfDays() == null){
            throw new IllegalArgumentException("Amount of days is required");
        }
        if(approvalExpirationTime.getUserId() == null){
            throw new IllegalArgumentException("User id is required");
        }else{
            if(dacUserDAO.findDACUserById(approvalExpirationTime.getUserId()) == null){
                throw new IllegalArgumentException("The specified user id does not exist");
            }
        }
    }
}