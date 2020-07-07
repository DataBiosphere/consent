package org.broadinstitute.consent.http.models;

import java.time.Duration;
import java.util.Calendar;
import java.util.Date;
import java.util.Objects;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.broadinstitute.consent.http.util.DatasetUtil;

/**
 * Generate a row of summary data in the form of:
 *
 * <p>DAR ID: DAR-123-A-0 DAC ID: Broad DAC Dataset ID: DS-00001 Date Submitted: 01-01-2020 Date
 * Approved: 01-02-2020 Date Denied: 01-02-2020 DAR ToT: 1 day DAC Decision: Yes/No Algorithm
 * Decision: Yes/No Structured Research Purpose Decision: Yes/No
 *
 * <p>TODO: Future task to add Used DUOS Algorithm for Decision Support: Yes/No
 */
public class DarDecisionMetrics {

  private String darId;
  private String dacId;
  private String datasetId;
  private Date dateSubmitted;
  private Date dateApproved;
  private Date dateDenied;
  private String darTurnaroundTime;
  private long darTurnaroundTimeMillis;
  private String dacDecision;
  private String algorithmDecision;
  private String srpDecision;

  public DarDecisionMetrics(
      DataAccessRequest dar,
      Dac dac,
      DataSet dataset,
      Election accessElection,
      Election rpElection,
      Match match) {
    this.setDarId(dar);
    this.setDacId(dac);
    this.setDatasetId(dataset);
    this.setDacDecision(accessElection);
    this.setDateSubmitted(accessElection);
    this.setDateApproved(accessElection);
    this.setDateDenied(accessElection);
    this.setDarTurnaroundTime(accessElection);
    this.setAlgorithmDecision(match);
    this.setSrpDecision(rpElection);
  }

  public String getDarId() {
    return darId;
  }

  private void setDarId(DataAccessRequest dar) {
    if (Objects.nonNull(dar) && Objects.nonNull(dar.getData()))
      this.darId = dar.getData().getDarCode();
  }

  public String getDacId() {
    return dacId;
  }

  private void setDacId(Dac dac) {
    if (Objects.nonNull(dac)) {
      this.dacId = dac.getName();
    }
  }

  public String getDatasetId() {
    return datasetId;
  }

  private void setDatasetId(DataSet dataset) {
    if (Objects.nonNull(dataset)) {
      this.datasetId = DatasetUtil.parseAlias(dataset.getAlias());
    }
  }

  public Date getDateSubmitted() {
    return dateSubmitted;
  }

  private void setDateSubmitted(Election election) {
    if (Objects.nonNull(election)) {
      this.dateSubmitted = election.getCreateDate();
    }
  }

  public Date getDateApproved() {
    return dateApproved;
  }

  /**
   * Use the update date as a proxy if vote date doesn't exist
   *
   * <p>TODO: Need a story to track updating the final vote date properly
   *
   * @param election The election
   */
  private void setDateApproved(Election election) {
    if (Objects.nonNull(election) && election.getFinalAccessVote()) {
      if (Objects.nonNull(election.getFinalVoteDate())) {
        this.dateApproved = election.getFinalVoteDate();
      } else {
        this.dateApproved = election.getLastUpdateDate();
      }
    }
  }

  public Date getDateDenied() {
    return dateDenied;
  }

  /**
   * Use the update date as a proxy if vote date doesn't exist
   *
   * <p>TODO: Need a story to track updating the final vote date properly
   *
   * @param election The election
   */
  private void setDateDenied(Election election) {
    if (Objects.nonNull(election) && !election.getFinalAccessVote()) {
      if (Objects.nonNull(election.getFinalVoteDate())) {
        this.dateDenied = election.getFinalVoteDate();
      } else {
        this.dateDenied = election.getLastUpdateDate();
      }
    }
  }

  public String getDarTurnaroundTime() {
    return darTurnaroundTime;
  }

  /**
   * Use the update date as a proxy if vote date doesn't exist
   *
   * <p>TODO: Need a story to track updating the final vote date properly
   *
   * @param election The election
   */
  private void setDarTurnaroundTime(Election election) {
    if (Objects.nonNull(election)) {
      Date finalVoteDate =
          Objects.nonNull(election.getFinalVoteDate())
              ? election.getFinalVoteDate()
              : election.getLastUpdateDate();
      if (Objects.nonNull(finalVoteDate)) {
        Calendar submittedDate = Calendar.getInstance();
        Calendar finalDate = Calendar.getInstance();
        submittedDate.setTime(this.getDateSubmitted());
        finalDate.setTime(finalVoteDate);
        Duration duration = Duration.between(submittedDate.toInstant(), finalDate.toInstant());
        this.darTurnaroundTimeMillis = duration.toMillis();
        this.darTurnaroundTime =
            DurationFormatUtils.formatDurationWords(duration.toMillis(), true, true);
      }
    }
  }

  public long getDarTurnaroundTimeMillis() {
    return darTurnaroundTimeMillis;
  }

  public String getDacDecision() {
    return dacDecision;
  }

  private void setDacDecision(Election election) {
    if (Objects.nonNull(election) && election.getFinalAccessVote()) {
      this.dacDecision = election.getFinalAccessVote() ? "Yes" : "No";
    }
  }

  public String getAlgorithmDecision() {
    return algorithmDecision;
  }

  private void setAlgorithmDecision(Match match) {
    if (Objects.nonNull(match) && Objects.nonNull(match.getMatch())) {
      this.algorithmDecision = match.getMatch() ? "Yes" : "No";
    }
  }

  public String getSrpDecision() {
    return srpDecision;
  }

  /**
   * Use finalAccessVote as a proxy if finalVote is null
   *
   * <p>TODO: Need a story to track updating the final vote date properly
   *
   * @param election The election
   */
  private void setSrpDecision(Election election) {
    if (Objects.nonNull(election)) {
      Boolean rpVote =
          Objects.nonNull(election.getFinalVote())
              ? election.getFinalVote()
              : Objects.nonNull(election.getFinalAccessVote())
                  ? election.getFinalAccessVote()
                  : null;
      if (Objects.nonNull(rpVote)) {
        this.srpDecision = rpVote ? "Yes" : "No";
      }
    }
  }
}
