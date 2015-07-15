package org.genomebridge.consent.http.service;

import java.util.List;

import org.genomebridge.consent.http.models.PendingCase;

import com.sun.jersey.api.NotFoundException;

public interface PendingCaseAPI {

    List<PendingCase> describeConsentPendingCases(Integer dacUserId) throws NotFoundException;

    List<PendingCase> describeDataRequestPendingCases(Integer requestId) throws NotFoundException;

}
