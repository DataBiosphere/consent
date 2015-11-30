package org.broadinstitute.consent.http.service;


import java.io.IOException;
import org.broadinstitute.consent.http.db.mongo.MongoConsentDB;

public interface MatchProcessAPI {

    void processMatchesForConsent(String consentId) throws IOException, UnknownIdentifierException;

    void processMatchesForPurpose(String purposeId) throws IOException, UnknownIdentifierException;

    void removeMatchesForPurpose(String consentId);

    void removeMatchesForConsent(String consentId);
    
    void setMongoDBInstance(MongoConsentDB mongo);
}
