package org.broadinstitute.consent.http.models;

import com.google.gson.Gson;
import java.util.Collection;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;
import org.broadinstitute.consent.http.enumeration.VoteType;
import org.broadinstitute.consent.http.util.DatasetUtil;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
      Match match,
      Collection<Vote> accessVotes,
      Collection<Vote> rpVotes) {

    Gson gson = new Gson();
    Logger logger = LoggerFactory.getLogger(this.getClass());
    logger.info("dar: " + gson.toJson(dar));
    logger.info("dac: " + gson.toJson(dac));
    logger.info("dataset: " + gson.toJson(dataset));
    logger.info("accessElection: " + gson.toJson(accessElection));
    logger.info("rpElection: " + gson.toJson(rpElection));
    logger.info("match: " + gson.toJson(match));
    logger.info("accessVotes: " + gson.toJson(accessVotes));
    logger.info("rpVotes: " + gson.toJson(rpVotes));

    this.setDarId(dar.getData().getDarCode());
    if (Objects.nonNull(dac)) {
      this.setDacId(dac.getName());
    }
    if (Objects.nonNull(dataset)) {
      this.setDacId(DatasetUtil.parseAlias(dataset.getAlias()));
    }
    if (Objects.nonNull(accessElection)) {
      this.setDateSubmitted(accessElection.getCreateDate());
    }
    Vote finalAccessVote = null;
    Optional<Vote> finalAccessVoteOpt =
        accessVotes.stream()
            .filter(v -> v.getType().equalsIgnoreCase(VoteType.FINAL.getValue()))
            .findFirst();
    if (finalAccessVoteOpt.isPresent()) {
      finalAccessVote = finalAccessVoteOpt.get();
      if (Objects.nonNull(finalAccessVote.getVote())) {
        if (finalAccessVote.getVote()) {
          this.setDateApproved(finalAccessVote.getUpdateDate());
          this.setDacDecision("Yes");
        } else {
          this.setDateDenied(finalAccessVote.getUpdateDate());
          this.setDacDecision("No");
        }
      }
    }

    if (Objects.nonNull(finalAccessVote)) {
      DateTime tot = null;
      DateTime voteTime = new DateTime(finalAccessVote.getUpdateDate());
      if (Objects.nonNull(this.getDateSubmitted())) {
        tot = voteTime.minus(this.getDateSubmitted().getTime());
      } else if (Objects.nonNull(this.getDateDenied())) {
        tot = voteTime.minus(this.getDateDenied().getTime());
      }
      if (Objects.nonNull(tot)) {
        this.setDarTurnaroundTime(tot.toString());
      }
    }

    if (Objects.nonNull(match)) {
      String decision = match.getMatch() ? "Yes" : "No";
      this.setAlgorithmDecision(decision);
    }

    Vote finalRpVote = null;
    Optional<Vote> finalRpVoteOpt =
        accessVotes.stream()
            .filter(v -> v.getType().equalsIgnoreCase(VoteType.FINAL.getValue()))
            .findFirst();
    if (finalRpVoteOpt.isPresent()) {
      finalRpVote = finalRpVoteOpt.get();
    }
    if (Objects.nonNull(finalRpVote) && Objects.nonNull(finalRpVote.getVote())) {
      String decision = finalRpVote.getVote() ? "Yes" : "No";
      this.setSrpDecision(decision);
    }

  }

  public String getDarId() {
    return darId;
  }

  public void setDarId(String darId) {
    this.darId = darId;
  }

  public String getDacId() {
    return dacId;
  }

  public void setDacId(String dacId) {
    this.dacId = dacId;
  }

  public String getDatasetId() {
    return datasetId;
  }

  public void setDatasetId(String datasetId) {
    this.datasetId = datasetId;
  }

  public Date getDateSubmitted() {
    return dateSubmitted;
  }

  public void setDateSubmitted(Date dateSubmitted) {
    this.dateSubmitted = dateSubmitted;
  }

  public Date getDateApproved() {
    return dateApproved;
  }

  public void setDateApproved(Date dateApproved) {
    this.dateApproved = dateApproved;
  }

  public Date getDateDenied() {
    return dateDenied;
  }

  public void setDateDenied(Date dateDenied) {
    this.dateDenied = dateDenied;
  }

  public String getDarTurnaroundTime() {
    return darTurnaroundTime;
  }

  public void setDarTurnaroundTime(String darTurnaroundTime) {
    this.darTurnaroundTime = darTurnaroundTime;
  }

  public String getDacDecision() {
    return dacDecision;
  }

  public void setDacDecision(String dacDecision) {
    this.dacDecision = dacDecision;
  }

  public String getAlgorithmDecision() {
    return algorithmDecision;
  }

  public void setAlgorithmDecision(String algorithmDecision) {
    this.algorithmDecision = algorithmDecision;
  }

  public String getSrpDecision() {
    return srpDecision;
  }

  public void setSrpDecision(String srpDecision) {
    this.srpDecision = srpDecision;
  }
}
