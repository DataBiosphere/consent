package org.broadinstitute.consent.http.db.mapper;

import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.DarCollectionSummary;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.Vote;

import java.lang.Integer;
import java.util.Objects;
import org.jdbi.v3.core.result.RowView;
import org.postgresql.util.PGobject;
import org.jdbi.v3.core.result.LinkedHashMapRowReducer;

import java.util.Map;

public class DarCollectionSummaryReducer implements LinkedHashMapRowReducer<Integer, DarCollectionSummary>, RowMapperHelper {

  @Override
  public void accumulate(Map<Integer, DarCollectionSummary> map, RowView rowView) {
    
    DarCollectionSummary summary = map.computeIfAbsent(
      rowView.getColumn("collection_id", Integer.class),
      id -> rowView.getRow(DarCollectionSummary.class)
    );
    try {
      String darDataString = rowView.getColumn("dar_data", PGobject.class).getValue();
      DataAccessRequestData data = translate(darDataString);
      summary.setName(data.getProjectTitle());

      Election election = rowView.getRow(Election.class);
      if(Objects.nonNull(election)) {
        summary.addElection(election);
      }

      Vote vote = rowView.getRow(Vote.class);
      if(Objects.nonNull(vote)) {
        summary.addVote(vote);
        if(Objects.nonNull(vote.getVote())) {
          summary.setHasVoted(true);
        }
      }
    } catch(Exception e) {
      //ignore any exceptions
    }


  }
}