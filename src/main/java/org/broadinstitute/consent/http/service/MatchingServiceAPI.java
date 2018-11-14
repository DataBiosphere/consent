package org.broadinstitute.consent.http.service;

import org.broadinstitute.consent.http.models.Match;

import java.util.List;

public interface MatchingServiceAPI {

    Match findSingleMatch(String consentId, String purposeId);

    List<Match> findMatchesForConsent(String consentId);

    Match findMatchForPurpose(String purposeId);
}
