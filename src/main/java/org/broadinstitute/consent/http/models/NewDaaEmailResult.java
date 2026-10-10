package org.broadinstitute.consent.http.models;

/** How many new-DAA emails went out and how many failed. */
public record NewDaaEmailResult(int sent, int failed) {}
