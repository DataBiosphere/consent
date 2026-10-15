package org.broadinstitute.consent.http.models;

import java.time.Instant;
import java.util.List;
import org.broadinstitute.consent.http.enumeration.BlogCategory;
import org.broadinstitute.consent.http.enumeration.BlogStatus;

public record BlogPost(
    Integer blogPostId,
    String slug,
    String title,
    String excerpt,
    String contentMd,
    BlogCategory category,
    List<String> tags,
    BlogStatus status,
    Instant publishDate,
    boolean featured,
    String authorDisplayName,
    String parentSlug,
    String legacyUrl,
    Integer authorUserId,
    Instant createDate,
    Integer createUserId,
    Instant updateDate,
    Integer updateUserId,
    int version) {}
