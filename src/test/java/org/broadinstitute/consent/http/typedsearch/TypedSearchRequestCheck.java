package org.broadinstitute.consent.http.typedsearch;

import static org.broadinstitute.consent.http.typedsearch.TypedSearchFixtures.fields;
import static org.broadinstitute.consent.http.typedsearch.TypedSearchFixtures.projection;
import static org.broadinstitute.consent.http.typedsearch.TypedSearchFixtures.strings;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Checks a typed search request against the contract ({@code docs/plans/typed-search-contract.md})
 * and the key lists in the fixture {@code registry/}: the fields of each view, the scope, filters,
 * facets, sort, page, and the projection of each scope and role.
 */
final class TypedSearchRequestCheck {

  private static final int MAX_ROWS = 10_000;
  private static final int MAX_IDS = 10_000;
  private static final int MAX_FILTERS = 1_024;
  private static final int MAX_VALUES = 1_024;
  private static final int MAX_TEXT = 1_000;
  private static final int MAX_FACET_SIZE = 10_000;
  private static final Pattern DATE = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");

  private TypedSearchRequestCheck() {}

  /**
   * Checks a request against the contract. Adds a rule that the request breaks to {@code shape} (a
   * 400), and a scope that the roles cannot use to {@code access} (a 403).
   */
  static void checkRequest(
      JsonNode request, Set<String> roles, List<String> shape, List<String> access)
      throws Exception {
    String viewName = request.path("view").asText();
    JsonNode view = TypedSearchFixtures.registry().get(viewName);
    if (view == null) {
      shape.add("unknown view: " + viewName);
      return;
    }
    // The scope first: a valid scope that the roles cannot use is a 403 before any other 400.
    String scope = checkScope(request.path("scope"), view, shape);
    if (scope != null) {
      Set<String> scopeRoles = strings(view.path("scopes").get(scope));
      if (!scopeRoles.isEmpty() && scopeRoles.stream().noneMatch(roles::contains)) {
        access.add("roles " + roles + " cannot use scope " + scope);
      }
    }
    fields(request, strings(view.get("requestFields")), Set.of("view", "scope"), "request", shape);
    if (view.has("assetTypes") != request.has("assetType")) {
      shape.add("assetType is required with the assets view, and only there");
    }
    JsonNode asset =
        view.has("assetTypes")
            ? view.path("assetTypes").get(request.path("assetType").asText())
            : null;
    if (view.has("assetTypes") && asset == null) {
      shape.add("unknown assetType: " + request.path("assetType").asText());
      return;
    }

    JsonNode search = request.path("search");
    if (!search.isMissingNode() && !search.isTextual()) {
      shape.add("search must be text");
    } else if (search.asText("").length() > MAX_TEXT) {
      shape.add("search is longer than " + MAX_TEXT);
    }
    checkFilters(request.path("filters"), filterKinds(view, asset), shape);
    checkFacets(
        request.path("facets"),
        strings(asset != null ? asset.get("facets") : view.get("facets")),
        shape);
    if (request.has("sort")) {
      JsonNode sort = request.get("sort");
      fields(sort, Set.of("key", "order"), Set.of("key"), "sort", shape);
      if (!strings(asset != null ? asset.get("sorts") : view.get("sorts"))
          .contains(sort.path("key").asText())) {
        shape.add("unknown sort key: " + sort.path("key").asText());
      }
      if (sort.has("order") && !Set.of("asc", "desc").contains(sort.get("order").asText())) {
        shape.add("sort order must be asc or desc");
      }
    }
    if (request.has("page")) {
      JsonNode page = request.get("page");
      fields(page, Set.of("offset", "limit"), Set.of(), "page", shape);
      if (page.has("offset")
          && (!page.get("offset").canConvertToExactIntegral() || page.get("offset").asLong() < 0)) {
        shape.add("page.offset must be 0 or more");
      }
      if (page.has("limit")
          && (!page.get("limit").isInt()
              || page.get("limit").asInt() < 0
              || page.get("limit").asInt() > MAX_ROWS)) {
        shape.add("page.limit must be 0 to " + MAX_ROWS);
      }
    }
    if (scope != null && view.has("scopeProjections")) {
      String projection = projection(request, view, scope, roles);
      JsonNode rule = view.path("scopeProjections").get(scope);
      if (!strings(rule.get("permitted")).contains(projection)) {
        shape.add("scope " + scope + " does not permit projection " + projection);
      }
      Set<String> projectionRoles = strings(view.path("projectionRoles").get(projection));
      if (!projectionRoles.isEmpty() && projectionRoles.stream().noneMatch(roles::contains)) {
        shape.add("roles " + roles + " cannot use projection " + projection);
      }
      if (strings(view.get("singleIdProjections")).contains(projection)
          && request.path("scope").path(scope).size() != 1) {
        shape.add("projection " + projection + " needs a list of one id");
      }
    } else if (asset != null
        && request.has("projection")
        && !"row".equals(request.get("projection").asText())) {
      shape.add("unknown projection: " + request.get("projection").asText());
    }
  }

