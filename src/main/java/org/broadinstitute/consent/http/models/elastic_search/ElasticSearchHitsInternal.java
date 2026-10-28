package org.broadinstitute.consent.http.models.elastic_search;

import java.util.List;
import java.util.Map;

public class ElasticSearchHitsInternal {

  private final List<Map<String, Object>> hits;

  public ElasticSearchHitsInternal(List<Map<String, Object>> hits) {
    this.hits = hits;
  }

  /** The {@code _source} document of each hit. */
  @SuppressWarnings("unchecked")
  public List<Map<String, Object>> getHits() {
    return hits.stream().map(hit -> (Map<String, Object>) hit.get("_source")).toList();
  }
}
