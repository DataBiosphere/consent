package org.broadinstitute.consent.http.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.Gson;
import org.apache.commons.lang3.builder.EqualsBuilder;

import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Map;

//represents a multi-dataset access request
public class DarCollection {

  public static Map<String, String> acceptableSortFields = Map.of(
      "projectTitle", "projectTitle",
      "researcher", "researcher",
      "darCode", "dar_code",
      "institution", "institution_name"
  );

  public static String defaultTokenSortField = "darCode";

  @JsonProperty
  private Integer darCollectionId;

  @JsonProperty
  private String darCode;

  @JsonProperty
  private Timestamp createDate;

  @JsonProperty
  private Integer createUserId;

  @JsonProperty
  private Timestamp updateDate;

  @JsonProperty
  private Integer updateUserId;

  @JsonProperty
  private List<DataAccessRequest> dars;

  @JsonProperty
  private Set<DataSet> datasets;

  @JsonProperty
  private List<Election> elections;

  public DarCollection() {
    this.createDate = new Timestamp(System.currentTimeMillis());
    this.datasets = new HashSet<>();
  }

  public DarCollection deepCopy() {
    Gson gson = new Gson();
    String json = gson.toJson(this);
    return gson.fromJson(json, DarCollection.class);
  }

  public Integer getDarCollectionId() {
    return darCollectionId;
  }

  public void setDarCollectionId(Integer darCollectionId) {
    this.darCollectionId = darCollectionId;
  }

  public String getDarCode() {
    return darCode;
  }

  public void setDarCode(String darCode) {
    this.darCode = darCode;
  }

  public Timestamp getCreateDate() {
    return createDate;
  }

  public void setCreateDate(Timestamp createDate) {
    this.createDate = createDate;
  }

  public Integer getCreateUserId() {
    return createUserId;
  }

  public void setCreateUserId(Integer createUserId) {
    this.createUserId = createUserId;
  }

  public Date getUpdateDate() {
    return updateDate;
  }

  public void setUpdateDate(Timestamp updateDate) {
    this.updateDate = updateDate;
  }

  public Integer getUpdateUserId() {
    return updateUserId;
  }

  public void setUpdateUserId(Integer updateUserId) {
    this.updateUserId = updateUserId;
  }

  public List<DataAccessRequest> getDars() {
    if (Objects.isNull(dars)) {
      return new ArrayList<>();
    }
    return dars;
  }

  public void setDars(List<DataAccessRequest> dars) {
    this.dars = dars;
  }

  public void addDar(DataAccessRequest dar) {
    if (Objects.isNull(dars)) {
      this.setDars(new ArrayList<>());
    }
    dars.add(dar);
  }

  public void addDataset(DataSet dataset) {
    this.datasets.add(dataset);
  }

  public void setDatasets(Set<DataSet> datasets) {
    this.datasets = datasets;
  }

  public Set<DataSet> getDatasets() {
    return datasets;
  }

  public void setElections(List<Election> elections) {
    this.elections = elections;
  }

  public List<Election> getElections() {
    return elections;
  }

  public void addElection(Election election) {
    if(Objects.isNull(this.elections)) {
      this.elections = new ArrayList<>();
    }
    elections.add(election);
  }

  @Override
  public boolean equals(Object obj){
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;

    DarCollection other = (DarCollection) obj;
    return new EqualsBuilder()
      .append(this.getDarCollectionId(), other.getDarCollectionId())
      .append(this.getDarCode(), other.getDarCode())
      .isEquals();
  }

  @Override
  public String toString() {
    return new Gson().toJson(this);
  }
}
