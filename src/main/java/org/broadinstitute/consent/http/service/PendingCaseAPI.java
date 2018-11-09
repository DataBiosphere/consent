package org.broadinstitute.consent.http.service;

import org.broadinstitute.consent.http.models.PendingCase;
import org.broadinstitute.consent.http.models.dto.DataOwnerCase;

import javax.ws.rs.NotFoundException;
import java.util.List;

public interface PendingCaseAPI {

    List<PendingCase> describeConsentPendingCases(Integer dacUserId) throws NotFoundException;

    List<PendingCase> describeDataRequestPendingCases(Integer requestId) throws NotFoundException;

    List<DataOwnerCase> describeDataOwnerPendingCases(Integer dataOwnerId);
}
