package org.broadinstitute.consent.http.models;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

public record Presentation(
    String title,
    URI url,
    LocalDate date,
    String authors,
    String datasetCitation,
    Boolean citation,
    String presentationId,
    String studyId,
    Contact presenter,
    String event,
    String location,
    String format,
    String access,
    List<String> tags) {}
