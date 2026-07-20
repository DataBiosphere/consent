package org.broadinstitute.consent.http.mail.message;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.StringWriter;
import java.util.Map;
import java.util.Objects;
import org.broadinstitute.consent.http.AbstractTestHelper;
import org.broadinstitute.consent.http.configurations.FreeMarkerConfiguration;
import org.broadinstitute.consent.http.mail.freemarker.FreeMarkerTemplateHelper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;

/**
 * Base class for mail message template tests. Provides shared FreeMarker helper setup and common
 * utilities for rendering and inspecting templates.
 */
abstract class AbstractMailMessageTest extends AbstractTestHelper {

  protected FreeMarkerTemplateHelper helper;

  /** Rendered output of a FreeMarker template, exposing both the raw content and parsed DOM. */
  protected record RenderedTemplate(String content, Document document) {}

  @BeforeEach
  void setUpHelper() {
    FreeMarkerConfiguration freeMarkerConfig = new FreeMarkerConfiguration();
    freeMarkerConfig.setTemplateDirectory("/freemarker");
    freeMarkerConfig.setDefaultEncoding("UTF-8");
    helper = new FreeMarkerTemplateHelper(freeMarkerConfig);
  }

  /**
   * Renders the given message's FreeMarker template and returns both the raw HTML string and the
   * parsed Jsoup {@link Document}.
   */
  protected RenderedTemplate renderTemplate(MailMessage message, String serverUrl)
      throws Exception {
    var template = helper.getTemplate(message.getTemplateName());
    var out = new StringWriter();
    template.process(message.createModel(serverUrl), out);
    var content = out.toString();
    return new RenderedTemplate(content, Jsoup.parse(content));
  }

  /** Asserts that the message model contains the expected required fields and values. */
  protected void assertRequiredModelFields(
      MailMessage message, Map<String, Object> expectedRequiredFields) {
    Map<String, Object> createdModel = message.createModel();
    expectedRequiredFields.forEach(
        (key, value) ->
            assertEquals(value, createdModel.get(key), "Unexpected model value for " + key));
  }

  /** Returns the visible text of the element with the given {@code id}, failing if absent. */
  protected String getElementTextById(Document document, String id) {
    return Objects.requireNonNull(document.getElementById(id)).text();
  }

  /** Returns {@code true} if an element with the given {@code id} exists in the document. */
  protected boolean hasElementWithId(Document document, String id) {
    return document.getElementById(id) != null;
  }
}
