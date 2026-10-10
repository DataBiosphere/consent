package org.broadinstitute.consent.http.models;

/** How many new-DAA emails were sent, skipped because the recipient opted out, or failed. */
public record NewDaaEmailResult(int sent, int skipped, int failed) {}
