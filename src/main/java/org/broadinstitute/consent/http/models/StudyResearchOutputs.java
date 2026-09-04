package org.broadinstitute.consent.http.models;

import java.util.List;

public record StudyResearchOutputs(
    List<Presentation> presentations,
    List<Publication> publications,
    List<IntellectualProperty> intellectualProperties) {}
