package org.broadinstitute.consent.http.db.mapper;

import org.broadinstitute.consent.http.models.DarCollectionSummary;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.Vote;

import java.lang.Integer;
import java.util.Objects;
import org.jdbi.v3.core.result.RowView;
import org.jdbi.v3.core.result.LinkedHashMapRowReducer;

import java.util.Map;

public class DarCollectionSummaryReducer implements LinkedHashMapRowReducer<Integer, DarCollectionSummary>, RowMapperHelper {

  @Override
  public void accumulate(Map<Integer, DarCollectionSummary> map, RowView rowView) {
    
    DarCollectionSummary summary = map.computeIfAbsent(
      rowView.getColumn("dar_collection_id", Integer.class),
      id -> rowView.getRow(DarCollectionSummary.class)
    );
    try {
      Integer electionId = rowView.getColumn("electionid", Integer.class);
      if(Objects.isNull(summary.findElection(electionId))) {
        Election election = rowView.getRow(Election.class);
        if(Objects.nonNull(election)) {
          summary.addElection(election);
          summary.addDatasetId(election.getDataSetId());
        }
      }

      Vote vote = rowView.getRow(Vote.class);
      if(Objects.nonNull(vote)) {
        summary.addVote(vote);
        if(Objects.nonNull(vote.getVote())) {
          summary.setHasVoted(true);
        }
      }

      Integer datasetId = rowView.getColumn("dd_datasetid", Integer.class);
      if(Objects.nonNull(datasetId)) {
        summary.addDatasetId(datasetId);
      }

    } catch(Exception e) {
      //Don't handle exceptions (for now)
    }


  }
}