package org.broadinstitute.consent.http.db.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
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
    setStringFieldValue(resultSet, "institution_name", institution::setName);
    setStringFieldValue(resultSet, "it_director_name", institution::setItDirectorName);
    setStringFieldValue(resultSet, "it_director_email", institution::setItDirectorEmail);
    setStringFieldValue(resultSet, "institution_url", institution::setInstitutionUrl);
    setStringFieldValue(resultSet, "org_chart_url", institution::setOrgChartUrl);
    setStringFieldValue(resultSet, "verification_url", institution::setVerificationUrl);
    setStringFieldValue(resultSet, "verification_filename", institution::setVerificationFilename);
    setNonZeroFieldValue(resultSet, "create_user", institution::setCreateUserId);
    setNonZeroFieldValue(resultSet, "update_user", institution::setUpdateUserId);
    setDateFieldValue(resultSet, "create_date", institution::setCreateDate);
    setDateFieldValue(resultSet, "update_date", institution::setUpdateDate);
    if (hasColumn(resultSet, "duns_number")) {
      institution.setDunsNumber(resultSet.getInt("duns_number"));
    }
    if (hasColumn(resultSet, "organization_type")) {
      OrganizationType type = OrganizationType.getOrganizationTypeFromString(
          resultSet.getString("organization_type"));
      institution.setOrganizationType(type);
    }
    if (hasColumn(resultSet, "domain")) {
      String domain = resultSet.getString("domain");
      if (!StringUtils.isBlank(domain)) {
        institution.addDomain(domain);
      }
    }
    institutionMap.put(institution.getId(), institution);
    return institution;
  }
}
