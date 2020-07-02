package org.broadinstitute.consent.http.models;

import com.google.gson.Gson;
import java.util.Collection;
import java.util.Objects;
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

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private String darId;
  private String dacId;
  private String datasetId;
  private String dateSubmitted;
  private String dateApproved;
  private String dateDenied;
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
    logger.info("dar: " + gson.toJson(dar));
    logger.info("dac: " + gson.toJson(dac));
    logger.info("dataset: " + gson.toJson(dataset));
    logger.info("accessElection: " + gson.toJson(accessElection));
    logger.info("rpElection: " + gson.toJson(rpElection));
    logger.info("match: " + gson.toJson(match));
    logger.info("accessVotes: " + gson.toJson(accessVotes));
    logger.info("rpVotes: " + gson.toJson(rpVotes));

    this.setDarId(dar.getReferenceId());
    if (Objects.nonNull(dac)) {
      this.setDacId(dac.getName());
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

  public String getDateSubmitted() {
    return dateSubmitted;
  }

  public void setDateSubmitted(String dateSubmitted) {
    this.dateSubmitted = dateSubmitted;
  }

  public String getDateApproved() {
    return dateApproved;
  }

  public void setDateApproved(String dateApproved) {
    this.dateApproved = dateApproved;
  }

  public String getDateDenied() {
    return dateDenied;
  }

  public void setDateDenied(String dateDenied) {
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
