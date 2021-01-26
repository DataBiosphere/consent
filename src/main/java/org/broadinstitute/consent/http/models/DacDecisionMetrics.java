package org.broadinstitute.consent.http.models;

import org.broadinstitute.consent.http.models.dto.DataSetDTO;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Generate a row of dac decision data.
 */
public class DacDecisionMetrics implements DecisionMetrics {

  private Dac dac;
  private List<DataSetDTO> datasets;
  private List<DarDecisionMetrics> metrics;
  private Integer chairCount;
  private Integer memberCount;
  private Integer datasetCount;
  private Integer darsReceived;
  private Integer percentDARsReviewed;
  private Double averageTurnaroundTimeMillis;
  private Integer averageTurnaroundTime;
  private Integer percentRevealAlgorithm;
  private Integer percentAgreementAlgorithm;
  private Integer percentSRPAccurate;

  private static final String JOINER = "\t";
  public static final String headerRow = String.join(
    JOINER,
    "DAC ID",
    "# of DAC Members",
    "# of DAC Chairs",
    "# of Datasets",
    "# of DARs Received",
    "% of DARs Reviewed",
    "Average DAR Turnaround Time",
    "% Reveal DUOS Algorithm",
    "% Agreement with DUOS Algorithm",
    "% Structured Research Purpose Accurate",
    "\n");

  public String toString(String joiner) {
    return String.join(
      joiner,
      getValue(this.getDac().getName()),
      getValue(getMemberCount()),
      getValue(getChairCount()),
      getValue(getDatasetCount()),
      getValue(getDarsReceived()),
      getValue(getPercentDARsReviewed()),
      getValue(getAverageTurnaroundTime()),
      getValue(getPercentRevealAlgorithm()),
      getValue(getPercentAgreementAlgorithm()),
      getValue(getPercentSRPAccurate()),
      "\n");
  }

  public DacDecisionMetrics(Dac dac, List<DataSetDTO> datasets, List<DarDecisionMetrics> metrics) {
    this.setDac(dac);
    this.setDatasets(datasets);
    this.setMetrics(metrics);
    this.setChairCount(dac);
    this.setMemberCount(dac);
    this.setDatasetCount(datasets);

    this.setDarsReceived(metrics.size());
    List<DarDecisionMetrics> completedDarMetrics =
      metrics.stream()
        .filter(m -> Objects.nonNull(m.getDacDecision()))
        .collect(Collectors.toList());
    if (!metrics.isEmpty()) {
      long percentReviewed = (long) completedDarMetrics.size() / (long) metrics.size() * 100;
      this.setPercentDARsReviewed((int) percentReviewed);
    }
    completedDarMetrics.stream()
      .filter(m -> Objects.nonNull(m.getTurnaroundTimeMillis()))
      .mapToLong(DarDecisionMetrics::getTurnaroundTimeMillis)
      .average()
      .ifPresent(this::setAverageTurnaroundTimeMillis);
    this.setAverageTurnaroundTime();

    List<DarDecisionMetrics> agreementMetrics =
      completedDarMetrics.stream()
        .filter(m -> Objects.nonNull(m.getAlgorithmDecision()))
        .filter(m -> Objects.nonNull(m.getDacDecision()))
        .filter(m -> m.getAlgorithmDecision().equalsIgnoreCase(m.getDacDecision()))
        .collect(Collectors.toList());

    if (!completedDarMetrics.isEmpty()) {
      long percentAgreement = (long) agreementMetrics.size() / (long) completedDarMetrics.size();
      this.setPercentAgreementAlgorithm((int) percentAgreement);

      List<DarDecisionMetrics> srpMetrics =
        completedDarMetrics.stream()
          .filter(m -> Objects.nonNull(m.getSrpDecision()))
          .filter(m -> m.getSrpDecision().equalsIgnoreCase("yes"))
          .collect(Collectors.toList());
      long percentSrp = (long) srpMetrics.size() / (long) completedDarMetrics.size();
      this.setPercentSRPAccurate((int) percentSrp);
    }
    setPercentRevealAlgorithm(0); // Placeholder for "% Reveal DUOS Algorithm"
  }

