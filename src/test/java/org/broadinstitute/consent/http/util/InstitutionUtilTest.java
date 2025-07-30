package org.broadinstitute.consent.http.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.Gson;
import jakarta.ws.rs.BadRequestException;
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
  void testGetInvalidInstitutionDomainsAllValid() {
    Institution institution = new Institution();
    institution.setDomains(Arrays.asList("google.com", "broad.mit.edu", "broadinstitute.org"));

    List<String> invalidDomains = InstitutionUtil.getInvalidInstitutionDomains(institution);
    assertTrue(invalidDomains.isEmpty());
  }

  @Test
  void testGetInvalidInstitutionDomainsMixedValidity() {
    Institution institution = new Institution();
    institution.setDomains(Arrays.asList(
        "broadinstitute.org", // valid
        "uconn.edu",          // valid
        "mail.google.com",    // valid
        "www.uconn.edu",      // valid
        "café.com",      //valid
        "invalid",            // invalid
        "",                   // invalid
        null                  // invalid
    ));

    List<String> invalidDomains = InstitutionUtil.getInvalidInstitutionDomains(institution);
    assertEquals(3, invalidDomains.size());
    assertTrue(invalidDomains.contains("invalid"));
    assertTrue(invalidDomains.contains(""));
    assertTrue(invalidDomains.contains(null));
  }

  @Test
  void testGetInvalidInstitutionDomainsEmptyList() {
    Institution institution = new Institution();
    institution.setDomains(Collections.emptyList());

    List<String> invalidDomains = InstitutionUtil.getInvalidInstitutionDomains(institution);
    assertTrue(invalidDomains.isEmpty());
  }

  @Test
  void testValidateInstitutionDomainsAllValid() {
    Institution institution = new Institution();
    institution.setDomains(Arrays.asList("google.com", "broad.mit.edu", "broadinstitute.org"));

    assertDoesNotThrow(() -> InstitutionUtil.validateInstitutionDomains(institution));
  }

  @Test
  void testValidateInstitutionDomainsContainsInvalid() {
    Institution institution = new Institution();
    institution.setDomains(Arrays.asList(
        "broadinstitute.org", // valid
        "uconn.edu",          // valid
        "sub.example.com",    // valid
        "www.test.edu",       // valid
        "café.com",     //valid
        "invalid",            // invalid
        "",                   // invalid
        null                  // invalid
    ));

    Exception exception = null;
    try {
      InstitutionUtil.validateInstitutionDomains(institution);
    } catch (Exception e) {
      exception = e;
    }

    assertInstanceOf(BadRequestException.class, exception);
    assertEquals("Invalid domain(s) provided for institution: invalid, , null", exception.getMessage());
  }

  @Test
  void testCanonicalizeInstitutionNameValidInput() {
    assertEquals("Harvard University", InstitutionUtil.canonicalizeInstitutionName("Harvard University"));
    assertEquals("University of Connecticut", InstitutionUtil.canonicalizeInstitutionName("  University of Connecticut  "));
  }

  @Test
  void testCanonicalizeInstitutionNameQuotes() {
    // Test left/right double quotation marks
    assertEquals("A 'Real' University", InstitutionUtil.canonicalizeInstitutionName("A \"Real\" University"));

    // Test left/right single quotation marks
    assertEquals("St. John's University", InstitutionUtil.canonicalizeInstitutionName("St. John’s University"));
    assertEquals("Mount St. Mary's College", InstitutionUtil.canonicalizeInstitutionName("Mount St. Mary‘s College"));

    // Test low-9 quotation marks
    assertEquals("A 'Real' University", InstitutionUtil.canonicalizeInstitutionName("A ‚Real„ University"));
  }

  @Test
  void testCanonicalizeInstitutionNameWhitespace() {
    assertEquals("Harvard University", InstitutionUtil.canonicalizeInstitutionName("  Harvard University  "));
    assertEquals("Connecticut College", InstitutionUtil.canonicalizeInstitutionName("\t Connecticut College \n"));
  }
}
