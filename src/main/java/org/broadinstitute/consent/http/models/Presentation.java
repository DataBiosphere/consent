package org.broadinstitute.consent.http.models;

public record Presentation(String title, String link, String date, String authors, String datasetCitation,
                           Boolean citation) {
}
