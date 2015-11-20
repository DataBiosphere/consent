package org.genomebridge.consent.http.service;

import org.genomebridge.consent.http.db.mongo.MongoConsentDB;
import org.genomebridge.consent.http.enumeration.ElectionType;
import org.genomebridge.consent.http.models.Election;
import org.genomebridge.consent.http.models.Vote;

import javax.ws.rs.NotFoundException;
import java.io.IOException;
import java.util.List;

public interface ElectionAPI {

    Election createElection(Election rec, String referenceId, ElectionType electionType) throws IllegalArgumentException, IOException;

    Election updateElectionById(Election rec, Integer electionId) throws IllegalArgumentException, NotFoundException;

    Election updateFinalAccessVoteDataRequestElection(Integer electionId);

    Election describeConsentElection(String consentId) throws NotFoundException;

    Election describeDataRequestElection(String requestId) throws NotFoundException;

    Election describeElectionById(Integer electionId);

    void deleteElection(String referenceId, Integer electionId) throws IllegalArgumentException, NotFoundException;

    List<Election> cancelOpenElectionAndReopen()  ;

    List<Election> describeClosedElectionsByType(String type);

    void setMongoDBInstance(MongoConsentDB mongo);

    Integer findRPElectionByElectionAccessId(Integer accessElectionId);

    boolean validateCollectEmailCondition(Vote vote);

    boolean validateCollectDAREmailCondition(Vote vote);
}
