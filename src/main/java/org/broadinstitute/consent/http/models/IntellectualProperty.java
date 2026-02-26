package org.broadinstitute.consent.http.models;

import java.util.List;

public record IntellectualProperty(
    String type,
    String title,
    String assignee,
    String patentNumber,
    String filingDate,
    String status,
    String url,
    String contact,
    String ipId,
    String studyId,
    List<String> tags) {}
