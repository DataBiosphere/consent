package org.broadinstitute.consent.http.db.mapper;

import org.broadinstitute.consent.http.models.DarCollectionSummary;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.Vote;

import java.lang.Integer;
import java.util.Objects;
import org.jdbi.v3.core.result.RowView;
import org.jdbi.v3.core.mapper.MappingException;
import org.jdbi.v3.core.mapper.NoSuchMapperException;
import org.jdbi.v3.core.result.LinkedHashMapRowReducer;

import java.util.Map;

public class DarCollectionSummaryReducer implements LinkedHashMapRowReducer<Integer, DarCollectionSummary>, RowMapperHelper {

  @Override
  public void accumulate(Map<Integer, DarCollectionSummary> map, RowView rowView) {
    
    DarCollectionSummary summary = map.computeIfAbsent(
      rowView.getColumn("dar_collection_id", Integer.class),
      id -> rowView.getRow(DarCollectionSummary.class)
    );
    Election election;
    Vote vote;
    Integer datasetId;
    try {

      datasetId = rowView.getColumn("dd_datasetid", Integer.class);
      if (Objects.nonNull(datasetId)) {
        summary.addDatasetId(datasetId);
      }

      election = rowView.getRow(Election.class);
      if(Objects.nonNull(election.getElectionId())) {
        summary.addElection(election);
        summary.addDatasetId(election.getDataSetId());
      }

      vote = rowView.getRow(Vote.class);
      if(Objects.nonNull(vote.getVoteId())) {
        summary.addVote(vote);
      }

    } catch(NoSuchMapperException e) {
      //ignore these exceptions, just means there's no elections and votes on the collection for this query
    }
  }
}