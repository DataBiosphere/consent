package org.broadinstitute.consent.http.models;

/** A study_property row as stored, before its value is parsed. */
public record StoredStudyProperty(Integer studyPropertyId, String key, String type, String value) {}
