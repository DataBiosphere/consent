package org.broadinstitute.consent.http.service;

import com.mongodb.BasicDBObject;
import com.mongodb.Block;
import com.mongodb.client.FindIterable;
import org.apache.commons.collections.CollectionUtils;
import org.broadinstitute.consent.http.db.ConsentDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.db.VoteDAO;
import org.broadinstitute.consent.http.db.mongo.MongoConsentDB;
import org.broadinstitute.consent.http.enumeration.ElectionStatus;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.ConsentManage;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.util.DarConstants;
import org.broadinstitute.consent.http.util.DarUtil;
import org.bson.Document;
import org.bson.types.ObjectId;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class ConsentService {

    private ConsentDAO consentDAO;
    private ElectionDAO electionDAO;
    private MongoConsentDB mongo;
    private VoteDAO voteDAO;
    private DacService dacService;

    @Inject
    public ConsentService(ConsentDAO consentDAO, ElectionDAO electionDAO, MongoConsentDB mongo,
                          VoteDAO voteDAO, DacService dacService) {
        this.consentDAO = consentDAO;
        this.electionDAO = electionDAO;
        this.mongo = mongo;
        this.voteDAO = voteDAO;
        this.dacService = dacService;
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
            ObjectId[] objarray = new ObjectId[referenceIds.size()];
            for (int i = 0; i < referenceIds.size(); i++)
                objarray[i] = new ObjectId(referenceIds.get(i));
            BasicDBObject in = new BasicDBObject("$in", objarray);
            BasicDBObject q = new BasicDBObject(DarConstants.ID, in);
            FindIterable<Document> dataAccessRequests = mongo.getDataAccessRequestCollection().find(q);
            List<Integer> datasetIds = new ArrayList<>();
            dataAccessRequests.forEach((Block<Document>) dar -> {
                List<Integer> dataSets = DarUtil.getIntegerList(dar, DarConstants.DATASET_ID);
                datasetIds.addAll(dataSets);
            });
            List<String> consentIds = new ArrayList<>();
            if (CollectionUtils.isNotEmpty(datasetIds)) {
                consentIds.addAll(consentDAO.getAssociationConsentIdsFromDatasetIds(datasetIds));
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
