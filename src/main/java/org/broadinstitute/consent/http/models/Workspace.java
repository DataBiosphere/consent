package org.broadinstitute.consent.http.models;

import java.net.URI;
import java.util.List;

public record Workspace(
    String workspaceId,
    String studyId,
    String name,
    String platform,
    URI url,
    String description,
    List<String> tools,
    String access,
    List<String> tags) {}
