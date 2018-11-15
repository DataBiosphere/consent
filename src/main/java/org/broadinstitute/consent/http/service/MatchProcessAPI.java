package org.broadinstitute.consent.http.service;


import org.broadinstitute.consent.http.db.mongo.MongoConsentDB;

import java.io.IOException;

public interface MatchProcessAPI {

    void processMatchesForConsent(String consentId) throws IOException, UnknownIdentifierException;

    void processMatchesForPurpose(String purposeId) throws IOException, UnknownIdentifierException;

    void removeMatchesForPurpose(String consentId);

    void removeMatchesForConsent(String consentId);
}
