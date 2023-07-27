package org.broadinstitute.consent.http.models.elastic_search;

import com.google.gson.internal.LinkedTreeMap;

public class ElasticSearchHits {

  private final ElasticSearchHitsInternal hits;

  public ElasticSearchHits(ElasticSearchHitsInternal hits) {
    this.hits = hits;
  }

  public LinkedTreeMap[] getHits() {
    return hits.getHits();
  }
}
