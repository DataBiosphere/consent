package org.broadinstitute.consent.http.models.elastic_search;

import java.util.List;
import java.util.Map;

public class ElasticSearchHits {

  private final ElasticSearchHitsInternal hits;

  public ElasticSearchHits(ElasticSearchHitsInternal hits) {
    this.hits = hits;
  }

  public List<Map<String, Object>> getHits() {
    return hits.getHits();
  }
}