  private static String checkScope(JsonNode scope, JsonNode view, List<String> shape) {
    if (!scope.isObject() || scope.size() != 1) {
      shape.add("scope must have exactly one key");
      return null;
    }
    String key = scope.fieldNames().next();
    JsonNode value = scope.get(key);
    if (!view.path("scopes").has(key)) {
      shape.add("scope not accepted by the view: " + key);
      return null;
    }
    boolean valid =
        switch (key) {
          case "library" -> value.isNull() || libraries().contains(value.asText(""));
          case "ownedByMe" -> value.isBoolean() && value.asBoolean();
          case "studyId" -> value.isInt() && value.asInt() > 0;
          case "dacIds", "datasetIds" -> idList(value, n -> n.isInt() && n.asInt() > 0);
          case "datasetIdentifiers" -> idList(value, n -> n.isTextual() && !n.asText().isBlank());
          default -> false;
        };
    if (!valid) {
      shape.add("scope value not valid: " + key);
      return null;
    }
    return key;
  }

  private static boolean idList(JsonNode value, Predicate<JsonNode> element) {
    if (!value.isArray() || value.isEmpty() || value.size() > MAX_IDS) {
      return false;
    }
    for (JsonNode n : value) {
      if (!element.test(n)) {
        return false;
      }
    }
    return true;
  }

  private static void checkFilters(
      JsonNode filters, Map<String, String> kinds, List<String> shape) {
    if (filters.isMissingNode()) {
      return;
    }
    if (!filters.isArray() || filters.size() > MAX_FILTERS) {
      shape.add("filters must be a list of at most " + MAX_FILTERS);
      return;
    }
    int values = 0;
    for (JsonNode filter : filters) {
      String key = filter.path("key").asText();
      String kind = kinds.get(key);
      if (kind == null) {
        shape.add("unknown filter key: " + key);
        continue;
      }
      boolean range = kind.endsWith("-range");
      fields(
          filter,
          range ? Set.of("key", "gte", "lte") : Set.of("key", "values"),
          Set.of("key"),
          "filter " + key,
          shape);
      if (range) {
        checkRange(filter, kind, key, shape);
      } else if (!filter.path("values").isArray() || filter.path("values").isEmpty()) {
        shape.add("filter " + key + " needs a list of one or more values");
      } else {
        for (JsonNode v : filter.get("values")) {
          values++;
          boolean ok =
              "boolean".equals(kind)
                  ? v.isBoolean()
                  : v.isTextual() && v.asText().length() <= MAX_TEXT;
          if (!ok) {
            shape.add("filter " + key + " has a value of the wrong type or length");
          }
        }
      }
    }
    if (values > MAX_VALUES) {
      shape.add("more than " + MAX_VALUES + " filter values");
    }
  }

  private static void checkRange(JsonNode filter, String kind, String key, List<String> shape) {
    JsonNode gte = filter.get("gte");
    JsonNode lte = filter.get("lte");
    if (gte == null && lte == null) {
      shape.add("range " + key + " needs gte, lte, or both");
      return;
    }
    for (JsonNode end : new JsonNode[] {gte, lte}) {
      if (end != null
          && ("date-range".equals(kind)
              ? !end.isTextual() || !isDate(end.asText())
              : !end.isNumber())) {
        shape.add("range " + key + " has an end of the wrong type");
        return;
      }
    }
    if (gte != null
        && lte != null
        && ("date-range".equals(kind)
            ? gte.asText().compareTo(lte.asText()) > 0
            : gte.asDouble() > lte.asDouble())) {
      shape.add("range " + key + " has gte greater than lte");
    }
  }

  private static void checkFacets(JsonNode facets, Set<String> keys, List<String> shape) {
    if (facets.isMissingNode()) {
      return;
    }
    if (!facets.isArray()) {
      shape.add("facets must be a list");
      return;
    }
    Set<String> seen = new HashSet<>();
    for (JsonNode facet : facets) {
      String key = facet.path("key").asText();
      fields(facet, Set.of("key", "size"), Set.of("key"), "facet " + key, shape);
      if (!keys.contains(key)) {
        shape.add("unknown facet key: " + key);
      }
      if (!seen.add(key)) {
        shape.add("facet key twice: " + key);
      }
      int size = facet.path("size").asInt(TypedSearchFixtures.DEFAULT_FACET_SIZE);
      if (facet.has("size") && !facet.get("size").isInt() || size < 1 || size > MAX_FACET_SIZE) {
        shape.add("facet " + key + " size must be 1 to " + MAX_FACET_SIZE);
      }
    }
  }

  /** The filter keys of a view: its own keys, and the row filter keys of every asset type. */
  private static Map<String, String> filterKinds(JsonNode view, JsonNode asset) {
    Map<String, String> kinds = new HashMap<>();
    view.path("filters").properties().forEach(e -> kinds.put(e.getKey(), e.getValue().asText()));
    JsonNode datasets = TypedSearchFixtures.registry().get("datasets");
    if (datasets != null && !view.has("filters")) {
      datasets
          .path("filters")
          .properties()
          .forEach(e -> kinds.put(e.getKey(), e.getValue().asText()));
    }
    JsonNode assets = TypedSearchFixtures.registry().get("assets");
    if (assets != null) {
      for (JsonNode type : assets.path("assetTypes")) {
        type.path("filters")
            .properties()
            .forEach(e -> kinds.put(e.getKey(), e.getValue().asText()));
      }
    }
    if (asset != null) {
      asset.path("filters").properties().forEach(e -> kinds.put(e.getKey(), e.getValue().asText()));
    }
    return kinds;
  }

  private static boolean isDate(String text) {
    try {
      return DATE.matcher(text).matches() && LocalDate.parse(text) != null;
    } catch (DateTimeParseException e) {
      return false;
    }
  }

  private static Set<String> libraries() {
    return strings(TypedSearchFixtures.registry().get("datasets").get("libraries"));
  }
}
