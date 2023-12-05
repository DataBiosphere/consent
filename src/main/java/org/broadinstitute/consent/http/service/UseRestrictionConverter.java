package org.broadinstitute.consent.http.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.commons.collections4.CollectionUtils;
import org.broadinstitute.consent.http.configurations.ServicesConfiguration;
import org.broadinstitute.consent.http.enumeration.DataUseTranslationType;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.models.OntologyEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("WeakerAccess")
public class UseRestrictionConverter {

  private static final Logger LOGGER = LoggerFactory.getLogger("UseRestrictionConverter");
  private static final ObjectMapper mapper = new ObjectMapper();
  private final ServicesConfiguration servicesConfiguration;
  private final Client client;

  public UseRestrictionConverter(Client client, ServicesConfiguration config) {
    this.client = client;
    this.servicesConfiguration = config;
  }

  /**
   * This method, and its counterpart that processes a DataAccessRequest, translates DAR questions
   * to a DataUse
   *
   * @param json String in json form
   * @return DataUse
   */
  @SuppressWarnings("unchecked")
  public DataUse parseDataUsePurpose(String json) {
    Map<String, Object> form = parseAsMap(json);
    DataUse dataUse = new DataUse();

    //
    //    Research related entries
    //
    if (Boolean.parseBoolean(form.getOrDefault("methods", false).toString())) {
      dataUse.setMethodsResearch(true);
    }
    if (Boolean.parseBoolean(form.getOrDefault("population", false).toString())) {
      dataUse.setPopulationStructure(true);
    }
    if (Boolean.parseBoolean(form.getOrDefault("controls", false).toString())) {
      dataUse.setControlSetOption("Yes");
    }

    //
    //    Diseases related entries
    //
    ArrayList<HashMap<String, String>> ontologies = (ArrayList<HashMap<String, String>>) form.get(
        "ontologies");
    if (CollectionUtils.isNotEmpty(ontologies)) {
      List<String> restrictions = ontologies
          .stream()
          .filter(Objects::nonNull)
          .filter(hashMap -> hashMap.containsKey("id"))
          .map(hashMap -> hashMap.get("id"))
          .collect(Collectors.toList());
      if (!restrictions.isEmpty()) {
        dataUse.setDiseaseRestrictions(restrictions);
      }
    }

    //
    //    gender, age and commercial status entries
    //
    boolean forProfitOnly = Boolean.parseBoolean(form.getOrDefault("forProfit", false).toString());
    dataUse.setCommercialUse(forProfitOnly);

    // limited to one gender + children analysis
    boolean oneGenderOnly = Boolean.parseBoolean(form.getOrDefault("onegender", false).toString());
    String selectedGender = (String) form.getOrDefault("gender", "X");
    boolean pediatricsOnly = Boolean.parseBoolean(form.getOrDefault("pediatric", false).toString());

    if (oneGenderOnly) {
      if (selectedGender.equalsIgnoreCase("M")) {
        dataUse.setGender("Male");
      } else if (selectedGender.equalsIgnoreCase("F")) {
        dataUse.setGender("Female");
      }
    }

    if (pediatricsOnly) {
      dataUse.setPediatric(true);
    }

    if (Boolean.parseBoolean(form.getOrDefault("poa", false).toString())) {
      dataUse.setPopulationOriginsAncestry(true);
    }

    if (Boolean.parseBoolean(form.getOrDefault("hmb", false).toString())) {
      dataUse.setHmbResearch(true);
    }

    return dataUse;
  }

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
      if (Objects.nonNull(dar.getData().getMethods()) && dar.getData().getMethods()) {
        dataUse.setMethodsResearch(dar.getData().getMethods());
      }
      if (Objects.nonNull(dar.getData().getPopulation()) && dar.getData().getPopulation()) {
        dataUse.setPopulation(dar.getData().getPopulation());
      }
      if (Objects.nonNull(dar.getData().getControls()) && dar.getData().getControls()) {
        dataUse.setControls(dar.getData().getControls());
      }

      //
      //    Diseases related entries
      //

      List<String> ontologies = dar.getData()
        .getOntologies()
        .stream()
        .map(OntologyEntry::getId)
        .toList();
      if (CollectionUtils.isNotEmpty(ontologies)) {
        dataUse.setDiseaseRestrictions(ontologies);
      }

      // commercial status
      if (Objects.nonNull(dar.getData().getForProfit()) && dar.getData().getForProfit()) {
        dataUse.setNonProfitUse(!dar.getData().getForProfit());
        dataUse.setCommercialUse(dar.getData().getForProfit());
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
      if (Objects.nonNull(dar.getData().getPediatric())) {
        if (dar.getData().getPediatric()) {
          dataUse.setPediatric(true);
        }
      }

      if (Objects.nonNull(dar.getData().getHmb())) {
        if (dar.getData().getHmb()) {
          dataUse.setHmbResearch(true);
        }
      }

      // Other Conditions
      if (Objects.nonNull(dar.getData().getOther())) {
        dataUse.setOtherRestrictions(dar.getData().getOther());
      }
      if (Objects.nonNull(dar.getData().getOtherText())) {
        dataUse.setOther(dar.getData().getOtherText());
      }

      if (Objects.nonNull(dar.getData().getNotHealth())) {
        dataUse.setNotHealth(dar.getData().getNotHealth());
        dataUse.setNonBiomedical(dar.getData().getNotHealth());
      }

      if ((Objects.nonNull(dar.getData().getIllegalBehavior())) && dar.getData().getIllegalBehavior()) {
        dataUse.setIllegalBehavior(dar.getData().getIllegalBehavior());
      }

      if ((Objects.nonNull(dar.getData().getSexualDiseases())) && dar.getData().getSexualDiseases()) {
        dataUse.setSexualDiseases(dar.getData().getSexualDiseases());
      }

      if ((Objects.nonNull(dar.getData().getStigmatizedDiseases())) && dar.getData().getStigmatizedDiseases()) {
        dataUse.setStigmatizeDiseases(dar.getData().getStigmatizedDiseases());
      }

      if ((Objects.nonNull(dar.getData().getVulnerablePopulation())) && dar.getData().getVulnerablePopulation()) {
        dataUse.setVulnerablePopulations(dar.getData().getVulnerablePopulation());
      }

      if ((Objects.nonNull(dar.getData().getPsychiatricTraits())) && dar.getData().getPsychiatricTraits()) {
        dataUse.setPsychologicalTraits(dar.getData().getPsychiatricTraits());
      }

      if ((Objects.nonNull(dar.getData().getNotHealth())) && dar.getData().getNotHealth()) {
        dataUse.setNotHealth(dar.getData().getNotHealth());
      }

    }
    return dataUse;
  }

  public String translateDataUse(DataUse dataUse, DataUseTranslationType type) {
    WebTarget target = client.target(
        servicesConfiguration.getOntologyURL() + "translate?for=" + type.getValue());
    Response response = target.request(MediaType.APPLICATION_JSON)
        .post(Entity.json(dataUse.toString()));
    if (response.getStatus() == 200) {
      try {
        return response.readEntity(String.class);
      } catch (Exception e) {
        LOGGER.error("Error parsing response from Ontology service: " + e);
      }
    }
    LOGGER.error("Error response from Ontology service: " + response.readEntity(String.class));
    return null;
  }

  public Map<String, Object> parseAsMap(String str) {
    ObjectReader reader = mapper.readerFor(Map.class);
    try {
      return reader.readValue(str);
    } catch (IOException e) {
      LOGGER.debug("While parsing as a Map ...", e);
    }
    return null;
  }
}
