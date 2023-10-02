package org.broadinstitute.consent.http.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.Gson;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.broadinstitute.consent.http.util.gson.GsonUtil;

//represents a multi-dataset access request
public class DarCollection {

  //This query is specific to DAR Collections, which is why it's defined here
  public static final String DAR_FILTER_QUERY_COLUMNS =
      "dar.id AS dar_id, dar.reference_id AS dar_reference_id, dar.collection_id AS dar_collection_id, "
          +
          "dar.parent_id AS dar_parent_id, dar.draft AS dar_draft, dar.user_id AS dar_userId, " +
          "dar.create_date AS dar_create_date, dar.sort_date AS dar_sort_date, dar.submission_date AS dar_submission_date, "
          +
          "dar.update_date AS dar_update_date, (regexp_replace(dar.data #>> '{}', '\\\\u0000', '', 'g'))::jsonb AS data, " +
          "(regexp_replace(dar.data #>> '{}', '\\\\u0000', '', 'g'))::jsonb ->> 'projectTitle' as projectTitle ";

  @JsonProperty
  private Integer darCollectionId;

  @JsonProperty
  private String darCode;

  @JsonProperty
  private Timestamp createDate;

  @JsonProperty
  private User createUser;

  @JsonProperty
  private Integer createUserId;

  @JsonProperty
  private Timestamp updateDate;

  @JsonProperty
  private Integer updateUserId;

  @JsonProperty
  private Map<String, DataAccessRequest> dars;

  @JsonProperty
  private Set<Dataset> datasets;

  public DarCollection() {
    this.createDate = new Timestamp(System.currentTimeMillis());
    this.datasets = new HashSet<>();
  }

  public DarCollection deepCopy() {
    Gson gson = GsonUtil.buildGson();
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

  public User getCreateUser() {
    return createUser;
  }

  public void setCreateUser(User createUser) {
    this.createUser = createUser;
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

  public Map<String, DataAccessRequest> getDars() {
    if (Objects.isNull(dars)) {
      return new HashMap<>();
    }
    return dars;
  }

  public void setDars(Map<String, DataAccessRequest> dars) {
    this.dars = dars;
  }

  public void addDar(DataAccessRequest dar) {
    if (Objects.isNull(dars)) {
      this.setDars(new HashMap<>());
    }
    if (Objects.nonNull(dar)) {
      String referenceId = dar.getReferenceId();
      DataAccessRequest savedDar = dars.get(referenceId);
      if (Objects.isNull(savedDar)) {
        dars.put(referenceId, dar);
      }
    }
  }

  public void addDataset(Dataset dataset) {
    this.datasets.add(dataset);
  }

  public void setDatasets(Set<Dataset> datasets) {
    this.datasets = datasets;
  }

  public Set<Dataset> getDatasets() {
    return datasets;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }

    DarCollection other = (DarCollection) obj;
    return new EqualsBuilder()
        .append(this.getDarCollectionId(), other.getDarCollectionId())
        .append(this.getDarCode(), other.getDarCode())
        .isEquals();
  }

  @Override
  public String toString() {
    return GsonUtil.buildGson().toJson(this);
  }
}
