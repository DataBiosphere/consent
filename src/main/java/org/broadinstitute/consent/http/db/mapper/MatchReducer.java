package org.broadinstitute.consent.http.db.mapper;

import java.util.Date;
import java.util.Map;
import org.broadinstitute.consent.http.models.Match;
import org.jdbi.v3.core.result.LinkedHashMapRowReducer;
import org.jdbi.v3.core.result.RowView;

public class MatchReducer implements LinkedHashMapRowReducer<Integer, Match>, RowMapperHelper {

  @Override
  public void accumulate(Map<Integer, Match> map, RowView rowView) {
    Match match = map.computeIfAbsent(rowView.getColumn("match_id", Integer.class),
        id -> rowView.getRow(Match.class));
    hasOptionalColumn(rowView, "consent", String.class).ifPresent(match::setConsent);
    hasOptionalColumn(rowView, "purpose", String.class).ifPresent(match::setPurpose);
    hasOptionalColumn(rowView, "algorithm_version", String.class).ifPresent(match::setAlgorithmVersion);
    hasOptionalColumn(rowView, "match_entity", Boolean.class).ifPresent(match::setMatch);
    hasOptionalColumn(rowView, "abstain", Boolean.class).ifPresent(match::setAbstain);
    hasOptionalColumn(rowView, "failed", Boolean.class).ifPresent(match::setFailed);
    hasOptionalColumn(rowView, "create_date", Date.class).ifPresent(match::setCreateDate);
    hasOptionalColumn(rowView, "rationale", String.class).ifPresent(match::addRationale);
  }
}
