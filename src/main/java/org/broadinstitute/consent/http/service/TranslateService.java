package org.broadinstitute.consent.http.service;

import org.bson.Document;

public interface TranslateService {

    String generateStructuredTranslatedRestriction(Document dar, Boolean needsManualReview);

}
