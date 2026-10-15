package org.broadinstitute.consent.http.models;

import java.time.Instant;
import java.util.List;
import org.broadinstitute.consent.http.enumeration.BlogCategory;
import org.broadinstitute.consent.http.enumeration.BlogStatus;

/** The fields an admin writes when creating or updating a blog post. */
public record BlogPostFields(
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
    Integer authorUserId) {

  public BlogPostFields {
    tags = tags == null ? List.of() : List.copyOf(tags);
  }
}
