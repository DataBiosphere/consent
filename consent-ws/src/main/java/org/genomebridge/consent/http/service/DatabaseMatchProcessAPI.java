package org.genomebridge.consent.http.service;


import org.apache.commons.collections.CollectionUtils;
import org.genomebridge.consent.http.db.ConsentDAO;
import org.genomebridge.consent.http.models.Match;
import java.util.List;

public class DatabaseMatchProcessAPI extends AbstractMatchProcessAPI {

    private MatchAPI matchAPI;
    private MatchingServiceAPI matchingServiceAPI;
    private ConsentDAO consentDAO;


    public static void initInstance(ConsentDAO consentDAO) {
        MatchProcessAPIHolder.setInstance(new DatabaseMatchProcessAPI(consentDAO));
    }

    private DatabaseMatchProcessAPI(ConsentDAO consentDAO) {
        matchAPI = AbstractMatchAPI.getInstance();
        matchingServiceAPI = AbstractMatchingServiceAPI.getInstance();
        this.consentDAO = consentDAO;
    }

    @Override
    public void processMatchesForConsent(String consentId) {
        if(consentDAO.checkManualReview(consentId)){
            removeMatch(consentId);
        }else{
            List<Match> matches = matchingServiceAPI.findMatchesForConsent(consentId);
            saveMatch(matches);
        }
    }

    @Override
    public void processMatchesForPurpose(String purposeId) {
        if(consentDAO.checkManualReview(consentId)){
            removeMatch(consentId);
        }else{
            List<Match> matches = matchingServiceAPI.findMatchesForConsent(consentId);
            saveMatch(matches);
        }
    }

    private void removeMatch(String consentId) {
        List<Match> matchList = matchAPI.findMatchByConsentId(consentId);
        if(CollectionUtils.isNotEmpty(matchList)){
            matchList.stream().forEach((match) -> {
                matchAPI.deleteMatch(match.getId());
            });
        }
    }

    private void saveMatch(List<Match> matches) {
        if (CollectionUtils.isNotEmpty(matches)) {
            matches.stream().forEach((match) -> {
               matchAPI.create(match);
            });
        }
    }



}
