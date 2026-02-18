package org.broadinstitute.consent.http.matching;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import jakarta.ws.rs.core.StreamingOutput;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.consent.http.enumeration.DataUseTranslationType;
import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.service.ontology.OntologyDAO;
import org.broadinstitute.consent.http.service.ontology.OntologyTerm;
import org.broadinstitute.consent.http.util.ConsentLogger;
import org.broadinstitute.consent.http.util.gson.GsonUtil;

public class TranslationUtil implements ConsentLogger {

  public static final String DATASET_HEADER =
      "Samples are restricted for use under the following conditions:";
  public static final String PURPOSE_HEADER =
      "Research is limited to samples restricted for use under the following conditions:";
  protected static final String FEMALE = "Female";
  protected static final String MALE = "Male";
  private static final String GRU = "Data is available for general research use. [GRU]";
  public static final String DS = "Data use is limited for studying: %s [DS]";
  public static final String HMB = "Data is limited for health/medical/biomedical research. [HMB]";
  private static final String POA =
      "Future use for population origins or ancestry research is prohibited. [POA]";
  public static final String NMDS =
      "Data use for methods development research ONLY within the bounds of other data use limitations. [NMDS]";
  public static final String NCU = "Commercial use prohibited. [NCU]";
  private static final String OTHER = "Other restrictions: %s.";
  private static final String SECONDARY_OTHER = "Secondary other restrictions: %s.";
  private static final String ETHICS_APPROVAL = "Local ethics committee approval is required.";
  private static final String COLLABORATION_REQUIRED =
      "Collaboration with the primary study investigators required. [COL]";
  private static final String GEO_RESTRICTION = "Geographical restrictions: %s.";
  private static final String GSO = "Future use is limited to genetic studies only [GSO]";
  private static final String PUB_REQUIRED =
      "Publishing results of studies using the data available to the larger scientific community is required";
  private static final String PUB_MORATORIUM =
      "Publishing moratorium until '%s' is in effect. [MOR]";
  public static final String NCTRL =
      "Future use as a control set for diseases other than those specified is prohibited. [NCTRL]";
  private static final String RS_M = "Data use is limited to research on males. [RS-M]";
  private static final String RS_FM = "Data use is limited to research on females. [RS-FM]";
  private static final String RS_PD = "Data use is limited to pediatric research. [RS-PD]";
  private static final String POP =
      "Future use for study variation in the general population (e.g. calling variants and/or studying their distribution). [POP]";

  private final OntologyDAO ontologyDAO;
  private final Gson gson = GsonUtil.getInstance();

  public TranslationUtil(OntologyDAO ontologyDAO) {
    this.ontologyDAO = ontologyDAO;
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
      List<OntologyTerm> terms = findTermsByIds(dataUse.getDiseaseRestrictions());
      List<String> labels = new ArrayList<>(terms.stream().map(OntologyTerm::label).toList());
      if (!labels.isEmpty()) {
        String dsRestrictions =
            labels.stream()
                .filter(Objects::nonNull)
                .filter(r -> !r.isEmpty())
                .collect(Collectors.joining(", "));
        summary.add(String.format(DS, dsRestrictions));
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
      summary.add(NCU);
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
}
