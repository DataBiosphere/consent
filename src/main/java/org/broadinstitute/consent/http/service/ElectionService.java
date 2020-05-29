package org.broadinstitute.consent.http.service;

import com.google.inject.Inject;
import org.broadinstitute.consent.http.db.ConsentDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.enumeration.ElectionStatus;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.Election;

import javax.ws.rs.NotFoundException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ElectionService {

    private ConsentDAO consentDAO;
    private ElectionDAO electionDAO;
    private DacService dacService;
    private DataAccessRequestService dataAccessRequestService;

    @Inject
    public ElectionService(ConsentDAO consentDAO, ElectionDAO electionDAO, DacService dacService,
                           DataAccessRequestService dataAccessRequestService) {
        this.consentDAO = consentDAO;
        this.electionDAO = electionDAO;
        this.dacService = dacService;
        this.dataAccessRequestService = dataAccessRequestService;
    }

    public List<Election> describeClosedElectionsByType(String type, AuthUser authUser) {
        List<Election> elections;
        if (type.equals(ElectionType.DATA_ACCESS.getValue())) {
            elections = dacService.filterElectionsByDAC(
                    electionDAO.findLastDataAccessElectionsWithFinalVoteByStatus(ElectionStatus.CLOSED.getValue()),
                    authUser);
            List<String> referenceIds = elections.stream().map(Election::getReferenceId).collect(Collectors.toList());
            List<DataAccessRequest> dataAccessRequests = dataAccessRequestService.
                    getDataAccessRequestsByReferenceIds(referenceIds);
            elections.forEach(election -> {
                Optional<DataAccessRequest> darOption = dataAccessRequests.stream().
                        filter(d -> d.getReferenceId().equals(election.getReferenceId())).
                        findFirst();
                darOption.ifPresent(dar -> {
                    election.setDisplayId(dar.getData().getDarCode());
                    election.setProjectTitle(dar.getData().getProjectTitle());
                });
            });
        } else {
            elections = dacService.filterElectionsByDAC(
                    electionDAO.findElectionsWithFinalVoteByTypeAndStatus(type, ElectionStatus.CLOSED.getValue()),
                    authUser
            );
            if (!elections.isEmpty()) {
                List<String> consentIds = elections.stream().map(Election::getReferenceId).collect(Collectors.toList());
                Collection<Consent> consents = consentDAO.findConsentsFromConsentsIDs(consentIds);
                elections.forEach(election -> {
                    List<Consent> c = consents.stream().filter(cs -> cs.getConsentId().equals(election.getReferenceId())).
                            collect(Collectors.toList());
                    election.setDisplayId(c.get(0).getName());
                    election.setConsentGroupName(c.get(0).getGroupName());
                });
            }
        }

        if (elections.isEmpty()) {
            throw new NotFoundException("Couldn't find any closed elections");
        }
        return elections.stream().distinct().collect(Collectors.toList());
    }

}
