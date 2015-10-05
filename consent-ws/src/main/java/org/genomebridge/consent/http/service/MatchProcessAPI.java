package org.genomebridge.consent.http.service;


import java.io.IOException;

public interface MatchProcessAPI {

    void processMatchesForConsent(String consentId) throws IOException, UnknownIdentifierException;

    void processMatchesForPurpose(String purposeId) throws IOException, UnknownIdentifierException;

    void removeMatchesForPurpose(String consentId);

    void removeMatchesForConsent(String consentId);

}
