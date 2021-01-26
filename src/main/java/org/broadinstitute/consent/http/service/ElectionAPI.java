package org.broadinstitute.consent.http.service;

import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.DataSet;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.Vote;

import javax.ws.rs.NotFoundException;
import java.util.List;
import java.util.Map;

@Deprecated // Use ElectionService
public interface ElectionAPI {

    Election createElection(Election rec, String referenceId, ElectionType electionType) throws Exception;

    Election updateElectionById(Election rec, Integer electionId) throws IllegalArgumentException, NotFoundException;

    Election updateFinalAccessVoteDataRequestElection(Integer electionId) throws Exception;

    Election describeConsentElection(String consentId) throws NotFoundException;

    Election describeDataRequestElection(String requestId) throws NotFoundException;

    Election describeElectionById(Integer electionId);

    Election describeElectionByVoteId(Integer voteId) throws NotFoundException;

    void deleteElection(String referenceId, Integer electionId) throws IllegalArgumentException, NotFoundException;

    Integer findRPElectionByElectionAccessId(Integer accessElectionId);

    boolean validateCollectEmailCondition(Vote vote);

    boolean validateCollectDAREmailCondition(Vote vote);

    void closeDataOwnerApprovalElection(Integer electionId);

    boolean checkDataOwnerToCloseElection(Integer electionId);

    List<Election> createDataSetElections(String referenceId, Map<User, List<DataSet>> dataOwnerDataSet);

    boolean isDataSetElectionOpen();

    String darDatasetElectionStatus(String darReferenceId);

    Election getConsentElectionByDARElectionId(Integer electionId);

}
