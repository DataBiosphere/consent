package org.broadinstitute.consent.http.service;

import org.broadinstitute.consent.http.models.ApprovalExpirationTime;
import javax.ws.rs.NotFoundException;


public interface ApprovalExpirationTimeAPI {

    ApprovalExpirationTime create(ApprovalExpirationTime approvalExpirationTime);

    ApprovalExpirationTime update(ApprovalExpirationTime approvalExpirationTime, Integer id) throws NotFoundException;

    ApprovalExpirationTime findApprovalExpirationTime();

    ApprovalExpirationTime findApprovalExpirationTimeById(Integer id);

    void deleteApprovalExpirationTime(Integer id);
}
