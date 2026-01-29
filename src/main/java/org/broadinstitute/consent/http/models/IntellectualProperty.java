package org.broadinstitute.consent.http.models;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

public record IntellectualProperty(
    String type,
    String title,
    String assignee,
    String patentNumber,
    LocalDate filingDate,
    String status,
    URI url,
    String contact,
    String ipId,
    String studyId,
    List<String> tags) {}
