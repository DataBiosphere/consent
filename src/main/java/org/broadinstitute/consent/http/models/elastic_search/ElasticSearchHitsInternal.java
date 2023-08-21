package org.broadinstitute.consent.http.models.elastic_search;

import com.google.gson.internal.LinkedTreeMap;
import java.util.Arrays;

public class ElasticSearchHitsInternal {

  private final LinkedTreeMap<String, Object>[] hits;

  public ElasticSearchHitsInternal(LinkedTreeMap<String, Object>[] hits) {
    this.hits = hits;
  }

  public LinkedTreeMap[] getHits() {
    return Arrays.stream(hits).map(hit -> (LinkedTreeMap) hit.get("_source"))
        .toArray(LinkedTreeMap[]::new);
  }
}
