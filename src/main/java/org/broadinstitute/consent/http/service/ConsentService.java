package org.broadinstitute.consent.http.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.inject.Inject;
import org.apache.commons.collections.CollectionUtils;
import org.broadinstitute.consent.http.db.ConsentDAO;
import org.broadinstitute.consent.http.db.DataAccessRequestDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.db.VoteDAO;
import org.broadinstitute.consent.http.enumeration.ElectionStatus;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.ConsentManage;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.DatasetDetailEntry;
import org.broadinstitute.consent.http.models.Election;

public class ConsentService {

    private ConsentDAO consentDAO;
    private ElectionDAO electionDAO;
    private VoteDAO voteDAO;
    private DacService dacService;
    private DataAccessRequestDAO dataAccessRequestDAO;

    @Inject
    public ConsentService(ConsentDAO consentDAO, ElectionDAO electionDAO, VoteDAO voteDAO, DacService dacService,
                          DataAccessRequestDAO dataAccessRequestDAO) {
        this.consentDAO = consentDAO;
        this.electionDAO = electionDAO;
        this.voteDAO = voteDAO;
        this.dacService = dacService;
        this.dataAccessRequestDAO = dataAccessRequestDAO;
    }

    public Consent getById(String id) throws UnknownIdentifierException {
        Consent consent = consentDAO.findConsentById(id);
        if (consent == null) {
            throw new UnknownIdentifierException(String.format("Could not find consent with id %s", id));
        }
        Election election = electionDAO.findLastElectionByReferenceIdAndType(id, ElectionType.TRANSLATE_DUL.getValue());
        if (election != null) {
            consent.setLastElectionStatus(election.getStatus());
            consent.setLastElectionArchived(election.getArchived());
        }
        return consent;
    }

    public void updateConsentDac(String consentId, Integer dacId) {
        consentDAO.updateConsentDac(consentId, dacId);
    }

    public List<ConsentManage> describeConsentManage(AuthUser authUser) {
        List<ConsentManage> consentManageList = new ArrayList<>();
        consentManageList.addAll(collectUnreviewedConsents(consentDAO.findUnreviewedConsents()));
        consentManageList.addAll(consentDAO.findConsentManageByStatus(ElectionStatus.OPEN.getValue()));
        consentManageList.addAll(consentDAO.findConsentManageByStatus(ElectionStatus.CANCELED.getValue()));
        List<ConsentManage> closedElections = consentDAO.findConsentManageByStatus(ElectionStatus.CLOSED.getValue());
        closedElections.forEach(consentManage -> {
            Boolean vote = voteDAO.findChairPersonVoteByElectionId(consentManage.getElectionId());
            consentManage.setVote(vote != null && vote ? "Approved" : "Denied");
        });
        consentManageList.addAll(closedElections);
        consentManageList.sort((c1, c2) -> c2.getSortDate().compareTo(c1.getSortDate()));
        List<Election> openElections = electionDAO.findElectionsWithFinalVoteByTypeAndStatus(ElectionType.DATA_ACCESS.getValue(), ElectionStatus.OPEN.getValue());
        if (!openElections.isEmpty()) {
            List<String> referenceIds = openElections.stream().map(Election::getReferenceId).collect(Collectors.toList());
            List<DataAccessRequest> dataAccessRequests = dataAccessRequestDAO.findByReferenceIds(referenceIds);
            List<String> datasetIds =
                dataAccessRequests.stream()
                    .filter(Objects::nonNull)
                    .map(DataAccessRequest::getData)
                    .filter(Objects::nonNull)
                    .map(DataAccessRequestData::getDatasetDetail)
                    .filter(Objects::nonNull)
                    .flatMap(List::stream)
                    .map(DatasetDetailEntry::getDatasetId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            List<String> consentIds = new ArrayList<>();
            if (CollectionUtils.isNotEmpty(datasetIds)) {
                List<Integer> datasetIdIntValues = datasetIds.stream().map(Integer::valueOf).collect(Collectors.toList());
                consentIds.addAll(consentDAO.getAssociationConsentIdsFromDatasetIds(datasetIdIntValues));
            }

            for (ConsentManage consentManage : consentManageList) {
                if (consentIds.stream().anyMatch(cm -> cm.equals(consentManage.getConsentId()))) {
                    consentManage.setEditable(false);
                } else {
                    consentManage.setEditable(true);
                }
            }
        }
        return dacService.filterConsentManageByDAC(consentManageList, authUser);
    }

    public Integer getUnReviewedConsents(AuthUser authUser) {
        Collection<Consent> consents = consentDAO.findUnreviewedConsents();
        return dacService.filterConsentsByDAC(consents, authUser).size();
    }

    private List<ConsentManage> collectUnreviewedConsents(List<Consent> consents) {
        String UNREVIEWED = "un-reviewed"; // TODO: Fix this in https://broadinstitute.atlassian.net/browse/DUOS-469
        List<ConsentManage> consentManageList = consents.stream().map(ConsentManage::new).collect(Collectors.toList());
        consentManageList.forEach(c -> c.setElectionStatus(UNREVIEWED));
        consentManageList.forEach(c -> c.setEditable(true));
        return consentManageList;
    }

}
