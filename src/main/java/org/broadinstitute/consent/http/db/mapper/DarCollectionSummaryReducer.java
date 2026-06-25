package org.broadinstitute.consent.http.db.mapper;

import java.sql.Timestamp;
import java.util.Map;
import java.util.Objects;
import org.broadinstitute.consent.http.models.CloseoutSupplement;
import org.broadinstitute.consent.http.models.DarCollectionSummary;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.Vote;
import org.broadinstitute.consent.http.util.gson.GsonUtil;
import org.jdbi.v3.core.mapper.MappingException;
import org.jdbi.v3.core.mapper.NoSuchMapperException;
import org.jdbi.v3.core.result.LinkedHashMapRowReducer;
import org.jdbi.v3.core.result.RowView;

public class DarCollectionSummaryReducer
    implements LinkedHashMapRowReducer<Integer, DarCollectionSummary>, RowMapperHelper {

  @Override
  public void accumulate(Map<Integer, DarCollectionSummary> map, RowView rowView) {
    DarCollectionSummary summary =
        map.computeIfAbsent(
            rowView.getColumn("dar_collection_id", Integer.class),
            _ -> rowView.getRow(DarCollectionSummary.class));

    accumulateCloseout(summary, rowView);
    hasOptionalColumn(rowView, "dd_datasetid", Integer.class).ifPresent(summary::addDatasetId);
    hasOptionalColumn(rowView, "dac_name", String.class).ifPresent(summary::addDacName);
    accumulateLatestDarFields(summary, rowView);
    accumulateElection(summary, rowView);
    accumulateVote(summary, rowView);
  }

  private void accumulateCloseout(DarCollectionSummary summary, RowView rowView) {
    if (hasColumn(rowView, "closeout", String.class)) {
      String string = rowView.getColumn("closeout", String.class);
      CloseoutSupplement closeout =
          GsonUtil.getInstance().fromJson(string, CloseoutSupplement.class);
      summary.setCloseoutSupplement(closeout);
    }
  }

  private void accumulateLatestDarFields(DarCollectionSummary summary, RowView rowView) {
    try {
      String darReferenceId = rowView.getColumn("latest_dar_reference_id", String.class);
      if (Objects.nonNull(darReferenceId)) {
        summary.setLatestReferenceId(darReferenceId);
      }
      hasOptionalColumn(rowView, "latest_dar_parent_id", Integer.class)
          .ifPresent(
              darParentId -> summary.addParentChildRelationship(darParentId, darReferenceId));
      hasOptionalColumn(rowView, "latest_dar_requires_so_approval", Boolean.class)
          .ifPresent(summary::setRequiresSOApproval);
      hasOptionalColumn(rowView, "latest_dar_so_approver_id", Integer.class)
          .ifPresent(summary::setSoApproverId);
      hasOptionalColumn(rowView, "latest_dar_so_approver_timestamp", Timestamp.class)
          .ifPresent(summary::setSoApproverTimestamp);
      hasOptionalColumn(rowView, "latest_dar_update_date", Timestamp.class)
          .ifPresent(summary::setUpdateDate);
      hasOptionalColumn(rowView, "non_tech_rus", String.class).ifPresent(summary::setNonTechRus);
      hasOptionalColumn(rowView, "dar_status", String.class)
          .ifPresent(s -> summary.addStatus(s, darReferenceId));
      hasOptionalColumn(rowView, "signingOfficialEmail", String.class)
          .ifPresent(summary::setSigningOfficialEmail);
    } catch (MappingException _) {
      // ignore exception, it means dar_status and dar_reference_id wasn't included for this query
    }
  }

  private void accumulateElection(DarCollectionSummary summary, RowView rowView) {
    try {
      Election election = rowView.getRow(Election.class);
      if (Objects.nonNull(election.getElectionId())) {
        summary.addElection(election);
        summary.addDatasetId(election.getDatasetId());
      }
    } catch (MappingException | NoSuchMapperException _) {
      // Indicates that we do not have an election for this summary
    }
  }

  private void accumulateVote(DarCollectionSummary summary, RowView rowView) {
    try {
      Vote vote = rowView.getRow(Vote.class);
      if (Objects.nonNull(vote.getVoteId())) {
        summary.addVote(vote);
      }
    } catch (MappingException | NoSuchMapperException _) {
      // Indicates that we do not have a vote for this summary
    }
  }
}
