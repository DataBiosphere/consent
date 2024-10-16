package org.broadinstitute.consent.http.db.mapper;

import java.lang.reflect.InvocationTargetException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.UUID;
import org.broadinstitute.consent.http.exceptions.NoMatchingClassException;
import org.broadinstitute.consent.http.models.DraftInterface;
import org.broadinstitute.consent.http.models.User;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

public class DraftSubmissionInterfaceMapper implements RowMapper<DraftInterface>, RowMapperHelper {

  @Override
  public DraftInterface map(ResultSet rs, StatementContext ctx) throws SQLException, NoMatchingClassException {
    if (!hasColumn(rs, "schema_class")) {
      throw new NoMatchingClassException("Missing class name.");
    }

    DraftInterface dsi;
    String className = rs.getString("schema_class");

    try {
      dsi = (DraftInterface) Class.forName(className).getDeclaredConstructor().newInstance();
    } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException |
             InstantiationException | IllegalAccessException classNotFoundException) {
      throw new NoMatchingClassException(className);
    }

    if (hasColumn(rs, "name")) {
      dsi.setName(rs.getString("name"));
    }

    if (hasColumn(rs, "uuid")) {
      dsi.setUUID(UUID.fromString(rs.getString("uuid")));
    }

    if (hasColumn(rs, "create_date")) {
      dsi.setCreateDate(rs.getTimestamp("create_date"));
    }

    if (hasColumn(rs, "update_date")) {
      dsi.setUpdateDate(rs.getTimestamp("update_date"));
    }

    if (hasColumn(rs, "json")) {
      dsi.setJson(rs.getString("json"));
    }

    if (hasColumn(rs, "uu_user_id")) {
      User updateUser = buildUserFromResult(rs.getInt("uu_user_id"),
          rs.getString("uu_email"),
          rs.getString("uu_display_name"),
          rs.getTimestamp("uu_create_date"),
          rs.getBoolean("uu_email_preference"),
          rs.getInt("uu_institution_id"),
          rs.getString("uu_era_commons_id"));
      dsi.setUpdateUser(updateUser);
    }

    if (hasColumn(rs, "cu_user_id")) {
      User createUser = buildUserFromResult(rs.getInt("cu_user_id"),
          rs.getString("cu_email"),
          rs.getString("cu_display_name"),
          new Date(rs.getTimestamp("cu_create_date").getTime()),
          rs.getBoolean("cu_email_preference"),
          rs.getInt("cu_institution_id"),
          rs.getString("cu_era_commons_id"));
      dsi.setCreateUser(createUser);
    }

    return dsi;
  }

  private User buildUserFromResult(Integer userId, String email, String displayName,
      Date createDate, boolean emailPreference, Integer institutionId, String eraCommonsId) {
    User user = new User();
    user.setUserId(userId);
    user.setEmail(email);
    user.setDisplayName(displayName);
    user.setCreateDate(createDate);
    user.setEmailPreference(emailPreference);
    return user;
  }
}
