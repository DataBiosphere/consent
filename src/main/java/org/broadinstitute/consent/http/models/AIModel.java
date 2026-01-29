package org.broadinstitute.consent.http.models;

import java.net.URI;
import java.util.List;

public record AIModel(
    String modelId,
    String studyId,
    String name,
    String description,
    URI url,
    String format,
    String license,
    List<String> trainedOnDatasets,
    Contact maintainer,
    List<String> tags) {}
