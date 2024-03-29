package org.broadinstitute.consent.http.models;

public record Publication(String title, String pubmedId, String date, String authors,
                          String bibliographicCitation,
                          String datasetCitation, Boolean citation) {

}
