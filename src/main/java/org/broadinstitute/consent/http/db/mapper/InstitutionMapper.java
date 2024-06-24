package org.broadinstitute.consent.http.db.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import org.broadinstitute.consent.http.enumeration.OrganizationType;
import org.broadinstitute.consent.http.models.Institution;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

public class InstitutionMapper implements RowMapper<Institution>, RowMapperHelper {

  private final Map<Integer, Institution> institutionMap = new HashMap<>();


  @Override
  public Institution map(ResultSet resultSet, StatementContext statementContext)
      throws SQLException {

    Institution institution;
    int institutionId = resultSet.getInt("institution_id");

    if (institutionMap.containsKey(institutionId)) {
      institution = institutionMap.get(institutionId);
    } else {
      institution = new Institution();
      institution.setId(institutionId);
    }
    if (hasColumn(resultSet, "institution_name")) {
      institution.setName(resultSet.getString("institution_name"));
    }
    if (hasColumn(resultSet, "it_director_name")) {
      institution.setItDirectorName(resultSet.getString("it_director_name"));
    }
    if (hasColumn(resultSet, "it_director_email")) {
      institution.setItDirectorEmail(resultSet.getString("it_director_email"));
    }
    if (hasColumn(resultSet, "institution_url")) {
      institution.setInstitutionUrl(resultSet.getString("institution_url"));
    }
    if (hasColumn(resultSet, "duns_number")) {
      institution.setDunsNumber(resultSet.getInt("duns_number"));
    }
    if (hasColumn(resultSet, "org_chart_url")) {
      institution.setOrgChartUrl(resultSet.getString("org_chart_url"));
    }
    if (hasColumn(resultSet, "verification_url")) {
      institution.setVerificationUrl(resultSet.getString("verification_url"));
    }
    if (hasColumn(resultSet, "verification_filename")) {
      institution.setVerificationFilename(resultSet.getString("verification_filename"));
    }
    if (hasColumn(resultSet, "organization_type")) {
      OrganizationType type = OrganizationType.getOrganizationTypeFromString(
          resultSet.getString("organization_type"));
      institution.setOrganizationType(type);
    }
    if (hasColumn(resultSet, "create_user") && resultSet.getInt("create_user") > 0) {
      institution.setCreateUserId(resultSet.getInt("create_user"));
    }
    if (hasColumn(resultSet, "create_date")) {
      institution.setCreateDate(resultSet.getDate("create_date"));
    }
    if (hasColumn(resultSet, "update_user") && resultSet.getInt("update_user") > 0) {
      institution.setUpdateUserId(resultSet.getInt("update_user"));
    }
    if (hasColumn(resultSet, "update_date")) {
      institution.setUpdateDate(resultSet.getDate("update_date"));
    }

    institutionMap.put(institution.getId(), institution);
    return institution;
  }

}
