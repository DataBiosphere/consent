package org.broadinstitute.consent.http.service;

import org.broadinstitute.consent.http.db.mongo.MongoConsentDB;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.models.DACUser;
import org.broadinstitute.consent.http.models.DataSet;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.Vote;
import org.broadinstitute.consent.http.models.dto.ElectionStatusDTO;
import org.bson.Document;

import javax.ws.rs.NotFoundException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface ElectionAPI {

    Election createElection(Election rec, String referenceId, ElectionType electionType) throws Exception;

    Election updateElectionById(Election rec, Integer electionId) throws IllegalArgumentException, NotFoundException;

    Election updateFinalAccessVoteDataRequestElection(Integer electionId);

    Election describeConsentElection(String consentId) throws NotFoundException;

    Election describeDataRequestElection(String requestId) throws NotFoundException;

    Election describeElectionById(Integer electionId);

    void deleteElection(String referenceId, Integer electionId) throws IllegalArgumentException, NotFoundException;

    List<Election> cancelOpenElectionAndReopen() throws Exception;

    List<Election> describeClosedElectionsByType(String type);

    void setMongoDBInstance(MongoConsentDB mongo);

    Integer findRPElectionByElectionAccessId(Integer accessElectionId);

    boolean validateCollectEmailCondition(Vote vote);

    boolean validateCollectDAREmailCondition(Vote vote);

    void closeDataOwnerApprovalElection(Integer electionId);

    boolean checkDataOwnerToCloseElection(Integer electionId);

    List<Election> findExpiredElections(String electionType);

    List<Election> createDataSetElections(String referenceId, Map<DACUser, List<DataSet>> dataOwnerDataSet);

    boolean isDataSetElectionOpen();

    String darDatasetElectionStatus(String darReferenceId);

    List<ElectionStatusDTO> describeElectionsByConsentId(String consentId);

    List<ElectionStatusDTO> describeElectionByDARs(List<Document> darList);
}
