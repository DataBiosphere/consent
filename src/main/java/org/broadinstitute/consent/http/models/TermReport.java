package org.broadinstitute.consent.http.models;

import java.util.List;

/** The ontology terms cited by the most DARs submitted in the range, most first. */
public record TermReport(String from, String to, List<TermDarCount> terms) {}
