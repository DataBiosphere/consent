package org.broadinstitute.consent.http.matching;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import com.google.inject.Inject;
import jakarta.ws.rs.core.StreamingOutput;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.consent.http.enumeration.DataUseTranslationType;
import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.models.ontology.DataUseSummary;
import org.broadinstitute.consent.http.models.ontology.DataUseTerm;
import org.broadinstitute.consent.http.service.ontology.OntologyDAO;
import org.broadinstitute.consent.http.service.ontology.OntologyTerm;
import org.broadinstitute.consent.http.util.ConsentLogger;
import org.broadinstitute.consent.http.util.gson.GsonUtil;
import org.jdbi.v3.core.Jdbi;

public class TranslationUtil implements ConsentLogger {

  public static final String DATASET_HEADER =
      "Samples are restricted for use under the following conditions:";
  public static final String PURPOSE_HEADER =
      "Research is limited to samples restricted for use under the following conditions:";
  protected static final String FEMALE = "Female";
  protected static final String MALE = "Male";
  public static final String GRU = "Data is available for general research use. [GRU]";
  public static final String DS = "Data use is limited for studying: %s [DS]";
  public static final String HMB = "Data is limited for health/medical/biomedical research. [HMB]";
  protected static final String POA =
      "Future use for population origins or ancestry research is prohibited. [POA]";
  public static final String NMDS =
      "Data use for methods development research ONLY within the bounds of other data use limitations. [NMDS]";
  public static final String NPU =
      "Use is limited to non-profit and non-commercial research. [NPU]";
  protected static final String OTHER = "Other restrictions: %s.";
  private static final String OTHER_CODE = "OTHER";
  protected static final String SECONDARY_OTHER = "Secondary other restrictions: %s.";
  protected static final String ETHICS_APPROVAL = "Local ethics committee approval is required.";
  protected static final String COLLABORATION_REQUIRED =
      "Collaboration with the primary study investigators required. [COL]";
  protected static final String GEO_RESTRICTION = "Geographical restrictions: %s.";
  protected static final String GSO = "Future use is limited to genetic studies only [GSO]";
  protected static final String PUB_REQUIRED =
      "Publishing results of studies using the data available to the larger scientific community is required";
  protected static final String PUB_MORATORIUM =
      "Publishing moratorium until '%s' is in effect. [MOR]";
  public static final String NCTRL =
      "Future use as a control set for diseases other than those specified is prohibited. [NCTRL]";
  protected static final String RS_M = "Data use is limited to research on males. [RS-M]";
  protected static final String RS_FM = "Data use is limited to research on females. [RS-FM]";
  protected static final String RS_PD = "Data use is limited to pediatric research. [RS-PD]";
  protected static final String POP =
      "Future use for study variation in the general population (e.g. calling variants and/or studying their distribution). [POP]";

  /**
   * One entry of the dataset summary, shared by {@link #translateSummary(DataUse)} and {@link
   * #summaryCodes(DataUse)}. A null description leaves the entry out of the summary only.
   */
  private record SummaryRule(
      String code,
      boolean primary,
      Predicate<DataUse> applies,
      BiFunction<TranslationUtil, DataUse, String> description) {}

