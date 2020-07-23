package org.broadinstitute.consent.http.models;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.broadinstitute.consent.http.models.dto.DataSetDTO;

/**
 * Generate a row of dac decision data.
 *
 * <p>TODO: Future task to add Used DUOS Algorithm for Decision Support: Yes/No
 */
public class DacDecisionMetrics {

  Dac dac;
  List<DataSetDTO> datasets;
  List<DarDecisionMetrics> metrics;
  Integer chairCount;
  Integer memberCount;
  Integer datasetCount;
  Integer darsReceived;
  Integer percentDARsReviewed;
  Double averageTurnaroundTimeMillis;
  String averageTurnaroundTime;
  Integer percentRevealAlgorithm;
  Integer percentAgreementAlgorithm;
  Integer percentSRPAccurate;

  public static String getHeaderRow(String joiner) {
    return String.join(
        joiner,
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
  }

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
        "",
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

  public String getAverageTurnaroundTime() {
    return averageTurnaroundTime;
  }

  private void setAverageTurnaroundTime() {
    if (Objects.nonNull(this.getAverageTurnaroundTimeMillis())) {
      this.averageTurnaroundTime =
          DurationFormatUtils.formatDurationWords(
              this.getAverageTurnaroundTimeMillis().longValue(), true, true);
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
