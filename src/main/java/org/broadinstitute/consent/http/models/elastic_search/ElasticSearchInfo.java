package org.broadinstitute.consent.http.models.elastic_search;

import com.google.gson.annotations.SerializedName;

public record ElasticSearchInfo(
    String name,
    @SerializedName("cluster_name") String clusterName,
    @SerializedName("cluster_uuid") String clusterUuid,
    ElasticSearchVersion version,
    String tagline) {}
