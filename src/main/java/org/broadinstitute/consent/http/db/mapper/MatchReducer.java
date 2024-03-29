package org.broadinstitute.consent.http.db.mapper;

import java.util.Date;
import java.util.Map;
import java.util.Objects;
import org.broadinstitute.consent.http.models.Match;
import org.jdbi.v3.core.result.LinkedHashMapRowReducer;
import org.jdbi.v3.core.result.RowView;

public class MatchReducer implements LinkedHashMapRowReducer<Integer, Match>, RowMapperHelper {

  @Override
  public void accumulate(Map<Integer, Match> map, RowView rowView) {
    Match match = map.computeIfAbsent(rowView.getColumn("matchid", Integer.class),
        id -> rowView.getRow(Match.class));
    if (hasColumn(rowView, "consent", String.class)) {
      match.setConsent(rowView.getColumn("consent", String.class));
    }
    if (hasColumn(rowView, "purpose", String.class)) {
      match.setPurpose(rowView.getColumn("purpose", String.class));
    }
    if (hasColumn(rowView, "algorithm_version", String.class)) {
      match.setAlgorithmVersion(rowView.getColumn("algorithm_version", String.class));
    }
    if (hasColumn(rowView, "matchentity", Boolean.class)) {
      match.setMatch(rowView.getColumn("matchentity", Boolean.class));
    }
    if (hasColumn(rowView, "abstain", Boolean.class)) {
      match.setAbstain(rowView.getColumn("abstain", Boolean.class));
    }
    if (hasColumn(rowView, "failed", Boolean.class)) {
      match.setFailed(rowView.getColumn("failed", Boolean.class));
    }
    if (hasColumn(rowView, "createdate", Date.class)) {
      match.setCreateDate(rowView.getColumn("createdate", Date.class));
    }
    if (hasColumn(rowView, "rationale", String.class)) {
      String rationale = rowView.getColumn("rationale", String.class);
      if (Objects.nonNull(rationale) && !rationale.isBlank()) {
        match.addRationale(rowView.getColumn("rationale", String.class));
      }
    }
  }

}
