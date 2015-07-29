package org.genomebridge.consent.http.service;

import java.util.Date;

import org.apache.commons.lang.StringUtils;
import org.genomebridge.consent.http.db.ConsentDAO;
import org.genomebridge.consent.http.db.DataRequestDAO;
import org.genomebridge.consent.http.db.ElectionDAO;
import org.genomebridge.consent.http.enumeration.ElectionStatus;
import org.genomebridge.consent.http.enumeration.ElectionType;
import org.genomebridge.consent.http.models.Election;

import com.sun.jersey.api.NotFoundException;

/**
 * Implementation class for ElectionAPI on top of ElectionDAO database support.
 */
public class DatabaseElectionAPI extends AbstractElectionAPI {

    private ElectionDAO electionDAO;
    private ConsentDAO consentDAO;
    private DataRequestDAO dataRequestDAO;

    /**
     * Initialize the singleton API instance using the provided DAO. This method
     * should only be called once during application initialization (from the
     * run() method). If called a second time it will throw an
     * IllegalStateException. Note that this method is not synchronized, as it
     * is not intended to be called more than once.
     *
     * @param dao The Data Access Object instance that the API should use to
     *            read/write data.
     */
    public static void initInstance(ElectionDAO dao, ConsentDAO consentDAO, DataRequestDAO dataRequestDAO) {
        ElectionAPIHolder.setInstance(new DatabaseElectionAPI(dao, consentDAO, dataRequestDAO));

    }

    /**
     * The constructor is private to force use of the factory methods and
     * enforce the singleton pattern.
     *
     * @param dao The Data Access Object used to read/write data.
     */
    private DatabaseElectionAPI(ElectionDAO dao, ConsentDAO consentDAO, DataRequestDAO dataRequestDAO) {
        this.electionDAO = dao;
        this.consentDAO = consentDAO;
        this.dataRequestDAO = dataRequestDAO;
    }

    @Override
    public Election createElection(Election election, String referenceId, Boolean isConsent) throws
            IllegalArgumentException {
        validateReferenceId(referenceId, isConsent);
        validateExistentElection(referenceId);
        validateStatus(election.getStatus());
        setGeneralFields(election, referenceId, isConsent);
        Integer id = electionDAO.insertElection(election.getElectionType(),
                election.getFinalVote(), election.getFinalRationale(), election.getStatus(),
                election.getCreateDate(), election.getReferenceId());
        return electionDAO.findElectionById(id);
    }


    @Override
    public Election updateElectionById(Election rec, Integer electionId) {
        validateStatus(rec.getStatus());
        if (rec.getStatus() == null) {
            rec.setStatus(ElectionStatus.OPEN.getValue());
        }
        if (electionDAO.findElectionById(electionId) == null) {
            throw new NotFoundException("Election for specified id does not exist");
        }
        electionDAO.updateElectionById(electionId, rec.getFinalVote(), rec.getFinalRationale(), rec.getStatus());
        return electionDAO.findElectionById(electionId);
    }

    @Override
    public Election describeConsentElection(String consentId) {
        if (consentDAO.checkConsentbyId(consentId) == null) {
            throw new NotFoundException("Invalid ConsentId");
        }
        Election election = electionDAO.getOpenElectionByReferenceId(consentId);
        if (election == null) {
            throw new NotFoundException("Election was not found");
        }
        return election;
    }

    @Override
    public void deleteElection(String referenceId, Integer id) {
        if (electionDAO.findElectionsByReferenceId(referenceId) == null) {
            throw new IllegalArgumentException("Does not exist an election for the specified id");
        }
        electionDAO.deleteElectionById(id);

    }

    @Override
    public Election describeDataRequestElection(Integer requestId) {
        validateDataRequestId(requestId);
        Election election = electionDAO.getOpenElectionByReferenceId(requestId.toString());
        if (election == null) {
            throw new NotFoundException("Election was not found");
        }
        return election;
    }

    private void setGeneralFields(Election election, String referenceId, Boolean isConsent) {
        election.setCreateDate(new Date());
        election.setReferenceId(referenceId);
        if (isConsent) {
            election.setElectionType(electionDAO
                    .findElectionTypeByType(ElectionType.TRANSLATE_DUL.getValue()));
        } else {
            election.setElectionType(electionDAO
                    .findElectionTypeByType(ElectionType.DATA_ACCESS.getValue()));
        }

        if (StringUtils.isEmpty(election.getStatus())) {
            election.setStatus(ElectionStatus.OPEN.getValue());
        }
    }

    private void validateReferenceId(String referenceId, Boolean isConsent) {
        if (isConsent) {
            validateConsentId(referenceId);
        } else {
            validateDataRequestId(Integer.valueOf(referenceId));
        }
    }

    private void validateDataRequestId(Integer dataRequest) {
        if (dataRequest != null && dataRequestDAO.checkDataRequestbyId(dataRequest) == null) {
            throw new IllegalArgumentException("Invalid id: " + dataRequest);
        }
    }

    private void validateExistentElection(String referenceId) {
        Election election = electionDAO.getOpenElectionByReferenceId(referenceId);
        if (election != null) {
            throw new IllegalArgumentException(
                    "An open election already exists for the specified id. Election id: "
                            + election.getElectionId());
        }
    }

    private void validateConsentId(String referenceId) {
        if (referenceId == null || consentDAO.checkConsentbyId(referenceId) == null) {
            throw new IllegalArgumentException("Invalid id: " + referenceId);
        }
    }

    private void validateStatus(String status) {
        if (StringUtils.isNotEmpty(status)) {
            if (ElectionStatus.getValue(status) == null) {
                throw new IllegalArgumentException(
                        "Invalid value. Valid status are: " + ElectionStatus.getValues());
            }
        }
    }


}
