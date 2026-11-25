package org.broadinstitute.consent.http.typedsearch;

import static org.broadinstitute.consent.http.typedsearch.TypedSearchFixtures.fields;
import static org.broadinstitute.consent.http.typedsearch.TypedSearchFixtures.names;
import static org.broadinstitute.consent.http.typedsearch.TypedSearchFixtures.projection;
import static org.broadinstitute.consent.http.typedsearch.TypedSearchFixtures.strings;

import com.fasterxml.jackson.databind.JsonNode;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Checks the body of a typed search response against the contract ({@code
 * docs/plans/typed-search-contract.md}): the page, the total, the row fields of the projection, the
 * asset key, the facets, and the totals of the counts view.
 */
final class TypedSearchResponseCheck {

  private static final int DEFAULT_LIMIT = 25;

  private TypedSearchResponseCheck() {}

  /** Adds each rule of the contract that the body breaks to {@code errors}. */
  static void checkBody(JsonNode request, Set<String> roles, JsonNode body, List<String> errors) {
    String viewName = request.path("view").asText();
    JsonNode view = TypedSearchFixtures.registry().get(viewName);
    if (view == null || request.path("scope").size() != 1) {
      errors.add("the request has no known view or no single scope; see TypedSearchRequestTest");
      return;
    }
    if (view.has("assetTypes")
        && !view.path("assetTypes").has(request.path("assetType").asText())) {
      errors.add("the request has no known assetType; see TypedSearchRequestTest");
      return;
    }
    if (!viewName.equals(body.path("view").asText())) {
      errors.add("body.view must be " + viewName);
    }
    if (view.has("totals")) {
      fields(body, Set.of("view", "totals"), Set.of("view", "totals"), "body", errors);
      if (!strings(view.get("totals")).equals(names(body.path("totals")))) {
        errors.add("totals must have exactly the totals of the counts view");
      }
      body.path("totals")
          .forEach(
              t -> {
                if (!t.isInt() || t.asInt() < 0) {
                  errors.add("a total must be a whole number, 0 or more");
                }
              });
      return;
    }
    fields(
        body,
        Set.of("view", "total", "page", "results", "facets"),
        Set.of("view", "total", "page", "results"),
        "body",
        errors);
    long offset = request.path("page").path("offset").asLong(0);
    int limit = request.path("page").path("limit").asInt(DEFAULT_LIMIT);
    if (body.path("page").path("offset").asLong(-1) != offset
        || body.path("page").path("limit").asInt(-1) != limit) {
      errors.add("body.page must be the page of the request");
    }
    JsonNode total = body.path("total");
    JsonNode results = body.path("results");
    if (!total.canConvertToExactIntegral() || total.asLong() < 0) {
      errors.add("body.total must be a whole number, 0 or more");
    } else if (!results.isArray()
        || results.size() > limit
        || results.size() > Math.max(0, total.asLong() - offset)) {
      errors.add("results must hold at most limit rows, and at most total minus offset");
    }
    Set<String> allowed = rowFields(request, view, roles);
    String assetType = request.path("assetType").asText(null);
    for (JsonNode row : results) {
      if (!row.isObject()) {
        errors.add("each row must be an object");
        continue;
      }
      Set<String> paths = new HashSet<>();
      flatten("", row, allowed, paths);
      paths.removeAll(allowed);
      if (!paths.isEmpty()) {
        errors.add("row fields outside the projection: " + paths);
      }
      if (hasNull(row)) {
        errors.add("a field without a value is absent, not null (contract section 7)");
      }
      JsonNode dataUse = row.path("dataUse");
      if (allowed.contains("dataUse")
          && !(dataUse.path("primary").isArray() && dataUse.path("secondary").isArray())) {
        errors.add("dataUse is always { primary: [...], secondary: [...] } (contract section 7)");
      }
      if (assetType != null) {
        checkAssetKey(row, view.path("assetTypes").get(assetType), errors);
      }
    }
    checkFacetBody(request.path("facets"), body.path("facets"), errors);
  }

  /** The value of a bucket as UTF-8 bytes, which the contract compares unsigned ("C"). */
  private static byte[] bytes(JsonNode bucket) {
    return bucket.path("value").asText().getBytes(StandardCharsets.UTF_8);
  }

  private static boolean hasNull(JsonNode node) {
    if (node.isNull()) {
      return true;
    }
    for (JsonNode child : node) {
      if (hasNull(child)) {
        return true;
      }
    }
    return false;
  }

  private static void checkAssetKey(JsonNode row, JsonNode asset, List<String> errors) {
    for (String field : List.of("studyId", "studyName", "assetKey")) {
      if (!row.has(field)) {
        errors.add("an asset row needs " + field);
      }
    }
    String key = row.path("assetKey").asText();
    String form = asset.path("assetKey").asText();
    JsonNode id = row.path(asset.path("idField").asText());
    if (!key.matches(form + ":\\d+")) {
      errors.add("assetKey " + key + " must be " + form + ":<n>");
    } else if ("id".equals(form) && (!id.isInt() || !key.equals("id:" + id.asInt()))) {
      errors.add("assetKey " + key + " must use the record id of the row");
    }
  }

  private static void checkFacetBody(JsonNode requested, JsonNode facets, List<String> errors) {
    Set<String> keys = new HashSet<>();
    requested.forEach(f -> keys.add(f.path("key").asText()));
    if (!keys.equals(names(facets))) {
      errors.add("body.facets must have one entry for each requested facet key");
      return;
    }
    for (JsonNode facet : requested) {
      JsonNode buckets = facets.get(facet.path("key").asText());
      int size = facet.path("size").asInt(TypedSearchFixtures.DEFAULT_FACET_SIZE);
      if (buckets == null || !buckets.isArray() || buckets.size() > size) {
        errors.add("facet " + facet.path("key").asText() + " has more buckets than its size");
        continue;
      }
      JsonNode previous = null;
      for (JsonNode bucket : buckets) {
        fields(bucket, Set.of("value", "count"), Set.of("value", "count"), "bucket", errors);
        if (bucket.path("value").isNull()
            || !bucket.path("count").isInt()
            || bucket.path("count").asInt() < 1) {
          errors.add("a bucket has a value that is not null and a count of 1 or more");
        }
        if (previous != null
            && (previous.path("count").asInt() < bucket.path("count").asInt()
                || previous.path("count").asInt() == bucket.path("count").asInt()
                    && Arrays.compareUnsigned(bytes(previous), bytes(bucket)) > 0)) {
          errors.add("buckets must be by count descending, then by value");
        }
        previous = bucket;
      }
    }
  }

  private static Set<String> rowFields(JsonNode request, JsonNode view, Set<String> roles) {
    if (view.has("assetTypes")) {
      Set<String> fields = new HashSet<>(strings(view.get("rowFields")));
      fields.addAll(
          strings(view.path("assetTypes").path(request.path("assetType").asText()).get("fields")));
      return fields;
    }
    String scope = request.path("scope").fieldNames().next();
    return strings(view.path("projections").get(projection(request, view, scope, roles)));
  }

  private static void flatten(String prefix, JsonNode node, Set<String> allowed, Set<String> out) {
    node.properties()
        .forEach(
            e -> {
              String path = prefix + e.getKey();
              boolean nested =
                  e.getValue().isObject()
                      && allowed.stream().anyMatch(a -> a.startsWith(path + "."));
              if (nested) {
                flatten(path + ".", e.getValue(), allowed, out);
              } else {
                out.add(path);
              }
            });
  }
}
