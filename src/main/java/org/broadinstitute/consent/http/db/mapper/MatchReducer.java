package org.broadinstitute.consent.http.db.mapper;

import org.broadinstitute.consent.http.models.Match;
import org.jdbi.v3.core.result.LinkedHashMapRowReducer;
import org.jdbi.v3.core.result.RowView;

import java.util.Date;
import java.util.Map;
import java.util.Objects;

public class MatchReducer implements LinkedHashMapRowReducer<Integer, Match>, RowMapperHelper {

    @Override
    public void accumulate(Map<Integer, Match> map, RowView rowView) {
        Match match = map.computeIfAbsent(rowView.getColumn("matchid", Integer.class), id -> rowView.getRow(Match.class));
        if (hasColumn(rowView, "consent",String.class)) {
            match.setConsent(rowView.getColumn("consent", String.class));
        }
        if (hasColumn(rowView, "purpose",String.class)) {
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
        if (hasColumn(rowView, "failure_reason", String.class)) {
            String failure = rowView.getColumn("failure_reason", String.class);
            if (Objects.nonNull(failure) && !failure.isBlank()) {
                match.addFailureReason(rowView.getColumn("failure_reason", String.class));
            }
        }
    }

}
