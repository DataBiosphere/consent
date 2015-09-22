package org.genomebridge.consent.http.service;

import org.genomebridge.consent.http.db.mongo.MongoConsentDB;
import org.genomebridge.consent.http.models.Election;

import javax.ws.rs.NotFoundException;
import java.util.List;

public interface ElectionAPI {

    Election createElection(Election rec, String referenceId, Boolean isConsent) throws IllegalArgumentException;

    Election updateElectionById(Election rec, Integer electionId) throws IllegalArgumentException, NotFoundException;

    Election updateFinalAccessVoteDataRequestElection(Integer electionId);

    Election describeConsentElection(String consentId) throws NotFoundException;

    Election describeDataRequestElection(String requestId) throws NotFoundException;

    Election describeElectionById(Integer electionId);

    void deleteElection(String referenceId, Integer electionId) throws IllegalArgumentException, NotFoundException;

    List<Election> cancelOpenElectionAndReopen();

    List<Election> describeClosedElectionsByType(String type);

    void setMongoDBInstance(MongoConsentDB mongo);
}
