package org.broadinstitute.consent.http.models;

import java.util.List;

public record Presentation(String title,
                           String url,
                           String date,
                           String authors,
                           String datasetCitation,
                           Boolean citation,
                           String presentationId,
                           String studyId,
                           Presenter presenter,
                           String event,
                           String location,
                           String format,
                           String access,
                           List<String> tags) {

}