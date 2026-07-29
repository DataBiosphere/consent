package org.broadinstitute.consent.http.models.elastic_search;

/**
 * A single line of the Elasticsearch security capability inventory.
 *
 * @param name the capability, e.g. {@code Document-level security (DLS)}
 * @param verdict whether the capability is available
 * @param detail human-readable expansion of the verdict
 * @param evidence the probe the verdict is drawn from, so a reader can re-run it by hand
 */
public record ElasticSearchCapability(
    String name, CapabilityVerdict verdict, String detail, String evidence) {}
