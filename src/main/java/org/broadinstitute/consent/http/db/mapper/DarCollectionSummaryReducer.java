package org.broadinstitute.consent.http.db.mapper;

import java.util.Map;
import java.util.Objects;
import org.broadinstitute.consent.http.models.DarCollectionSummary;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.Vote;
import org.jdbi.v3.core.mapper.MappingException;
import org.jdbi.v3.core.mapper.NoSuchMapperException;
import org.jdbi.v3.core.result.LinkedHashMapRowReducer;
import org.jdbi.v3.core.result.RowView;

public class DarCollectionSummaryReducer implements
    LinkedHashMapRowReducer<Integer, DarCollectionSummary>, RowMapperHelper {

  @Override
  public void accumulate(Map<Integer, DarCollectionSummary> map, RowView rowView) {

    DarCollectionSummary summary = map.computeIfAbsent(
        rowView.getColumn("dar_collection_id", Integer.class),
        id -> rowView.getRow(DarCollectionSummary.class)
    );
    Election election;
    Vote vote;
    Integer datasetId;
    String darStatus;
    String darReferenceId;

    try {
      datasetId = rowView.getColumn("dd_datasetid", Integer.class);
      if (Objects.nonNull(datasetId)) {
        summary.addDatasetId(datasetId);
      }

      if (hasColumn(rowView, "dac_name", String.class)) {
        if (Objects.nonNull(rowView.getColumn("dac_name", String.class))) {
          summary.addDacName(rowView.getColumn("dac_name", String.class));
        }
      }

      try {
        darReferenceId = rowView.getColumn("dar_reference_id", String.class);
        if (Objects.nonNull(darReferenceId)) {
          summary.addReferenceId(darReferenceId);
        }

        darStatus = rowView.getColumn("dar_status", String.class);
        if (Objects.nonNull(darStatus)) {
          summary.addStatus(darStatus, darReferenceId);
        }
      } catch (MappingException e) {
        //ignore exception, it means dar_status and dar_reference_id wasn't included for this query
      }

      try {
        election = rowView.getRow(Election.class);
        if (Objects.nonNull(election.getElectionId())) {
          summary.addElection(election);
          summary.addDatasetId(election.getDatasetId());
        }
      } catch (MappingException e) {
        // Indicates that we do not have an election for this summary
      }

      try {
        vote = rowView.getRow(Vote.class);
        if (Objects.nonNull(vote.getVoteId())) {
          summary.addVote(vote);
        }
      } catch (MappingException e) {
        // Indicates that we do not have an election for this summary
      }

    } catch (NoSuchMapperException e) {
      //ignore these exceptions, just means there's no elections and votes on the collection for this query
    }
  }
}