  /** The summary entries, in summary order. */
  private static final List<SummaryRule> SUMMARY_RULES =
      List.of(
          new SummaryRule("GRU", true, d -> BooleanUtils.isTrue(d.getGeneralUse()), (t, d) -> GRU),
          new SummaryRule(
              "DS",
              true,
              d -> d.getDiseaseRestrictions() != null && !d.getDiseaseRestrictions().isEmpty(),
              TranslationUtil::diseaseDescription),
          new SummaryRule("HMB", true, d -> BooleanUtils.isTrue(d.getHmbResearch()), (t, d) -> HMB),
          new SummaryRule(
              "NPOA",
              true,
              d -> BooleanUtils.isTrue(d.getPopulationOriginsAncestry()),
              (t, d) -> POA),
          new SummaryRule(
              OTHER_CODE,
              true,
              d -> StringUtils.isNotBlank(d.getOther()),
              (t, d) -> String.format(OTHER, d.getOther())),
          new SummaryRule(
              "NMDS", false, d -> BooleanUtils.isTrue(d.getMethodsResearch()), (t, d) -> NMDS),
          new SummaryRule(
              "NPU", false, d -> BooleanUtils.isTrue(d.getNonProfitUse()), (t, d) -> NPU),
          new SummaryRule(
              OTHER_CODE,
              false,
              d -> StringUtils.isNotBlank(d.getSecondaryOther()),
              (t, d) -> String.format(SECONDARY_OTHER, d.getSecondaryOther())),
          new SummaryRule(
              "IRB",
              false,
              d -> BooleanUtils.isTrue(d.getEthicsApprovalRequired()),
              (t, d) -> ETHICS_APPROVAL),
          new SummaryRule(
              "COL",
              false,
              d -> BooleanUtils.isTrue(d.getCollaboratorRequired()),
              (t, d) -> COLLABORATION_REQUIRED),
          new SummaryRule(
              "GS",
              false,
              d -> StringUtils.isNotBlank(d.getGeographicalRestrictions()),
              (t, d) -> String.format(GEO_RESTRICTION, d.getGeographicalRestrictions())),
          new SummaryRule(
              "GSO", false, d -> BooleanUtils.isTrue(d.getGeneticStudiesOnly()), (t, d) -> GSO),
          new SummaryRule(
              "PUB",
              false,
              d -> BooleanUtils.isTrue(d.getPublicationResults()),
              (t, d) -> PUB_REQUIRED),
          new SummaryRule(
              "MOR",
              false,
              d -> StringUtils.isNotBlank(d.getPublicationMoratorium()),
              (t, d) -> String.format(PUB_MORATORIUM, d.getPublicationMoratorium())),
          new SummaryRule(
              "NCTRL", false, d -> BooleanUtils.isTrue(d.getControls()), (t, d) -> NCTRL),
          new SummaryRule(
              "POP-M", false, d -> MALE.equalsIgnoreCase(d.getGender()), (t, d) -> RS_M),
          new SummaryRule(
              "POP-F", false, d -> FEMALE.equalsIgnoreCase(d.getGender()), (t, d) -> RS_FM),
          new SummaryRule(
              "POP-PD", false, d -> BooleanUtils.isTrue(d.getPediatric()), (t, d) -> RS_PD),
          new SummaryRule(
              "POP", false, d -> BooleanUtils.isTrue(d.getPopulation()), (t, d) -> POP));

  private final OntologyDAO ontologyDAO;
  private final Gson gson = GsonUtil.getInstance();

  @Inject
  public TranslationUtil(Jdbi jdbi) {
    this.ontologyDAO = jdbi.onDemand(OntologyDAO.class);
  }

  /**
   * Generate a textual summary that consists of an ordered list of restrictions.
   *
   * @param dataUse The DataUse object
   * @return Summary list of restrictions in string format
   */
  // Suppress warning for method complexity (S3776). Due for reevaluation after initial migration
  // Suppress warning for switch statement (S1301). This is more readable as a switch than if/else
  // statements.
  @SuppressWarnings({"java:S3776", "java:S1301"})
  public String translate(DataUse dataUse, DataUseTranslationType type) {
    List<String> summary = new ArrayList<>();
    switch (type) {
      case DATASET -> summary.add(DATASET_HEADER);
      case PURPOSE -> summary.add(PURPOSE_HEADER);
      case null -> throw new IllegalArgumentException("Translation type is required");
    }

    if (dataUse == null) {
      return String.join("\n", summary);
    }
    if (BooleanUtils.isTrue(dataUse.getGeneralUse())) {
      summary.add(GRU);
    }

    if (dataUse.getDiseaseRestrictions() != null && !dataUse.getDiseaseRestrictions().isEmpty()) {
      List<String> labels = findLabelsForTermIds(dataUse.getDiseaseRestrictions());
      if (!labels.isEmpty()) {
        summary.add(String.format(DS, String.join(", ", labels)));
      }
    }

    if (BooleanUtils.isTrue(dataUse.getHmbResearch())) {
      summary.add(HMB);
    }

    if (BooleanUtils.isTrue(dataUse.getPopulationOriginsAncestry())) {
      summary.add(POA);
    }

    if (BooleanUtils.isTrue(dataUse.getMethodsResearch())) {
      summary.add(NMDS);
    }

    if (BooleanUtils.isTrue(dataUse.getNonProfitUse())) {
      summary.add(NPU);
    }

    if (StringUtils.isNotBlank(dataUse.getOther())) {
      summary.add(String.format(OTHER, dataUse.getOther()));
    }

    if (StringUtils.isNotBlank(dataUse.getSecondaryOther())) {
      summary.add(String.format(SECONDARY_OTHER, dataUse.getSecondaryOther()));
    }

    if (BooleanUtils.isTrue(dataUse.getEthicsApprovalRequired())) {
      summary.add(ETHICS_APPROVAL);
    }

    if (BooleanUtils.isTrue(dataUse.getCollaboratorRequired())) {
      summary.add(COLLABORATION_REQUIRED);
    }

    if (StringUtils.isNotBlank(dataUse.getGeographicalRestrictions())) {
      summary.add(String.format(GEO_RESTRICTION, dataUse.getGeographicalRestrictions()));
    }

    if (BooleanUtils.isTrue(dataUse.getGeneticStudiesOnly())) {
      summary.add(GSO);
    }

    if (BooleanUtils.isTrue(dataUse.getPublicationResults())) {
      summary.add(PUB_REQUIRED);
    }

    if (StringUtils.isNotBlank(dataUse.getPublicationMoratorium())) {
      summary.add(String.format(PUB_MORATORIUM, dataUse.getPublicationMoratorium()));
    }

    if (BooleanUtils.isTrue(dataUse.getControls())) {
      summary.add(NCTRL);
    }

    if (Optional.ofNullable(dataUse.getGender()).orElse("na").equalsIgnoreCase(MALE)) {
      summary.add(RS_M);
    }

    if (Optional.ofNullable(dataUse.getGender()).orElse("na").equalsIgnoreCase(FEMALE)) {
      summary.add(RS_FM);
    }

    if (BooleanUtils.isTrue(dataUse.getPediatric())) {
      summary.add(RS_PD);
    }

    if (BooleanUtils.isTrue(dataUse.getPopulation())) {
      summary.add(POP);
    }

    return String.join("\n", summary);
  }

