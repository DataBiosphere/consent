package org.broadinstitute.consent.http.db.mapper;

import org.broadinstitute.consent.http.models.LibraryCard;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class LibraryCardMapper implements RowMapper<LibraryCard>, RowMapperHelper {

  private final Map<Integer, LibraryCard>  libraryCardMap = new HashMap<>();


  @Override
  public LibraryCard map(ResultSet resultSet, StatementContext statementContext) throws SQLException {

    LibraryCard libraryCard;
    int libraryCardId = resultSet.getInt("id");

    if (libraryCardMap.containsKey(libraryCardId)) {
      libraryCard = libraryCardMap.get(libraryCardId);
    } else {
      libraryCard = new LibraryCard();
      libraryCard.setId(libraryCardId);
    }
    if (hasColumn(resultSet, "user_id")) {
      libraryCard.setUserId(resultSet.getInt("user_id"));
    }
    if (hasColumn(resultSet, "institution_id")) {
      libraryCard.setInstitutionId(resultSet.getInt("institution_id"));
    }
    if (hasColumn(resultSet, "era_commons_id")) {
      libraryCard.setEraCommonsId(resultSet.getString("era_commons_id"));
    }
    if (hasColumn(resultSet, "user_name")) {
      libraryCard.setName(resultSet.getString("user_name"));
    }
    if (hasColumn(resultSet, "user_email")) {
      libraryCard.setEmail(resultSet.getString("user_email"));
    }
    if (hasColumn(resultSet, "create_user_id")) {
      libraryCard.setCreateUser(resultSet.getInt("create_user"));
    }
    if (hasColumn(resultSet, "create_date")) {
      libraryCard.setCreateDate(resultSet.getDate("create_date"));
    } 
    if (hasColumn(resultSet, "update_user_id")) {
      libraryCard.setUpdateUser(resultSet.getInt("update_user"));
    }
    if (hasColumn(resultSet, "update_date")) {
      libraryCard.setUpdateDate(resultSet.getDate("update_date"));
    }
    libraryCardMap.put(libraryCard.getId(), libraryCard);
    return libraryCard;
  }
}