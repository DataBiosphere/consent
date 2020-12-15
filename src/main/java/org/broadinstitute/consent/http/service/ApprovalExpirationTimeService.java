package org.broadinstitute.consent.http.service;

import com.google.inject.Inject;
import java.util.Date;
import org.broadinstitute.consent.http.db.ApprovalExpirationTimeDAO;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.models.ApprovalExpirationTime;
import javax.ws.rs.NotFoundException;
import org.broadinstitute.consent.http.util.DarConstants;

public class ApprovalExpirationTimeService {

    private final ApprovalExpirationTimeDAO approvalExpirationTimeDAO;
    private final UserDAO userDAO;

    @Inject
    public ApprovalExpirationTimeService(ApprovalExpirationTimeDAO approvalExpirationTimeDAO,
        UserDAO userDAO) {
        this.approvalExpirationTimeDAO = approvalExpirationTimeDAO;
        this.userDAO = userDAO;
    }

    public ApprovalExpirationTime create(ApprovalExpirationTime approvalExpirationTime) {
        if (approvalExpirationTimeDAO.findApprovalExpirationTime() != null) {
            throw new IllegalArgumentException("Approval expiration time is already set");
        }
        validateRequiredFields(approvalExpirationTime);
        Integer id = approvalExpirationTimeDAO.insertApprovalExpirationTime(new Date(), approvalExpirationTime.getAmountOfDays(), approvalExpirationTime.getUserId());
        return findApprovalExpirationTimeById(id);
    }

    public ApprovalExpirationTime update(ApprovalExpirationTime approvalExpirationTime, Integer id) throws NotFoundException {
        validateRequiredFields(approvalExpirationTime);
        approvalExpirationTimeDAO.updateApprovalExpirationTime(id, approvalExpirationTime.getAmountOfDays(), new Date(), approvalExpirationTime.getUserId());
        return findApprovalExpirationTimeById(id);
    }

    public ApprovalExpirationTime findApprovalExpirationTime() {
        ApprovalExpirationTime approvalExpirationTime =  approvalExpirationTimeDAO.findApprovalExpirationTime();
        if(approvalExpirationTime == null){
            approvalExpirationTime = new ApprovalExpirationTime();
            approvalExpirationTime.setAmountOfDays(DarConstants.DEFAULT_AMOUNT_OF_DAYS);
            approvalExpirationTime.setDisplayName(DarConstants.DUOS_DEFAULT);
        }
        return approvalExpirationTime;
    }

    public ApprovalExpirationTime findApprovalExpirationTimeById(Integer id) throws NotFoundException {
        ApprovalExpirationTime approvalExpirationTime = approvalExpirationTimeDAO.findApprovalExpirationTimeById(id);
        if(approvalExpirationTime == null){
            throw new NotFoundException("Approval expiration time for the specified id does not exist");
        }
        return approvalExpirationTime;
    }

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
            if(userDAO.findUserById(approvalExpirationTime.getUserId()) == null){
                throw new IllegalArgumentException("The specified user id does not exist");
            }
        }
    }
}
