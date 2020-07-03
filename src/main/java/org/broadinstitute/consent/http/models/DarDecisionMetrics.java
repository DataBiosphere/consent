package org.broadinstitute.consent.http.models;

import java.time.Duration;
import java.util.Calendar;
import java.util.Date;
import java.util.Objects;
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

  public void setDarId(DataAccessRequest dar) {
    if (Objects.nonNull(dar) && Objects.nonNull(dar.getData()))
    this.darId = dar.getData().getDarCode();
  }

  public String getDacId() {
    return dacId;
  }

  public void setDacId(Dac dac) {
    if (Objects.nonNull(dac)) {
      this.dacId = dac.getName();
    }
  }

  public String getDatasetId() {
    return datasetId;
  }

  public void setDatasetId(DataSet dataset) {
    if (Objects.nonNull(dataset)) {
      this.datasetId = DatasetUtil.parseAlias(dataset.getAlias());
    }
  }

  public Date getDateSubmitted() {
    return dateSubmitted;
  }

  public void setDateSubmitted(Election election) {
    if (Objects.nonNull(election)) {
      this.dateSubmitted = election.getCreateDate();
    }
  }

  public Date getDateApproved() {
    return dateApproved;
  }

  public void setDateApproved(Election election) {
    if (Objects.nonNull(election) && election.getFinalAccessVote()) {
      this.dateApproved = election.getFinalVoteDate();
    }
  }

  public Date getDateDenied() {
    return dateDenied;
  }

  public void setDateDenied(Election election) {
    if (Objects.nonNull(election) && !election.getFinalAccessVote()) {
      this.dateDenied = election.getFinalVoteDate();
    }
  }

  public String getDarTurnaroundTime() {
    return darTurnaroundTime;
  }

  public void setDarTurnaroundTime(Election election) {
    if (Objects.nonNull(election) && Objects.nonNull(election.getFinalVote())) {
      Calendar submittedDate = Calendar.getInstance();
      Calendar finalDate = Calendar.getInstance();
      submittedDate.setTime(this.getDateSubmitted());
      finalDate.setTime(election.getFinalVoteDate());
      Duration duration = Duration.between(submittedDate.toInstant(), finalDate.toInstant());
      this.darTurnaroundTime = duration.toString();
    }
  }

  public String getDacDecision() {
    return dacDecision;
  }

  public void setDacDecision(Election election) {
    if (Objects.nonNull(election) && election.getFinalAccessVote()) {
      this.dacDecision = election.getFinalAccessVote() ? "Yes" : "No";
    }
  }

  public String getAlgorithmDecision() {
    return algorithmDecision;
  }

  public void setAlgorithmDecision(Match match) {
    if (Objects.nonNull(match) && Objects.nonNull(match.getMatch())) {
      this.algorithmDecision = match.getMatch() ? "Yes" : "No";
    }
  }

  public String getSrpDecision() {
    return srpDecision;
  }

  public void setSrpDecision(Election election) {
    if (Objects.nonNull(election) && Objects.nonNull(election.getFinalVote())) {
      this.srpDecision = election.getFinalVote() ? "Yes" : "No";
    }
  }
}
