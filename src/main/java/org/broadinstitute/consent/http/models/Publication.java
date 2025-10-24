package org.broadinstitute.consent.http.models;

import java.util.List;

public record Publication(String title,
                          String pubmedId,
                          String publishedDate,
                          List<Author> authors,
                          String bibliographicCitation,
                          String datasetCitation,
                          Boolean citation,
                          String publicationId,
                          String studyId,
                          String journal,
                          String doi,
                          String url,
                          String access,
                          List<String> tags
) {

}
