package org.broadinstitute.consent.http.util;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import java.io.File;
import java.io.IOException;
import org.slf4j.LoggerFactory;

/**
 * Utility class to bundle OpenAPI specifications with $ref resolution.
 *
 * <p>This class is invoked during a Maven build to create a single bundled openapi resource file.
 */
public class OpenAPIBundler {

  static final String INPUT_FILE = "src/main/resources/assets/api-docs.yaml";
  static final String OUTPUT_DIR = "target/classes/assets";

  /**
   * Bundle an OpenAPI specification file by resolving all $ref references.
   *
   * @throws IOException if file operations fail
   */
  public static void bundleOpenAPI() throws IOException {
    File inputFile = new File(INPUT_FILE);
    File outputDir = new File(OUTPUT_DIR);

    if (!inputFile.exists()) {
      throw new IOException("Input file does not exist: " + INPUT_FILE);
    }

    if (!outputDir.exists() && !outputDir.mkdirs()) {
      throw new IOException("Failed to create output directory: " + OUTPUT_DIR);
    }

    System.out.println("Parsing OpenAPI spec from: " + inputFile.getAbsolutePath());

    // Suppress "infinite loop" debug logging from OpenAPI due to circular refs
    Logger parserLogger = (Logger) LoggerFactory.getLogger("io.swagger.v3.parser");
    Level originalLevel = parserLogger.getLevel();
    parserLogger.setLevel(Level.WARN);

    // Configure parser to resolve all references
    ParseOptions parseOptions = new ParseOptions();
    parseOptions.setResolve(true);
    parseOptions.setResolveFully(true);

    // Parse the OpenAPI specification
    OpenAPIV3Parser parser = new OpenAPIV3Parser();
    SwaggerParseResult result =
        parser.readLocation(inputFile.getAbsolutePath(), null, parseOptions);

    // Restore original logging level
    parserLogger.setLevel(originalLevel);

    // Check for messages/warnings
    if (result.getMessages() != null && !result.getMessages().isEmpty()) {
      System.out.println("Warning messages:");
      result.getMessages().forEach(msg -> System.out.println("  - " + msg));
    }

    // Ensure we got a valid OpenAPI object
    if (result.getOpenAPI() == null) {
      throw new IOException("Failed to parse OpenAPI specification. Check the messages above.");
    }

    // Write bundled output in both JSON and YAML formats
    File jsonFile = new File(outputDir, "openapi.json");
    File yamlFile = new File(outputDir, "openapi.yaml");

    Json.pretty().writeValue(jsonFile, result.getOpenAPI());
    Yaml.pretty().writeValue(yamlFile, result.getOpenAPI());
  }

  public static void main(String[] args) {
    try {
      bundleOpenAPI();
      System.out.println("✓ OpenAPI bundling completed successfully.");
    } catch (Exception e) {
      System.err.println("Error bundling OpenAPI: " + e.getMessage());
      System.exit(1);
    }
  }
}
