package org.broadinstitute.consent.http.models.dto.registration.template;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Path;
import org.broadinstitute.consent.http.util.gson.GsonUtil;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * The OpenAPI examples are the only hand-written copy of the wire shape, so they are read back
 * through the response type: an example that survives a rename of a field it does not mention would
 * document an endpoint that no longer exists.
 */
class TemplateValidationResponseExamplesTest {

  private static final Path SPEC =
      Path.of("src/main/resources/assets/paths/draftStudyDatasetTemplateValidation.yaml");

  @ParameterizedTest
  @ValueSource(strings = {"valid", "invalid"})
  void testDocumentedExampleMatchesTheResponseModel(String example) throws IOException {
    JsonNode documented =
        new YAMLMapper()
            .readTree(SPEC.toFile())
            // requiredAt, not at: a missing pointer round-trips to JSON null through Gson, and
            // would pass the assertion below while documenting nothing.
            .requiredAt(
                "/post/responses/200/content/application~1json/examples/%s/value"
                    .formatted(example));

    TemplateValidationResponse parsed =
        GsonUtil.getInstance().fromJson(documented.toString(), TemplateValidationResponse.class);

    assertEquals(
        JsonParser.parseString(documented.toString()),
        JsonParser.parseString(GsonUtil.getInstance().toJson(parsed)),
        example);
  }
}