  public Dac getDac() {
    return dac;
  }

  private void setDac(Dac dac) {
    this.dac = dac;
  }

  public List<DataSetDTO> getDatasets() {
    return datasets;
  }

  private void setDatasets(List<DataSetDTO> datasets) {
    this.datasets = datasets;
  }

  public List<DarDecisionMetrics> getMetrics() {
    return metrics;
  }

  private void setMetrics(List<DarDecisionMetrics> metrics) {
    this.metrics = metrics;
  }

  public Integer getChairCount() {
    return chairCount;
  }

  private void setChairCount(Dac dac) {
    if (Objects.nonNull(dac.getChairpersons()) && !dac.getChairpersons().isEmpty()) {
      this.chairCount = dac.getChairpersons().size();
    }
  }

  public Integer getMemberCount() {
    return memberCount;
  }

  private void setMemberCount(Dac dac) {
    if (Objects.nonNull(dac.getMembers()) && !dac.getMembers().isEmpty()) {
      this.memberCount = dac.getMembers().size();
    }
  }

  public Integer getDatasetCount() {
    return datasetCount;
  }

  private void setDatasetCount(List<DataSetDTO> datasets) {
    if (Objects.nonNull(datasets) && !datasets.isEmpty()) {
      this.datasetCount = datasets.size();
    }
  }

  public Integer getDarsReceived() {
    return darsReceived;
  }

  private void setDarsReceived(Integer darsReceived) {
    this.darsReceived = darsReceived;
  }

  public Integer getPercentDARsReviewed() {
    return percentDARsReviewed;
  }

  private void setPercentDARsReviewed(Integer percentDARsReviewed) {
    this.percentDARsReviewed = percentDARsReviewed;
  }

  public Double getAverageTurnaroundTimeMillis() {
    return averageTurnaroundTimeMillis;
  }

  private void setAverageTurnaroundTimeMillis(Double averageTurnaroundTimeMillis) {
    this.averageTurnaroundTimeMillis = averageTurnaroundTimeMillis;
  }

  public Integer getAverageTurnaroundTime() {
    return averageTurnaroundTime;
  }

  private void setAverageTurnaroundTime() {
    if (Objects.nonNull(this.getAverageTurnaroundTimeMillis())) {
      //this will only ever catch an exception if there is no final date, or an unreasonable amount of time between them
      //in this case, the upward bound of Integer is displayed
      try {
        this.averageTurnaroundTime = Math.toIntExact(TimeUnit.MILLISECONDS.toDays(this.averageTurnaroundTimeMillis.longValue()));
      } catch (ArithmeticException e) {
        this.averageTurnaroundTime = 2147483647;
      }
    }
  }

  public Integer getPercentRevealAlgorithm() {
    return percentRevealAlgorithm;
  }

  private void setPercentRevealAlgorithm(Integer percentRevealAlgorithm) {
    this.percentRevealAlgorithm = percentRevealAlgorithm;
  }

  public Integer getPercentAgreementAlgorithm() {
    return percentAgreementAlgorithm;
  }

  private void setPercentAgreementAlgorithm(Integer percentAgreementAlgorithm) {
    this.percentAgreementAlgorithm = percentAgreementAlgorithm;
  }

  public Integer getPercentSRPAccurate() {
    return percentSRPAccurate;
  }

  private void setPercentSRPAccurate(Integer percentSRPAccurate) {
    this.percentSRPAccurate = percentSRPAccurate;
  }

  private String getValue(String str) {
    return Objects.nonNull(str) ? str : "";
  }

  private String getValue(Integer i) {
    return Objects.nonNull(i) ? i.toString() : "";
  }
}
