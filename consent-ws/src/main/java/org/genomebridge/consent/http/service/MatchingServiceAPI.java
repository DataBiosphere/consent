package org.genomebridge.consent.http.service;

import org.genomebridge.consent.http.models.Match;

import java.util.List;

public interface MatchingServiceAPI {

    Match findSingleMatch(String consentId, String purposeId);

    List<Match> findMatchesForConsent(String consentId);

    List<Match> findMatchesForPurpose(String purposeId);
}
