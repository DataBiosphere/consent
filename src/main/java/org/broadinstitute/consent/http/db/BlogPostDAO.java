package org.broadinstitute.consent.http.db;

import java.util.List;
import org.broadinstitute.consent.http.db.mapper.BlogPostMapper;
import org.broadinstitute.consent.http.enumeration.BlogCategory;
import org.broadinstitute.consent.http.enumeration.BlogStatus;
import org.broadinstitute.consent.http.models.BlogPost;
import org.broadinstitute.consent.http.models.BlogPostFields;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindMethods;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

@RegisterRowMapper(BlogPostMapper.class)
public interface BlogPostDAO {

  @SqlQuery("SELECT * FROM blog_post WHERE blog_post_id = :blogPostId AND deleted = false")
  BlogPost findById(@Bind("blogPostId") Integer blogPostId);

  /** Every post an admin can manage, including drafts and scheduled posts. */
  @SqlQuery(
      """
      SELECT * FROM blog_post
      WHERE deleted = false
        AND (CAST(:category AS text) IS NULL OR category = :category)
        AND (CAST(:status AS text) IS NULL OR status = :status)
      ORDER BY update_date DESC, blog_post_id DESC
      """)
  List<BlogPost> findAll(
      @Bind("category") BlogCategory category, @Bind("status") BlogStatus status);

  @SqlUpdate(
      """
      INSERT INTO blog_post
        (slug, title, excerpt, content_md, category, tags, status, publish_date, featured,
         author_display_name, parent_slug, legacy_url, author_user_id,
         create_user_id, update_user_id)
      VALUES
        (:f.slug, :f.title, :f.excerpt, :f.contentMd, :f.category, CAST(:f.tags AS text[]),
         :f.status, :f.publishDate, :f.featured, :f.authorDisplayName, :f.parentSlug,
         :f.legacyUrl, :f.authorUserId, :userId, :userId)
      """)
  @GetGeneratedKeys("blog_post_id")
  Integer insert(@BindMethods("f") BlogPostFields fields, @Bind("userId") Integer userId);

  /**
   * Updates the post only if it is unchanged since the caller loaded it.
   *
   * @return 0 when the post is missing, deleted, or no longer at {@code loadedVersion}
   */
  @SqlUpdate(
      """
      UPDATE blog_post SET
        slug = :f.slug, title = :f.title, excerpt = :f.excerpt, content_md = :f.contentMd,
        category = :f.category, tags = CAST(:f.tags AS text[]), status = :f.status,
        publish_date = :f.publishDate, featured = :f.featured,
        author_display_name = :f.authorDisplayName, parent_slug = :f.parentSlug,
        legacy_url = :f.legacyUrl, author_user_id = :f.authorUserId,
        update_date = now(), update_user_id = :userId, version = version + 1
      WHERE blog_post_id = :blogPostId AND deleted = false AND version = :loadedVersion
      """)
  int update(
      @Bind("blogPostId") Integer blogPostId,
      @BindMethods("f") BlogPostFields fields,
      @Bind("userId") Integer userId,
      @Bind("loadedVersion") int loadedVersion);

  @SqlUpdate(
      """
      UPDATE blog_post SET deleted = true, delete_date = now(), delete_user_id = :userId
      WHERE blog_post_id = :blogPostId AND deleted = false
      """)
  int softDelete(@Bind("blogPostId") Integer blogPostId, @Bind("userId") Integer userId);
}
