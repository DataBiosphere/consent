package org.broadinstitute.consent.http.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.broadinstitute.consent.http.enumeration.BlogCategory;
import org.broadinstitute.consent.http.enumeration.BlogStatus;
import org.broadinstitute.consent.http.models.BlogPost;
import org.broadinstitute.consent.http.models.BlogPostFields;
import org.broadinstitute.consent.http.models.User;
import org.jdbi.v3.core.statement.UnableToExecuteStatementException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BlogPostDAOTest extends DAOTestHelper {

  private static final Instant PAST = Instant.now().minus(1, ChronoUnit.DAYS);

  private BlogPostDAO blogPostDAO;
  private User admin;

  @BeforeEach
  void setUpDao() {
    blogPostDAO = jdbi.onDemand(BlogPostDAO.class);
    admin = createUser();
  }

  @Test
  void testInsertAndFindById() {
    BlogPostFields fields =
        new BlogPostFields(
            "getting-started",
            "Getting started",
            "An excerpt",
            "# Heading",
            BlogCategory.USER_GUIDE,
            List.of("dac", "help"),
            BlogStatus.DRAFT,
            null,
            true,
            "A. Author",
            "help",
            "https://example.org/help/",
            admin.getUserId());

    Integer id = blogPostDAO.insert(fields, admin.getUserId());

    BlogPost post = blogPostDAO.findById(id);
    assertEquals(id, post.blogPostId());
    assertEquals("getting-started", post.slug());
    assertEquals("Getting started", post.title());
    assertEquals("An excerpt", post.excerpt());
    assertEquals("# Heading", post.contentMd());
    assertEquals(BlogCategory.USER_GUIDE, post.category());
    assertEquals(List.of("dac", "help"), post.tags());
    assertEquals(BlogStatus.DRAFT, post.status());
    assertNull(post.publishDate());
    assertTrue(post.featured());
    assertEquals("A. Author", post.authorDisplayName());
    assertEquals("help", post.parentSlug());
    assertEquals("https://example.org/help/", post.legacyUrl());
    assertEquals(admin.getUserId(), post.authorUserId());
    assertEquals(admin.getUserId(), post.createUserId());
    assertEquals(admin.getUserId(), post.updateUserId());
    assertNotNull(post.createDate());
    assertNotNull(post.updateDate());
  }

  @Test
  void testUpdateRequiresTheLoadedVersion() {
    Integer id = insert("a-post", BlogCategory.BLOG_POST, BlogStatus.DRAFT, null);
    int loadedVersion = blogPostDAO.findById(id).version();

    BlogPostFields edit = fields("a-post", BlogCategory.BLOG_POST, BlogStatus.PUBLISHED, PAST);
    assertEquals(1, blogPostDAO.update(id, edit, admin.getUserId(), loadedVersion));
    BlogPostFields staleEdit = fields("a-post", BlogCategory.BLOG_POST, BlogStatus.ARCHIVED, null);
    assertEquals(0, blogPostDAO.update(id, staleEdit, admin.getUserId(), loadedVersion));

    BlogPost updated = blogPostDAO.findById(id);
    assertEquals(BlogStatus.PUBLISHED, updated.status());
    assertEquals(PAST.truncatedTo(ChronoUnit.MICROS), updated.publishDate());
    assertEquals(loadedVersion + 1, updated.version());
  }

  @Test
  void testNullTagsAreStoredAsEmpty() {
    BlogPostFields fields =
        new BlogPostFields(
            "no-tags",
            "Title",
            null,
            "Body",
            BlogCategory.PAGE,
            null,
            BlogStatus.DRAFT,
            null,
            false,
            null,
            null,
            null,
            null);

    BlogPost post = blogPostDAO.findById(blogPostDAO.insert(fields, admin.getUserId()));

    assertEquals(List.of(), post.tags());
  }

  @Test
  void testSlugIsUniquePerCategoryAmongActivePosts() {
    Integer id = insert("same-slug", BlogCategory.BLOG_POST, BlogStatus.DRAFT, null);
    insert("same-slug", BlogCategory.USER_GUIDE, BlogStatus.DRAFT, null);

    assertThrows(
        UnableToExecuteStatementException.class,
        () -> insert("same-slug", BlogCategory.BLOG_POST, BlogStatus.DRAFT, null));

    assertEquals(1, blogPostDAO.softDelete(id, admin.getUserId()));
    assertNotNull(insert("same-slug", BlogCategory.BLOG_POST, BlogStatus.DRAFT, null));
  }

  @Test
  void testSlugFormatIsEnforced() {
    assertThrows(
        UnableToExecuteStatementException.class,
        () -> insert("Not A Slug", BlogCategory.PAGE, BlogStatus.DRAFT, null));
  }

  @Test
  void testSoftDeleteHidesThePost() {
    Integer id = insert("gone", BlogCategory.PAGE, BlogStatus.PUBLISHED, PAST);

    assertEquals(1, blogPostDAO.softDelete(id, admin.getUserId()));

    assertNull(blogPostDAO.findById(id));
    assertTrue(blogPostDAO.findAll(null, null).isEmpty());
    BlogPostFields edit = fields("gone", BlogCategory.PAGE, BlogStatus.DRAFT, null);
    assertEquals(0, blogPostDAO.update(id, edit, admin.getUserId(), 0));
    assertEquals(0, blogPostDAO.softDelete(id, admin.getUserId()));
  }

  @Test
  void testFindAllFiltersByCategoryAndStatus() {
    insert("draft-post", BlogCategory.BLOG_POST, BlogStatus.DRAFT, null);
    insert("live-post", BlogCategory.BLOG_POST, BlogStatus.PUBLISHED, PAST);
    insert("live-page", BlogCategory.PAGE, BlogStatus.PUBLISHED, PAST);

    assertEquals(3, blogPostDAO.findAll(null, null).size());
    assertEquals(2, blogPostDAO.findAll(BlogCategory.BLOG_POST, null).size());
    List<BlogPost> published = blogPostDAO.findAll(BlogCategory.BLOG_POST, BlogStatus.PUBLISHED);
    assertEquals(List.of("live-post"), published.stream().map(BlogPost::slug).toList());
  }

  private Integer insert(
      String slug, BlogCategory category, BlogStatus status, Instant publishDate) {
    return blogPostDAO.insert(fields(slug, category, status, publishDate), admin.getUserId());
  }

  private BlogPostFields fields(
      String slug, BlogCategory category, BlogStatus status, Instant publishDate) {
    return new BlogPostFields(
        slug,
        "Title " + slug,
        null,
        "Body",
        category,
        List.of(),
        status,
        publishDate,
        false,
        null,
        null,
        null,
        admin.getUserId());
  }
}
