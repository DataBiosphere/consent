package org.genomebridge.consent.http.service;

import org.genomebridge.consent.http.models.Match;
import java.util.List;

public interface MatchAPI {

    Match create(Match rec);

    Match update(Match match, Integer id);

    Match findMatchById(Integer id);

    Match findMatchByConsentIdAndPurposeId(String consentId, String purposeId);

    List<Match> findMatchByConsentId(String consentId);

    List<Match> findMatchByPurposeId(String purposeId);

    void deleteMatch(Integer id);

    void deleteMatches(List<Integer> ids);

    void createMatches(List<Match> matches);

}
