package org.broadinstitute.consent.http.util;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.core.util.Yaml;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

/**
 * Utility class to bundle OpenAPI specifications with $ref resolution.
 *
 * This class is invoked during a Maven build to create a single bundled openapi
 * resource file.
 */
public class OpenAPIBundler {

    /**
     * Bundle an OpenAPI specification file by resolving all $ref references.
     *
     * @param inputFilePath Path to the input OpenAPI YAML file
     * @param outputDirPath Path to the output directory
     * @throws IOException if file operations fail
     */
    public static void bundleOpenAPI(String inputFilePath, String outputDirPath) throws IOException {
        File inputFile = new File(inputFilePath);
        File outputDir = new File(outputDirPath);

        if (!inputFile.exists()) {
            throw new IOException("Input file does not exist: " + inputFilePath);
        }

        if (!outputDir.exists() && !outputDir.mkdirs()) {
            throw new IOException("Failed to create output directory: " + outputDirPath);
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
        SwaggerParseResult result = parser.readLocation(inputFile.getAbsolutePath(), null, parseOptions);

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
        if (args.length < 2) {
            System.err.println("Usage: OpenAPIBundler <input-yaml-file> <output-directory>");
            System.exit(1);
        }

        String inputFilePath = args[0];
        String outputDirPath = args[1];

        try {
            bundleOpenAPI(inputFilePath, outputDirPath);
            System.out.println("✓ OpenAPI bundling completed successfully.");
        } catch (Exception e) {
            System.err.println("Error bundling OpenAPI: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
