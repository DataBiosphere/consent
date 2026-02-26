package org.broadinstitute.consent.http.service;

import java.util.List;
import java.util.Objects;
import org.apache.commons.collections4.CollectionUtils;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.models.OntologyEntry;
import org.broadinstitute.consent.http.util.ConsentLogger;

public class UseRestrictionConverter implements ConsentLogger {

  public UseRestrictionConverter() {}

  /**
   * This method, and its counterpart that processes a map, translates DAR questions to a DataUse
   *
   * @param dar Data Access Request
   * @return DataUse
   */
  public DataUse parseDataUsePurpose(DataAccessRequest dar) {
    DataUse dataUse = new DataUse();
    if (Objects.nonNull(dar) && Objects.nonNull(dar.getData())) {
      //
      //    Research related entries
      //
      if (Objects.nonNull(dar.getData().getAiLlmUse())
          && Boolean.TRUE.equals(dar.getData().getAiLlmUse())) {
        dataUse.setAiLlmUse(dar.getData().getAiLlmUse());
      }
      if (Objects.nonNull(dar.getData().getMethods())
          && Boolean.TRUE.equals(dar.getData().getMethods())) {
        dataUse.setMethodsResearch(dar.getData().getMethods());
      }
      if (Objects.nonNull(dar.getData().getPopulation())
          && Boolean.TRUE.equals(dar.getData().getPopulation())) {
        dataUse.setPopulation(dar.getData().getPopulation());
      }
      if (Objects.nonNull(dar.getData().getControls())
          && Boolean.TRUE.equals(dar.getData().getControls())) {
        dataUse.setControls(dar.getData().getControls());
      }

      //
      //    Diseases related entries
      //

      List<String> ontologies =
          dar.getData().getOntologies().stream().map(OntologyEntry::getId).toList();
      if (CollectionUtils.isNotEmpty(ontologies)) {
        dataUse.setDiseaseRestrictions(ontologies);
      }

      // commercial status
      if (Objects.nonNull(dar.getData().getForProfit())) {
        Boolean isForProfit = Boolean.TRUE.equals(dar.getData().getForProfit());
        dataUse.setNonProfitUse(!isForProfit);
      }

      // gender
      if (Objects.nonNull(dar.getData().getGender())) {
        String selectedGender = dar.getData().getGender();
        if (selectedGender.equalsIgnoreCase("M")) {
          dataUse.setGender("Male");
        } else if (selectedGender.equalsIgnoreCase("F")) {
          dataUse.setGender("Female");
        }
      }
      // pediatric
      if (Objects.nonNull(dar.getData().getPediatric())
          && (Boolean.TRUE.equals(dar.getData().getPediatric()))) {
        dataUse.setPediatric(true);
      }

      if (Objects.nonNull(dar.getData().getHmb())
          && (Boolean.TRUE.equals(dar.getData().getHmb()))) {
        dataUse.setHmbResearch(true);
      }

      // Other Conditions
      if (Objects.nonNull(dar.getData().getOther())
          && Boolean.TRUE.equals(dar.getData().getOther())
          && Objects.nonNull(dar.getData().getOtherText())) {
        dataUse.setOther(dar.getData().getOtherText());
      }

      if ((Objects.nonNull(dar.getData().getIllegalBehavior()))
          && Boolean.TRUE.equals(dar.getData().getIllegalBehavior())) {
        dataUse.setIllegalBehavior(dar.getData().getIllegalBehavior());
      }

      if ((Objects.nonNull(dar.getData().getSexualDiseases()))
          && Boolean.TRUE.equals(dar.getData().getSexualDiseases())) {
        dataUse.setSexualDiseases(dar.getData().getSexualDiseases());
      }

      if ((Objects.nonNull(dar.getData().getStigmatizedDiseases()))
          && Boolean.TRUE.equals(dar.getData().getStigmatizedDiseases())) {
        dataUse.setStigmatizeDiseases(dar.getData().getStigmatizedDiseases());
      }

      if ((Objects.nonNull(dar.getData().getVulnerablePopulation()))
          && Boolean.TRUE.equals(dar.getData().getVulnerablePopulation())) {
        dataUse.setVulnerablePopulations(dar.getData().getVulnerablePopulation());
      }

      if ((Objects.nonNull(dar.getData().getPsychiatricTraits()))
          && Boolean.TRUE.equals(dar.getData().getPsychiatricTraits())) {
        dataUse.setPsychologicalTraits(dar.getData().getPsychiatricTraits());
      }

      if ((Objects.nonNull(dar.getData().getNotHealth()))
          && Boolean.TRUE.equals(dar.getData().getNotHealth())) {
        dataUse.setNotHealth(dar.getData().getNotHealth());
      }
    }
    return dataUse;
  }
}
