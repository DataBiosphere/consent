package org.broadinstitute.consent.http.typedsearch;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Reads the typed search fixtures (DT-3848) and the key lists in {@code registry/}, which stand in
 * for the server registry until DT-3849 builds it. {@code README.md} in the fixture folder
 * describes the format.
 */
final class TypedSearchFixtures {

  /** The facet size of the contract when a facet names none. */
  static final int DEFAULT_FACET_SIZE = 10;

  static final ObjectMapper MAPPER =
      new ObjectMapper().enable(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY);
  private static final String ROOT = "fixtures/typed-search";
  private static Map<String, JsonNode> registry;

  private TypedSearchFixtures() {}

  /** The fixture files, as paths relative to the fixture folder. */
  static Stream<String> fixtures() throws IOException, URISyntaxException {
    Path root = root();
    try (Stream<Path> files = Files.walk(root)) {
      return files
          .filter(f -> f.toString().endsWith(".json"))
          .filter(f -> !root.relativize(f).startsWith("registry"))
          .filter(f -> !root.relativize(f).startsWith("self-test"))
          .map(f -> root.relativize(f).toString())
          .sorted()
          .toList()
          .stream();
    }
  }

  static JsonNode read(String name) throws IOException, URISyntaxException {
    return MAPPER.readTree(Files.readString(root().resolve(name)));
  }

  /** The registry of each view, by view name. */
  static synchronized Map<String, JsonNode> registry() {
    if (registry == null) {
      Map<String, JsonNode> views = new HashMap<>();
      try (Stream<Path> files = Files.list(root().resolve("registry"))) {
        for (Path file : files.filter(f -> f.toString().endsWith(".json")).toList()) {
          JsonNode view = MAPPER.readTree(Files.readString(file));
          views.put(view.path("view").asText(), view);
        }
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      } catch (URISyntaxException e) {
        throw new IllegalStateException(e);
      }
      registry = views;
    }
    return registry;
  }

  static Set<String> roles(JsonNode fixture) {
    return strings(fixture.path("user").path("roles"));
  }

  /** The projection of a request: the one it names, or the default of its scope and roles. */
  static String projection(JsonNode request, JsonNode view, String scope, Set<String> roles) {
    if (request.has("projection")) {
      return request.get("projection").asText();
    }
    JsonNode rule = view.path("scopeProjections").path(scope);
    for (Map.Entry<String, JsonNode> e : rule.path("roleDefaults").properties()) {
      if (roles.contains(e.getKey())) {
        return e.getValue().asText();
      }
    }
    return rule.path("default").asText();
  }

  /** Adds an error for each field that is not permitted and for each required field missing. */
  static void fields(
      JsonNode node,
      Set<String> permitted,
      Set<String> required,
      String where,
      List<String> errors) {
    if (!node.isObject()) {
      errors.add(where + " must be an object");
      return;
    }
    for (String name : names(node)) {
      if (!permitted.contains(name)) {
        errors.add(where + " has a field that is not permitted: " + name);
      }
    }
    for (String name : required) {
      if (!node.has(name)) {
        errors.add(where + " needs the field " + name);
      }
    }
  }

  static Set<String> names(JsonNode node) {
    Set<String> names = new HashSet<>();
    node.fieldNames().forEachRemaining(names::add);
    return names;
  }

  static Set<String> strings(JsonNode node) {
    Set<String> values = new HashSet<>();
    if (node != null) {
      node.forEach(v -> values.add(v.asText()));
    }
    return values;
  }

  private static Path root() throws URISyntaxException {
    return Path.of(
        Objects.requireNonNull(TypedSearchFixtures.class.getClassLoader().getResource(ROOT))
            .toURI());
  }
}
