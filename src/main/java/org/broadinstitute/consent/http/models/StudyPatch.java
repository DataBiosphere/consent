package org.broadinstitute.consent.http.models;

import java.util.List;

public record StudyPatch(
    String name,
    String description,
    List<String> dataTypes,
    String piName,
    Boolean publicVisibility) {}
