package org.broadinstitute.consent.http.service;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.collections.CollectionUtils;
import org.broadinstitute.consent.http.db.ConsentDAO;
import org.broadinstitute.consent.http.models.Match;
import org.broadinstitute.consent.http.util.DarConstants;
import org.bson.Document;

public class DatabaseMatchProcessAPI extends AbstractMatchProcessAPI {

    private MatchAPI matchAPI;
    private MatchingServiceAPI matchingServiceAPI;
    private ConsentDAO consentDAO;
    private DataAccessRequestService dataAccessRequestService;

    public static void initInstance(ConsentDAO consentDAO, DataAccessRequestService dataAccessRequestService) {
        MatchProcessAPIHolder.setInstance(new DatabaseMatchProcessAPI(consentDAO, dataAccessRequestService));
    }

    private DatabaseMatchProcessAPI(ConsentDAO consentDAO, DataAccessRequestService dataAccessRequestService) {
        this.matchAPI = AbstractMatchAPI.getInstance();
        this.matchingServiceAPI = AbstractMatchingServiceAPI.getInstance();
        this.consentDAO = consentDAO;
        this.dataAccessRequestService = dataAccessRequestService;
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
        Document rp = dataAccessRequestService.getDataAccessRequestByReferenceIdAsDocument(purposeId);
        if (rp != null && rp.get(DarConstants.RESTRICTION) != null) {
            Match match = matchingServiceAPI.findMatchForPurpose(purposeId);
            saveMatch(Arrays.asList(match));
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
