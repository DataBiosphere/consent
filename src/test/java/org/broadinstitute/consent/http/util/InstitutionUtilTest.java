package org.broadinstitute.consent.http.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.Gson;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.broadinstitute.consent.http.models.Institution;
import org.broadinstitute.consent.http.util.gson.GsonUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InstitutionUtilTest {

  private InstitutionUtil util;

  private Institution initMockInstitution() {
    Institution mockInstitution = new Institution();
    mockInstitution.setName("Test Name");
    mockInstitution.setCreateDate(new Date());
    mockInstitution.setCreateUserId(1);
    mockInstitution.setUpdateDate(new Date());
    mockInstitution.setUpdateUserId(1);
    mockInstitution.setId(1);
    return mockInstitution;
  }

  private void initUtil() {
    util = new InstitutionUtil();
  }

  @Test
  void testGsonBuilderAdmin() {
    initUtil();
    Institution mockInstitution = initMockInstitution();
    Gson builder = util.getGsonBuilder(true);
    String json = builder.toJson(mockInstitution);
    Institution deserialized = GsonUtil.getInstance().fromJson(json, Institution.class);
    assertEquals(mockInstitution.getName(), deserialized.getName());
    assertEquals(mockInstitution.getCreateUserId(), deserialized.getCreateUserId());
    assertEquals(mockInstitution.getUpdateUserId(), deserialized.getUpdateUserId());
    assertEquals(mockInstitution.getCreateDate().toString(),
        deserialized.getCreateDate().toString());
    assertEquals(mockInstitution.getUpdateDate().toString(),
        deserialized.getUpdateDate().toString());
    assertEquals(mockInstitution.getId(), deserialized.getId());
  }

  @Test
  void testGsonBuilderNonAdmin() {
    initUtil();
    Institution mockInstitution = initMockInstitution();
    Gson builder = util.getGsonBuilder(false);
    String json = builder.toJson(mockInstitution);
    assertEquals("{\"id\":1,\"name\":\"Test Name\"}", json);
  }

  @Test
  void testIsValidInstitutionDomainValidDomains() {
    assertTrue(InstitutionUtil.isValidInstitutionDomain("foo.com"));
    assertTrue(InstitutionUtil.isValidInstitutionDomain("bar.org"));
    assertTrue(InstitutionUtil.isValidInstitutionDomain("baz.edu"));
    assertTrue(InstitutionUtil.isValidInstitutionDomain("foo.bar.dev"));
    assertTrue(InstitutionUtil.isValidInstitutionDomain("nih.gov"));
    assertTrue(InstitutionUtil.isValidInstitutionDomain("broadinstitute.org"));
    assertTrue(InstitutionUtil.isValidInstitutionDomain("with-some-dashes.com"));
  }

  @Test
  void testIsValidInstitutionDomainInvalidDomains() {
    // Invalid domains
    assertFalse(InstitutionUtil.isValidInstitutionDomain("invalid"));
    assertFalse(InstitutionUtil.isValidInstitutionDomain("test."));
    assertFalse(InstitutionUtil.isValidInstitutionDomain(".com"));

    // Empty or null
    assertFalse(InstitutionUtil.isValidInstitutionDomain(""));
    assertFalse(InstitutionUtil.isValidInstitutionDomain("   "));
    assertFalse(InstitutionUtil.isValidInstitutionDomain(null));

    // Invalid characters
    assertFalse(InstitutionUtil.isValidInstitutionDomain("test@domain.com"));
    assertFalse(InstitutionUtil.isValidInstitutionDomain("domain withaspace.com"));
    assertFalse(InstitutionUtil.isValidInstitutionDomain("domain_with_underscores.com"));
  }

  @Test
  void testGetInvalidInstitutionDomainsAllValid() {
    initUtil();
    Institution institution = new Institution();
    institution.setDomains(Arrays.asList("example.com", "test.edu", "google.org"));

    List<String> invalidDomains = InstitutionUtil.getInvalidInstitutionDomains(institution);
    assertTrue(invalidDomains.isEmpty());
  }

  @Test
  void testGetInvalidInstitutionDomainsMixedValidity() {
    initUtil();
    Institution institution = new Institution();
    institution.setDomains(Arrays.asList(
        "broadinstitute.org", // valid
        "uconn.edu",          // valid
        "sub.example.com",    // valid (subdomain)
        "www.test.edu",       // valid (subdomain)
        "invalid",            // invalid (no TLD)
        "",                   // invalid (empty)
        null                  // invalid (null)
    ));

    List<String> invalidDomains = InstitutionUtil.getInvalidInstitutionDomains(institution);
    assertEquals(3, invalidDomains.size());
    assertTrue(invalidDomains.contains("invalid"));
    assertTrue(invalidDomains.contains(""));
    assertTrue(invalidDomains.contains(null));
  }

  @Test
  void testGetInvalidInstitutionDomainsEmptyList() {
    initUtil();
    Institution institution = new Institution();
    institution.setDomains(Collections.emptyList());

    List<String> invalidDomains = InstitutionUtil.getInvalidInstitutionDomains(institution);
    assertTrue(invalidDomains.isEmpty());
  }

  @Test
  void testCanonicalizeInstitutionNameValidInput() {
    assertEquals("Harvard University", InstitutionUtil.canonicalizeInstitutionName("Harvard University"));
    assertEquals("University of Connecticut", InstitutionUtil.canonicalizeInstitutionName("  University of Connecticut  "));
  }

  @Test
  void testCanonicalizeInstitutionNameCurlyQuotes() {
    // Test left/right double quotation marks
    assertEquals("A 'Real' University", InstitutionUtil.canonicalizeInstitutionName("A \"Real\" University"));

    // Test left/right single quotation marks
    assertEquals("St. John's University", InstitutionUtil.canonicalizeInstitutionName("St. John’s University"));
    assertEquals("Mount St. Mary's College", InstitutionUtil.canonicalizeInstitutionName("Mount St. Mary's College"));

    // Test low-9 quotation marks
    assertEquals("Test 'Quote' School", InstitutionUtil.canonicalizeInstitutionName("Test ‚Quote„ School"));
  }

  @Test
  void testCanonicalizeInstitutionNameDoubleToSingleQuotes() {
    assertEquals("The 'Elite' University", InstitutionUtil.canonicalizeInstitutionName("The \"Elite\" University"));
    assertEquals("Harvard 'School' of Medicine", InstitutionUtil.canonicalizeInstitutionName("Harvard \"School\" of Medicine"));
  }

  @Test
  void testCanonicalizeInstitutionNameInvalidInput() {
    assertNull(InstitutionUtil.canonicalizeInstitutionName(null));
    assertNull(InstitutionUtil.canonicalizeInstitutionName(""));
    assertNull(InstitutionUtil.canonicalizeInstitutionName("   "));
    assertNull(InstitutionUtil.canonicalizeInstitutionName("\t\n"));
  }

  @Test
  void testCanonicalizeInstitutionNameWhitespace() {
    assertEquals("Trimmed University", InstitutionUtil.canonicalizeInstitutionName("  Trimmed University  "));
    assertEquals("Spaced College", InstitutionUtil.canonicalizeInstitutionName("\t Spaced College \n"));
  }
}
