package org.broadinstitute.consent.http.models;

import com.google.common.collect.Streams;
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
  private static final String YES = "Yes";
  private static final String NO = "No";
  private String darId;
  private String dacName;
  private String datasetId;
  private Integer countUniqueUser;
  private Date dateSubmitted;
  private Date dateApproved;
  private Date dateDenied;
  private Integer turnaroundTime;
  private Long turnaroundTimeMillis;
  private String dacDecision;
  private String algorithmDecision;
  private String srpDecision;

  private static final String JOINER = "\t";
  public static final String headerRow = String.join(
    JOINER,
    "DAR ID",
    "DAC ID",
    "DAC UID",
    "Dataset ID",
    "Count of Unique Users",
    "Date Submitted",
    "Date Approved",
    "Date Denied",
    "DAR ToT",
    "Dac Decision",
    "Algorithm Decision",
    "Agreement Vote",
    "Structured Research Purpose Decision",
    "\n");

  public DarDecisionMetrics(
    DataAccessRequest dar,
    Dac dac,
    Dataset dataset,
    Election accessElection,
    Election rpElection,
    Match match,
    String darCode) {
    this.setDarId(darCode);
    this.setDacName(dac);
    this.setDatasetId(dataset);
    this.setCountUniqueUser(dar);
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
      getValue(getDacUID(getDacName())),
      getValue(getDatasetId()),
      getValue(getCountUniqueUsers()),
      getValue(getDateSubmitted()),
      getValue(getDateApproved()),
      getValue(getDateDenied()),
      getValue(getTurnaroundTime()),
      getValue(getDacDecision()),
      getValue(getAlgorithmDecision()),
      getValue(getAgreementVote()),
      getValue(getSrpDecision()),
      "\n");
  }

  public String getDarId() {
    return darId;
  }

  private void setDarId(String darCode) {
    this.darId = darCode;
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

  private void setDatasetId(Dataset dataset) {
    if (Objects.nonNull(dataset)) {
      this.datasetId = dataset.getDatasetIdentifier();
    }
  }

  private void setCountUniqueUser(DataAccessRequest dar) {
    this.countUniqueUser =
      (Objects.nonNull(dar.getData())) ?
        (int) Streams
          .concat(
            dar.getData().getLabCollaborators().stream(),
            dar.getData().getInternalCollaborators().stream())
          .filter(Objects::nonNull)
          .map(Collaborator::getEmail)
          .filter(Objects::nonNull)
          .map(String::toLowerCase)
          .distinct()
          .count()
        : 0;
  }

  public Integer getCountUniqueUsers() { return countUniqueUser; }

  public Date getDateSubmitted() {
    return dateSubmitted;
  }

  private void setDateSubmitted(DataAccessRequest dar) {
    if (Objects.nonNull(dar)) {
      this.dateSubmitted = dar.getSubmissionDate();
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
    if (Objects.nonNull(election)
      && Objects.nonNull(election.getFinalAccessVote())
      && election.getFinalAccessVote()) {
      if (Objects.nonNull(election.getFinalVoteDate())) {
        this.dateApproved = election.getFinalVoteDate();
      } else {
        this.dateApproved = election.getLastUpdate();
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
    if (Objects.nonNull(election)
      && Objects.nonNull(election.getFinalAccessVote())
      && !election.getFinalAccessVote()) {
      if (Objects.nonNull(election.getFinalVoteDate())) {
        this.dateDenied = election.getFinalVoteDate();
      } else {
        this.dateDenied = election.getLastUpdate();
      }
    }
  }

  public Integer getTurnaroundTime() {
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
          : election.getLastUpdate();
      if (Objects.nonNull(finalVoteDate)) {
        Calendar submittedDate = Calendar.getInstance();
        Calendar finalDate = Calendar.getInstance();
        submittedDate.setTime(this.getDateSubmitted());
        finalDate.setTime(finalVoteDate);
        Duration duration = Duration.between(submittedDate.toInstant(), finalDate.toInstant());
        this.turnaroundTimeMillis = duration.toMillis();
        this.turnaroundTime = this.convertMillisToDays(this.turnaroundTimeMillis);
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
    //NOTE: finalVote is pulled from the associated vote
    //Vote records are vastly more reliable than election vote status
    if (Objects.nonNull(election) && Objects.nonNull(election.getFinalVote())) {
      this.dacDecision = election.getFinalVote() ? YES : NO;
    }
  }

  public String getAlgorithmDecision() {
    return algorithmDecision;
  }

  private void setAlgorithmDecision(Match match) {
    if (Objects.nonNull(match) && Objects.nonNull(match.getMatch())) {
      this.algorithmDecision = match.getMatch() ? YES : NO;
    }
  }

  private String getAgreementVote() {
    if (Objects.nonNull(getDacDecision()) && Objects.nonNull(getAlgorithmDecision())) {
      return getDacDecision().equalsIgnoreCase(getAlgorithmDecision()) ? YES : NO;
    }
    return null;
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
    if (Objects.nonNull(dacDecision) && Objects.nonNull(election)) {
      Boolean rpVote =
        Objects.nonNull(election.getFinalVote())
          ? election.getFinalVote()
          : Objects.nonNull(election.getFinalAccessVote())
          ? election.getFinalAccessVote()
          : null;
      if (Objects.nonNull(rpVote)) {
        this.srpDecision = rpVote ? YES : NO;
      }
    }
  }

  private String getValue(String str) {
    return Objects.nonNull(str) ? str : "";
  }

  private String getValue(Integer i) {
    return Objects.nonNull(i) ? i.toString() : "";
  }

  private String getValue(Date date) {
    return Objects.nonNull(date) ? sdf.format(date) : "";
  }
}
