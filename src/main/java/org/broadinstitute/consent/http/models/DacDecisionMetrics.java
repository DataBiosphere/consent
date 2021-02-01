package org.broadinstitute.consent.http.models;

import org.broadinstitute.consent.http.models.dto.DataSetDTO;

import java.util.List;
import java.util.Objects;
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
    "DAC UID",
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
    String name = this.getDac().getName();
    return String.join(
      joiner,
      getValue(name),
      getValue(getDacUID(name)),
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
      int percentReviewed = createPercentage(completedDarMetrics.size(), metrics.size());
      this.setPercentDARsReviewed(percentReviewed);
    }

    completedDarMetrics.stream()
      .filter(m -> Objects.nonNull(m.getTurnaroundTimeMillis()))
      .mapToLong(DarDecisionMetrics::getTurnaroundTimeMillis)
      .average()
      .ifPresent(this::setAverageTurnaroundTimeMillis);

    this.setAverageTurnaroundTime();
    this.setPercentAgreementAlgorithm(completedDarMetrics);
    this.setPercentSRPAccurate(completedDarMetrics);
    this.setPercentRevealAlgorithm(null); //not implemented yet, will be empty column
  }

  public Integer createPercentage(int num, int denom) {
    return (int) (((double) num / (double) denom) * 100);

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
      this.averageTurnaroundTime = this.convertMillisToDays(this.averageTurnaroundTimeMillis.longValue());
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

  private void setPercentAgreementAlgorithm(List<DarDecisionMetrics> completedDarMetrics) {
    if (!completedDarMetrics.isEmpty()) {
      List<DarDecisionMetrics> agreementMetricsDenominator =
        completedDarMetrics.stream()
          .filter(m -> Objects.nonNull(m.getAlgorithmDecision()))
          .collect(Collectors.toList());

      List<DarDecisionMetrics> agreementMetricsNumerator =
        agreementMetricsDenominator.stream()
          .filter(m -> m.getAlgorithmDecision().equalsIgnoreCase(m.getDacDecision()))
          .collect(Collectors.toList());

      int percentAgreement = createPercentage(agreementMetricsNumerator.size(), agreementMetricsDenominator.size());
      this.percentAgreementAlgorithm = percentAgreement;
    }
  }

  public Integer getPercentSRPAccurate() {
    return percentSRPAccurate;
  }

  private void setPercentSRPAccurate(List<DarDecisionMetrics> completedDarMetrics) {
    List<DarDecisionMetrics> srpMetricsDenominator =
      completedDarMetrics.stream()
        .filter(m -> Objects.nonNull(m.getSrpDecision()))
        .collect(Collectors.toList());

    List<DarDecisionMetrics> srpMetricsNumerator =
      srpMetricsDenominator.stream()
        .filter(m -> m.getSrpDecision().equalsIgnoreCase("yes"))
        .collect(Collectors.toList());
    int percentSrp = createPercentage(srpMetricsNumerator.size(), srpMetricsDenominator.size());
    this.percentSRPAccurate = percentSrp;
  }

  private String getValue(String str) {
    return Objects.nonNull(str) ? str : "";
  }

  private String getValue(Integer i) {
    return Objects.nonNull(i) ? i.toString() : "";
  }
}
