package org.broadinstitute.consent.http.util;

import java.util.List;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.OntologyEntry;

public class DataAccessRequestDataBuilder {

  private final DataAccessRequestData data;

  public DataAccessRequestDataBuilder() {
    data = new DataAccessRequestData();
  }

  public DataAccessRequestData build() {
    return data;
  }

  public DataAccessRequestDataBuilder setHmb(boolean b) {
    data.setHmb(b);
    return this;
  }

  public DataAccessRequestDataBuilder setDiseases(boolean b) {
    data.setDiseases(b);
    return this;
  }

  public DataAccessRequestDataBuilder setOther(boolean b) {
    data.setOther(b);
    return this;
  }

  public DataAccessRequestDataBuilder setOtherText(String b) {
    data.setOtherText(b);
    return this;
  }

  public DataAccessRequestDataBuilder setControls(boolean b) {
    data.setControls(b);
    return this;
  }

  public DataAccessRequestDataBuilder setPopulation(boolean b) {
    data.setPopulation(b);
    return this;
  }

  public DataAccessRequestDataBuilder setForProfit(boolean b) {
    data.setForProfit(b);
    return this;
  }

  public DataAccessRequestDataBuilder setPediatric(boolean b) {
    data.setPediatric(b);
    return this;
  }

  public DataAccessRequestDataBuilder setVulnerablePopulation(boolean b) {
    data.setVulnerablePopulation(b);
    return this;
  }

  public DataAccessRequestDataBuilder setIllegalBehavior(boolean b) {
    data.setIllegalBehavior(b);
    return this;
  }

  public DataAccessRequestDataBuilder setSexualDiseases(boolean b) {
    data.setSexualDiseases(b);
    return this;
  }

  public DataAccessRequestDataBuilder setPsychiatricTraits(boolean b) {
    data.setPsychiatricTraits(b);
    return this;
  }

  public DataAccessRequestDataBuilder setNotHealth(boolean b) {
    data.setNotHealth(b);
    return this;
  }

  public DataAccessRequestDataBuilder setStigmatizedDiseases(boolean b) {
    data.setStigmatizedDiseases(b);
    return this;
  }

  public DataAccessRequestDataBuilder setAddiction(boolean b) {
    data.setAddiction(b);
    return this;
  }

  public DataAccessRequestDataBuilder setGender(String b) {
    data.setGender(b);
    return this;
  }

  public DataAccessRequestDataBuilder setOntologies(List<OntologyEntry> l) {
    data.setOntologies(l);
    return this;
  }
}
