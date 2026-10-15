package org.broadinstitute.consent.http.models;

public record NewDaaEmailResult(int sent, int skipped, int failed) {}
