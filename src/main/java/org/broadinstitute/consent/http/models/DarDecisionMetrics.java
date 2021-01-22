package org.broadinstitute.consent.http.models;

import org.apache.commons.lang3.time.DurationFormatUtils;
import org.broadinstitute.consent.http.util.DatasetUtil;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Calendar;
import java.util.Date;
import java.util.Objects;

/**
 * Generate a row of dar decision data in the form of:
 *
 * <p>DAR ID: DAR-123-A-0 DAC ID: Broad DAC Dataset ID: DS-00001 Date Submitted: 01-01-2020 Date
 * Approved: 01-02-2020 Date Denied: 01-02-2020 DAR ToT: 1 day DAC Decision: Yes/No Algorithm
 * Decision: Yes/No Structured Research Purpose Decision: Yes/No
 */
public class DarDecisionMetrics implements DecisionMetrics {

  private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
  private String darId;
  private String dacName;
  private String datasetId;
  private Date dateSubmitted;
  private Date dateApproved;
  private Date dateDenied;
  private String turnaroundTime;
  private Long turnaroundTimeMillis;
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
    this.setDacName(dac);
    this.setDatasetId(dataset);
    this.setDacDecision(accessElection);
    this.setDateSubmitted(dar);
    this.setDateApproved(accessElection);
    this.setDateDenied(accessElection);
    this.setTurnaroundTime(accessElection);
    this.setAlgorithmDecision(match);
    this.setSrpDecision(rpElection);
  }

  public String toString(String joiner) {
    return String.join(
        joiner,
        getValue(this.getDarId()),
        getValue(getDacName()),
        getValue(getDatasetId()),
        getValue(getDateSubmitted()),
        getValue(getDateApproved()),
        getValue(getDateDenied()),
        getValue(getTurnaroundTime()),
        getValue(getDacDecision()),
        getValue(getAlgorithmDecision()),
        getValue(getSrpDecision()),
        "\n");
  }

  public String getDarId() {
    return darId;
  }

  private void setDarId(DataAccessRequest dar) {
    if (Objects.nonNull(dar) && Objects.nonNull(dar.getData()))
      this.darId = dar.getData().getDarCode();
  }

  public String getDacName() {
    return dacName;
  }

  private void setDacName(Dac dac) {
    if (Objects.nonNull(dac)) {
      this.dacName = dac.getName();
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

  private void setDateSubmitted(DataAccessRequest dar) {
    if (Objects.nonNull(dar)) {
      this.dateSubmitted = dar.getCreateDate();
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

  public String getTurnaroundTime() {
    return turnaroundTime;
  }

  /**
   * Use the update date as a proxy if vote date doesn't exist
   *
   * <p>TODO: Need a story to track updating the final vote date properly
   *
   * @param election The election
   */
  private void setTurnaroundTime(Election election) {
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
        this.turnaroundTimeMillis = duration.toMillis();
        this.turnaroundTime =
            DurationFormatUtils.formatDurationWords(duration.toMillis(), true, true);
      }
    }
  }

  public Long getTurnaroundTimeMillis() {
    return turnaroundTimeMillis;
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

  private String getValue(String str) {
    return Objects.nonNull(str) ? str : "";
  }

  private String getValue(Date date) {
    return Objects.nonNull(date) ? sdf.format(date) : "";
  }
}
