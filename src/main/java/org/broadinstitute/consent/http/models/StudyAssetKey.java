package org.broadinstitute.consent.http.models;

/** A live asset row's id and the client id it was converted from, if any. */
public record StudyAssetKey(Integer id, String legacyKey) {}
