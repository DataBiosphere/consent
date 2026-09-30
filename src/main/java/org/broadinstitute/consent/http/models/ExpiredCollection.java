package org.broadinstitute.consent.http.models;

import java.time.Instant;
import org.broadinstitute.consent.http.enumeration.AccessEndReason;

public record ExpiredCollection(
    Integer collectionId, String darCode, Instant accessEnd, AccessEndReason reason) {}
