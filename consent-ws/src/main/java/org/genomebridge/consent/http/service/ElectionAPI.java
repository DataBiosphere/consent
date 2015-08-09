package org.genomebridge.consent.http.service;

import org.genomebridge.consent.http.models.Election;

import com.sun.jersey.api.NotFoundException;

public interface ElectionAPI {

    Election createElection(Election rec, String referenceId, Boolean isConsent) throws IllegalArgumentException;

    Election updateElectionById(Election rec, Integer electionId) throws IllegalArgumentException, NotFoundException;

    Election describeConsentElection(String consentId) throws NotFoundException;

    Election describeDataRequestElection(Integer requestId) throws NotFoundException;

    void deleteElection(String referenceId, Integer electionId) throws IllegalArgumentException, NotFoundException;

}
