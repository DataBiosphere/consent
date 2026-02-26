package org.broadinstitute.consent.http.models.elastic_search;

import com.google.gson.annotations.SerializedName;

public record ElasticSearchVersion(
    String number,
    @SerializedName("build_flavor") String buildFlavor,
    @SerializedName("build_type") String buildType,
    @SerializedName("build_hash") String buildHash,
    @SerializedName("build_date") String buildDate,
    @SerializedName("build_snapshot") Boolean buildSnapshot,
    @SerializedName("lucene_version") String luceneVersion,
    @SerializedName("minimum_wire_compatibility_version") String minimumWireCompatibilityVersion,
    @SerializedName("minimum_index_compatibility_version") String minimumIndexCompatibilityVersion,
    String distribution) {}
