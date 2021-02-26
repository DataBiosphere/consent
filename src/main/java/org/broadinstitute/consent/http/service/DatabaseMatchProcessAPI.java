package org.broadinstitute.consent.http.service;


import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;
import org.broadinstitute.consent.http.db.ConsentDAO;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.Match;

public class DatabaseMatchProcessAPI extends AbstractMatchProcessAPI {

    private final MatchAPI matchAPI;
    private final MatchingServiceAPI matchingServiceAPI;
    private final ConsentDAO consentDAO;
    private final DataAccessRequestService dataAccessRequestService;

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
        DataAccessRequest dar = dataAccessRequestService.findByReferenceId(purposeId);
        if (Objects.nonNull(dar)) {
            Match match = matchingServiceAPI.findMatchForPurpose(dar.getReferenceId());
            saveMatch(Collections.singletonList(match));
        }

    }

    @Override
    public void removeMatchesForPurpose(String purposeId) {
        List<Match> matches = matchAPI.findMatchByPurposeId(purposeId);
        if (CollectionUtils.isNotEmpty(matches)) {
            matchAPI.deleteMatches(matches.stream().map(Match::getId).collect(Collectors.toList()));
        }
    }

    @Override
    public void removeMatchesForConsent(String consentId) {
        List<Match> matches = matchAPI.findMatchByConsentId(consentId);
        if (CollectionUtils.isNotEmpty(matches)) {
            matchAPI.deleteMatches(matches.stream().map(Match::getId).collect(Collectors.toList()));
        }

    }


    private void saveMatch(List<Match> matches) {
        matchAPI.createMatches(matches);
    }

}
