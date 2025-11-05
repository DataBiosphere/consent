package org.broadinstitute.consent.http.db.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.broadinstitute.consent.http.enumeration.OrganizationType;
import org.broadinstitute.consent.http.models.Institution;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

public class InstitutionMapper implements RowMapper<Institution>, RowMapperHelper {

  @Override
  public Institution map(ResultSet resultSet, StatementContext statementContext)
      throws SQLException {
    Institution institution;
    int institutionId = resultSet.getInt("institution_id");
    institution = new Institution();
    institution.setId(institutionId);
    setStringFieldValue(resultSet, "institution_name", institution::setName);
    setStringFieldValue(resultSet, "it_director_name", institution::setItDirectorName);
    setStringFieldValue(resultSet, "it_director_email", institution::setItDirectorEmail);
    setStringFieldValue(resultSet, "institution_url", institution::setInstitutionUrl);
    setStringFieldValue(resultSet, "org_chart_url", institution::setOrgChartUrl);
    setStringFieldValue(resultSet, "verification_url", institution::setVerificationUrl);
    setStringFieldValue(resultSet, "verification_filename", institution::setVerificationFilename);
    setNonZeroFieldValue(resultSet, "duns_number", institution::setDunsNumber);
    setNonZeroFieldValue(resultSet, "create_user", institution::setCreateUserId);
    setNonZeroFieldValue(resultSet, "update_user", institution::setUpdateUserId);
    setDateFieldValue(resultSet, "create_date", institution::setCreateDate);
    setDateFieldValue(resultSet, "update_date", institution::setUpdateDate);
    if (hasColumn(resultSet, "organization_type")) {
      OrganizationType type =
          OrganizationType.getOrganizationTypeFromString(resultSet.getString("organization_type"));
      institution.setOrganizationType(type);
    }
    return institution;
  }
}
