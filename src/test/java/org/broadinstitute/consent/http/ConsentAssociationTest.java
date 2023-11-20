package org.broadinstitute.consent.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.jackson.Jackson;
import java.util.ArrayList;
import java.util.Collections;
import org.broadinstitute.consent.http.models.ConsentAssociation;
import org.junit.jupiter.api.Test;

/**
 * Unit Tests for ConsentAssociation object.
 * <p/>
 * Created by egolin on 9/16/14.
 */
public class ConsentAssociationTest {

  private static final ObjectMapper MAPPER = Jackson.newObjectMapper();

  private static final String CONSENT_ASSOCIATION = """
      {"associationType":"sample","elements":["SM-5678","SM-1234"]}""";

  private static ConsentAssociation buildConsentAssociation(String atype, String... elements) {
    final ArrayList<String> elem_list = new ArrayList<>();
    Collections.addAll(elem_list, elements);
    return new ConsentAssociation(atype, elem_list);
  }

  @Test
  public void serializesToJSON() throws Exception {
    final ConsentAssociation consent_association = buildConsentAssociation("sample", "SM-1234",
        "SM-5678");
    assertEquals(MAPPER.writeValueAsString(consent_association), CONSENT_ASSOCIATION);
  }

  @Test
  public void deserializesFromJSON() throws JsonProcessingException {
    final ConsentAssociation consent_association = buildConsentAssociation("sample", "SM-1234",
        "SM-5678");
    assertEquals(
        MAPPER.readValue(CONSENT_ASSOCIATION, ConsentAssociation.class),
        consent_association);
  }

  @Test
  public void testEqualsTrue() {
    final ConsentAssociation consent_assoc1 = buildConsentAssociation("sample", "SM-1234",
        "SM-5678");
    final ConsentAssociation consent_assoc2 = buildConsentAssociation("sample", "SM-1234",
        "SM-5678");
    assertEquals(consent_assoc1, consent_assoc2);
  }

  @Test
  public void testEqualsNotMatchingElements() {
    final ConsentAssociation consent_assoc1 = buildConsentAssociation("sample", "SM-1234",
        "SM-5678");
    final ConsentAssociation consent_assoc2 = buildConsentAssociation("sample", "SM-4321",
        "SM-8765");
    assertNotEquals(consent_assoc1, consent_assoc2);
  }

  @Test
  public void testEqualsNotMatchingAssociationType() {
    final ConsentAssociation consent_assoc1 = buildConsentAssociation("sample", "SM-1234",
        "SM-5678");
    final ConsentAssociation consent_assoc2 = buildConsentAssociation("sampleSet", "SM-1234",
        "SM-5678");
    assertNotEquals(consent_assoc1, consent_assoc2);
  }

  @Test
  public void testToString() {
    final ConsentAssociation consent_association = buildConsentAssociation("sample", "SM-1234",
        "SM-5678");
    assertNotNull(consent_association.toString());
  }

  @Test
  public void testIsAssociationType() {
    final ConsentAssociation consent_association = buildConsentAssociation("sample", "SM-1234",
        "SM-5678");
    assertTrue(consent_association.isAssociationType("sample"));
    assertFalse(consent_association.isAssociationType("sampleSet"));
  }
}