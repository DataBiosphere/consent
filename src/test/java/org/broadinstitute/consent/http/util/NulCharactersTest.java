package org.broadinstitute.consent.http.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

class NulCharactersTest {

  /**
   * The escape as it appears in a document, built rather than written out: the compiler decodes a
   * source-level escape into the character itself, comments included.
   */
  private static final String ESCAPE = "\\" + "u0000";

  private static final String BACKSLASH = "\\";

  @Test
  void testStripFromDropsTheCharacter() {
    assertEquals("Greg", NulCharacters.stripFrom("Greg\0"));
    assertEquals("Greg", NulCharacters.stripFrom("\0G\0r\0e\0g\0"));
  }

  @Test
  void testStripFromLeavesAValueWithoutOneAlone() {
    String name = "Greg";
    assertSame(name, NulCharacters.stripFrom(name));
    assertNull(NulCharacters.stripFrom(null));
  }

  @Test
  void testStripFromJsonTextDropsTheEscape() {
    assertEquals(
        "{\"n\": \"Greg\"}", NulCharacters.stripFromJsonText("{\"n\": \"Greg" + ESCAPE + "\"}"));
  }

  @Test
  void testStripFromJsonTextDropsAdjacentEscapes() {
    assertEquals(
        "{\"n\": \"Greg\"}",
        NulCharacters.stripFromJsonText("{\"n\": \"" + ESCAPE + "Greg" + ESCAPE + ESCAPE + "\"}"));
  }

  @Test
  void testStripFromJsonTextKeepsAnEvenBackslashRun() {
    // Two backslashes are an escaped backslash, so what follows them is the literal text u0000.
    String json = "{\"n\": \"Greg" + BACKSLASH.repeat(2) + "u0000\"}";
    assertEquals(json, NulCharacters.stripFromJsonText(json));

    String longerRun = "{\"n\": \"Greg" + BACKSLASH.repeat(4) + "u0000\"}";
    assertEquals(longerRun, NulCharacters.stripFromJsonText(longerRun));
  }

  @Test
  void testStripFromJsonTextKeepsTheBackslashesAnOddRunEscapes() {
    // Three: an escaped backslash, then the escape. Only the escape and its own backslash go.
    assertEquals(
        "{\"n\": \"Greg" + BACKSLASH.repeat(2) + "\"}",
        NulCharacters.stripFromJsonText("{\"n\": \"Greg" + BACKSLASH.repeat(3) + "u0000\"}"));
    assertEquals(
        "{\"n\": \"Greg" + BACKSLASH.repeat(4) + "\"}",
        NulCharacters.stripFromJsonText("{\"n\": \"Greg" + BACKSLASH.repeat(5) + "u0000\"}"));
  }

  @Test
  void testStripFromJsonTextKeepsEveryOtherEscape() {
    String json = "{\"n\": \"a" + BACKSLASH + "nb" + BACKSLASH + "u0001c" + BACKSLASH + "\\\"d\"}";
    assertEquals(json, NulCharacters.stripFromJsonText(json));
  }

  @Test
  void testStripFromJsonTextDropsTheRawCharacter() {
    assertEquals("{\"n\": \"Greg\"}", NulCharacters.stripFromJsonText("{\"n\": \"Greg\0\"}"));
  }

  @Test
  void testStripFromJsonTextKeepsABackslashRunTheTextEndsOn() {
    // The scan leaves the run because the text ran out, not because a non-backslash stopped it.
    assertEquals(BACKSLASH, NulCharacters.stripFromJsonText("\0" + BACKSLASH));
  }

  @Test
  void testStripFromJsonTextLeavesADocumentWithoutOneAlone() {
    String json = "{\"n\": \"Greg\"}";
    assertSame(json, NulCharacters.stripFromJsonText(json));
    assertNull(NulCharacters.stripFromJsonText(null));
  }

  @Test
  void testStripFromJsonTextLeavesTextThatIsNotJsonAlone() {
    // The cast, not this, is what refuses a document; a body it cannot parse arrives unchanged.
    assertSame("Hello world!", NulCharacters.stripFromJsonText("Hello world!"));
    assertEquals(BACKSLASH, NulCharacters.stripFromJsonText(BACKSLASH + "\0"));
  }
}
