package org.broadinstitute.consent.http.service;

import com.mongodb.BasicDBObject;
import com.mongodb.Block;
import com.mongodb.client.FindIterable;
import org.apache.commons.collections.CollectionUtils;
import org.broadinstitute.consent.http.db.ConsentDAO;
import org.broadinstitute.consent.http.db.DACUserDAO;
import org.broadinstitute.consent.http.db.DacDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.db.VoteDAO;
import org.broadinstitute.consent.http.db.mongo.MongoConsentDB;
import org.broadinstitute.consent.http.enumeration.ElectionStatus;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.ConsentManage;
import org.broadinstitute.consent.http.models.DACUser;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.util.DarConstants;
import org.bson.Document;
import org.bson.types.ObjectId;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ConsentService {

    private ConsentDAO consentDAO;
    private DacDAO dacDAO;
    private DACUserDAO dacUserDAO;
    private ElectionDAO electionDAO;
    private MongoConsentDB mongo;
    private VoteDAO voteDAO;

    @Inject
    public ConsentService(ConsentDAO consentDAO, DacDAO dacDAO, DACUserDAO dacUserDAO, ElectionDAO electionDAO, MongoConsentDB mongo, VoteDAO voteDAO) {
        this.consentDAO = consentDAO;
        this.dacDAO = dacDAO;
        this.dacUserDAO = dacUserDAO;
        this.electionDAO = electionDAO;
        this.mongo = mongo;
        this.voteDAO = voteDAO;
    }

    @SuppressWarnings("unchecked")
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
            List<String> datasetNames = new ArrayList<>();
            dataAccessRequests.forEach((Block<Document>) dar -> {
                List<String> dataSets = dar.get(DarConstants.DATASET_ID, List.class);
                datasetNames.addAll(dataSets);
            });
            List<String> objectIds = new ArrayList<>();
            if (CollectionUtils.isNotEmpty(datasetNames)) {
                objectIds = consentDAO.getAssociationsConsentIdfromDataSetIds(datasetNames);
            }

            for (ConsentManage consentManage : consentManageList) {
                if (objectIds.stream().anyMatch(cm -> cm.equals(consentManage.getConsentId()))) {
                    consentManage.setEditable(false);
                } else {
                    consentManage.setEditable(true);
                }
            }
        }
        return filterConsentManageByDAC(consentManageList, authUser);
    }

    public Integer getUnReviewedConsents(AuthUser authUser) {
        Collection<Consent> consents = consentDAO.findUnreviewedConsents();
        return filterConsentsByDAC(consents, authUser).size();
    }

    private List<ConsentManage> filterConsentManageByDAC(List<ConsentManage> consentManages, AuthUser authUser) {
        if (isAuthUserAdmin(authUser)) {
            return consentManages;
        }
        List<Integer> dacIds = getDacIdsForUser(authUser);
        if (dacIds.isEmpty()) {
            return Collections.emptyList();
        }
        return consentManages.stream().
                filter(c -> c.getDacId() != null).
                filter(c -> dacIds.contains(c.getDacId())).
                collect(Collectors.toList());
    }

    private List<ConsentManage> collectUnreviewedConsents(List<Consent> consents) {
        String UNREVIEWED = "un-reviewed"; // TODO: Should this be a real `ElectionStatus` enum? See what that could impact elsewhere.
        List<ConsentManage> consentManageList = consents.stream().map(ConsentManage::new).collect(Collectors.toList());
        consentManageList.forEach(c -> c.setElectionStatus(UNREVIEWED));
        consentManageList.forEach(c -> c.setEditable(true));
        return consentManageList;
    }

    private Collection<Consent> filterConsentsByDAC(Collection<Consent> consents, AuthUser authUser) {
        if (isAuthUserAdmin(authUser)) {
            return consents;
        }
        List<Integer> dacIds = getDacIdsForUser(authUser);
        if (dacIds.isEmpty()) {
            return Collections.emptyList();
        }
        return consents.stream().
                filter(c -> c.getDacId() != null).
                filter(c -> dacIds.contains(c.getDacId())).
                collect(Collectors.toList());
    }

    private boolean isAuthUserAdmin(AuthUser authUser) {
        DACUser user = dacUserDAO.findDACUserByEmail(authUser.getName());
        return user.getRoles().stream().
                anyMatch(ur -> ur.getRoleId().equals(UserRoles.ADMIN.getRoleId()));
    }

    private List<Integer> getDacIdsForUser(AuthUser authUser) {
        return dacDAO.findDacsForUser(authUser.getName())
                .stream()
                .map(Dac::getDacId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

}
