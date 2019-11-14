package org.broadinstitute.consent.http.service;

import com.google.inject.Inject;
import com.mongodb.BasicDBObject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCursor;
import org.broadinstitute.consent.http.db.ConsentDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.db.mongo.MongoConsentDB;
import org.broadinstitute.consent.http.enumeration.ElectionStatus;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.util.DarConstants;
import org.bson.Document;
import org.bson.types.ObjectId;

import javax.ws.rs.NotFoundException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class ElectionService {

    private ConsentDAO consentDAO;
    private ElectionDAO electionDAO;
    private MongoConsentDB mongo;
    private DacService dacService;

    @Inject
    public ElectionService(ConsentDAO consentDAO, ElectionDAO electionDAO, MongoConsentDB mongo,
                           DacService dacService) {
        this.consentDAO = consentDAO;
        this.electionDAO = electionDAO;
        this.mongo = mongo;
        this.dacService = dacService;
    }

    public List<Election> describeClosedElectionsByType(String type, AuthUser authUser) {
        List<Election> elections;
        if (type.equals(ElectionType.DATA_ACCESS.getValue())) {
            elections = dacService.filterElectionsByDAC(
                    electionDAO.findRequestElectionsWithFinalVoteByStatus(ElectionStatus.CLOSED.getValue()),
                    authUser);
            List<String> referenceIds = elections.stream().map(Election::getReferenceId).collect(Collectors.toList());
            ObjectId[] objarray = new ObjectId[referenceIds.size()];
            for (int i = 0; i < referenceIds.size(); i++)
                objarray[i] = new ObjectId(referenceIds.get(i));
            BasicDBObject in = new BasicDBObject("$in", objarray);
            BasicDBObject q = new BasicDBObject(DarConstants.ID, in);
            FindIterable<Document> dataAccessRequests = mongo.getDataAccessRequestCollection().find(q);
            elections.forEach(election -> {
                try (MongoCursor<Document> itr = dataAccessRequests.iterator()) {
                    while (itr.hasNext()) {
                        Document next = itr.next();
                        if (next.get(DarConstants.ID).toString().equals(election.getReferenceId())) {
                            election.setDisplayId(next.get(DarConstants.DAR_CODE).toString());
                            election.setProjectTitle(next.get(DarConstants.PROJECT_TITLE).toString());
                        }
                    }
                }
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
        return elections;
    }

}
