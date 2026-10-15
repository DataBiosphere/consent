package org.broadinstitute.consent.http.db.mapper;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.broadinstitute.consent.http.enumeration.BlogCategory;
import org.broadinstitute.consent.http.enumeration.BlogStatus;
import org.broadinstitute.consent.http.models.BlogPost;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

public class BlogPostMapper implements RowMapper<BlogPost> {

  @Override
  public BlogPost map(ResultSet rs, StatementContext ctx) throws SQLException {
    return new BlogPost(
        rs.getInt("blog_post_id"),
        rs.getString("slug"),
        rs.getString("title"),
        rs.getString("excerpt"),
        rs.getString("content_md"),
        BlogCategory.valueOf(rs.getString("category")),
        tags(rs.getArray("tags")),
        BlogStatus.valueOf(rs.getString("status")),
        toInstant(rs.getTimestamp("publish_date")),
        rs.getBoolean("featured"),
        rs.getString("author_display_name"),
        rs.getString("parent_slug"),
        rs.getString("legacy_url"),
        rs.getObject("author_user_id", Integer.class),
        toInstant(rs.getTimestamp("create_date")),
        rs.getInt("create_user_id"),
        toInstant(rs.getTimestamp("update_date")),
        rs.getInt("update_user_id"),
        rs.getInt("version"));
  }

  private static Instant toInstant(Timestamp timestamp) {
    return timestamp == null ? null : timestamp.toInstant();
  }

  private static List<String> tags(Array array) throws SQLException {
    if (array == null) {
      return List.of();
    }
    try {
      return List.of((String[]) array.getArray());
    } finally {
      array.free();
    }
  }
}