  /**
   * Generate a structured summary of the data use. This method is Dataset specific.
   *
   * @param dataUse The DataUse
   * @return DataUseSummary The structured summary
   */
  public DataUseSummary translateSummary(DataUse dataUse) {
    DataUseSummary summary = new DataUseSummary();
    if (Objects.isNull(dataUse)) {
      return summary;
    }
    List<DataUseTerm> primary = new ArrayList<>();
    List<DataUseTerm> secondary = new ArrayList<>();
    for (SummaryRule rule : SUMMARY_RULES) {
      if (rule.applies().test(dataUse)) {
        String description = rule.description().apply(this, dataUse);
        if (description != null) {
          (rule.primary() ? primary : secondary).add(new DataUseTerm(rule.code(), description));
        }
      }
    }
    summary.setPrimary(primary);
    summary.setSecondary(secondary);
    return summary;
  }

  /**
   * List the codes of {@link #translateSummary(DataUse)}, primary then secondary, without their
   * descriptions or free-text values. Two differences from that summary: a code appears once, so a
   * primary and a secondary {@code OTHER} merge into one; and {@code DS} appears whenever there are
   * disease restrictions, while the summary drops it when no ontology label resolves.
   *
   * @param dataUse The DataUse
   * @return Distinct codes in summary order
   */
  public static List<String> summaryCodes(DataUse dataUse) {
    if (Objects.isNull(dataUse)) {
      return List.of();
    }
    return SUMMARY_RULES.stream()
        .filter(rule -> rule.applies().test(dataUse))
        .map(SummaryRule::code)
        .distinct()
        .toList();
  }

  /** The DS description, or null when no ontology label resolves. */
  private String diseaseDescription(DataUse dataUse) {
    List<String> labels = findLabelsForTermIds(dataUse.getDiseaseRestrictions());
    return labels.isEmpty() ? null : String.format(DS, String.join(", ", labels));
  }

  protected List<OntologyTerm> findTermsByIds(List<String> ids) {
    try {
      String[] idArray = ids.toArray(new String[0]);
      StreamingOutput output = ontologyDAO.findByTermIds(idArray);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      output.write(baos);
      String jsonString = baos.toString(StandardCharsets.UTF_8);
      return JsonParser.parseString(jsonString).getAsJsonArray().asList().stream()
          .map(t -> gson.fromJson(t, OntologyTerm.class))
          .toList();
    } catch (Exception e) {
      logException("Unable to retrieve term ids: " + String.join(", ", ids), e);
      return List.of();
    }
  }

  private List<String> findLabelsForTermIds(List<String> ids) {
    List<OntologyTerm> terms = findTermsByIds(ids);
    return terms.stream()
        .map(OntologyTerm::label)
        .filter(Objects::nonNull)
        .filter(r -> !r.isBlank())
        .toList();
  }
}
