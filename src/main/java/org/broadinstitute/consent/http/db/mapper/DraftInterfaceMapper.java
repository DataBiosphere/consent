package org.broadinstitute.consent.http.db.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.UUID;
import org.broadinstitute.consent.http.enumeration.DraftType;
import org.broadinstitute.consent.http.exceptions.UnknownDraftTypeException;
import org.broadinstitute.consent.http.models.DraftBuilder;
import org.broadinstitute.consent.http.models.DraftInterface;
import org.broadinstitute.consent.http.models.User;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;


public class DraftInterfaceMapper implements RowMapper<DraftInterface>, RowMapperHelper {

  @Override
  public DraftInterface map(ResultSet rs, StatementContext ctx) throws SQLException, UnknownDraftTypeException {
    DraftInterface dsi = getDraftImplByType(rs);

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
          rs.getBoolean("uu_email_preference"));
      dsi.setUpdateUser(updateUser);
    }

    if (hasColumn(rs, "cu_user_id")) {
      User createUser = buildUserFromResult(rs.getInt("cu_user_id"),
          rs.getString("cu_email"),
          rs.getString("cu_display_name"),
          new Date(rs.getTimestamp("cu_create_date").getTime()),
          rs.getBoolean("cu_email_preference"));
      dsi.setCreateUser(createUser);
    }

    return dsi;
  }

  private User buildUserFromResult(Integer userId, String email, String displayName,
      Date createDate, boolean emailPreference) {
    User user = new User();
    user.setUserId(userId);
    user.setEmail(email);
    user.setDisplayName(displayName);
    user.setCreateDate(createDate);
    user.setEmailPreference(emailPreference);
    return user;
  }

  private DraftInterface getDraftImplByType(ResultSet rs) throws SQLException, UnknownDraftTypeException {
    try {
      String type = rs.getString("draft_type");
      DraftType draftType = DraftType.fromValue(
          type);
      return DraftBuilder.from(draftType);
    } catch (NullPointerException ex) {
      throw new UnknownDraftTypeException("Draft type was not found.");
    }
  }
}
