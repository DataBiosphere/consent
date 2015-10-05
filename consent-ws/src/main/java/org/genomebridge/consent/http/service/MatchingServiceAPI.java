package org.genomebridge.consent.http.service;

import org.genomebridge.consent.http.models.Match;

import javax.ws.rs.NotFoundException;
import java.io.IOException;
import java.util.List;

public interface MatchingServiceAPI {

    Match findSingleMatch(String consentId, String purposeId) throws IllegalArgumentException, IOException, UnknownIdentifierException;

    List<Match> findMatchesForConsent(String consentId) throws IllegalArgumentException, NotFoundException, UnknownIdentifierException, IOException;

    List<Match> findMatchesForPurpose(String purposeId) throws NotFoundException, IOException, UnknownIdentifierException;
}
