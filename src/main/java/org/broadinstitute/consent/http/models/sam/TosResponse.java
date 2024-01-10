package org.broadinstitute.consent.http.models.sam;

public record TosResponse(String acceptedOn, Boolean isCurrentVersion, String latestAcceptedVersion,
                          Boolean permitsSystemUsage) {

}
