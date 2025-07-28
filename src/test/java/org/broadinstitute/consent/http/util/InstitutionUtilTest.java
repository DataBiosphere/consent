package org.broadinstitute.consent.http.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

//  @Test
//  void testCanonicalizeDomain() {
//    assertEquals("example.com", InstitutionUtil.canonicalizeDomain("EXAMPLE.COM"));
//    assertEquals("test.edu", InstitutionUtil.canonicalizeDomain("Test.Edu"));
//    assertEquals("google.org", InstitutionUtil.canonicalizeDomain("  google.org  "));
//    assertEquals("university.net", InstitutionUtil.canonicalizeDomain("  UNIVERSITY.NET  "));
//  }

  @Test
  void testGetInvalidInstitutionDomainsAllValid() {
    initUtil();
    Institution institution = new Institution();
    institution.setDomains(Arrays.asList("example.com", "test.edu", "google.org"));

    List<String> invalidDomains = util.getInvalidInstitutionDomains(institution);
    assertTrue(invalidDomains.isEmpty());
  }

  @Test
  void testGetInvalidInstitutionDomainsMixedValidity() {
    initUtil();
    Institution institution = new Institution();
    institution.setDomains(Arrays.asList(
        "example.com",        // valid
        "sub.example.com",    // invalid (subdomain)
        "test.edu",           // valid
        "invalid",            // invalid (no TLD)
        "google.org",         // valid
        "",                   // invalid (empty)
        "www.test.edu"        // invalid (subdomain)
    ));

    List<String> invalidDomains = InstitutionUtil.getInvalidInstitutionDomains(institution);
    assertEquals(4, invalidDomains.size());
    assertTrue(invalidDomains.contains("sub.example.com"));
    assertTrue(invalidDomains.contains("invalid"));
    assertTrue(invalidDomains.contains(""));
    assertTrue(invalidDomains.contains("www.test.edu"));
  }

  @Test
  void testGetInvalidInstitutionDomainsEmptyList() {
    initUtil();
    Institution institution = new Institution();
    institution.setDomains(Collections.emptyList());

    List<String> invalidDomains = InstitutionUtil.getInvalidInstitutionDomains(institution);
    assertTrue(invalidDomains.isEmpty());
  }
}
