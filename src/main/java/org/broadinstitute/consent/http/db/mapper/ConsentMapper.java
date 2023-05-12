package org.broadinstitute.consent.http.db.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.DataUse;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

public class ConsentMapper implements RowMapper<Consent> {

  private Map<String, Consent> consentMap = new HashMap<>();

  public Consent map(ResultSet r, StatementContext ctx) throws SQLException {
    Consent consent;
    if (!consentMap.containsKey(r.getString("consent_id"))) {
      consent = new Consent();
      consent.setConsentId(r.getString("consent_id"));
    } else {
      consent = consentMap.get(r.getString("consent_id"));
    }
    consent.setRequiresManualReview(r.getBoolean("requires_manual_review"));
    consent.setDataUseLetter(r.getString("data_use_letter"));
    consent.setDulName(r.getString("dul_name"));
    consent.setDataUse(DataUse.parseDataUse(r.getString("data_use")).orElse(null));
    consent.setName(r.getString("name"));
    consent.setCreateDate(r.getTimestamp("create_date"));
    consent.setSortDate(r.getTimestamp("sort_date"));
    consent.setLastUpdate(r.getTimestamp("last_update"));
    consent.setTranslatedUseRestriction(r.getString("translated_use_restriction"));
    consent.setGroupName(r.getString("group_name"));
    consent.setUpdated(r.getBoolean("updated"));
    consentMap.put(consent.getConsentId(), consent);
    return consent;
  }
}
