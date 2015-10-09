package org.genomebridge.consent.http.service;


import com.mongodb.BasicDBObject;
import org.apache.commons.collections.CollectionUtils;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.genomebridge.consent.http.db.ConsentDAO;
import org.genomebridge.consent.http.db.mongo.MongoConsentDB;
import org.genomebridge.consent.http.models.Match;
import java.util.ArrayList;
import java.util.List;

public class DatabaseMatchProcessAPI extends AbstractMatchProcessAPI {

    private MatchAPI matchAPI;
    private MatchingServiceAPI matchingServiceAPI;
    private ConsentDAO consentDAO;
    private MongoConsentDB mongo;

    public static void initInstance(ConsentDAO consentDAO, MongoConsentDB mongo) {
        MatchProcessAPIHolder.setInstance(new DatabaseMatchProcessAPI(consentDAO, mongo));
    }

    private DatabaseMatchProcessAPI(ConsentDAO consentDAO, MongoConsentDB mongo) {
        this.matchAPI = AbstractMatchAPI.getInstance();
        this.matchingServiceAPI = AbstractMatchingServiceAPI.getInstance();
        this.consentDAO = consentDAO;
        this.mongo = mongo;
    }

    @Override
    public void processMatchesForConsent(String consentId) {
        removeMatchesForConsent(consentId);
        if (!consentDAO.checkManualReview(consentId)) {
            List<Match> matches = matchingServiceAPI.findMatchesForConsent(consentId);
            saveMatch(matches);
        }
    }

    @Override
    public void processMatchesForPurpose(String purposeId) {
        removeMatchesForPurpose(purposeId);
        BasicDBObject query = new BasicDBObject("_id", new ObjectId(purposeId));
        Document rp = mongo.getDataAccessRequestCollection().find(query).first();
        if (rp != null && rp.get("restriction") != null) {
            List<Match> matches = matchingServiceAPI.findMatchesForPurpose(purposeId);
            saveMatch(matches);
        }

    }

    @Override
    public void removeMatchesForPurpose(String purposeId) {
        List<Match> matches = matchAPI.findMatchByPurposeId(purposeId);
        if (CollectionUtils.isNotEmpty(matches)) {
            matchAPI.deleteMatches(getIds(matches));
        }
    }

    @Override
    public void removeMatchesForConsent(String consentId) {
        List<Match> matches = matchAPI.findMatchByConsentId(consentId);
        if (CollectionUtils.isNotEmpty(matches)) {
            matchAPI.deleteMatches(getIds(matches));
        }

    }


    private void saveMatch(List<Match> matches) {
        matchAPI.createMatches(matches);
    }

    private List<Integer> getIds(List<Match> matches) {
        List<Integer> ids = new ArrayList<>();
        matches.stream().forEach((match) -> ids.add(match.getId()));
        return ids;
    }

}
