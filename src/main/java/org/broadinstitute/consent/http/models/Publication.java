package org.broadinstitute.consent.http.models;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

public record Publication(
    String title,
    String pubmedId,
    LocalDate publishedDate,
    List<Author> authors,
    String bibliographicCitation,
    String datasetCitation,
    Boolean citation,
    String publicationId,
    String studyId,
    String journal,
    String doi,
    URI url,
    String access,
    List<String> tags) {}
