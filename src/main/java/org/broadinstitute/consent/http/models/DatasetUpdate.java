package org.broadinstitute.consent.http.models;

import com.google.gson.Gson;
import java.util.List;
import org.broadinstitute.consent.http.util.gson.GsonUtil;

public record DatasetUpdate(
    String name,
    Integer dacId,
    List<DatasetProperty> properties
) {

  private static final Gson GSON = GsonUtil.gsonBuilderWithAdapters().create();

  public DatasetUpdate(String json) {
    this(GSON.fromJson(json, DatasetUpdate.class).getName(),
        GSON.fromJson(json, DatasetUpdate.class).getDacId(),
        GSON.fromJson(json, DatasetUpdate.class).getDatasetProperties());
  }

  public String getName() {
    return this.name;
  }

  public Integer getDacId() {
    return this.dacId;
  }

  public List<DatasetProperty> getDatasetProperties() {
    return this.properties;
  }
}
