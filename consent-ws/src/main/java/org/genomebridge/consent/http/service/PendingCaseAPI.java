package org.genomebridge.consent.http.service;

import org.genomebridge.consent.http.models.PendingCase;

import javax.ws.rs.NotFoundException;
import java.util.List;

public interface PendingCaseAPI {

    List<PendingCase> describeConsentPendingCases(Integer dacUserId) throws NotFoundException;

    List<PendingCase> describeDataRequestPendingCases(Integer requestId) throws NotFoundException;

}
