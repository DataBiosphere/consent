package org.broadinstitute.consent.http.models;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * Entity representing a Data Access Committee
 */
public class Dac {

  private Integer dacId;

  private String name;

  private String description;

  private Date createDate;

  private Date updateDate;

  private List<User> chairpersons;

  private List<User> members;

  private List<Dataset> datasets;

  private final List<Integer> electionIds = new ArrayList<>();

  private final List<Integer> datasetIds = new ArrayList<>();

  private String email;

  private DataAccessAgreement associatedDaa;

  public Dac() {
  }

  public Integer getDacId() {
    return dacId;
  }

  public void setDacId(Integer dacId) {
    this.dacId = dacId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Date getCreateDate() {
    return createDate;
  }

  public void setCreateDate(Date createDate) {
    this.createDate = createDate;
  }

  public Date getUpdateDate() {
    return updateDate;
  }

  public void setUpdateDate(Date updateDate) {
    this.updateDate = updateDate;
  }

  public List<User> getChairpersons() {
    return chairpersons;
  }

  public void setChairpersons(List<User> chairpersons) {
    this.chairpersons = chairpersons;
  }

  public List<User> getMembers() {
    return members;
  }

  public void setMembers(List<User> members) {
    this.members = members;
  }

  public List<Integer> getElectionIds() {
    return electionIds;
  }

  public void addElectionId(Integer electionId) {
    this.electionIds.add(electionId);
  }

  public List<Integer> getDatasetIds() {
    return datasetIds;
  }

  public void addDatasetId(Integer datasetId) {
    this.datasetIds.add(datasetId);
  }


  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public DataAccessAgreement getAssociatedDaa() {
    return associatedDaa;
  }

  public void setAssociatedDaa(DataAccessAgreement associatedDaa) {
    this.associatedDaa = associatedDaa;
  }

  public void addDataset(Dataset dataset) {
    if (Objects.isNull(datasets)) {
      datasets = new ArrayList<>();
    }
    datasets.add(dataset);
    if (!datasetIds.contains(dataset.getDatasetId())) {
      addDatasetId(dataset.getDatasetId());
    }
  }
}
