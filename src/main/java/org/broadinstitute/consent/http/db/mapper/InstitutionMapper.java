package org.broadinstitute.consent.http.db.mapper;

import org.broadinstitute.consent.http.models.Institution;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class InstitutionMapper implements RowMapper<Institution>, RowMapperHelper {

  private final Map<Integer, Institution>  institutionMap = new HashMap<>();


  @Override
  public Institution map(ResultSet resultSet, StatementContext statementContext) throws SQLException {
    Institution institution;

    int institutionId = resultSet.getInt("institution_id"));
    if (institutionMap.containsKey(institutionId) {
      institution = institutionMap.get(institutionId);
    } else {
      institution = new Institution();
      institution.setId(institutionId);
    }
    institution.setName(resultSet.getString("institution_name"));
    institution.setItDirectorName(resultSet.getString("it_director_name"));
    institution.setItDirectorEmail(resultSet.getString("it_director_email"));
    institution.setCreateUser(resultSet.getInt("create_user"));
    institution.setCreateDate(resultSet.getDate("create_date"));
    institution.setUpdateUser(resultSet.getInt("update_user"));
    institution.setUpdateDate(resultSet.getDate("update_date"));
    institutionMap.put(institution.getId(), institution);
    return institution;
  }
}
