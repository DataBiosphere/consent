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
    assertTrue(InstitutionUtil.isValidInstitutionDomain("nih.gov"));
    assertTrue(InstitutionUtil.isValidInstitutionDomain("broadinstitute.org"));
    assertTrue(InstitutionUtil.isValidInstitutionDomain("with-some-dashes.com"));
  }

  @Test
  void testIsValidInstitutionDomainInvalidDomains() {
    // Subdomains
    assertFalse(InstitutionUtil.isValidInstitutionDomain("foo.bar.baz"));
    assertFalse(InstitutionUtil.isValidInstitutionDomain("mail.google.com"));
    assertFalse(InstitutionUtil.isValidInstitutionDomain("www.broadinstitute.org"));

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
        "sub.example.com",    // invalid (subdomain)
        "www.test.edu",       // invalid (subdomain)
        "invalid",            // invalid (no TLD)
        "",                   // invalid (empty)
        null                  // invalid (null)
    ));

    List<String> invalidDomains = InstitutionUtil.getInvalidInstitutionDomains(institution);
    assertEquals(5, invalidDomains.size());
    assertTrue(invalidDomains.contains("sub.example.com"));
    assertTrue(invalidDomains.contains("invalid"));
    assertTrue(invalidDomains.contains(""));
    assertTrue(invalidDomains.contains("www.test.edu"));
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
  void testCanonicalizeNameValidInput() {
    assertEquals("Harvard University", InstitutionUtil.canonicalizeName("Harvard University"));
    assertEquals("University of Connecticut", InstitutionUtil.canonicalizeName("  University of Connecticut  "));
  }

  @Test
  void testCanonicalizeNameCurlyQuotes() {
    // Test left/right double quotation marks
    assertEquals("A 'Real' University", InstitutionUtil.canonicalizeName("A \"Real\" University"));

    // Test left/right single quotation marks
    assertEquals("St. John's University", InstitutionUtil.canonicalizeName("St. John’s University"));
    assertEquals("Mount St. Mary's College", InstitutionUtil.canonicalizeName("Mount St. Mary's College"));

    // Test low-9 quotation marks
    assertEquals("Test 'Quote' School", InstitutionUtil.canonicalizeName("Test ‚Quote„ School"));
  }

  @Test
  void testCanonicalizeNameDoubleToSingleQuotes() {
    assertEquals("The 'Elite' University", InstitutionUtil.canonicalizeName("The \"Elite\" University"));
    assertEquals("Harvard 'School' of Medicine", InstitutionUtil.canonicalizeName("Harvard \"School\" of Medicine"));
  }

  @Test
  void testCanonicalizeNameInvalidInput() {
    assertNull(InstitutionUtil.canonicalizeName(null));
    assertNull(InstitutionUtil.canonicalizeName(""));
    assertNull(InstitutionUtil.canonicalizeName("   "));
    assertNull(InstitutionUtil.canonicalizeName("\t\n"));
  }

  @Test
  void testCanonicalizeNameWhitespace() {
    assertEquals("Trimmed University", InstitutionUtil.canonicalizeName("  Trimmed University  "));
    assertEquals("Spaced College", InstitutionUtil.canonicalizeName("\t Spaced College \n"));
  }
}
